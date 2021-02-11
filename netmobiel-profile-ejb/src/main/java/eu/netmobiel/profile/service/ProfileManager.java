package eu.netmobiel.profile.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Principal;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.filter.ComplimentFilter;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.filter.ReviewFilter;
import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.repository.ComplimentDao;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.ReviewDao;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
@DeclareRoles({ "admin" })
public class ProfileManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

	@Resource(lookup = "java:global/profileService/imageFolder")
	private String profileServiceImageFolder;
	
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private ReviewDao reviewDao;
    
    @Inject
    private ComplimentDao complimentDao;
    
    public ProfileManager() {
    }

    @RolesAllowed({ "admin" })
	public @NotNull PagedResult<Profile> listProfiles(ProfileFilter filter, Cursor cursor) throws BadRequestException {
    	// As an optimisation we could first call the data. If less then maxResults are received, we can deduce the totalCount and thus omit
    	// the additional call to determine the totalCount.
    	// For now don't do conditional things. First always total count, then data if data is requested. 
    	// Get the total count
    	cursor.validate(MAX_RESULTS, 0);
    	PagedResult<Long> prs = profileDao.listProfiles(filter, Cursor.COUNTING_CURSOR);
    	List<Profile> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = profileDao.listProfiles(filter, cursor);
    		results = profileDao.loadGraphs(pids.getData(), Profile.HOME_PROFILE_ENTITY_GRAPH, Profile::getId);
    	}
    	return new PagedResult<Profile>(results, cursor, prs.getTotalCount());
	}

    public Profile getProfileByManagedIdentity(String managedId) throws NotFoundException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, Profile.FULL_PROFILE_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	// Initialize the fields that did not come with the query.
    	// What is faster, using the fullest graph, or use the one above? We have to test.
    	if (profile.getSearchPreferences() != null) {
    		profile.getSearchPreferences().getAllowedTraverseModes().size();
    		profile.getSearchPreferences().getLuggageOptions().size();
    	}
    	if (profile.getRidesharePreferences() != null) {
    		profile.getRidesharePreferences().getLuggageOptions().size();
    	}
    	profile.getAddresses().size();
    	return profile;
    }

    public String getFcmTokenByManagedIdentity(String managedId) throws NotFoundException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, null)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	return profile.getFcmToken();
    }

    public String getImagePathByManagedIdentity(String managedId) throws NotFoundException {
    	Profile profile = profileDao.findByManagedIdentity(managedId, null)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	return profile.getImagePath();
    }
    
	public void updateProfileByManagedIdentity(String managedId, Profile newProfile)  throws NotFoundException {
    	Profile dbprofile = profileDao.findByManagedIdentity(managedId, null)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	profileDao.detach(dbprofile);
    	newProfile.setId(dbprofile.getId());
    	newProfile.getSearchPreferences().setId(dbprofile.getId());
    	newProfile.getRidesharePreferences().setId(dbprofile.getId());
    	newProfile.getAddresses().forEach(addr -> addr.setProfile(newProfile));
    	profileDao.merge(newProfile);
	}
	

    public void removeProfile(String managedId) throws NotFoundException {
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
    	Profile profile = profileDao.findByManagedIdentity(managedId, null)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	if (!privileged && !me.getName().equals(managedId)) {
			new SecurityException("You have no privilege to remove the profile of someone else");
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
    		String oldFolder = URLDecoder.decode(parts[parts.length - 2], StandardCharsets.UTF_8);
    		String oldFilename = URLDecoder.decode(parts[parts.length - 1], StandardCharsets.UTF_8);
    		oldFile = Path.of(oldFolder, oldFilename);
    	}
		try {
			Files.createDirectories(newPath.getParent());
			Files.write(newPath, image, StandardOpenOption.CREATE_NEW);
	    	if (oldFile != null) {
	    		Files.deleteIfExists(Paths.get(profileServiceImageFolder).resolve(oldFile));
	    	}
			profile.setImagePath(String.format("/images/%s/%s", URLEncoder.encode(folder, StandardCharsets.UTF_8), URLEncoder.encode(filename, StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new UpdateException("Error writing or replacing image " + newPath , e);
		}

	}
	
	/* ===================  REVIEW  ==================== */

	public Long createReview(Review review) throws BadRequestException, NotFoundException {
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (review.getReceiver() == null) {
			throw new BadRequestException("Review receiver is a mandatory parameter");
		} else {
	    	Profile yourProfile = profileDao.getReferenceByManagedIdentity(review.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + review.getReceiver().getManagedIdentity()));
	    	review.setReceiver(yourProfile);
		}
		if (me.getName().equals(review.getReceiver().getManagedIdentity())) {
			throw new BadRequestException("You cannot review yourself");
		}
		if (review.getSender() == null) {
	    	Profile myProfile = profileDao.getReferenceByManagedIdentity(me.getName())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + me.getName()));
	    	review.setSender(myProfile);
		} else if (!me.getName().equals(review.getSender().getManagedIdentity()) && !privileged) {
			new SecurityException("You have no privilege to review on behalf of someone else");
		}
		if (review.getReview() == null) {
			throw new BadRequestException("Review text is a mandatory parameter");
		}
		reviewDao.save(review);
		return review.getId();
	}

	public @NotNull PagedResult<Review> listReviews(ReviewFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (! privileged && filter.getReceiver() != null && !filter.getReceiver().equals(me.getName())) {
			new SecurityException("You have no privilege to list reviews received by someone else");
		}
		if (! privileged && filter.getSender() != null && !filter.getSender().equals(me.getName())) {
			new SecurityException("You have no privilege to list reviews sent by someone else");
		}
		if (! privileged && filter.getReceiver() == null) {
			filter.setReceiver(me.getName());
		}
		filter.validate();
    	PagedResult<Long> prs = reviewDao.listReviews(filter, Cursor.COUNTING_CURSOR);
    	List<Review> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = reviewDao.listReviews(filter, cursor);
    		results = reviewDao.loadGraphs(pids.getData(), Review.LIST_REVIEWS_ENTITY_GRAPH, Review::getId);
    	}
    	return new PagedResult<Review>(results, cursor, prs.getTotalCount());
	}

	public Review getReview(Long reviewId) throws NotFoundException {
		return reviewDao.find(reviewId)
				.orElseThrow(() -> new NotFoundException("No such review: " + reviewId));
	}

	@RolesAllowed({ "admin" })
	public void removeReview(Long reviewId) throws NotFoundException {
		Review c = reviewDao.find(reviewId)
				.orElseThrow(() -> new NotFoundException("No such review: " + reviewId));
		reviewDao.remove(c);
	}

/* ===================  COMPLIMENT  ==================== */

	public Long createCompliment(Compliment compliment) throws BadRequestException, NotFoundException {
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (compliment.getReceiver() == null) {
			throw new BadRequestException("Compliment receiver is a mandatory parameter");
		} else {
	    	Profile yourProfile = profileDao.getReferenceByManagedIdentity(compliment.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + compliment.getReceiver().getManagedIdentity()));
	    	compliment.setReceiver(yourProfile);
		}
		if (me.getName().equals(compliment.getReceiver().getManagedIdentity())) {
			throw new BadRequestException("You cannot compliment yourself");
		}
		if (compliment.getSender() == null) {
	    	Profile myProfile = profileDao.getReferenceByManagedIdentity(me.getName())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + me.getName()));
	    	compliment.setSender(myProfile);
		} else if (!me.getName().equals(compliment.getSender().getManagedIdentity()) && !privileged) {
			new SecurityException("You have no privilege to assign a compliment on behalf of someone else");
		}
		if (compliment.getCompliment() == null) {
			throw new BadRequestException("Compliment is a mandatory parameter");
		}
		complimentDao.save(compliment);
		return compliment.getId();
	}

	public @NotNull PagedResult<Compliment> listCompliments(ComplimentFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (! privileged && filter.getReceiver() != null && !filter.getReceiver().equals(me.getName())) {
			new SecurityException("You have no privilege to list compliments received by someone else");
		}
		if (! privileged && filter.getSender() != null && !filter.getSender().equals(me.getName())) {
			new SecurityException("You have no privilege to list compliments sent by someone else");
		}
		if (! privileged && filter.getReceiver() == null) {
			filter.setReceiver(me.getName());
		}
		filter.validate();
    	PagedResult<Long> prs = complimentDao.listCompliments(filter, Cursor.COUNTING_CURSOR);
    	List<Compliment> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = complimentDao.listCompliments(filter, cursor);
    		results = complimentDao.loadGraphs(pids.getData(), Compliment.LIST_COMPLIMENTS_ENTITY_GRAPH, Compliment::getId);
    	}
    	return new PagedResult<Compliment>(results, cursor, prs.getTotalCount());
	}

	public Compliment getCompliment(Long complimentId) throws NotFoundException {
		return complimentDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
	}

    @RolesAllowed({ "admin" })
	public void removeCompliment(Long complimentId) throws NotFoundException {
		Compliment c = complimentDao.find(complimentId)
				.orElseThrow(() -> new NotFoundException("No such compliment: " + complimentId));
		complimentDao.remove(c);
	}

}
