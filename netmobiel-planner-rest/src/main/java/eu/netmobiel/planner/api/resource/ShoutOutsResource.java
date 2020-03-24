package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.planner.api.ShoutOutsApi;
import eu.netmobiel.planner.api.mapping.PageMapper;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.service.TripManager;

@ApplicationScoped
public class ShoutOutsResource implements ShoutOutsApi {
	private static final Integer DEFAULT_DEP_ARR_RADIUS = 10000;
	
	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private PageMapper pageMapper;

    @EJB
    private TripManager tripManager;


	@Override
    public Response listShoutOuts(String location, OffsetDateTime startTime, Integer depArrRadius, Integer travelRadius, Integer maxResults, Integer offset) { 
    	Response rsp = null;
    	if (location == null) {
    		throw new BadRequestException("Missing mandatory parameter: location");
    	}
		try {
			Integer smallRadius = depArrRadius != null ? depArrRadius : DEFAULT_DEP_ARR_RADIUS; 
			PagedResult<Trip> result = tripManager.listShoutOuts(GeoLocation.fromString(location), 
					startTime != null ? startTime.toInstant() : Instant.now(), 
					smallRadius, travelRadius != null ? travelRadius : smallRadius, maxResults, offset);
			rsp = Response.ok(pageMapper.mapInDetail(result)).build();
		} catch (Exception e) {
			throw new InternalServerErrorException(e);
		}
    	return rsp;
	}


}
