package eu.netmobiel.overseer.processor;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.report.ReportKey;
import eu.netmobiel.commons.report.SpssReportBase;
import eu.netmobiel.communicator.model.ActivityReport;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.overseer.model.ActivitySpssReport;
import eu.netmobiel.planner.model.PassengerBehaviourReport;
import eu.netmobiel.planner.model.PassengerModalityBehaviourReport;
import eu.netmobiel.planner.service.TripManager;

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
	private TripManager tripManager;

	@Inject
    private Logger log;

    private static final String SUBJECT = "${subjectPrefix} Netmobiel Rapportage ${reportDate} - ${name}";
    private static final String BODY = 
    			"Bijgaand de maandelijkse (deel)rapportage van het gebruik van het NetMobiel platform.\n";

    /**
     * Returns whether the report job is running.
     * @return
     */
	public boolean isJobRunning() {
    	return jobRunning;
    }

	/**
	 * Start the report job. This is an asynchronous job.
	 */
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
    		
    		createAndSendActivityReport(since, until, reportDate);
    		createAndSendPassengerReport(since, until, reportDate);
    		
    	} catch (Exception e) {
			log.error("Error creating report", e);
		} finally {
    		jobRunning = false;
    	}
    }

	protected void createAndSendActivityReport(ZonedDateTime since, ZonedDateTime until, String reportDate) {
    	try {
			List<ActivityReport> activityReport = publisherService.reportActivity(since.toInstant(), until.toInstant());
			Writer writer = convertToCsv(activityReport, ActivityReport.class);
			
			Collection<ActivitySpssReport> spssReport = createSpssReport(activityReport, ActivitySpssReport.class); 
			Writer spssWriter = convertToCsvforSpss(spssReport, ActivitySpssReport.class, since, until);
			
			Map<String, Writer> reports = new LinkedHashMap<>();
			reports.put(String.format("%s-report-%s.csv", "activity", reportDate), writer);
			reports.put(String.format("%s-report-spss-%s.csv", "activity", reportDate), spssWriter);
	
			sendReports("Activiteitsniveau", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending activity report", e);
    	}
	}

	protected void createAndSendPassengerReport(ZonedDateTime since, ZonedDateTime until, String reportDate) {
    	try {
			List<PassengerBehaviourReport> passengerReport = tripManager.reportPassengerBehaviour(since.toInstant(), until.toInstant());
			Writer passengerBehaviourWriter = convertToCsv(passengerReport, PassengerBehaviourReport.class);
			List<PassengerModalityBehaviourReport> passengerModalityReport = tripManager.reportPassengerModalityBehaviour(since.toInstant(), until.toInstant());
			Writer passengerModalityBehaviourWriter = convertToCsv(passengerModalityReport, PassengerModalityBehaviourReport.class);
			
			Map<String, Writer> reports = new LinkedHashMap<>();
			reports.put(String.format("%s-report-%s.csv", "passenger-behaviour", reportDate), passengerBehaviourWriter);
			reports.put(String.format("%s-report-%s.csv", "passenger-modality-behaviour", reportDate), passengerModalityBehaviourWriter);
	
			sendReports("Reisgedrag Passagier", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending passenger behaviour report", e);
    	}
	}

	/**
	 * Class for creating a fixed order header for OpenCSV. Field names and order are retrieved
	 * by reflection.
	 *
	 * @param <T> the report record type.
	 */
	private static class FixedOrderColumnNameMappingStrategy<T> extends HeaderColumnNameMappingStrategy<T> {

		/**
		 * Creates a list of all fields that should appear in the report. The condition is the 
		 * presence of either annotation: CsvBindByName, CsvBindAndJoinByName.
		 * This method does a depth-first recursive inspection.
		 * @param fields The accumulator for the fields discovered.
		 * @param clazz the class to inspect.
		 */
		protected void addAllReportFields(List<Field> fields, Class<?> clazz) {
			Class<?> superClazz = clazz.getSuperclass();
			if (superClazz != null) {
				addAllReportFields(fields, superClazz);
			}
			for (Field f :  clazz.getDeclaredFields()) {
				if (f.isAnnotationPresent(CsvBindByName.class) || f.isAnnotationPresent(CsvBindAndJoinByName.class)) {
					fields.add(f);
				}
			}
		}
		
		@Override
		public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
			List<Field> fields = new ArrayList<>();
			addAllReportFields(fields, bean.getClass());
			List<String> headers = fields.stream().map(f -> f.getName()).collect(Collectors.toList());
		    String[] header = headers.toArray(new String[headers.size()]);
			headerIndex.initializeHeaderIndex(header);
		    return header;
		}
	}

	/**
	 * Creates a Writer with CSV records from a list of report records. 
	 * @param <T> The type of the report record.
	 * @param report The list of records.
	 * @param beanClazz the type of the report record. 
	 * @return A Writer with a CSV records.
	 * @throws Exception In case of trouble.
	 */
	protected <T> Writer convertToCsv(List<T> report, Class<T> beanClazz) throws Exception {
		try (Writer writer = new StringWriter()) {
			FixedOrderColumnNameMappingStrategy<T> strategy = new FixedOrderColumnNameMappingStrategy<T>();
		    strategy.setType(beanClazz);
		    StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
		         .withMappingStrategy(strategy)
		         .build();
		    beanToCsv.write(report);
		    return writer;
		}
	}

	/**
	 * Class for creating a fixed order and multi-valued header for OpenCSV, specifically for processing by SPSS. 
	 * Field names and order are retrieved by reflection. Each combination of indicator, year, month has its own 
	 * column in the report. Year and month columns are generated based on the provided since and until parameters
	 * to avoid any gaps in case of absent values for a certain month.
	 *
	 * @param <T> the SPSS report record type.
	 */
	private static class SpssHeaderColumnNameMappingStrategy<T> extends FixedOrderColumnNameMappingStrategy<T> {
		private ZonedDateTime since;
		private ZonedDateTime until;
		
		public SpssHeaderColumnNameMappingStrategy(ZonedDateTime since, ZonedDateTime until) {
			this.since = since;
			this.until = until;
		}
		
		@Override
		public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
			List<Field> fields = new ArrayList<>();
			addAllReportFields(fields, bean.getClass());
		    List<String> headers = new ArrayList<>();
		    List<String> suffixes = new ArrayList<>();
		    ZonedDateTime date = since;
		    while (date.isBefore(until)) {
		    	suffixes.add(String.format("_%d_%02d", date.getYear(), date.getMonthValue()));
		    	date = date.plusMonths(1);
		    }
		    for (Field f: fields) {
		    	if (f.isAnnotationPresent(CsvBindAndJoinByName.class)) {
		    		suffixes.forEach(suffix -> headers.add(f.getName() + suffix));
		    	} else {
		    		headers.add(f.getName());
		    	}
		    }
		    String[] header = headers.toArray(new String[headers.size()]);
			headerIndex.initializeHeaderIndex(header);
		    return header;
		}
	}

	/**
	 * Creates a Writer with CSV records for SPSS. 
	 * @param <T> The SPSS record type.
	 * @param spssReport the collection with SPSS records.
	 * @param beanClazz the class of the SPSS record
	 * @param since The start of the report period. Used to calculate the column expansion. 
	 * @param until The end (exclusive) of the report period.
	 * @return a Writer with the CSV records for SPSS.
	 * @throws Exception
	 */
	protected <T> Writer convertToCsvforSpss(Collection<T> spssReport, Class<T> beanClazz, ZonedDateTime since, ZonedDateTime until) throws Exception {
		try (Writer writer = new StringWriter()) {
			SpssHeaderColumnNameMappingStrategy<T> strategy = new SpssHeaderColumnNameMappingStrategy<T>(since, until);
		    strategy.setType(beanClazz);
		    StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
		         .withMappingStrategy(strategy)
		         .withApplyQuotesToAll(true)
		         .build();
		    beanToCsv.write(spssReport.stream());
		    return writer;
		}
	}

	
	/**
	 * Creates the SPSS variant of a report: Denormalise the report by creating additional year/month columns for each report value.
	 * In a SPSS report there is at most one record for each managed identity.  
	 * @param <S> the SPSS variant of the report record 
	 * @param <R> the normal report record
	 * @param report The list of report records to convert 
	 * @param spssClazz The class of the SPSS report record to convert to.
	 * @return A collection of SPSS records, one record for each managed identity.
	 */
	protected <S extends SpssReportBase<R>, R extends ReportKey> Collection<S> createSpssReport(List<R> report, Class<S> spssClazz) {
		Map<String, S> spssReportMap = new LinkedHashMap<>();
		for (R ar : report) {
    		spssReportMap.computeIfAbsent(ar.getManagedIdentity(), k -> {
				try {
					return spssClazz.getDeclaredConstructor(String.class).newInstance(k);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalStateException("Error instantiating spss report class", e);
				}
				
			})
   			.addReportValues(ar);
		}
		return spssReportMap.values();
	}

	protected void sendReports(String name, String reportDate, Map<String, Writer> reports) {
		log.info("Sending report to " + reportRecipient);
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("subjectPrefix", subjectPrefix);
		valuesMap.put("reportDate", reportDate);
		valuesMap.put("name", name);
		StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
		String subject = substitutor.replace(SUBJECT);
		String body = substitutor.replace(BODY);
	    sendEmail(subject, body, reportRecipient, reports);
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
