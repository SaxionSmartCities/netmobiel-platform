package eu.netmobiel.profile.api.resource;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ImageHelper;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.api.ProfilesApi;
import eu.netmobiel.profile.api.mapping.PlaceMapper;
import eu.netmobiel.profile.api.mapping.ProfileMapper;
import eu.netmobiel.profile.api.model.FirebaseToken;
import eu.netmobiel.profile.api.model.ImageUploadRequest;
import eu.netmobiel.profile.api.model.Page;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Place;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.PlaceManager;
import eu.netmobiel.profile.service.ProfileManager;

@RequestScoped
public class ProfilesResource extends BasicResource implements ProfilesApi {

	@Inject
	private ProfileMapper profileMapper;
	@Inject
	private PlaceMapper placeMapper;

	@Inject
	private ProfileManager profileManager;

	@Inject
	private PlaceManager placeManager;

	@Context
	private HttpServletRequest request;

	/**
	 * Creates a new profile. If the user is not authenticated, it must be a fresh new user.
	 * If the user is authenticated, then it must be the user's own profile or it is a delegate that wants to 
	 * create a new user as part of a delegation.
	 */
	@Override
	public Response createProfile(eu.netmobiel.profile.api.model.Profile profile) {
    	Response rsp = null;
		try {
			Profile domprof = profileMapper.map(profile);
			// Role is verified in EJB method
			Long id = profileManager.createProfile(domprof);
			String urn = UrnHelper.createUrn(Profile.URN_PREFIX, id);
			rsp = Response.created(URI.create(urn)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
    public Response getProfileStatus(String xDelegator, String profileId) {
		String mid = resolveIdentity(xDelegator, profileId);
		if (!request.isUserInRole("admin") && !mid.equals(securityIdentity.getEffectivePrincipal().getName())
				&& !mid.equals(securityIdentity.getRealUser().getManagedIdentity())) {
    		throw new SecurityException("You have no access rights");
		}
		boolean userExists = profileManager.userExists(mid);
		return Response.status(userExists ? Status.NO_CONTENT : Status.NOT_FOUND).build();
    }

    @Override
	public Response getProfile(String xDelegator, String profileId, Boolean _public) {
    	ResponseBuilder rspb = null;
		try {
			// Only admin and effective owner can view the full profile, others see the public profile.
			String mid = resolveIdentity(xDelegator, profileId);
			// The profile is always completely initialized, but may only be filled in part,
			// depending on the privileges of the caller.
			boolean complete = request.isUserInRole("admin") || mid.equals(securityIdentity.getEffectivePrincipal().getName());
			if (Boolean.TRUE.equals(_public)) {
				complete = false;
			}
			eu.netmobiel.profile.api.model.Profile apiProfile = null;
			if (complete) {
				Profile profile = profileManager.getCompleteProfileByManagedIdentity(mid);
				apiProfile = profileMapper.mapComplete(profile);
			} else {
				Profile profile = profileManager.getFlatProfileByManagedIdentity(mid);
				apiProfile = profileMapper.mapPublicProfile(profile);
			}
   			rspb = Response.ok(apiProfile);
		} catch (NotFoundException ex) {
			throw new WebApplicationException(ex);
		}
		return rspb.build();
	}

	@Override
	public Response listProfiles(String text, String role, Boolean details, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			final boolean privileged = request.isUserInRole("admin") || request.isUserInRole("delegate"); 
			if (! privileged) {
				throw new SecurityException("You have no privilege to list the profiles owned by someone else");
			}
			Cursor cursor = new Cursor(maxResults, offset);
			ProfileFilter filter = new ProfileFilter();
			filter.setText(text);
			filter.setUserRole(role);
	    	PagedResult<Profile> results = profileManager.listProfiles(filter, cursor);
	    	Page page;
	    	if (request.isUserInRole("admin") && Boolean.TRUE.equals(details)) {
	    		page = profileMapper.mapShallow(results);
	    	} else {
	    		page = profileMapper.mapSecondary(results);
	    	}
    		rsp = Response.ok(page).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updateProfile(String xDelegator, String profileId, eu.netmobiel.profile.api.model.Profile apiProfile) {
    	Response rsp = null;
		try {
			// Only admin and effective owner can update the profile
			String mid = resolveIdentity(xDelegator, profileId);
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && !me.equals(mid)) {
				throw new SecurityException("You have no privilege to update the profile owned by someone else");
			}
			Profile domainProfile = profileMapper.map(apiProfile);
			profileManager.updateProfileByManagedIdentity(mid, domainProfile);
   			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response uploadImage(String xDelegator, String profileId, ImageUploadRequest imageUploadRequest) {
		Response rsp = null;
		try {
			ImageHelper.DecodedImage di = ImageHelper.decodeImage(imageUploadRequest.getImage(), new String[] { "jpg", "png" });

			String mid = resolveIdentity(xDelegator, profileId);
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && ! me.equals(mid)) {
				throw new SecurityException("You have no privilege to update the image of a profile owned by someone else");
			}
	    	profileManager.uploadImage(mid, di.filetype, di.decodedImage);
   			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	/**
	 * Delete a profile. For now only an admin can do that.
	 * @param xDelegator the delegator, if any.
	 * @param profileId the managed identity of the profile to delete.  
	 */
	@Override
	public Response deleteProfile(String xDelegator, String profileId) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(xDelegator, profileId);
//			Principal me = sessionContext.getCallerPrincipal();
//			boolean privileged = sessionContext.isCallerInRole("admin");
//	    	if (!privileged && !me.getName().equals(managedId)) {
//				new SecurityException("You have no privilege to remove the profile of someone else");
//	    	}

	    	profileManager.removeProfile(mid);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	// =========================   Firebase Messaging Token  =========================
	
	@Override
	public Response getFcmToken(String xDelegator, String profileId) {
		Response rsp = null;
		try {
			// Only admin and effective owner can update the profile
			String mid = resolveIdentity(xDelegator, profileId);
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && !me.equals(mid)) {
				throw new SecurityException("You have no privilege to update the profile owned by someone else");
			}
			Profile profile = profileManager.getFlatProfileByManagedIdentity(mid);
			FirebaseToken token = new FirebaseToken();
			token.setToken(profile.getFcmToken());
			if (profile.getFcmTokenTimestamp() != null) {
				token.setLastUpdate(profile.getFcmTokenTimestamp().atOffset(ZoneOffset.UTC));
			}
			rsp = Response.ok(token).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response updateFcmToken(String xDelegator, String profileId, FirebaseToken firebaseToken) {
		Response rsp = null;
		try {
			// Only admin and effective owner can update the profile
			String mid = resolveIdentity(xDelegator, profileId);
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && !me.equals(mid)) {
				throw new SecurityException("You have no privilege to update the profile owned by someone else");
			}
			Profile profile = profileManager.getFlatProfileByManagedIdentity(mid);
			// Always update to force update of timestamp of token
			profile.setFcmToken(firebaseToken.getToken());
			profile.setFcmTokenTimestamp(Instant.now());
			profileManager.updateProfileByManagedIdentity(mid, profile);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	// =========================   PLACE  =========================
	
	// Security is handled by PlaceManager!
	@Override
	public Response createPlace(String xDelegator, String profileId, eu.netmobiel.profile.api.model.Place place) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(xDelegator, profileId);
			Place p = placeMapper.mapApiPlace(place);
	    	Long id = placeManager.createPlace(mid, p);
	    	String urn = UrnHelper.createUrn(Place.URN_PREFIX, id);
			rsp = Response.created(URI.create(urn)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPlace(String xDelegator, String profileId, String placeId) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(xDelegator, profileId);
	    	Place place = placeManager.getPlace(mid, UrnHelper.getId(placeId));
			rsp = Response.ok(placeMapper.mapPlace(place)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPlaces(String xDelegator, String profileId, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(xDelegator, profileId);
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Place> results = placeManager.listPlaces(mid, cursor);
			rsp = Response.ok(placeMapper.mapPlacesPage(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updatePlace(String xDelegator, String profileId, String placeId, eu.netmobiel.profile.api.model.Place apiPlace) {
    	Response rsp = null;
		try {
			String mid = resolveIdentity(xDelegator, profileId);
			Place place= placeMapper.mapApiPlace(apiPlace);
			placeManager.updatePlace(mid, UrnHelper.getId(placeId), place);
   			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response deletePlace(String xDelegator, String profileId, String placeId) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(xDelegator, profileId);
	    	placeManager.removePlace(mid, UrnHelper.getId(placeId));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	
}
