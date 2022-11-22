package eu.netmobiel.planner.model;

import javax.ws.rs.core.Response;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.rideshare.model.Ride;

public class RideshareResult {
	private PlannerReport report;
	private PagedResult<Ride> page;
	
	public RideshareResult() {
		
	}
	
	public RideshareResult(PlannerReport report) {
		 this.report = report;
	}

	public PlannerReport getReport() {
		return report;
	}

	public boolean hasError() {
		return report.getStatusCode() != Response.Status.OK.getStatusCode();
	}

	public PagedResult<Ride> getPage() {
		return page;
	}

	public void setPage(PagedResult<Ride> page) {
		this.page = page;
	}

}
