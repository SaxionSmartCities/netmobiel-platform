package eu.netmobiel.banker.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.IncentivesApi;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.service.RewardService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;

@ApplicationScoped
public class IncentivesResource implements IncentivesApi {

	@Inject
    private RewardService rewardService;
	
	@Inject
	private PageMapper pageMapper;

	@Override
	public Response getIncentives(Boolean disabledToo, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor(maxResults, offset);
	    	PagedResult<Incentive> results = rewardService.listIncentives(Boolean.TRUE.equals(disabledToo), cursor);
			rsp = Response.ok(pageMapper.mapIncentives(results)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

}
