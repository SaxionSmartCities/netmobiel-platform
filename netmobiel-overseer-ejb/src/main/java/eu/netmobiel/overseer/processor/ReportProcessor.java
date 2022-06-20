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
import java.util.Set;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
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

import eu.netmobiel.banker.service.BankerReportService;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.report.ActivityReport;
import eu.netmobiel.commons.report.DriverBehaviourReport;
import eu.netmobiel.commons.report.IncentiveModelDriverReport;
import eu.netmobiel.commons.report.IncentiveModelPassengerReport;
import eu.netmobiel.commons.report.PassengerBehaviourReport;
import eu.netmobiel.commons.report.PassengerModalityBehaviourReport;
import eu.netmobiel.commons.report.ProfileReport;
import eu.netmobiel.commons.report.ReportKey;
import eu.netmobiel.commons.report.ReportKeyWithModality;
import eu.netmobiel.commons.report.ReportPeriodKey;
import eu.netmobiel.commons.report.RideReport;
import eu.netmobiel.commons.report.ShoutOutRecipientReport;
import eu.netmobiel.commons.report.SpssReportBase;
import eu.netmobiel.commons.report.SpssReportWithModality;
import eu.netmobiel.commons.report.TripReport;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.service.CommunicatorReportService;
import eu.netmobiel.overseer.model.ActivitySpssReport;
import eu.netmobiel.overseer.model.DriverBehaviourSpssReport;
import eu.netmobiel.overseer.model.IncentiveModelDriverSpssReport;
import eu.netmobiel.overseer.model.IncentiveModelPassengerSpssReport;
import eu.netmobiel.overseer.model.PassengerBehaviourSpssReport;
import eu.netmobiel.overseer.model.PassengerModalityBehaviourSpssReport;
import eu.netmobiel.planner.service.PlannerReportService;
import eu.netmobiel.profile.service.ProfileReportService;
import eu.netmobiel.profile.service.ReviewManager;
import eu.netmobiel.rideshare.service.RideshareReportService;

/**
 * Stateless bean for running a report on Netmobiel.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NEVER)
@Logging
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
	private BankerReportService bankerReportService;

	@Inject
	private CommunicatorReportService communicatorReportService;

	@Inject
	private PlannerReportService plannerReportService;

	@Inject
	private ProfileReportService profileReportService;

	@Inject
	private RideshareReportService rideshareReportService;

	@Inject
	private ReviewManager reviewManager;
	
	@Inject
    private Logger log;

    private static final String SUBJECT = "${subjectPrefix} Netmobiel Rapportage ${reportDate} - ${name}";
    private static final String BODY = 
    			"Bijgaand de maandelijkse (deel)rapportage van het gebruik van het Netmobiel platform.\n";

    /**
     * Runs the report on Netmobiel each first day of the month in the morning.
     */
	@Schedule(info = "Report on Netmobiel", dayOfMonth = "1", hour = "7", minute = "0", second = "0", persistent = true)
    public void timedStartReporting() {
		try {
			startReport();		
		} catch (Exception ex) {
			log.error("Error during timed reporting: " + ex.toString());
		}
	}
	
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
    		
    		Map<String, ProfileReport> profileReportMap = profileReportService.reportUsers();
    		createAndSendProfilesReport(reportDate, profileReportMap);
    		createAndSendActivityReport(since, until, reportDate, profileReportMap);
    		createAndSendPassengerBehaviourReport(since, until, reportDate, profileReportMap);
    		createAndSendDriverBehaviourReport(since, until, reportDate, profileReportMap);
    		createAndSendIncentiveModelPassengerReport(since, until, reportDate, profileReportMap);
    		createAndSendIncentiveModelDriverReport(since, until, reportDate, profileReportMap);
    		createAndSendRideshareRidesReport(since, until, reportDate, profileReportMap);
    		createAndSendTripsReport(since, until, reportDate, profileReportMap);
    		log.info("Done reporting");
    	} catch (Exception e) {
			log.error("Error creating report", e);
		} finally {
    		jobRunning = false;
    	}
    }

	private static String createReportFilename(String type, String reportDate) {
		return String.format("%s-report-%s.csv", type, reportDate);
	}
	
	private static String createSpssReportFilename(String type, String reportDate) {
		return String.format("%s-report-spss-%s.csv", type, reportDate);
	}

	protected void createAndSendProfilesReport(String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		// Get the rideshare users for driver-specific attributes
    		Map<String, ProfileReport> rideshareMap = rideshareReportService.reportUsers();
    		for (Map.Entry<String, ProfileReport> pr : profileReportMap.entrySet()) {
    			if (Boolean.TRUE.equals(pr.getValue().getIsDriver())) {
    				ProfileReport pr_rs = rideshareMap.get(pr.getKey());
    				if (pr_rs != null) {
    					pr.getValue().setNrActiveCars(pr_rs.getNrActiveCars());
    				}
    			}
			}
    		List<ProfileReport> report = profileReportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
			String ridesReport = convertToCsv(report, ProfileReport.class);
			Map<String, String> reports = new LinkedHashMap<>();
			reports.put(createReportFilename("profiles", reportDate), ridesReport);
			sendReports("Profielen", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending profiles report", e);
    	}
	}

	protected void copyProfileInfo(Collection<? extends ReportKey> target, Map<String, ProfileReport> sourceMap) {
		for (ReportKey r : target) {
			ProfileReport pr = sourceMap.get(r.getManagedIdentity());
			if (pr != null) {
				r.setHome(pr.getHome());
			}
		}
	}

	protected void createAndSendActivityReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
   		  	Map<String, ActivityReport> activityReportMap = communicatorReportService.reportActivity(since.toInstant(), until.toInstant());
   		  	Map<String, ActivityReport> profileActivityReportMap = profileReportService.reportUsageActivity(since.toInstant(), until.toInstant());
   		  	// Merge profile activity into communicator activity 
    		for (Map.Entry<String, ActivityReport> entry : profileActivityReportMap.entrySet()) {
    			activityReportMap.computeIfAbsent(entry.getKey(), key -> new ActivityReport(entry.getValue()))
				.setUsageDaysPerMonthCount(entry.getValue().getUsageDaysPerMonthCount());
			}
			List<ActivityReport> activityReport = activityReportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
   		  	copyProfileInfo(activityReport, profileReportMap);
   		  	
			String report = convertToCsv(activityReport, ActivityReport.class);
			
			Collection<ActivitySpssReport> spssReports = createSpssReport(activityReport, ActivitySpssReport.class); 
			String spssReport = convertToCsvforSpss(spssReports, ActivitySpssReport.class, since, until);
			
			Map<String, String> reports = new LinkedHashMap<>();
			final var type = "activity";
			reports.put(createReportFilename(type, reportDate), report);
			reports.put(createSpssReportFilename(type, reportDate), spssReport);
	
			sendReports("Activiteitsniveau", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending activity report", e);
    	}
	}

	protected void createAndSendPassengerBehaviourReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		Map<String, PassengerBehaviourReport> passengerReportMap = plannerReportService.reportPassengerBehaviour(since.toInstant(), until.toInstant());
			List<PassengerBehaviourReport> passengerReport = passengerReportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
   		  	copyProfileInfo(passengerReport, profileReportMap);
			String passengerBehaviour = convertToCsv(passengerReport, PassengerBehaviourReport.class);

			Collection<PassengerBehaviourSpssReport> spssReport = createSpssReport(passengerReport, PassengerBehaviourSpssReport.class); 
			String passengerBehaviourSpss = convertToCsvforSpss(spssReport, PassengerBehaviourSpssReport.class, since, until);

			Map<String, PassengerModalityBehaviourReport> passengerModalityReportMap = plannerReportService.reportPassengerModalityBehaviour(since.toInstant(), until.toInstant());
			List<PassengerModalityBehaviourReport> passengerModalityReport = passengerModalityReportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
   		  	copyProfileInfo(passengerModalityReport, profileReportMap);
			String passengerModalityBehaviour = convertToCsv(passengerModalityReport, PassengerModalityBehaviourReport.class);

			Collection<PassengerModalityBehaviourSpssReport> spssModalityReport = createSpssModalityReport(passengerModalityReport, PassengerModalityBehaviourSpssReport.class); 
			String passengerModalityBehaviourSpss = convertToCsvforSpss(spssModalityReport, PassengerModalityBehaviourSpssReport.class, since, until);

			Map<String, String> reports = new LinkedHashMap<>();
			final var type = "passenger-behaviour";
			reports.put(createReportFilename(type, reportDate), passengerBehaviour);
			reports.put(createSpssReportFilename(type, reportDate), passengerBehaviourSpss);
			final var typeModality = "passenger-modality-behaviour";
			reports.put(createReportFilename(typeModality, reportDate), passengerModalityBehaviour);
			reports.put(createSpssReportFilename(typeModality, reportDate), passengerModalityBehaviourSpss);
	
			sendReports("Reisgedrag Passagier", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending passenger behaviour report", e);
    	}
	}

	protected void createAndSendDriverBehaviourReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		Map<String, DriverBehaviourReport> driverReportMap = rideshareReportService.reportDriverActivity(since.toInstant(), until.toInstant());
			List<ShoutOutRecipientReport> shoutOutRecipientReport = 
					communicatorReportService.reportShoutOutActivity(since.toInstant(), until.toInstant());
			// Merge the shout-out report into the driver report.
			for (ShoutOutRecipientReport sorr : shoutOutRecipientReport) {
				driverReportMap.computeIfAbsent(sorr.getKey(), k -> new DriverBehaviourReport(sorr))
					.setShoutOutNotificationCount(sorr.getShoutOutNotificationCount());
				driverReportMap.get(sorr.getKey())
					.setShoutOutNotificationAckedCount(sorr.getShoutOutNotificationAckedCount());

			}
			List<DriverBehaviourReport> driverReport = driverReportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
   		  	copyProfileInfo(driverReport, profileReportMap);
			String driverBehaviour = convertToCsv(driverReport, DriverBehaviourReport.class);
			
			Collection<DriverBehaviourSpssReport> spssReport = createSpssReport(driverReport, DriverBehaviourSpssReport.class); 
			String driverBehaviourSpss = convertToCsvforSpss(spssReport, DriverBehaviourSpssReport.class, since, until);

			Map<String, String> reports = new LinkedHashMap<>();
			final var type = "driver-behaviour";
			reports.put(createReportFilename(type, reportDate), driverBehaviour);
			reports.put(createSpssReportFilename(type, reportDate), driverBehaviourSpss);
	
			sendReports("Reisgedrag Chauffeur", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending driver behaviour report", e);
    	}
	}

	protected void createAndSendIncentiveModelPassengerReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		Map<String, IncentiveModelPassengerReport> reportMap = bankerReportService.reportIncentivesPassenger(since.toInstant(), until.toInstant());
    		Map<String, IncentiveModelPassengerReport> tripsReviewedReportMap = profileReportService.reportIncentiveModelPassager(since.toInstant(), until.toInstant());
    		// Copy the review count into the main report
    		for (Map.Entry<String, IncentiveModelPassengerReport> entry : tripsReviewedReportMap.entrySet()) {
    			reportMap.computeIfAbsent(entry.getKey(), key -> new IncentiveModelPassengerReport(entry.getValue()))
				.setTripsReviewedCount(entry.getValue().getTripsReviewedCount());
			}
			List<IncentiveModelPassengerReport> report = reportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
   		  	copyProfileInfo(report, profileReportMap);
			String incentiveModel = convertToCsv(report, IncentiveModelPassengerReport.class);

			Collection<IncentiveModelPassengerSpssReport> spssReport = createSpssReport(report, IncentiveModelPassengerSpssReport.class); 
			String incentiveModelSpss = convertToCsvforSpss(spssReport, IncentiveModelPassengerSpssReport.class, since, until);

			Map<String, String> reports = new LinkedHashMap<>();
			final var type = "incentives-passenger";
			reports.put(createReportFilename(type, reportDate), incentiveModel);
			reports.put(createSpssReportFilename(type, reportDate), incentiveModelSpss);
	
			sendReports("Incentives Passagier", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending incentive model passenger report", e);
    	}
	}

	protected void createAndSendIncentiveModelDriverReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		Map<String, IncentiveModelDriverReport> reportMap = bankerReportService.reportIncentivesDriver(since.toInstant(), until.toInstant());
    		Map<String, IncentiveModelDriverReport> ridesReviewedReportMap = profileReportService.reportIncentiveModelDriver(since.toInstant(), until.toInstant());
    		// Copy the review count into the main report
    		for (Map.Entry<String, IncentiveModelDriverReport> entry : ridesReviewedReportMap.entrySet()) {
    			reportMap.computeIfAbsent(entry.getKey(), key -> new IncentiveModelDriverReport(entry.getValue()))
				.setRidesReviewedCount(entry.getValue().getRidesReviewedCount());
			}
			List<IncentiveModelDriverReport> report = reportMap.values().stream()
	    			.sorted()
	    			.collect(Collectors.toList());
   		  	copyProfileInfo(report, profileReportMap);
   		  	String incentiveModel = convertToCsv(report, IncentiveModelDriverReport.class);
			
			Collection<IncentiveModelDriverSpssReport> spssReport = createSpssReport(report, IncentiveModelDriverSpssReport.class); 
			String incentiveModelSpss = convertToCsvforSpss(spssReport, IncentiveModelDriverSpssReport.class, since, until);

			Map<String, String> reports = new LinkedHashMap<>();
			final var type = "incentives-driver";
			reports.put(createReportFilename(type, reportDate), incentiveModel);
			reports.put(createSpssReportFilename(type, reportDate), incentiveModelSpss);
	
			sendReports("Incentives Chauffeur", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending incentive model driver report", e);
    	}
	}

	protected void createAndSendRideshareRidesReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		List<RideReport> report = rideshareReportService.reportRides(since.toInstant(), until.toInstant());

    		// RSC-8 
    		List<String> passengerReviewContexts = report.stream()
    				.filter(r -> r.getTripUrn() != null)
    				.map(r -> r.getTripUrn())
    				.collect(Collectors.toList());
    		Set<String> passengerReviewExists = reviewManager.reviewExists(passengerReviewContexts);
    		for(RideReport r: report) {
   				r.setReviewedByPassenger(passengerReviewExists.contains(r.getTripUrn()));
    		}
    		// RSC-9 
    		List<String> driverReviewContexts = report.stream()
    				.map(r -> r.getRideUrn())
    				.collect(Collectors.toList());
    		Set<String> driverReviewExists = reviewManager.reviewExists(driverReviewContexts);
    		for(RideReport r: report) {
   				r.setReviewedByDriver(driverReviewExists.contains(r.getRideUrn()));
    		}

   		  	copyProfileInfo(report, profileReportMap);
   		  	String ridesReport = convertToCsv(report, RideReport.class);
			Map<String, String> reports = new LinkedHashMap<>();
			reports.put(createReportFilename("rides", reportDate), ridesReport);
	
			sendReports("Reis Chauffeur", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending Rideshare rides report", e);
    	}
	}

	protected void createAndSendTripsReport(ZonedDateTime since, ZonedDateTime until, String reportDate, Map<String, ProfileReport> profileReportMap) {
    	try {
    		List<TripReport> report = plannerReportService.reportTrips(since.toInstant(), until.toInstant());
    		// RSP-10 
    		List<String> passengerReviewContexts = report.stream()
    				.filter(r -> r.getTripUrn() != null)
    				.map(r -> r.getTripUrn())
    				.collect(Collectors.toList());
    		Set<String> passengerReviewExists = reviewManager.reviewExists(passengerReviewContexts);
    		for(TripReport r: report) {
   				r.setReviewedByPassenger(passengerReviewExists.contains(r.getTripUrn()));
    		}
    		// RSP-11 
    		List<String> driverReviewContexts = report.stream()
    				.map(r -> r.getRideUrn())
    				.collect(Collectors.toList());
    		Set<String> driverReviewExists = reviewManager.reviewExists(driverReviewContexts);
    		for(TripReport r: report) {
   				r.setReviewedByDriver(driverReviewExists.contains(r.getRideUrn()));
    		}

   		  	copyProfileInfo(report, profileReportMap);
   		  	String tripsReport= convertToCsv(report, TripReport.class);
			Map<String, String> reports = new LinkedHashMap<>();
			reports.put(createReportFilename("trips", reportDate), tripsReport);
	
			sendReports("Reis Passagier", reportDate, reports);
    	} catch (Exception e) {
			log.error("Error creating and sending passenger trips report", e);
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
	 * Creates a String with CSV records from a list of report records. 
	 * @param <T> The type of the report record.
	 * @param report The list of records.
	 * @param beanClazz the type of the report record. 
	 * @return A String  with a CSV records.
	 * @throws Exception In case of trouble.
	 */
	protected <T> String convertToCsv(List<T> report, Class<T> beanClazz) throws Exception {
		try (Writer writer = new StringWriter()) {
			FixedOrderColumnNameMappingStrategy<T> strategy = new FixedOrderColumnNameMappingStrategy<>();
		    strategy.setType(beanClazz);
		    StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
		         .withMappingStrategy(strategy)
		         .build();
		    beanToCsv.write(report);
		    return writer.toString();
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
	 * Creates a String with CSV records for SPSS. 
	 * @param <T> The SPSS record type.
	 * @param spssReport the collection with SPSS records.
	 * @param beanClazz the class of the SPSS record
	 * @param since The start of the report period. Used to calculate the column expansion. 
	 * @param until The end (exclusive) of the report period.
	 * @return a String with the CSV records for SPSS.
	 * @throws Exception
	 */
	protected <T> String convertToCsvforSpss(Collection<T> spssReport, Class<T> beanClazz, ZonedDateTime since, ZonedDateTime until) throws Exception {
		try (Writer writer = new StringWriter()) {
			SpssHeaderColumnNameMappingStrategy<T> strategy = new SpssHeaderColumnNameMappingStrategy<>(since, until);
		    strategy.setType(beanClazz);
		    StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
		         .withMappingStrategy(strategy)
		         .withApplyQuotesToAll(true)
		         .build();
		    beanToCsv.write(spssReport.stream());
		    return writer.toString();
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
	protected <S extends SpssReportBase<R>, R extends ReportPeriodKey> Collection<S> createSpssReport(List<R> report, Class<S> spssClazz) {
		Map<String, S> spssReportMap = new LinkedHashMap<>();
		for (R r : report) {
    		spssReportMap.computeIfAbsent(r.getManagedIdentity(), k -> {
				try {
					// Create a report line with the managed identity and the home locality
					return spssClazz.getDeclaredConstructor(String.class, String.class).newInstance(k, r.getHome());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalStateException("Error instantiating spss report class", e);
				}
				
			})
   			.addReportValues(r);
		}
		return spssReportMap.values();
	}

	/**
	 * Creates the SPSS variant of a modality report: Denormalise the report by creating additional year/month columns for each report value.
	 * In a SPSS report there is at most one record for each managed identity/modality combination.  
	 * @param <S> the SPSS variant of the report record 
	 * @param <R> the normal report record
	 * @param report The list of report records to convert 
	 * @param spssClazz The class of the SPSS report record to convert to.
	 * @return A collection of SPSS records, one record for each managed identity.
	 */
	protected <S extends SpssReportWithModality<R>, R extends ReportKeyWithModality> Collection<S> createSpssModalityReport(List<R> report, Class<S> spssClazz) {
		Map<String, S> spssReportMap = new LinkedHashMap<>();
		for (R r : report) {
    		spssReportMap.computeIfAbsent(r.getManagedIdentity(), k -> {
				try {
					// Create a report line with the managed identity, the home locality and the modality
					return spssClazz.getDeclaredConstructor(String.class, String.class, String.class).newInstance(k, r.getHome(), r.getModality());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalStateException("Error instantiating spss modality report class", e);
				}
				
			})
   			.addReportValues(r);
		}
		return spssReportMap.values();
	}
	
	protected void sendReports(String name, String reportDate, Map<String, String> reports) {
		log.info(String.format("Sending report '%s' to %s", name, reportRecipient));
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("subjectPrefix", subjectPrefix);
		valuesMap.put("reportDate", reportDate);
		valuesMap.put("name", name);
		StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
		String subject = substitutor.replace(SUBJECT);
		String body = substitutor.replace(BODY);
	    sendEmail(subject, body, reportRecipient, reports);
	}

	protected void sendEmail(String subject, String body, String recipient, Map<String, String> attachments) {
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
