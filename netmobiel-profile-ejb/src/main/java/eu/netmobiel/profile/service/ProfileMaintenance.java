package eu.netmobiel.profile.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.repository.ComplimentDao;
import eu.netmobiel.profile.repository.OldProfileDao;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.ReviewDao;

/**
 * Singleton startup bean for doing some maintenance on startup of the system.
 * 1. Migrate profiles to this profile service and assure all components know about all users.
 *  
 * @author Jaap Reitsma
 *
 */
@Singleton
@Startup
@Logging
public class ProfileMaintenance {

	@Inject
    private Logger log;

	@Inject
	private ProfileDao profileDao;

	@Inject
	private ReviewDao reviewDao;
	
	@Inject
	private ComplimentDao complimentDao;
    
	@Inject
	private OldProfileDao oldProfileDao;
	
    @Resource
    private SessionContext context;
    
	@PostConstruct
	public void initialize() {
//		log.info("Starting up the Profile Service, doing some maintenance tasks");
	}

	@Asynchronous
	public void processReportOnNetMobielUsers(Map<NetMobielModule, List<String>> moduleUsersMap) {
		// Collect all users, invert the mapping
		Set<String> allUsers = moduleUsersMap.values().stream()
				.flatMap(musers -> musers.stream())
				.collect(Collectors.toSet());
		log.info("Migrating the profiles: #" + allUsers.size());
		// For each user: Migrate the profile from the old profile service to the new one
		for (String mid : allUsers) {
			context.getBusinessObject(ProfileMaintenance.class).migrateProfile(mid);
		}
		// Naive 1 + N implementation for migration, but I don't care. There are not many to migrate.
		try {
			List<Review> allOldReviews = oldProfileDao.getReviews();
			log.info("Migrating the reviews: #" + allOldReviews.size());
			for (Review review : allOldReviews) {
				if (!reviewDao.findReviewByAttributes(review).isPresent()) {
					context.getBusinessObject(ProfileMaintenance.class).migrateReview(review);
				}
			}
		} catch (BusinessException e) {
			log.error("Failed to retrieve reviews for migration", e);
		}
		// Naive 1 + N implementation for migration, but I don't care. There are not many to migrate.
		try {
			List<Compliment> allOldCompliments = oldProfileDao.getCompliments();
			log.info("Migrating the compliments: #" + allOldCompliments.size());
			for (Compliment compliment : allOldCompliments) {
				if (!complimentDao.findComplimentByAttributes(compliment).isPresent()) {
					context.getBusinessObject(ProfileMaintenance.class).migrateCompliment(compliment);
				}
			}
		} catch (BusinessException e) {
			log.error("Failed to retrieve compliments for migration", e);
		}
		log.info("Migration of profiles finished");
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void migrateProfile(String managedIdentity) {
		if (!profileDao.userExists(managedIdentity)) {
			// Get the profile from the old profile service
			try {
				Profile profile = oldProfileDao.getProfile(managedIdentity);
				profileDao.save(profile);
			} catch (BusinessException e) {
				log.error("Failed to create profile for " + managedIdentity + " - " + e.toString());
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void migrateReview(Review review) {
		try {
	    	Profile rcvProfile = profileDao.getReferenceByManagedIdentity(review.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + review.getReceiver().getManagedIdentity()));
	    	review.setReceiver(rcvProfile);
	    	Profile sndProfile = profileDao.getReferenceByManagedIdentity(review.getSender().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + review.getSender().getManagedIdentity()));
	    	review.setSender(sndProfile);
			reviewDao.save(review);
		} catch (BusinessException e) {
			log.error("Failed to create review for " + review.getReceiver().getFamilyName() + " - " + e.toString());
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void migrateCompliment(Compliment compliment) {
		try {
	    	Profile rcvProfile = profileDao.getReferenceByManagedIdentity(compliment.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + compliment.getReceiver().getManagedIdentity()));
	    	compliment.setReceiver(rcvProfile);
	    	Profile sndProfile = profileDao.getReferenceByManagedIdentity(compliment.getSender().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + compliment.getSender().getManagedIdentity()));
	    	compliment.setSender(sndProfile);
	    	complimentDao.save(compliment);
		} catch (BusinessException e) {
			log.error("Failed to create compliment for " + compliment.getReceiver().getFamilyName() + " - " + e.toString());
		}
	}
}
