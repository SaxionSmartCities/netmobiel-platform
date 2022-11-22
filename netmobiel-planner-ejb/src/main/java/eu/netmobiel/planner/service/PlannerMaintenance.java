package eu.netmobiel.planner.service;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.here.search.HereSearchClient;

/**
 * Singleton startup bean for doing some maintenance on startup of the system
 * @author Jaap Reitsma
 *
 */
@Singleton
@Startup
public class PlannerMaintenance {

	@Inject
    private Logger log;
	@Inject
	private TripManager tripManager;
    @Inject
    private TripMonitor tripMonitor;
    
//    @Inject
//    private TransportOperatorRegistrar transportOperatorRegistrar;

	@Inject
    private HereSearchClient hereSearchClient;

	@Resource
    private TimerService timerService;

    private Timer geocodingTimer;

	@PostConstruct
	public void initialize() {
		log.info("Starting up the Planner, doing some maintenance tasks");
		tripMonitor.reviveTripMonitors();
		schedulePostalCodeGeocoding();
		// If enabled, at startup a spurious error might occur: 
		// RESTEASY008200: JSON Binding deserialization error: javax.json.bind.JsonbException: Can't infer a type for unmarshalling into: eu.netmobiel.tomp.api.model.OneOfassetTypeConditionsItems
		// Can't infer a type for unmarshalling into: eu.netmobiel.tomp.api.model.OneOfassetTypeConditionsItems
		// Jsonb is used instead of jackson despite of all exclusion in the deployment descriptor. Reason for the error is not known.
//		transportOperatorRegistrar.updateRegistry();
	}

	private static class GeocodingTimerInfo implements Serializable {
		private static final long serialVersionUID = 2140854309635368743L;
		private String title;

		public GeocodingTimerInfo(String info) {
			this.title = info;
		}

		@Override
		public String toString() {
			return title;
		}

		public String getTitle() {
			return title;
		}
	}
	
	/**
	 * Schedules an interval timer to do geocoding. To prevent rate limiting, do it slowly, no need
	 * for a hurry. The postal codes are used for reporting.
	 */
	private void schedulePostalCodeGeocoding() {
		TimerConfig tc = new TimerConfig(new GeocodingTimerInfo("Trip Geocoding Timer"), false); 
		geocodingTimer = timerService.createIntervalTimer(20 * 1000L, 15 * 1000L, tc);
		log.info("Started " + ((GeocodingTimerInfo) tc.getInfo()).getTitle());
	}

	/**
	 * Handle the timeout. In case of the geocoding timer, fetch the next geolocation. 
	 * If none is found then stop the geocoding interval timer.
	 * @param timer the timer causing the timeout.
	 */
	@Timeout
	public void onTimeout(Timer timer) {
		try {
			if (timer.getInfo() instanceof GeocodingTimerInfo) {
				boolean foundOne = fillNextMissingPostalCode();
				if (!foundOne) {
					// Ok, all is well, stop geocoding
					log.info("No more geocoding to do, stopping " + ((GeocodingTimerInfo) timer.getInfo()).getTitle());
					geocodingTimer.cancel();
				}
			}
		} catch(NoSuchObjectLocalException ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}
	
	/**
	 * Fetch the next geolocation without a postal code and reverse geocode it with the HERE service.
	 * In case of an error in the postal code (not an exception), the postal code is set to '??????', to prevent an endless
	 * loop. The postal code will be used for all fields and records with the exact same location.
	 * @return true if there was a missing postal code, false if the work is done.
	 */
	private boolean fillNextMissingPostalCode() {
		Optional<GeoLocation> location = tripManager.findNextMissingPostalCode();
		if (location.isPresent()) {
			String postalCode = hereSearchClient.getPostalCode6(location.get());
			if (postalCode == null || postalCode.isEmpty()) {
				log.warn("Error looking up postal code for " + location);
				// Give it a value to prevent endless loop
				postalCode = "??????";
			}
			int cnt = tripManager.assignPostalCode(location.get(), postalCode);
			log.info(String.format("Updated %d fields with postal code %s", cnt, postalCode));
		}
		return location.isPresent();
	}

}
