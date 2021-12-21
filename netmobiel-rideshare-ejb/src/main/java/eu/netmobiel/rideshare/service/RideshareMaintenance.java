package eu.netmobiel.rideshare.service;

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
 * Singleton startup bean for doing some maintenance on startup of the system.
 * 1. Restart ride monitors if necessary
 * 2. Do some reverse geocoding to fetch the postal code for each ride, if necessary.
 *  
 * @author Jaap Reitsma
 *
 */
@Singleton
@Startup
public class RideshareMaintenance {

	@Inject
    private Logger log;

	@Inject
	private RideManager rideManager;
	@Inject
	private RideMonitor rideMonitor;
    
	@Inject
    private HereSearchClient hereSearchClient;
    
	@Resource
    private TimerService timerService;

    private Timer geocodingTimer;
    
	@PostConstruct
	public void initialize() {
		log.info("Starting up the Rideshare, doing some maintenance tasks");
		rideMonitor.reviveRideMonitors();
		schedulePostalCodeGeocoding();
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
		TimerConfig tc = new TimerConfig(new GeocodingTimerInfo("Ride Geocoding Timer"), false); 
		geocodingTimer = timerService.createIntervalTimer(15 * 1000L, 15 * 1000L, tc);
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
		Optional<GeoLocation> location = rideManager.findNextMissingPostalCode();
		if (location.isPresent()) {
			String postalCode = hereSearchClient.getPostalCode6(location.get());
			if (postalCode == null || postalCode.isEmpty()) {
				log.warn("Error looking up postal code for " + location);
				// Give it a value to prevent endless loop
				postalCode = "??????";
			}
			int cnt = rideManager.assignPostalCode(location.get(), postalCode);
			log.info(String.format("Updated %d fields with postal code %s", cnt, postalCode));
		}
		return location.isPresent();
	}
}
