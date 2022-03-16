package eu.netmobiel.banker.service;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.IncentiveDao;
import eu.netmobiel.banker.repository.RewardDao;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;

/**
 * Stateless bean for the management of the ledger.
 * 
 * TODO: Security
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
public class RewardService {
	public static final Integer MAX_RESULTS = 10; 

    @SuppressWarnings("unused")
	@Inject
    private Logger log;
    
    @Resource
	protected SessionContext sessionContext;

    @Inject
    private BankerUserDao userDao;

    @Inject
    private RewardDao rewardDao;

    @Inject
    private IncentiveDao incentiveDao;

    @Inject @Created
	private Event<Reward> rewardCreatedEvent;
	
    @Inject @Removed
	private Event<Reward> rewardRemovedEvent;

    public Reward createReward(Incentive incentive, NetMobielUser recipient, String fact, Integer yield) throws NotFoundException {
    	BankerUser rcp = userDao.findByManagedIdentity(recipient.getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such user: " + recipient.getManagedIdentity()));
    	int rewardAmount = incentive.calculateAmountToReward(yield);
    	Reward reward = new Reward(incentive, rcp, fact, rewardAmount);
    	rewardDao.save(reward);
		// Inform other parties of the creation the new reward.
		rewardCreatedEvent.fire(reward);
    	return reward;
    }

    public Optional<Incentive> lookupIncentive(String incentiveCode) {
    	return incentiveCode == null ? Optional.empty() : incentiveDao.findByCode(incentiveCode);
    }
    
    public Optional<Reward> lookupRewardByFact(Incentive incentive, NetMobielUser recipient, String fact) throws NotFoundException {
    	BankerUser rcp = userDao.findByManagedIdentity(recipient.getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such user: " + recipient.getManagedIdentity()));
    	return rewardDao.findByFact(incentive, rcp, fact);
    }
 
    public Reward getReward(Long id) throws NotFoundException {
    	return rewardDao.loadGraph(id, Reward.GRAPH_WITH_INCENTIVE)
       			.orElseThrow(() -> new NotFoundException("No such reward: " + id));
    }
    
    /**
     * Lists the pending rewards, sorted from old to new ascending. Redeemable rewards are never pending.  
     * @param cursor the cursor 
     * @return A paged list of pending rewards
     * @throws BadRequestException
     */
    public PagedResult<Reward> listPendingRewards(Cursor cursor) throws BadRequestException {
    	cursor.validate(MAX_RESULTS, MAX_RESULTS);
    	PagedResult<Long> rwds = rewardDao.listPendingRewards(Cursor.COUNTING_CURSOR);
    	List<Reward> results = null;
    	if (!cursor.isCountingQuery() && rwds.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> rids = rewardDao.listPendingRewards(cursor);
    		results = rewardDao.loadGraphs(rids.getData(), null, Reward::getId);
    	}
    	return new PagedResult<>(results, cursor, rwds.getTotalCount());
    }

    /**
     * Reverse the payment and optionally remove the reward.
     * @param reward
     * @param paymentOnly
     * @throws NotFoundException 
     */
	public void withdrawReward(Reward reward, boolean paymentOnly) throws NotFoundException {
		rewardRemovedEvent.fire(reward);
		if (!paymentOnly) {
			Reward rdb = rewardDao.find(reward.getId(), null)
	       			.orElseThrow(() -> new NotFoundException("No such reward: " + reward.getId()));
			rewardDao.remove(rdb);
		}
	}

}
