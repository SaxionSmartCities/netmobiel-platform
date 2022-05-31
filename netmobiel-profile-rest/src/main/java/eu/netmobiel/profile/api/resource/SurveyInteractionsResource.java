package eu.netmobiel.profile.api.resource;

import java.net.URI;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.api.SurveyInteractionsApi;
import eu.netmobiel.profile.api.mapping.SurveyMapper;
import eu.netmobiel.profile.model.SurveyInteraction;
import eu.netmobiel.profile.service.SurveyManager;

@RequestScoped
public class SurveyInteractionsResource extends BasicResource implements SurveyInteractionsApi {

	@Inject
	private SurveyManager surveyManager;

	@Inject
	private SurveyMapper mapper;

	@Context
	private HttpServletRequest request;

	@Override
	public Response getSurveyInteractions(String xDelegator, String profileId, String surveyId, Boolean completedToo, String incentiveCode, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			final boolean privileged = request.isUserInRole("admin"); 
			if (!privileged && profileId == null) {
				profileId = "me";
			}
			String mid  = resolveIdentity(xDelegator, profileId);
			allowAdminOrEffectiveUser(request, mid);
			Cursor cursor = new Cursor(maxResults, offset);
			PagedResult<SurveyInteraction> sis = surveyManager.listSurveyInteractions(mid, surveyId, Boolean.TRUE.equals(completedToo), incentiveCode, cursor);
			rsp = Response.ok(mapper.mapWithUser(sis)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response createSurveyInteraction(String xDelegator, String profileId) {
		ResponseBuilder rspb = null;
		try {
			if (profileId == null) {
				profileId = "me";
			}
			String mid  = resolveIdentity(xDelegator, profileId);
			allowAdminOrEffectiveUser(request, mid);
			Optional<SurveyInteraction> sopt = surveyManager.inviteToSurvey(mid);
			if (sopt.isPresent()) {
				rspb = Response.created(URI.create(sopt.get().getUrn()));
			} else {
				rspb = Response.noContent();
			}
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rspb.build();
	}

	@Override
	public Response getSurveyInteraction(String xDelegator, String surveyInteractionId) {
		Response rsp = null;
		try {
			Long sid = UrnHelper.getId(SurveyInteraction.URN_PREFIX, surveyInteractionId);
			SurveyInteraction si = surveyManager.getSurveyInteraction(sid);
			allowAdminOrEffectiveUser(request, si.getProfile().getManagedIdentity());
			rsp = Response.ok(mapper.mapWithUser(si)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	/**
	 * Reverts a survey interaction for testing purposes. 
	 * @param surveyInteractionId the survey ID
	 * @param hard If true then remove the object from the database. Default false.
	 * @return A 204.
	 */
	@Override
    public Response deleteSurveyInteraction(String xDelegator, String surveyInteractionId, Boolean hard) {
		Response rsp = null;
		try {
			Long sid = UrnHelper.getId(SurveyInteraction.URN_PREFIX, surveyInteractionId);
			SurveyInteraction si = surveyManager.getSurveyInteraction(sid);
			allowAdminOrEffectiveUser(request, si.getProfile().getManagedIdentity());
			// Ok, we are allowed to proceed
			surveyManager.revertSurveyInteraction(sid, Boolean.TRUE.equals(hard));
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response onRedirect(String xDelegator, String surveyInteractionId) {
		Response rsp = null;
		try {
			Long sid = UrnHelper.getId(SurveyInteraction.URN_PREFIX, surveyInteractionId);
			SurveyInteraction si = surveyManager.getSurveyInteraction(sid);
			allowAdminOrEffectiveUser(request, si.getProfile().getManagedIdentity());
			surveyManager.markSurveyRedirect(sid);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response onSubmit(String xDelegator, String surveyInteractionId) {
		Response rsp = null;
		try {
			Long sid = UrnHelper.getId(SurveyInteraction.URN_PREFIX, surveyInteractionId);
			SurveyInteraction si = surveyManager.getSurveyInteraction(sid);
			allowAdminOrEffectiveUser(request, si.getProfile().getManagedIdentity());
			surveyManager.markSurveySubmitted(sid);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

}
