package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.WithdrawalRequestsApi;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;

@RequestScoped
public class WithdrawalRequestsResource implements WithdrawalRequestsApi {

	@Inject
    private LedgerService ledgerService;
	
	@Inject
	private PageMapper pageMapper;

	@Context
	private HttpServletRequest request;

	@Override
	public Response listWithdrawalRequests(String accountName, OffsetDateTime since, OffsetDateTime until,
			String status, Integer maxResults, Integer offset) {
		boolean adminView = request.isUserInRole("admin");
		if (!adminView) {
			throw new ForbiddenException("You are not allowed to list payment batches");
		}
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
			PaymentStatus ps = status == null ? null : PaymentStatus.valueOf(status);
	    	PagedResult<WithdrawalRequest> results = ledgerService.listWithdrawalRequests(accountName, si, ui, ps, maxResults, offset);
			rsp = Response.ok(pageMapper.mapWithdrawalRequests(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

}
