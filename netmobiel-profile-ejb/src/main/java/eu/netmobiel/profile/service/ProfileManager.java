package eu.netmobiel.profile.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.LegalReasonsException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.event.DelegatorAccountCreatedEvent;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.RidesharePreferences;
import eu.netmobiel.profile.model.SearchPreferences;
import eu.netmobiel.profile.model.UserRole;
import eu.netmobiel.profile.repository.KeycloakDao;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.RidesharePreferencesDao;
import eu.netmobiel.profile.repository.SearchPreferencesDao;

/**
 * Bean class for the Profile service. The security is handled only if specific roles are required. All other security constraints are handled one level higher. 
 */
@Stateless
@Logging
@DeclareRoles({ "admin", "delegate" })
public class ProfileManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

	@Resource(lookup = "java:global/profileService/imageFolder")
	private String profileServiceImageFolder;
	
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    @Inject
    private RidesharePreferencesDao ridesharePreferencesDao;
    @Inject
    private SearchPreferencesDao searchPreferencesDao;
    
    @Inject
    private KeycloakDao keycloakDao;

    @Inject
    private Event<DelegatorAccountCreatedEvent> delegatorAccountCreatedEvent;

	public @NotNull PagedResult<Profile> listProfiles(ProfileFilter filter, Cursor cursor) throws BadRequestException {
    	// As an optimisation we could first call the data. If less then maxResults are received, we can deduce the totalCount and thus omit
    	// the additional call to determine the totalCount.
    	// For now don't do conditional things. First always total count, then data if data is requested. 
    	// Get the total count
		filter.validate();
    	cursor.validate(MAX_RESULTS, 0);
    	PagedResult<Long> prs = profileDao.listProfiles(filter, Cursor.COUNTING_CURSOR);
    	List<Profile> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = profileDao.listProfiles(filter, cursor);
    		results = profileDao.loadGraphs(pids.getData(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH, Profile::getId);
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

    /**
     * Creates a new Keycloak account or attaches an existing Keycloak account (lookup by email) to the given profile.
     * In case of an existing account the attributes are copied into the profile.
     * In case of a new account a verification mail will be sent by Keycloak.
     * In both cases the managed identity is copied into the profile.
     * @param newProfile the new profile to attche to the Keyclolak account.
     * @throws BusinessException
     */
    private void createOrAttachKeycloakAccount(Profile newProfile) throws BusinessException {
		if (newProfile.getEmail() == null) {
			throw new BadRequestException("Email address is mandatory");
		}
		String managedIdentity = null;
		Optional<NetMobielUser> existingUser = keycloakDao.findUserByEmail(newProfile.getEmail());
		if (existingUser.isPresent()) {
			// No security incident, the user still has to logon. 
			// It is however possible to create a profile using someone else's email.
			// To use it you have to know the credentials or have access to the email box of the account.
			managedIdentity = existingUser.get().getManagedIdentity();
			newProfile.setFamilyName(existingUser.get().getFamilyName());
			newProfile.setGivenName(existingUser.get().getGivenName());
		} else {
			// Need a new keycloak account
			managedIdentity = keycloakDao.addUser(newProfile);
			// Update password is configured in Keycloak.
			// Only for new accounts, not for delegated accounts
			// Although we have set 'verify_email'in the authentication flow, the email is not sent. Try manual override...
			keycloakDao.verifyUserByEmail(managedIdentity);
			// In the authentication flow is also specified to update the password. That works. We ask it anyway. 
		}
		newProfile.setManagedIdentity(managedIdentity);
    }

    /**
     * Creates a new profile, either for the caller or for someone else (by someone with a privilege, not verified here).
     * If a profile is created by an authenticated user, this user will be registered as the creator if it
     * is not his/her personal profile.
     * The profile is connected to a new or existing Keycloak account, with the email address as key. 
     * @param profile the profile to create.
     * @return A fresh new profile.
     * @throws BusinessException
     */
    public String createProfile(Profile profile) throws BusinessException {
    	  // Validate required parameters.
		if (StringUtils.isAllBlank(profile.getEmail()) || 
				StringUtils.isAllBlank(profile.getGivenName()) || 
				StringUtils.isAllBlank(profile.getFamilyName())) {
			throw new BadRequestException("Email, firstName and familyName are mandatory profile attributes");
		}
		if (!profile.getConsent().isAcceptedTerms() || !profile.getConsent().isOlderThanSixteen()) {
			throw new LegalReasonsException("Terms have not been accepted.");
		}
		// In the current implementation the caller can be anonymous (old registration) of authenticated (new registration)
		// If the keycloak id differs then the caller is registering a delegator profile.
		// We want to verify the constraints before the potential account creation in Keycloak
		Optional<NetMobielUser> caller = SecurityIdentity.getKeycloakContext(sessionContext.getCallerPrincipal());
		/**
		 * Scenarios:
		 * 1. Unauthenticated user: Create or attach Keycloak account (key: email address), create (own) profile for that email address, 
		 * 2. Authenticated user: 
		 * 2.1 Caller profile exists and privileged: Create or attach Keycloak account (key: email address), create delegator account. Privilege required. 
		 * 2.2 Caller profile does not exist: Create caller profile
		 */
		DelegatorAccountCreatedEvent delegatorAccountCreated = null;
		if (! caller.isPresent()) {
			// No Keycloak user
			createOrAttachKeycloakAccount(profile);
		} else {
			// I am a keycloak user
			Optional<Profile> myProfile = profileDao.getReferenceByManagedIdentity(caller.get().getManagedIdentity());
		    if (myProfile.isPresent()) {
		    	// Create a profile for someone else. Email is the key to lookup the profile
				boolean privileged = sessionContext.isCallerInRole("admin") || sessionContext.isCallerInRole("delegate");
				if (!privileged) {
					throw new SecurityException("You don't have the privilege to create a profile on behalf of someone else");
				}
			    profile.setCreatedBy(myProfile.get());
			    // Inform participant about the account, also check the communication means (phone number etc.)
			    // Will throw BadRequestException is something is wrong
			    // Let Hibernate initialize myProfile
			    myProfile.get().getManagedIdentity();
			    delegatorAccountCreated = new DelegatorAccountCreatedEvent(myProfile.get(), profile);
				createOrAttachKeycloakAccount(profile);
		    } else {
		    	// Create a profile for me, I have already a Keycloak account
				profile.setManagedIdentity(caller.get().getManagedIdentity());
			}
		}
    	if (profile.getActingRole() == null) {
    		profile.setActingRole(profile.getUserRole() == UserRole.BOTH ? UserRole.PASSENGER : profile.getUserRole());
    	}

		// Note: If the profile already exists (i.e. same email address), then a constraint violation will occur.
		profile.initializeChildren();
		profileDao.save(profile);
		if (profile.getSearchPreferences() != null) {
			searchPreferencesDao.save(profile.getSearchPreferences());				
		}
		if (profile.getRidesharePreferences() != null) {
			ridesharePreferencesDao.save(profile.getRidesharePreferences());				
		}
		// Most potential problems should have happened now, inform participants, if necessary.
		if (delegatorAccountCreated != null) {
	    	EventFireWrapper.fire(delegatorAccountCreatedEvent, delegatorAccountCreated);
		}
		return profile.getManagedIdentity();
    }

    public Profile getCompleteProfileByManagedIdentity(String managedId) throws NotFoundException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, Profile.DEFAULT_PROFILE_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	profile.setRidesharePreferences(ridesharePreferencesDao.loadGraph(profile.getId(), RidesharePreferences.FULL_RIDESHARE_PREFS_ENTITY_GRAPH).orElse(null));
    	profile.setSearchPreferences(searchPreferencesDao.loadGraph(profile.getId(), SearchPreferences.FULL_SEARCH_PREFS_ENTITY_GRAPH).orElse(null));
    	profileDao.detach(profile);
    	if (profile.getRidesharePreferences() != null) {
    		ridesharePreferencesDao.detach(profile.getRidesharePreferences());
    	}
    	if (profile.getSearchPreferences() != null) {
    		searchPreferencesDao.detach(profile.getSearchPreferences());
    	}
    	// Initialize some defaults. Detach to prevent save to database. 
		profile.linkOneToOneChildren();
    	return profile;
    }

    /**
     * Returns the profile without initialization of the search and rideshare preferences. 
     * 
     * @param managedId the mananaged id to look up.
     * @return the plain profile.
     * @throws NotFoundException if no such user exists in the profile database. 
     */
    public Profile getFlatProfileByManagedIdentity(String managedId) throws NotFoundException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, Profile.DEFAULT_PROFILE_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	return profile;
    }

    /**
     * Updates all fields of the profile.
     * @param managedId
     * @param newProfile
     * @throws NotFoundException
     * @throws BusinessException 
     */
	public void updateProfileByManagedIdentity(String managedId, Profile newProfile)  throws NotFoundException, BusinessException {
    	Profile dbprofile = getCompleteProfileByManagedIdentity(managedId);
    	SearchPreferences searchPrefsDb = dbprofile.getSearchPreferences();
    	RidesharePreferences ridePrefsDb = dbprofile.getRidesharePreferences();
    	// Profile is detached at this point
		NetMobielUser newuser = new NetMobielUserImpl(managedId, newProfile.getGivenName(), newProfile.getFamilyName(), newProfile.getEmail());
		Optional<NetMobielUser> olduser = keycloakDao.getUser(managedId);
		if (! olduser.isPresent()) {
			throw new NotFoundException("No such user: " + managedId);
		}
		if (! newuser.isSame(olduser.get())) {
			// Some attributes differ, update the user in Keycloak.
			if (logger.isDebugEnabled()) {
				logger.debug("Update user attributes in Keycloak: " + newuser);
			}
			keycloakDao.updateUser(newuser);
		}
    	// Assure key attributes are set
    	newProfile.setManagedIdentity(managedId);
    	newProfile.setId(dbprofile.getId());
    	if (newProfile.getActingRole() == null) {
    		newProfile.setActingRole(dbprofile.getActingRole());
    	}
		newProfile.linkOneToOneChildren();
		// Overwrite whatever image path is provided
		newProfile.setImagePath(dbprofile.getImagePath());
		dbprofile = profileDao.merge(newProfile);
		// Transient properties are null now: dbprofile.getSearchPreferences, dbprofile.getRidesharePreferences
		
		// Copy the database identifiers
//		if (newProfile.getSearchPreferences() != null && newProfile.getSearchPreferences().getNumberOfPassengers() == 0) {
//			newProfile.getSearchPreferences().setNumberOfPassengers(1);
//		}
		logger.debug(String.format("DbProfile HC: %d, DbProfile.SearchPref: %d, DbSearchPref: %d", 
				System.identityHashCode(dbprofile), System.identityHashCode(dbprofile.getSearchPreferences()), System.identityHashCode(searchPrefsDb)));
		logger.debug(String.format("newProfile HC: %d, newProfile.SearchPref: %d, DbSearchPref: %d", 
				System.identityHashCode(newProfile), System.identityHashCode(newProfile.getSearchPreferences()), System.identityHashCode(searchPrefsDb)));
		if (newProfile.getSearchPreferences() != null) {
			newProfile.getSearchPreferences().setProfile(dbprofile);
			if (searchPrefsDb != null) {
				newProfile.getSearchPreferences().setId(searchPrefsDb.getId());
				searchPreferencesDao.merge(newProfile.getSearchPreferences());				
			} else {
				searchPreferencesDao.save(newProfile.getSearchPreferences());				
			}
		} else {
			// Ignore, we dont't remove preferences once they are set.
		}
//		if (newProfile.getRidesharePreferences() != null && newProfile.getRidesharePreferences().getMaxPassengers() == 0) {
//			newProfile.getRidesharePreferences().setMaxPassengers(1);;
//		}
		if (newProfile.getRidesharePreferences() != null) {
			newProfile.getRidesharePreferences().setProfile(dbprofile);
			if (ridePrefsDb != null) {
				newProfile.getRidesharePreferences().setId(ridePrefsDb.getId());
				ridesharePreferencesDao.merge(newProfile.getRidesharePreferences());				
			} else {
				ridesharePreferencesDao.save(newProfile.getRidesharePreferences());				
			}
		} else {
			// Ignore, we dont't remove preferences once they are set.
		}
	}
	
	/**
	 * Removes the profile. The Keycloak account is retained.
	 * @param managedId
	 * @throws NotFoundException
	 */
	@RolesAllowed({ "admin" })
    public void removeProfile(String managedId) throws NotFoundException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, Profile.DEFAULT_PROFILE_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	profile.setRidesharePreferences(ridesharePreferencesDao.find(profile.getId()).orElse(null));
    	profile.setSearchPreferences(searchPreferencesDao.find(profile.getId()).orElse(null));
    	if (profile.getRidesharePreferences() != null) {
    		ridesharePreferencesDao.remove(profile.getRidesharePreferences());
    	}
    	if (profile.getSearchPreferences() != null) {
    		searchPreferencesDao.remove(profile.getSearchPreferences());
    	}

    	profileDao.remove(profile);
    }

	public void uploadImage(String managedId, String mimetype, String filename, byte[] image) throws NotFoundException, UpdateException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, null)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	String folder = profile.getManagedIdentity().substring(0, 2);
    	Path newFile = Path.of(folder, filename); 
    	Path newPath = Paths.get(profileServiceImageFolder).resolve(newFile);
    	Path oldFile = null;
    	if (profile.getImagePath() != null) {
    		String[] parts = profile.getImagePath().split("/");
    		String oldFolder = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
    		String oldFilename = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
    		oldFile = Path.of(oldFolder, oldFilename);
    	}
		try {
			Files.createDirectories(newPath.getParent());
			Files.write(newPath, image, StandardOpenOption.CREATE_NEW);
	    	if (oldFile != null) {
	    		Files.deleteIfExists(Paths.get(profileServiceImageFolder).resolve(oldFile));
	    	}
			profile.setImagePath(String.format("%s/%s", URLEncoder.encode(folder, StandardCharsets.UTF_8), URLEncoder.encode(filename, StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new UpdateException("Error writing or replacing image " + newPath , e);
		}

	}
	
	/**
	 * Search for drivers that are eligible to drive a potential passenger to his/her destination.
	 * @param pickup the pickup location of the passenger. 
	 * @param dropOff the drop-off location of the passenger.
	 * @param driverMaxRadiusMeter The radius of the circles that limits the eligibility of the the driver 
	 * 			by demanding his living location to be in both the two large circles around the pickup and drop-off location. 
	 * @param driverNeighbouringRadiusMeter The radius of the circles that limits the eligibility of the the driver 
	 * 			by demanding his living location to be in the neighbourhood of one of the pickup or drop-off locations.
	 * @return A list of profiles of potential drivers, possibly empty.
	 * @throws BusinessException In case of trouble.
	 */
    public List<Profile> searchShoutOutProfiles(GeoLocation pickup, GeoLocation dropOff, int driverMaxRadiusMeter, int driverNeighbouringRadiusMeter) {
    	return profileDao.searchShoutOutProfiles(pickup, dropOff, driverMaxRadiusMeter, driverNeighbouringRadiusMeter);
    }
    
}
