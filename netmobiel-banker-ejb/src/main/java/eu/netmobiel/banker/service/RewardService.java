package eu.netmobiel.banker.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.filter.RewardFilter;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.IncentiveDao;
import eu.netmobiel.banker.repository.RewardDao;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.annotation.Updated;
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
	
    @Inject @Updated
	private Event<Reward> rewardUpdatedEvent;
	
    @Inject @Removed
	private Event<Reward> rewardRemovedEvent;

    /**
     * Lists the incentives, sorted from new to old descending.  
     * @param cursor the cursor 
     * @return A paged list of incentives
     * @throws BadRequestException
     */
    public PagedResult<Incentive> listIncentives(boolean disabledToo, Cursor cursor) throws BadRequestException {
    	cursor.validate(MAX_RESULTS, 0);
    	Long totalCount = incentiveDao.listIncentives(disabledToo, Cursor.COUNTING_CURSOR).getTotalCount();
    	List<Incentive> results = null;
    	if (!cursor.isCountingQuery() && totalCount > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = incentiveDao.listIncentives(disabledToo, cursor);
    		results = incentiveDao.loadGraphs(ids.getData(), null, Incentive::getId);
    	}
    	return new PagedResult<>(results, cursor, totalCount);
    }

    /**
     * Lists the rewards, sorted from new to old descending.
     * Security is handled in the rest layer.
     * @param graphName the entity graph to use for retrieval.
     * @param user the user for whom to retrieve the rewards
     * @param cancelledToo If true then include cancelled rewards in the listing    
     * @param cursor the cursor 
     * @return A paged list of incentives
     * @throws BadRequestException
     */
    public PagedResult<Reward> listRewards(String graphName, RewardFilter filter, Cursor cursor) throws BadRequestException {
    	filter.validate();
    	cursor.validate(MAX_RESULTS, 0);
    	Long totalCount = rewardDao.listRewards(filter, Cursor.COUNTING_CURSOR).getTotalCount();
    	List<Reward> results = null;
    	if (!cursor.isCountingQuery() && totalCount > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = rewardDao.listRewards(filter, cursor);
    		results = rewardDao.loadGraphs(ids.getData(), graphName, Reward::getId);
    	}
    	return new PagedResult<>(results, cursor, totalCount);
    }

    /**
     * Create a reward. This method is called by the system. 
     * @param incentive
     * @param recipient
     * @param fact
     * @param yield
     * @return
     * @throws NotFoundException
     */
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

    /**
     * Create a reward. This method is called by the system. 
     * @param incentive
     * @param recipient
     * @param fact
     * @param yield
     * @return
     * @throws NotFoundException
     */
    public Reward restoreReward(Reward reward, Integer yield) throws NotFoundException {
    	Reward rewarddb = rewardDao.loadGraph(reward.getId(), Reward.GRAPH_WITH_INCENTIVE_AND_RECIPIENT)
    			.orElseThrow(() -> new NotFoundException("No such reward: " + reward.getUrn()));
    	if (rewarddb.getCancelTime() == null) {
    		throw new IllegalStateException("Cannot restore a non-cancelled reward: " + rewarddb.getUrn());
    	}
    	int rewardAmount = rewarddb.getIncentive().calculateAmountToReward(yield);
    	rewarddb.setAmount(rewardAmount);
    	rewarddb.setRewardTime(Instant.now());
    	rewarddb.setCancelTime(null);
 		// Inform other parties of the creation the updated reward.
		rewardUpdatedEvent.fire(rewarddb);
    	return rewarddb;
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
     * Reverse the payment and optionally remove the reward.
     * @param reward
     * @param hard if true then remove the reward from the database. Note that the reward context in transaction and conversation becomes dangling in that case. 
     * @param paymentOnly if true then reverse payment only. If false, the reward is cancelled. Irrelevant if hard is set.
     * @throws NotFoundException 
     */
	public void removeReward(Long rid, boolean hard, boolean paymentOnly) throws NotFoundException {
		Reward rdb = rewardDao.loadGraph(rid, null)
       			.orElseThrow(() -> new NotFoundException("No such reward: " + rid));
		if (rdb.getCancelTime() == null) {
			rewardRemovedEvent.fire(rdb);
			if (!paymentOnly) {
				rdb.setCancelTime(Instant.now());
			}		
		}
		if (hard) {
			rewardDao.remove(rdb);
		} 
	}

}
