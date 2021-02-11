package eu.netmobiel.profile.api.resource;

import java.time.Instant;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.ProfilesApi;
import eu.netmobiel.profile.api.mapping.ProfileMapper;
import eu.netmobiel.profile.api.model.FirebaseTokenResponse;
import eu.netmobiel.profile.api.model.ImageResponse;
import eu.netmobiel.profile.api.model.ImageUploadRequest;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;

@ApplicationScoped
public class ProfilesResource implements ProfilesApi {

	@Inject
	private ProfileMapper mapper;

	@Inject
	private ProfileManager profileManager;

	@Override
	public Response createProfile(eu.netmobiel.profile.api.model.Profile profile) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response getProfile(String profileId) {
    	Response rsp = null;
		try {
        	Profile profile = profileManager.getProfileByManagedIdentity(profileId);
   			rsp = Response.ok(mapper.map(profile)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response getFcmToken(String profileId) {
    	Response rsp = null;
		try {
        	String token = profileManager.getFcmTokenByManagedIdentity(profileId);
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
        	String imagePath = profileManager.getImagePathByManagedIdentity(profileId);
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
			rsp = Response.ok(mapper.map(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	
	@Override
	public Response searchShoutOutDrivers(String withInAnyCircles, String withInAllCircles) {
//		Example: withInAnyCircles=[52.004166:6.517835:50,52.10:6.65:50]
//    	Response rsp = null;
		if (withInAnyCircles == null || withInAnyCircles.length() < 3) {
			throw new BadRequestException("withinAnyCircles is a mandatory parameter");
		}
		if (withInAllCircles == null || withInAllCircles.length() < 3) {
			throw new BadRequestException("withInAllCircles is a mandatory parameter");
		}
		String[] anyCircles = withInAnyCircles.substring(1, withInAnyCircles.length() - 1).split(",");
		if (anyCircles.length != 2) {
			throw new BadRequestException("withinAnyCircles should contain two circles");
		}
		throw new UnsupportedOperationException("searchShoutOutDrivers is not implemented");
	}

	@Override
	public Response updateProfile(String profileId, eu.netmobiel.profile.api.model.Profile apiProfile) {
    	Response rsp = null;
		try {
			Profile domainProfile = mapper.map(apiProfile);
			profileManager.updateProfileByManagedIdentity(profileId, domainProfile);
        	domainProfile = profileManager.getProfileByManagedIdentity(profileId);
   			rsp = Response.ok(mapper.map(domainProfile)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response uploadImage(String profileId, ImageUploadRequest imageUploadRequest) {
		Response rsp = null;
		try {
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
	    	profileManager.uploadImage(profileId, mimetype, filename, decodedImage);
        	Profile profile = profileManager.getProfileByManagedIdentity(profileId);
   			rsp = Response.ok(mapper.map(profile)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response deleteProfile(String profileId) {
		Response rsp = null;
		try {
	    	profileManager.removeProfile(profileId);
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}
}
