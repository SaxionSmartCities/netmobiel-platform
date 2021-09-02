package eu.netmobiel.profile.api.resource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
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

}
