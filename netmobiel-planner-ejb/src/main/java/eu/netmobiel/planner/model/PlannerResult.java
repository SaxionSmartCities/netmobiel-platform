package eu.netmobiel.planner.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

public class PlannerResult {
	private List<Itinerary> itineraries = new ArrayList<>();
	private PlannerReport report;
	
	public PlannerResult() {
		
	}
	
	public PlannerResult(PlannerReport report) {
		 this.report = report;
	}

	public List<Itinerary> getItineraries() {
		return itineraries;
	}

	public void addItineraries(Collection<Itinerary> itineraries) {
		itineraries.forEach(it -> it.getLegs().forEach(leg -> leg.setPlannerReport(report)));
		this.itineraries.addAll(itineraries);
	}

	public PlannerReport getReport() {
		return report;
	}

	public boolean hasError() {
		return report.getStatusCode() != Response.Status.OK.getStatusCode();
	}
}
