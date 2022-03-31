package eu.netmobiel.banker.api.resource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.text.StringSubstitutor;

import eu.netmobiel.banker.api.PaymentBatchesApi;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.mapping.PaymentBatchMapper;
import eu.netmobiel.banker.api.mapping.WithdrawalRequestMapper;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.rest.sepa.SepaCreditTransferDocument;
import eu.netmobiel.banker.rest.sepa.SepaGroupHeader;
import eu.netmobiel.banker.rest.sepa.SepaPaymentInformation;
import eu.netmobiel.banker.rest.sepa.SepaTransaction;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.WithdrawalService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;

@ApplicationScoped
public class PaymentBatchesResource implements PaymentBatchesApi {

	@Inject
    private BankerUserManager userManager;

	@Inject
    private WithdrawalService withdrawalService;
	
	@Inject
	private PageMapper pageMapper;

	@Inject
	private PaymentBatchMapper paymentBatchMapper;

	@Inject
	private WithdrawalRequestMapper withdrawalRequestBatchMapper;

	@Resource(mappedName="java:jboss/mail/NetMobiel")
    private Session mailSession;	

	@Override
	public Response createPaymentBatch() {
    	Response rsp = null;
		// The calling user will become a manager of the charity
		try {
			userManager.findOrRegisterCallingUser();
			String pbid = UrnHelper.createUrn(PaymentBatch.URN_PREFIX, withdrawalService.createPaymentBatch());
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
        	PaymentBatch pb = withdrawalService.getPaymentBatch(pbid);
        	Response.ResponseBuilder rspb;
        	if ("PAIN.001".equals(format)) {
    			rspb = Response.ok(createCreditTransferDocument(pb, true, Boolean.TRUE.equals(forceUniqueId)).toXml().toString(), MediaType.TEXT_XML);
        	} else if ("JSON".equals(format)) {
        		// JSON
    			rspb = Response.ok(paymentBatchMapper.mapWithWithdrawals(pb), MediaType.APPLICATION_JSON);
        	} else {
        		throw new BadRequestException("Format not known: " + format);
        	}
        	rsp = rspb.build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	private static final String SUBJECT = "Netmobiel Credit Transfer File ${dateFormatted}";
    private static final String BODY = 
    		  "Beste ${givenName},"
    		+ "\n\nDeze mail bevat het aangevraagde betalingsbestand van NetMobiel. "
    		+ "\n\nDe orderreferentie van dit bestand is ${orderReference}."
    		+ "Verwerk deze bij je bank."
    		+ "\n\nNa afronding bij de bank moet je in de NetMobiel App vervolgens de betreffende opdrachten goed- of afkeuren, "
    		+ " zodat de administratie in Netmobiel overeenkomt met de verwerking bij de bank."
    		+ "\n\nMet vriendelijke groet,\n\nNetMobiel Platform\n";
    
    @Override
    public Response mailPaymentBatchAsPAINFile(String paymentBatchId, Boolean forceUniqueId, Boolean pendingOnly) {
    	Response rsp = null;
		try {
			BankerUser caller = userManager.findCallingUser();
			if (caller == null || caller.getEmail() == null || caller.getEmail().isBlank()) {
				throw new BadRequestException("No email known");
			}
        	Long pbid = UrnHelper.getId(PaymentBatch.URN_PREFIX, paymentBatchId);
        	PaymentBatch pb = withdrawalService.getPaymentBatch(pbid);
   			String ctd = createCreditTransferDocument(pb, !Boolean.FALSE.equals(pendingOnly), Boolean.TRUE.equals(forceUniqueId)).toXml().toString();

   			LocalDateTime now = LocalDateTime.now();
   			Map<String, String> valuesMap = new HashMap<>();
   			valuesMap.put("givenName", caller.getGivenName());
   			valuesMap.put("dateFormatted", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
   			valuesMap.put("orderReference", pb.getOrderReference());

   			StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
   			String subject = substitutor.replace(SUBJECT);
   			String body = substitutor.replace(BODY);
   			Map<String, String> attachmentMap = new HashMap<>();
   			String filename = String.format("netmobiel-credit-transfer-%s.xml", now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")));
   			attachmentMap.put(filename, ctd);
   			sendEmail(subject, body, caller.getEmail(), attachmentMap, "text/xml");
   			rsp = Response.noContent().build();
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
	    	PagedResult<PaymentBatch> results = withdrawalService.listPaymentBatches(si, ui, ps, maxResults, offset);
			rsp = Response.ok(pageMapper.mapPaymentBatches(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

//	@Override
//	public Response settlePaymentBatch(String paymentBatchId) {
//		try {
//        	Long pbid = UrnHelper.getId(Charity.URN_PREFIX, paymentBatchId);
//        	withdrawalService.settlePaymentBatch(pbid);
//		} catch (BusinessException ex) {
//			throw new WebApplicationException(ex);
//		}
//		return Response.noContent().build();
//	}

	@Override
    public Response updatePaymentBatch(String paymentBatchId, eu.netmobiel.banker.api.model.PaymentBatch batch) {
		try {
        	Long pbid = UrnHelper.getId(Charity.URN_PREFIX, paymentBatchId);
        	List<WithdrawalRequest> wrs = withdrawalRequestBatchMapper.mapShallow(batch.getWithdrawalRequests());
        	withdrawalService.updatePaymentBatch(pbid, wrs);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
    }

	@Override
	public Response cancelPaymentBatch(String paymentBatchId, String reason) {
		try {
        	Long pbid = UrnHelper.getId(Charity.URN_PREFIX, paymentBatchId);
        	withdrawalService.cancelPaymentBatch(pbid, reason);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return Response.noContent().build();
	}

	private static SepaCreditTransferDocument createCreditTransferDocument(PaymentBatch pb, boolean pendingOnly, boolean forceUniqueBatchId) {
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
	
	private void sendEmail(String subject, String body, String recipient, Map<String, String> attachments, String mimeType) {
		try {
            MimeMessage msg = new MimeMessage(mailSession);
            msg.setRecipients(javax.mail.Message.RecipientType.TO, recipient);
//        	m.setFrom(reportRecipient);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            Multipart multipart = new MimeMultipart();
            // sets the multi-part as e-mail's content
            msg.setContent(multipart);
            
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/plain");
            multipart.addBodyPart(messageBodyPart);
    
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                byte[] poiBytes = entry.getValue().getBytes();
                DataSource dataSource = new ByteArrayDataSource(poiBytes, mimeType);
                BodyPart attachmentBodyPart = new MimeBodyPart();
                attachmentBodyPart.setDataHandler(new DataHandler(dataSource));
                attachmentBodyPart.setFileName(entry.getKey());
                multipart.addBodyPart(attachmentBodyPart);
			}
            
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new SystemException(String.format("Failed to send email on '%s' to %s", subject, recipient), e);
        }
	}
}
