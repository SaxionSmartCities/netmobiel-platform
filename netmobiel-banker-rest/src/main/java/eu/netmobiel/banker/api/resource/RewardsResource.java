package eu.netmobiel.banker.api.resource;

import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.RewardsApi;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.mapping.RewardMapper;
import eu.netmobiel.banker.filter.RewardFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.RewardService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * Generic rewards handling. 
 * All calls requrie admin or treasurer role.
 *  
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class RewardsResource implements RewardsApi {

	@Inject
    private RewardService rewardService;
	
	@Inject
    private BankerUserManager userManager;

	@Inject
	private PageMapper pageMapper;

	@Inject
	private RewardMapper rewardMapper;

	@Override
	public Response getRewards(String userId, OffsetDateTime since, OffsetDateTime until, Boolean paid, 
			String type, Boolean cancelled, String sortDir, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			BankerUser user = userId == null ? null : userManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
			RewardFilter filter = new RewardFilter(user, since, until, paid, type, cancelled, sortDir);
			Cursor cursor = new Cursor(maxResults, offset);
			
	    	PagedResult<Reward> results = rewardService.listRewards(Reward.GRAPH_WITH_INCENTIVE_AND_RECIPIENT, filter, cursor);
			rsp = Response.ok(pageMapper.mapRewardsDetailed(results)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

	@Override
	public Response getReward(String rewardId) {
		Response rsp = null;
		try {
        	Long rid = UrnHelper.getId(Account.URN_PREFIX, rewardId);
        	Reward reward = rewardService.getReward(rid);
			rsp = Response.ok(rewardMapper.mapWithDetails(reward)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response deleteReward(String rewardId, Boolean hard, Boolean paymentOnly) {
		Response rsp = null;
		try {
        	Long rid = UrnHelper.getId(Account.URN_PREFIX, rewardId);
        	rewardService.removeReward(rid, Boolean.TRUE.equals(hard), Boolean.TRUE.equals(paymentOnly));
			rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
