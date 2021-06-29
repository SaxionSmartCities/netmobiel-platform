package eu.netmobiel.profile.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.ExceptionUtil;

@ApplicationScoped
public class KeycloakDao {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    /**
     * The path to the service account file for the Profile Service.
     */
    @Resource(lookup = "java:global/profileService/serviceAccountPath")
    private String profileServiceAccountPath;

    @Resource(lookup = "java:global/application/stage", description = "The development stage of the application. Use one of DEV, ACC, PROD")
    private String applicationStage;

    private AdapterConfig profileServiceAccount;

    /**
     * Initializes the profile service account credentials. 
     */
    @PostConstruct
    void initialize() {
		try (final InputStream configStream = Files.newInputStream(Paths.get(profileServiceAccountPath))) {
			profileServiceAccount = JsonSerialization.readValue(configStream, AdapterConfig.class);
    	} catch (IOException ex) {
    		throw new SystemException("Unable to read profile service account configuration", ex);
		}
    }

	private Keycloak createKeycloakClient() {
		return KeycloakBuilder.builder()
				.serverUrl(profileServiceAccount.getAuthServerUrl())
				.realm(profileServiceAccount.getRealm())
				.clientId(profileServiceAccount.getResource())
				.clientSecret(profileServiceAccount.getCredentials().get("secret").toString())
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.build();
	}

	/**
	 * Creates a user in Keycloak.
	 * @param user The user to create. The email address is required and must be unique.
	 * @return The managed identity of the new user.
	 * @throws BusinessException 
	 */
	public String addUser(NetMobielUser user) throws BusinessException {
		String managedIdentity = null;
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserRepresentation urep = new UserRepresentation();
			urep.setEmail(user.getEmail());
			urep.setFirstName(user.getGivenName());
			urep.setLastName(user.getFamilyName());
			urep.setEnabled(true);
			try (Response response = realm.users().create(urep)) {
//				if (log.isDebugEnabled()) {
//					log.debug(String.format("AddUser: Response is %s %s", response.getStatus(), response.getStatusInfo()));
//				}
				if (response.getStatusInfo() == Response.Status.CREATED) {
					managedIdentity = CreatedResponseUtil.getCreatedId(response);
				} else if (response.getStatusInfo() == Response.Status.CONFLICT) {
					ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
					throw new DuplicateEntryException(error.getErrorMessage());
				} else {
					ExceptionUtil.throwExceptionFromResponse("Error adding user to Keycloak", response);
				}
			}
		}
		return managedIdentity;
	}

	public Optional<NetMobielUser> findUserByEmail(String email) throws BusinessException {
		NetMobielUserImpl user = null;
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			Optional<UserRepresentation> existingUser = Optional.empty(); 
			existingUser = realm.users().search(email).stream()
				.filter(ur -> email.equals(ur.getEmail()))
				.findFirst();
			if (existingUser.isPresent()) {
				UserRepresentation urep = existingUser.get();
				user = new NetMobielUserImpl(urep.getId(), urep.getFirstName(), urep.getLastName(), urep.getEmail());
			}
		}
		return Optional.ofNullable(user); 
	}

	public void removeUser(String managedIdentity) throws BusinessException {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			Response response = realm.users().delete(managedIdentity);
//			if (log.isDebugEnabled()) {
//				log.debug(String.format("RemoveUser: Response is %s %s", response.getStatus(), response.getStatusInfo()));
//			}
			if (response.getStatusInfo() != Response.Status.NO_CONTENT) {
				ExceptionUtil.throwExceptionFromResponse("Error removing user from Keycloak", response);
			}
		}
	}

	public void disableUser(String managedIdentity) throws BusinessException {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(managedIdentity);
			if (ur != null) {
				UserRepresentation urep = ur.toRepresentation();
				urep.setEnabled(false);
				ur.update(urep);
			} else {
				throw new NotFoundException("No such user: " + managedIdentity);
			}
		}
	}

	public void verifyUserByEmail(String managedIdentity) {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(managedIdentity);
			// We don't want to include the keycloak-server-spi jar. Use the String instead.
			ur.executeActionsEmail(List.of("VERIFY_EMAIL"));
		}
	}

	public void forceUpdatePassword(String managedIdentity) {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(managedIdentity);
			ur.executeActionsEmail(List.of("UPDATE_PASSWORD"));
		}
	}

	public Optional<NetMobielUser> getUser(String managedIdentity) throws BadRequestException {
		NetMobielUserImpl user = null;
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(managedIdentity);
			if (ur != null) {
				UserRepresentation urep = ur.toRepresentation();
				user = new NetMobielUserImpl(urep.getId(), urep.getFirstName(), urep.getLastName(), urep.getEmail());
			}
		} catch (javax.ws.rs.NotFoundException ex) {
			// Ignore
		}
		return Optional.ofNullable(user);
	}

	public PagedResult<NetMobielUser> listUsers(Cursor cursor) throws BadRequestException {
		cursor.validate(100, 0);
		List<NetMobielUser> users = null;
		Long totalcount = null;
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			if (cursor.isCountingQuery()) {
				totalcount = realm.users().count().longValue();
			} else {
				users = realm.users().list(cursor.getOffset(), cursor.getMaxResults())
						.stream()
						.map(ur -> new NetMobielUserImpl(ur.getId(), ur.getFirstName(), ur.getLastName(), ur.getEmail()))
						.collect(Collectors.toList());
			}
		}
		return new PagedResult<NetMobielUser>(users, cursor, totalcount);
	}

	/**
	 * Updates a user in Keycloak.
	 * @param user The user to update. If the email is modified, then it might need confirmation. This process is managed by Keycloak.  
	 * @throws BusinessException 
	 */
	public void updateUser(NetMobielUser user) {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(user.getManagedIdentity());
			if (ur != null) {
				UserRepresentation urep = ur.toRepresentation();
				urep.setEnabled(true);
				urep.setFirstName(user.getGivenName());
				urep.setLastName(user.getFamilyName());
				urep.setEmail(user.getEmail());
				ur.update(urep);
			}
		}
	}
	
	public void addDelegator(NetMobielUser delegate, NetMobielUser delegator) {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(delegate.getManagedIdentity());
			if (ur != null) {
				UserRepresentation urep = ur.toRepresentation();
				Map<String, List<String>> attribs = urep.getAttributes();
				if (attribs == null) {
					attribs = new HashMap<>();
					urep.setAttributes(attribs);
				}
    			String key = SecurityIdentity.getDelegatorsClaimName(applicationStage); 
				attribs.computeIfAbsent(key, k -> new ArrayList<>());
				List<String> delegators = attribs.get(key);
				if (! delegators.contains(delegator.getManagedIdentity())) {
					delegators.add(delegator.getManagedIdentity());
					ur.update(urep);
				}
			}
		}
	}

	public void removeDelegator(NetMobielUser delegate, NetMobielUser delegator) {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			UserResource ur = realm.users().get(delegate.getManagedIdentity());
			if (ur != null) {
				UserRepresentation urep = ur.toRepresentation();
				Map<String, List<String>> attribs = urep.getAttributes();
				if (attribs != null) {
	    			String key = SecurityIdentity.getDelegatorsClaimName(applicationStage); 
					attribs.computeIfAbsent(key, k -> new ArrayList<>());
					List<String> delegators = attribs.get(key);
					if (delegators != null && delegators.contains(delegator.getManagedIdentity())) {
						delegators.remove(delegator.getManagedIdentity());
						ur.update(urep);
					}
				}
			}
		}
	}
}
