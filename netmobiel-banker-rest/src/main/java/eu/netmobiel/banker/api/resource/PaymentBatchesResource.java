package eu.netmobiel.banker.api.resource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.banker.api.PaymentBatchesApi;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.mapping.PaymentBatchMapper;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.rest.sepa.SepaCreditTransferDocument;
import eu.netmobiel.banker.rest.sepa.SepaGroupHeader;
import eu.netmobiel.banker.rest.sepa.SepaPaymentInformation;
import eu.netmobiel.banker.rest.sepa.SepaTransaction;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;

@ApplicationScoped
public class PaymentBatchesResource implements PaymentBatchesApi {

	@Inject
    private BankerUserManager userManager;

	@Inject
    private LedgerService ledgerService;
	
	@Inject
	private PageMapper pageMapper;

	@Inject
	private PaymentBatchMapper paymentBatchMapper;

	@Override
	public Response createPaymentBatch() {
    	Response rsp = null;
		// The calling user will become a manager of the charity
		try {
			userManager.registerCallingUser();
			String pbid = BankerUrnHelper.createUrn(PaymentBatch.URN_PREFIX, ledgerService.createPaymentBatch());
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(pbid)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getPaymentBatch(String paymentBatchId, String format, Boolean forceUniqueId) {
    	Response rsp = null;
		try {
        	Long pbid = UrnHelper.getId(PaymentBatch.URN_PREFIX, paymentBatchId);
        	PaymentBatch pb = ledgerService.getPaymentBatch(pbid);
        	if ("PAIN.001".equals(format)) {
    			rsp = Response.ok(createCreditTransferDocument(pb, true, Boolean.TRUE.equals(forceUniqueId)).toXml().toString(), MediaType.TEXT_XML).build();
        	} else if ("JSON".equals(format)) {
        		// JSON
    			rsp = Response.ok(paymentBatchMapper.mapWithWithdrawals(pb), MediaType.APPLICATION_JSON).build();
        	} else {
        		throw new BadRequestException("Format not known: " + format);
        	}
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response listPaymentBatches(OffsetDateTime since, OffsetDateTime until, String status,
			Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
			PaymentStatus ps = status == null ? null : PaymentStatus.valueOf(status);
	    	PagedResult<PaymentBatch> results = ledgerService.listPaymentBatches(si, ui, ps, maxResults, offset);
			rsp = Response.ok(pageMapper.mapPaymentBatches(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response settlePaymentBatch(String paymentBatchId) {
		try {
        	Long pbid = UrnHelper.getId(Charity.URN_PREFIX, paymentBatchId);
        	ledgerService.settlePaymentBatch(pbid);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}


	@Override
	public Response cancelPaymentBatch(String paymentBatchId, String reason) {
		try {
        	Long pbid = UrnHelper.getId(Charity.URN_PREFIX, paymentBatchId);
        	ledgerService.cancelPaymentBatch(pbid);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	protected SepaCreditTransferDocument createCreditTransferDocument(PaymentBatch pb, boolean pendingOnly, boolean forceUniqueBatchId) {
		List<SepaTransaction> transactions = new ArrayList<>();
		for (WithdrawalRequest wr : pb.getWithdrawalRequests()) {
			if (pendingOnly && wr.getStatus().isFinal()) {
				continue;
			}
			transactions.add(new SepaTransaction.Builder(wr.getIban())
					.withAmount(BigDecimal.valueOf(wr.getAmountEurocents(), 2))
					.withEnd2EndId(wr.getOrderReference())
					.withName(wr.getIbanHolder())
					.withRemittance(wr.getDescription())
					.build()
					);
		}
		if (transactions.isEmpty()) {
			throw new BadRequestException("There are no pending requests anymore");
		}
		SepaPaymentInformation payinfo = new SepaPaymentInformation.Builder("PID-" + pb.getOrderReference())
				.of(transactions)
				.withAccount(pb.getOriginatorIban())
				.withAccountHolder(pb.getOriginatorIbanHolder())
				.build();
		String messageId = pb.getOrderReference();
		if (forceUniqueBatchId) {
			messageId = messageId + "-" + String.valueOf(Instant.now().getEpochSecond());
		}
		SepaGroupHeader header = new SepaGroupHeader.Builder(messageId)
				.of(transactions)
				.withInitiatingParty("NetMobiel")
				.build();
		return new SepaCreditTransferDocument.Builder()
				.with(header)
				.with(payinfo)
				.with(transactions)
				.build();
	}
	
}
