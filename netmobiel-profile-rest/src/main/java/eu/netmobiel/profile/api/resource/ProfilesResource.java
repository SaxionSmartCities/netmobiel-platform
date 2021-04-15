package eu.netmobiel.profile.api.resource;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.api.ProfilesApi;
import eu.netmobiel.profile.api.mapping.PlaceMapper;
import eu.netmobiel.profile.api.mapping.ProfileMapper;
import eu.netmobiel.profile.api.model.FirebaseTokenResponse;
import eu.netmobiel.profile.api.model.ImageResponse;
import eu.netmobiel.profile.api.model.ImageUploadRequest;
import eu.netmobiel.profile.api.model.ProfileResponse;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Place;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;

@RequestScoped
@Logging
public class ProfilesResource implements ProfilesApi {

	@Inject
	private ProfileMapper profileMapper;
	@Inject
	private PlaceMapper placeMapper;

	@Inject
	private ProfileManager profileManager;

	@Inject
	private SecurityIdentity securityIdentity;
	
//	private final BooleanSupplier isAdmin = () -> request.isUserInRole("admin");
//	private final Predicate<String> itIsMe = id -> request.getUserPrincipal().getName().equals(id);

    private String resolveIdentity(String profileId) {
		String mid = null;
		if ("me".equals(profileId)) {
			mid = securityIdentity.getEffectivePrincipal().getName();
		} else {
			mid = profileId;
		}
		return mid;
    }

	@Override
	public Response createProfile(eu.netmobiel.profile.api.model.Profile profile) {
    	Response rsp = null;
		try {
			Profile domprof = profileMapper.map(profile);
	    	Long id = profileManager.createProfile(domprof);
			rsp = Response.created(UriBuilder.fromResource(ProfilesApi.class)
					.path(ProfilesApi.class.getMethod("getProfile", String.class)).build(id)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getProfile(String profileId) {
    	Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
			// The profile is always completely initialized, but may only be filled in part,
			// depending on the privileges of the caller.
			Profile profile = profileManager.getProfileByManagedIdentity(mid);
        	ProfileResponse prsp = new ProfileResponse();
        	prsp.setProfiles(Collections.singletonList(profileMapper.mapComplete(profile)));
        	prsp.setMessage("Profile succesfully retrieved");
        	prsp.setSuccess(true);
   			rsp = Response.ok(prsp).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response getFcmToken(String profileId) {
    	Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
        	String token = profileManager.getFcmTokenByManagedIdentity(mid);
        	FirebaseTokenResponse ftr = new FirebaseTokenResponse();
        	ftr.setFcmToken(token);
   			rsp = Response.ok(ftr).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response getProfileImage(String profileId) {
    	Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
        	String imagePath = profileManager.getImagePathByManagedIdentity(mid);
        	ImageResponse ir = new ImageResponse();
        	ir.setImage(imagePath);
   			rsp = Response.ok(ir).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response listProfiles(String role, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor(maxResults, offset);
			ProfileFilter filter = new ProfileFilter();
			filter.setUserRole(role);
	    	PagedResult<Profile> results = profileManager.listProfiles(filter, cursor);
			rsp = Response.ok(profileMapper.mapShallow(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	
	@Override
	public Response updateProfile(String profileId, eu.netmobiel.profile.api.model.Profile apiProfile) {
    	Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
			Profile domainProfile = profileMapper.map(apiProfile);
			profileManager.updateProfileByManagedIdentity(mid, domainProfile);
        	domainProfile = profileManager.getProfileByManagedIdentity(profileId);
        	ProfileResponse prsp = new ProfileResponse();
        	prsp.setProfiles(Collections.singletonList(profileMapper.mapComplete(domainProfile)));
        	prsp.setMessage("Profile succesfully retrieved");
        	prsp.setSuccess(true);
   			rsp = Response.ok(prsp).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response uploadImage(String profileId, ImageUploadRequest imageUploadRequest) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
			// See https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL
			// This is how the client passes the image
			// Format: data:image/*;base64,
			final String prefix = "data:";
			String data = imageUploadRequest.getImage();
			if (data == null || ! data.startsWith(prefix)) {
				throw new BadRequestException("Uploaded image data must be a data url");
			}
			String[] parts = data.substring(prefix.length()).split(",");
			String spec[] = parts[0].split(";");
			String mimetype = spec[0];
			if (!mimetype.startsWith("image/")) {
				throw new BadRequestException("Uploaded file does not have an image mimetype: " + mimetype);
			}
			String filetype = mimetype.substring(mimetype.indexOf("/") + 1);
			if (!"png".equals(filetype) && !"jpg".equals(filetype)) {
				throw new BadRequestException("Uploaded image must be png or jpg; not supported " + mimetype);
			}
			String encoding = spec.length > 1 ? spec[1] : null;
			if (!encoding.equals("base64")) {
				throw new BadRequestException("Uploaded image encoding not supported: " + encoding);
			}
			byte[] decodedImage = Base64.getDecoder().decode(parts[1]);
			String filename = Instant.now().toEpochMilli() + "." + filetype;
	    	profileManager.uploadImage(mid, mimetype, filename, decodedImage);
        	Profile profile = profileManager.getProfileByManagedIdentity(mid);
        	ProfileResponse prsp = new ProfileResponse();
        	prsp.setProfiles(Collections.singletonList(profileMapper.mapComplete(profile)));
        	prsp.setMessage("Profile succesfully retrieved");
        	prsp.setSuccess(true);
   			rsp = Response.ok(prsp).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response deleteProfile(String profileId) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
	    	profileManager.removeProfile(mid);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	// =========================   PLACE  =========================
	
	@Override
	public Response createPlace(String profileId, eu.netmobiel.profile.api.model.Place place) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
			Place p = placeMapper.mapApiPlace(place);
	    	Long id = profileManager.createPlace(mid, p);
			rsp = Response.created(UriBuilder.fromResource(ProfilesApi.class)
					.path(ProfilesApi.class.getMethod("getPlace", String.class, String.class)).build(profileId, id)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPlace(String profileId, String placeId) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
	    	Place place = profileManager.getPlace(mid, UrnHelper.getId(placeId));
			rsp = Response.ok(placeMapper.mapPlace(place)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPlaces(String profileId, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Place> results = profileManager.listPlaces(mid, cursor);
			rsp = Response.ok(placeMapper.mapPlacesPage(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updatePlace(String profileId, String placeId, eu.netmobiel.profile.api.model.Place apiPlace) {
    	Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
			Place place= placeMapper.mapApiPlace(apiPlace);
			profileManager.updatePlace(mid, UrnHelper.getId(placeId), place);
   			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response deletePlace(String profileId, String placeId) {
		Response rsp = null;
		try {
			String mid = resolveIdentity(profileId);
	    	profileManager.removePlace(mid, UrnHelper.getId(placeId));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
