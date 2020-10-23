package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.PaymentBatchesApi;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.mapping.PaymentBatchMapper;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;

@RequestScoped
public class PaymentBatchesResource implements PaymentBatchesApi {

	@Inject
    private BankerUserManager userManager;

	@Inject
    private LedgerService ledgerService;
	
	@Inject
	private PageMapper pageMapper;

	@Inject
	private PaymentBatchMapper paymentBatchMapper;

	@Context
	private HttpServletRequest request;

    protected BankerUser resolveUserReference(String userId, boolean createIfNeeded) {
		BankerUser user = null;
		if ("me".equals(userId)) {
			user = createIfNeeded ? userManager.registerCallingUser() : userManager.findCallingUser();
		} else {
			user = userManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }


	@Override
	public Response createPaymentBatch() {
    	Response rsp = null;
		// The calling user will become a manager of the charity
		try {
			boolean adminView = request.isUserInRole("admin");
			if (!adminView) {
				throw new ForbiddenException("You are not allowed to create a payment batch");
			}
			BankerUser requestor = userManager.registerCallingUser();
			Long pbid = ledgerService.createPaymentBatch(requestor);
			rsp = Response.ok(ledgerService.getPaymentBatch(pbid)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPaymentBatch(String paymentBatchId, Object format) {
    	Response rsp = null;
		try {
			boolean adminView = request.isUserInRole("admin");
			if (!adminView) {
				throw new ForbiddenException("You are not allowed to view a payment batch");
			}
        	Long pbid = UrnHelper.getId(PaymentBatch.URN_PREFIX, paymentBatchId);
        	PaymentBatch pb = ledgerService.getPaymentBatch(pbid);
			rsp = Response.ok(paymentBatchMapper.mapShallow(pb)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response listPaymentBatches(OffsetDateTime since, OffsetDateTime until, Boolean settledToo,
			Integer maxResults, Integer offset) {
		boolean adminView = request.isUserInRole("admin");
		if (!adminView) {
			throw new ForbiddenException("You are not allowed to list payment batches");
		}
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
	    	PagedResult<PaymentBatch> results = ledgerService.listPaymentBatches(si, ui, settledToo, maxResults, offset);
			rsp = Response.ok(pageMapper.mapPaymentBatches(results)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response settlePaymentBatch(String paymentBatchId) {
		boolean adminView = request.isUserInRole("admin");
		if (!adminView) {
			throw new ForbiddenException("You are not allowed to settle a payment batch");
		}
		BankerUser settler = userManager.registerCallingUser();
		try {
        	Long pbid = UrnHelper.getId(Charity.URN_PREFIX, paymentBatchId);
        	ledgerService.settlePaymentBatch(settler, pbid);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	
}
