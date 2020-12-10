package eu.netmobiel.overseer.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.overseer.api.model.MaintenanceStatusType;
import eu.netmobiel.overseer.processor.ReportProcessor;

@Path("/maintenance")
@ApplicationScoped
public class MaintenanceResource {

    @Inject
    private ReportProcessor reportProcessor;

    @POST
    @Path("/report")
	public Response startReport() {
    	if (! reportProcessor.isJobRunning()) {
    		reportProcessor.startReport();
    	}
    	return Response.status(Status.ACCEPTED).build();
    }

    @GET
    @Path("/report")
    @Produces(MediaType.APPLICATION_JSON)
	public Response getReportStatus() {
    	MaintenanceStatusType status = new MaintenanceStatusType();
    	status.isRunning = reportProcessor.isJobRunning();
    	return Response.ok(status).build();
    }

}
