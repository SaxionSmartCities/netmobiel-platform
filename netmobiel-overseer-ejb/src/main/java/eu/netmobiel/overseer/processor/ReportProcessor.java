package eu.netmobiel.overseer.processor;

import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.collections4.comparators.FixedOrderComparator;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.communicator.model.ActivityReport;
import eu.netmobiel.communicator.model.ReportKey;
import eu.netmobiel.communicator.service.PublisherService;

/**
 * Stateless bean for running a report on NetMobiel.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ReportProcessor {

	@Resource(lookup = "java:global/report/recipientEmailAddress")
	private String reportRecipient;

	@Resource(lookup = "java:global/report/lookbackMonths")
	private Integer lookbackMonths;

	@Resource(lookup = "java:global/report/earliestStartAt")
	private String earliestStartAt;

	@Resource(lookup = "java:global/report/timeZone")
	private String timeZone;

	@Resource(lookup = "java:global/report/subjectPrefix")
	private String subjectPrefix;

	@Resource(mappedName="java:jboss/mail/NetMobiel")
    private Session mailSession;	

	private boolean jobRunning = false;
	
	@Inject
	private PublisherService publisherService;

	@Inject
    private Logger log;

    private static final String SUBJECT = "${subjectPrefix} Activation Report ${reportDate}";
    private static final String BODY = 
    			"Bijgaand de maandelijkse rapportage van het gebruik van het NetMobiel platform.\n";
    
	@Asynchronous
    public void startReport() {
    	if (jobRunning) {
    		throw new IllegalStateException("Operation already running");
    	}
    	jobRunning = true;
    	try {
    		log.info("Sending report to " + reportRecipient);
    		// Get the first day
    		ZonedDateTime earliestStart  = LocalDate.parse(earliestStartAt).atStartOfDay(ZoneId.of(timeZone));
    		ZonedDateTime until  = LocalDate.now().atStartOfDay(ZoneId.of(timeZone)).withDayOfMonth(1);
    		ZonedDateTime since = ZonedDateTime.from(until).minusMonths(lookbackMonths);
    		if (since.isBefore(earliestStart)) {
    			since = earliestStart;
    		}
    		String reportDate = until.format(DateTimeFormatter.ISO_LOCAL_DATE);
    		log.info(String.format("Start report %s for period %s - %s", reportDate, since.format(DateTimeFormatter.ISO_LOCAL_DATE), until.format(DateTimeFormatter.ISO_LOCAL_DATE)));

    		List<ActivityReport> activityReport = publisherService.reportActivity(since.toInstant(), until.toInstant());
    		Writer writer = convertToCsv(activityReport);
    		
    		Map<String, Writer> attachments = new LinkedHashMap<>();
    		attachments.put(String.format("%s-report-%s.csv", "activation", reportDate), writer);

    		log.info("Sending report to " + reportRecipient);
    		Map<String, String> valuesMap = new HashMap<>();
    		valuesMap.put("subjectPrefix", subjectPrefix);
    		valuesMap.put("reportDate", reportDate);
    		StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
    		String subject = substitutor.replace(SUBJECT);
    		String body = substitutor.replace(BODY);
    	    sendEmail(subject, body, reportRecipient, attachments);
    	} catch (Exception e) {
			log.error("Error creating report", e);
		} finally {
    		jobRunning = false;
    	}
    }

	protected Writer convertToCsv(List<ActivityReport> activityReport) throws Exception {
		try (Writer writer = new StringWriter()) {
			HeaderColumnNameMappingStrategy<ActivityReport> strategy = new HeaderColumnNameMappingStrategy<>();
		    strategy.setType(ActivityReport.class);
		    String[] headers = {"COMPARATOR_ASC", "KEY", "MANAGEDIDENTITY", "YEAR", "MONTH", "MESSAGECOUNT", "MESSAGEACKEDCOUNT", "NOTIFICATIONCOUNT", "NOTIFICATIONACKEDCOUNT"};
		    FixedOrderComparator<String> activityComparator = new FixedOrderComparator<>(headers); 
		    strategy.setColumnOrderOnWrite(activityComparator);
//		    MultiValuedMap<Class<?>, Field> fields = new HashSetValuedHashMap<>();
//		    fields.put(ActivityReport.class, ActivityReport.class.getDeclaredField("COMPARATOR_ASC"));
//		    strategy.ignoreFields(fields);
		    StatefulBeanToCsv<ActivityReport> beanToCsv = new StatefulBeanToCsvBuilder<ActivityReport>(writer)
		         .withMappingStrategy(strategy)
		         .withIgnoreField(ActivityReport.class, ReportKey.class.getDeclaredField("key"))
		         .withIgnoreField(ActivityReport.class, ActivityReport.class.getDeclaredField("COMPARATOR_ASC"))
		         .build();
		    beanToCsv.write(activityReport);
		    return writer;
		}
	}

	public boolean isJobRunning() {
    	return jobRunning;
    }

	protected void sendEmail(String subject, String body, String recipient, Map<String, Writer> attachments) {
		try {
            MimeMessage msg = new MimeMessage(mailSession);
            msg.setRecipients(RecipientType.TO, recipient);
//        	m.setFrom(reportRecipient);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            Multipart multipart = new MimeMultipart();
            // sets the multi-part as e-mail's content
            msg.setContent(multipart);
            
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/plain");
            multipart.addBodyPart(messageBodyPart);
    
            for (Map.Entry<String, Writer> entry : attachments.entrySet()) {
                byte[] poiBytes = entry.getValue().toString().getBytes();
                DataSource dataSource = new ByteArrayDataSource(poiBytes, "text/csv");
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
