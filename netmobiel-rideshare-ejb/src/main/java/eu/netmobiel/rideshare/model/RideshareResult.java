package eu.netmobiel.rideshare.model;

import javax.ws.rs.core.Response;

import eu.netmobiel.commons.model.PagedResult;

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
