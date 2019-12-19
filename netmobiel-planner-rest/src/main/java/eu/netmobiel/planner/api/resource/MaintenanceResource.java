package eu.netmobiel.planner.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.planner.api.model.MaintenanceStatusType;
import eu.netmobiel.planner.service.OTPMaintenanceService;

@Path("/maintenance")
@ApplicationScoped
public class MaintenanceResource {
    @Inject
    private OTPMaintenanceService otpService;


    @POST
    @Path("/otpdatabase")
    public Response startMaintenance() {
    	if (! otpService.isMaintenanceRunning()) {
        	otpService.startUpdatePublicTransportData();
    	}
    	return Response.status(Status.ACCEPTED).build();
    }

    @GET
    @Path("/otpdatabase")
    @Produces(MediaType.APPLICATION_JSON)
    public MaintenanceStatusType getMaintenanceStatus() {
    	MaintenanceStatusType status = new MaintenanceStatusType();
    	status.isRunning = otpService.isMaintenanceRunning();
    	return status;
    }
}
