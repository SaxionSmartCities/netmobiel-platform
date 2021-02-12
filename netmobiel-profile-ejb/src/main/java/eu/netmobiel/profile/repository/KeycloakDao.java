package eu.netmobiel.profile.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
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
	 * @param lenient If true than be permissive and allow an exisiting user, enabled the account, if necessary.
	 * @return The managed identity of the new user.
	 * @throws BusinessException 
	 */
	public String addUser(NetMobielUser user, boolean lenient) throws BusinessException {
		String managedIdentity = null;
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			Optional<UserRepresentation> existingUser = Optional.empty(); 
			if (lenient) {
				existingUser = realm.users().search(user.getEmail()).stream()
					.filter(ur -> user.getEmail().equals(ur.getEmail()))
					.findFirst();
			}
			if (existingUser.isPresent()) {
				UserRepresentation urep = existingUser.get();
				if (!Boolean.TRUE.equals(urep.isEnabled())) {
					UserResource ur = realm.users().get(urep.getId());
					urep.setEnabled(true);
					ur.update(urep);
				}
				managedIdentity = urep.getId();
			} else {
				UserRepresentation urep = new UserRepresentation();
				urep.setEmail(user.getEmail());
				urep.setFirstName(user.getGivenName());
				urep.setLastName(user.getFamilyName());
				urep.setEnabled(true);
//				urep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
				Response response = realm.users().create(urep);
				if (log.isDebugEnabled()) {
					log.debug(String.format("AddUser: Response is %s %s", response.getStatus(), response.getStatusInfo()));
				}
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
	
	public void removeUser(String managedIdentity) throws BusinessException {
		try (Keycloak kc = createKeycloakClient()) {
			RealmResource realm = kc.realm(profileServiceAccount.getRealm());
			Response response = realm.users().delete(managedIdentity);
			if (log.isDebugEnabled()) {
				log.debug(String.format("RemoveUser: Response is %s %s", response.getStatus(), response.getStatusInfo()));
			}
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
			ur.executeActionsEmail(List.of("VERIFY_EMAIL"));
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
		} catch (Exception ex) {
			throw new BadRequestException("Error getting user from keycloak", ex);
		}
		return Optional.ofNullable(user);
	}
}
