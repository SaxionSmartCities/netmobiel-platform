package eu.netmobiel.planner.service;

import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.OtpRoute;
import eu.netmobiel.planner.model.OtpStop;
import eu.netmobiel.planner.model.OtpTransfer;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;

/**
 * This bean has long running operations which are intended to run asynchronously. 
 * All database operations are delegated to OTPDataManager.
 *  
 * @author Jaap Reitsma
 *
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OTPMaintenanceService {
	private static final int CHUNK_SIZE_STOPS = 1000;
	private static final int CHUNK_SIZE_CLUSTERS = 500;
	private static final int CHUNK_SIZE_ROUTES= 100;
	private static final int CHUNK_SIZE_TRANSFERS= 1000;
	
    @Inject
    private Logger log;

    @Resource
    private SessionContext context;
    
    @Inject
    private OpenTripPlannerDao otpDao;

    @Inject
    private OTPDataManager  otpDataManager;

    private boolean maintenanceRunning = false;

    /**
     * Flag to control the scheduled update of the OTP data. Useful in production, not so useful in development.
     */
    @Resource(lookup = "java:global/planner/scheduledUpdatePublicTransportData")
    private Boolean automaticUpdatePublicTransportData;
    /**
     * Runs the update of the OTP data every monday morning. The cron job of building the graph has finished around 4:30 in the morning.
     */
	@Schedule(info = "Update OTP Data", dayOfWeek = "Mon", hour = "5", minute = "0", second = "0", persistent = true)
    public void timedUpdateOpenTripPlannerData() {
		try {
			if (automaticUpdatePublicTransportData) {
				startUpdatePublicTransportData();
			} else {
				log.info("Automatic update of OTP data is disabled by server configuration");
			}
		} catch (Exception ex) {
			log.error("Error during timed update of OTP data: " + ex.toString());
		}
	}
	
    private void updatePublicTransportStops() {
    	log.info("Fetch the stops and update");
        List<OtpStop> stops =  otpDao.fetchAllStops();
    	int nrChunks = stops.size() / CHUNK_SIZE_STOPS;
    	if (stops.size() % CHUNK_SIZE_STOPS > 0) {
    		nrChunks++;
    	}
    	otpDataManager.prepareUpdateStops();
    	for (int chunk = 0; chunk < nrChunks; chunk++) {
    		log.debug(String.format("Stops chunck %d/%d", chunk + 1, nrChunks));
    		List<OtpStop> sublist = stops.subList(chunk * CHUNK_SIZE_STOPS, Math.min((chunk + 1) * CHUNK_SIZE_STOPS, stops.size()));
    		// Force transaction demarcation
    		otpDataManager.bulkUpdateStops(sublist);
    	}
    	otpDataManager.finishUpdateStops();
    }

    private void updatePublicTransportClusters() {
    	log.info("Fetch the clusters and update");
        List<OtpCluster> clusters = otpDao.fetchAllClusters();
    	int nrChunks = clusters.size() / CHUNK_SIZE_CLUSTERS;
    	if (clusters.size() % CHUNK_SIZE_CLUSTERS > 0) {
    		nrChunks++;
    	}
    	otpDataManager.prepareUpdateClusters();
    	for (int chunk = 0; chunk < nrChunks; chunk++) {
    		log.debug(String.format("Clusters chunck %d/%d", chunk + 1, nrChunks));
    		List<OtpCluster> sublist = clusters.subList(chunk * CHUNK_SIZE_CLUSTERS, Math.min((chunk + 1) * CHUNK_SIZE_CLUSTERS, clusters.size()));
    		// Force transaction demarcation
    		otpDataManager.bulkUpdateClusters(sublist);
    	}
    	otpDataManager.finishUpdateClusters();
    }
    private void updatePublicTransportRoutes() {
    	log.info("Fetch the routes and update");
        List<OtpRoute> routes = otpDao.fetchAllRoutes();
    	int nrChunks = routes.size() / CHUNK_SIZE_ROUTES;
    	if (routes.size() % CHUNK_SIZE_ROUTES > 0) {
    		nrChunks++;
    	}
    	otpDataManager.prepareUpdateRoutes();
    	for (int chunk = 0; chunk < nrChunks; chunk++) {
    		log.debug(String.format("Routes chunck %d/%d", chunk + 1, nrChunks));
    		List<OtpRoute> sublist = routes.subList(chunk * CHUNK_SIZE_ROUTES, Math.min((chunk + 1) * CHUNK_SIZE_ROUTES, routes.size()));
    		// Force transaction demarcation
    		otpDataManager.bulkUpdateRoutes(sublist);
    	}
    	otpDataManager.finishUpdateRoutes();
    }

    @SuppressWarnings("unused")
	private void updatePublicTransportTransfers() {
    	log.info("Fetch the transfers and update");
        List<OtpTransfer> transfers = otpDao.fetchAllTransfers();
    	int nrChunks = transfers.size() / CHUNK_SIZE_TRANSFERS;
    	if (transfers.size() % CHUNK_SIZE_TRANSFERS > 0) {
    		nrChunks++;
    	}
    	for (int chunk = 0; chunk < nrChunks; chunk++) {
    		log.debug(String.format("Transfers chunck %d/%d", chunk + 1, nrChunks));
    		List<OtpTransfer> sublist = transfers.subList(chunk * CHUNK_SIZE_TRANSFERS, Math.min((chunk + 1) * CHUNK_SIZE_TRANSFERS, transfers.size()));
//    		// Force transaction demarcation
//    		context.getBusinessObject(OTPMaintenanceService.class).bulkUpdateTransfers(sublist);
    		otpDataManager.bulkUpdateTransfers(sublist);
    	}
    }

    @Asynchronous
    public void startUpdatePublicTransportData() {
    	if (maintenanceRunning) {
    		throw new IllegalStateException("Operation already running");
    	}
    	try {
    		maintenanceRunning = true;
    		log.debug("Start update public transport data");
	    	updatePublicTransportStops();
	    	updatePublicTransportClusters();
	    	updatePublicTransportRoutes();
	    	// Transfer does not add much info. Omit for now.
	//    	updatePublicTransportTransfers();
			otpDataManager.bulkUpdateClusterRoutes();
			otpDataManager.bulkUpdateStopRoutes();
    		log.debug("Update public transport data has completed succcessfully");
    	} catch (Exception ex) {
    		log.error("Update public transport data has completed with errors");
    		throw ex;
    	} finally {
    		maintenanceRunning = false;
    	}
    }

    public boolean isMaintenanceRunning() {
    	return maintenanceRunning;
    }
}
