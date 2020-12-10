package eu.netmobiel.planner.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.planner.api.MaintenanceApi;
import eu.netmobiel.planner.api.model.OperationStatus;
import eu.netmobiel.planner.service.OTPMaintenanceService;

@ApplicationScoped
public class MaintenanceResource implements MaintenanceApi {
    @Inject
    private OTPMaintenanceService otpService;

    
	@Override
	public Response startOtpMaintenance() {
    	if (! otpService.isMaintenanceRunning()) {
        	otpService.startUpdatePublicTransportData();
    	}
    	return Response.status(Status.ACCEPTED).build();
	}

	@Override
	public Response getOtpMaintenanceStatus() {
    	OperationStatus status = new OperationStatus();
    	status.setIsRunning(otpService.isMaintenanceRunning());
		return Response.ok(status).build();
	}

}
