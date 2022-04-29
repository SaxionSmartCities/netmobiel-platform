package eu.netmobiel.planner.api.resource;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.planner.api.UsersApi;
import eu.netmobiel.planner.api.mapping.UserMapper;
import eu.netmobiel.planner.api.model.UserReport;
import eu.netmobiel.planner.model.ModalityUsage;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.planner.service.TripManager;

/**
 * Implementation for the /users endpoint. The security has been placed in this handler. The service itself
 * does not impose restrictions.
 * 
 * The header parameter xDelegator is extracted by the generated Api, but remains unsued. The implementation uses a CDI method to 
 * produce and inject the security identity. 
 *
 * @author Jaap Reitsma
 *
 */
@RequestScoped
public class UsersResource extends PlannerResource implements UsersApi {

	@Inject
	private UserMapper userMapper;
	
    @Inject
    private TripManager tripManager;

	@Inject
    private PlannerUserManager userManager;

    @Inject
	private SecurityIdentity securityIdentity;

    @Context
    private HttpServletRequest request;
    
	@Override
	public Response getUserReport(String xDelegator, String userId) {
    	Response rsp = null;
		try {
			UserReport report = new UserReport();
			CallingContext<PlannerUser> context = userManager.findOrRegisterCallingContext(securityIdentity);
    		PlannerUser user = null;
	    	if ("me".equals(userId)) {
	    		user = context.getEffectiveUser();
	    	} else {
	    		user = userManager.resolveUrn(userId)
	    				.orElseThrow(() -> new IllegalStateException("Didn't expect user null from " + userId));
	    	}
	    	List<ModalityUsage> domReportPassenger = tripManager.reportTripModalityUseAsPassenger(user);
	    	report.setTripsAsPassenger(userMapper.map(domReportPassenger));
	    	List<ModalityUsage> domReportDriver = tripManager.reportTripModalityUseAsDriver(user);
	    	report.setTripsAsDriver(userMapper.map(domReportDriver));
	    	rsp = Response.ok(report).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

}
