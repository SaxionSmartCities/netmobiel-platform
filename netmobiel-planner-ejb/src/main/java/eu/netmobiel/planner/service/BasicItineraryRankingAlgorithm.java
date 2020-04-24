package eu.netmobiel.planner.service;

import java.time.Duration;
import java.time.Instant;

import eu.netmobiel.planner.model.Itinerary;

public class BasicItineraryRankingAlgorithm {
	private final static double TRANSFER_PENALTY = -30 * 60f;
	private final static double WAITING_TIME_PENALTY = -1f;
	private final static double WALK_DISTANCE_PENALTY = -1f;
	private final static double DURATION_PENALTY = -1f;
	private final static double DEPARTURE_PENALTY = -0.5f;
	private final static double ARRIVAL_PENALTY = -0.5f;
	
	public void calculateScore(Itinerary it, Instant fromDate, Instant toDate) {
		// The score is based on:
		// # of transfers: less is better
		// waiting time: less is better
		// total travel time: less is better
		// distance in time to intended departure or arrival time. Closer is better.
		double score = it.getTransfers() * TRANSFER_PENALTY +
				it.getWaitingTime() * WAITING_TIME_PENALTY +
				it.getWalkDistance() * WALK_DISTANCE_PENALTY +
				it.getDuration() * DURATION_PENALTY;
		if (fromDate != null) {
			score += Math.abs(Duration.between(fromDate, it.getDepartureTime()).getSeconds()) * DEPARTURE_PENALTY; 
		}
		if (toDate != null) {
			score += Math.abs(Duration.between(toDate, it.getArrivalTime()).getSeconds()) * ARRIVAL_PENALTY; 
		}
		it.setScore(score);
	}
}
