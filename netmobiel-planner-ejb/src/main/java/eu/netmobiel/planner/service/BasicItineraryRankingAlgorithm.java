package eu.netmobiel.planner.service;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import eu.netmobiel.planner.model.Itinerary;

public class BasicItineraryRankingAlgorithm {
	private final static double TRANSFER_PENALTY = -30 * 60f;
	private final static double WAITING_TIME_PENALTY = -1f;
	private final static double WALK_DISTANCE_PENALTY = -1f;
	private final static double DURATION_PENALTY = -1f;
	private final static double DEPARTURE_PENALTY = -0.5f;
	private final static double ARRIVAL_PENALTY = -0.5f;
	
	/**
	 * Simple and rather ad hoc emission rating logic. The value represents emission in gram per traveller kilometer.
	 * Only an zero emission gives the highest rating (actually the inverse value). 
	 */
	private final static int EMISSION_RATE_RATINGS[] = new int[] { 0, 50, 100, 150, 200 };
	
	public void calculateScore(Itinerary it, Instant travelTime, boolean isArriveTime) {
		// The score is based on:
		// # of transfers: less is better
		// waiting time: less is better
		// total travel time: less is better
		// distance in time to intended departure or arrival time. Closer is better.
		double score = it.getTransfers() * TRANSFER_PENALTY +
				it.getWaitingTime() * WAITING_TIME_PENALTY +
				it.getWalkDistance() * WALK_DISTANCE_PENALTY +
				it.getDuration() * DURATION_PENALTY;
		if (isArriveTime) {
			score += Math.abs(Duration.between(travelTime, it.getArrivalTime()).getSeconds()) * ARRIVAL_PENALTY; 
		} else {
			score += Math.abs(Duration.between(travelTime, it.getDepartureTime()).getSeconds()) * DEPARTURE_PENALTY; 
		}
		it.setScore(score);
	}

	public void calculateSustainabilityRating(Itinerary it) {
		if (it.getAverageCo2EmissionRate() != null) {
			int er = it.getAverageCo2EmissionRate();
			int ix = IntStream.range(0, EMISSION_RATE_RATINGS.length)
					.filter(i -> er <= EMISSION_RATE_RATINGS[i])
					.findFirst()
					.orElse(EMISSION_RATE_RATINGS.length - 1);
			// Range ix is 0 (good) - 4 (bad), this is the inverse rating,
			it.setSustainabilityRating(5 - ix);
		}
	}

}
