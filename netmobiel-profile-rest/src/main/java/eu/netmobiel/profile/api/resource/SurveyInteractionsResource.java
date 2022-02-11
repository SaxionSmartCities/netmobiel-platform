package eu.netmobiel.profile.api.resource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.UpdateException;
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
	public Response getSurveyInteractions(String xDelegator) {
		Response rsp = null;
		try {
			String me = securityIdentity.getEffectivePrincipal().getName();
			Optional<SurveyInteraction> sopt = surveyManager.inviteToSurvey(me);
			List<SurveyInteraction> sis = sopt.isEmpty() ? Collections.emptyList() : Collections.singletonList(sopt.get());
			rsp = Response.ok(mapper.map(sis)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@SuppressWarnings("resource")
	@Override
	public Response onRedirect(String xDelegator, String providerSurveyId) {
		Response rsp = null;
		try {
			String me = securityIdentity.getEffectivePrincipal().getName();
			surveyManager.onSurveyRedirect(me, providerSurveyId);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (UpdateException e) {
			rsp = Response.status(Status.GONE).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@SuppressWarnings("resource")
	@Override
	public Response onSubmit(String xDelegator, String providerSurveyId) {
		Response rsp = null;
		try {
			String me = securityIdentity.getEffectivePrincipal().getName();
			surveyManager.onSurveySubmitted(me, providerSurveyId);
			rsp = Response.noContent().build();
		} catch (UpdateException e) {
			rsp = Response.status(Status.GONE).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	/**
	 * Reverts a survey interaction for testing purposes. 
	 * @param providerSurveyId the survey ID
	 * @param userId The user id. If omitted the calling user
	 * @param scope One of: payment, reward, survey. If a survey is removed, then reward and payment are removed as well. 
	 * 				If a reward is removed, then the payment is removed too.
	 * @return A 204.
	 */
	@SuppressWarnings("resource")
    @DELETE
    @Path("/{surveyId}")
    public Response deleteSurveyInteraction(@PathParam("surveyId") String providerSurveyId, @QueryParam("user") String userId, @QueryParam("scope") String scope) {
		Response rsp = null;
		try {
			final boolean privileged = request.isUserInRole("admin"); 
			if (!privileged) {
				throw new SecurityException("You have no privilege to remove or reverse a survey interaction");
			}
			if (userId == null) {
				userId = "me";
			}
			String mid = resolveIdentity(null, userId);
			surveyManager.revertSurveyInteraction(mid, providerSurveyId, scope);
			rsp = Response.noContent().build();
		} catch (UpdateException e) {
			rsp = Response.status(Status.GONE).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

}
