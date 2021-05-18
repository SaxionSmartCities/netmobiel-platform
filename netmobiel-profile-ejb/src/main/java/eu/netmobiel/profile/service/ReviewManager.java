package eu.netmobiel.profile.service;

import java.security.Principal;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.filter.ReviewFilter;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.ReviewDao;

/**
 * Bean class for the Review service.  
 */
@Stateless
@Logging
@DeclareRoles({ "admin", "delegate" })
public class ReviewManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private ReviewDao reviewDao;
    

    public ReviewManager() {
    }

	public Long createReview(Review review) throws BadRequestException, NotFoundException {
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (review.getReceiver() == null) {
			throw new BadRequestException("Review receiver is a mandatory parameter");
		} else {
	    	Profile yourProfile = profileDao.getReferenceByManagedIdentity(review.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + review.getReceiver().getManagedIdentity()));
	    	review.setReceiver(yourProfile);
		}
		if (me.getName().equals(review.getReceiver().getManagedIdentity())) {
			throw new BadRequestException("You cannot review yourself");
		}
		if (review.getSender() == null) {
	    	Profile myProfile = profileDao.getReferenceByManagedIdentity(me.getName())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + me.getName()));
	    	review.setSender(myProfile);
		} else if (!me.getName().equals(review.getSender().getManagedIdentity()) && !privileged) {
			new SecurityException("You have no privilege to review on behalf of someone else");
		}
		if (review.getReview() == null) {
			throw new BadRequestException("Review text is a mandatory parameter");
		}
		reviewDao.save(review);
		return review.getId();
	}

	public @NotNull PagedResult<Review> listReviews(ReviewFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
		Principal me = sessionContext.getCallerPrincipal();
		boolean privileged = sessionContext.isCallerInRole("admin");
		if (! privileged && filter.getReceiver() != null && !filter.getReceiver().equals(me.getName())) {
			new SecurityException("You have no privilege to list reviews received by someone else");
		}
		if (! privileged && filter.getSender() != null && !filter.getSender().equals(me.getName())) {
			new SecurityException("You have no privilege to list reviews sent by someone else");
		}
		if (! privileged && filter.getReceiver() == null) {
			filter.setReceiver(me.getName());
		}
		filter.validate();
    	PagedResult<Long> prs = reviewDao.listReviews(filter, Cursor.COUNTING_CURSOR);
    	List<Review> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = reviewDao.listReviews(filter, cursor);
    		results = reviewDao.loadGraphs(pids.getData(), Review.LIST_REVIEWS_ENTITY_GRAPH, Review::getId);
    	}
    	return new PagedResult<Review>(results, cursor, prs.getTotalCount());
	}

	public Review getReview(Long reviewId) throws NotFoundException {
		return reviewDao.find(reviewId)
				.orElseThrow(() -> new NotFoundException("No such review: " + reviewId));
	}

	@RolesAllowed({ "admin" })
	public void removeReview(Long reviewId) throws NotFoundException {
		Review c = reviewDao.find(reviewId)
				.orElseThrow(() -> new NotFoundException("No such review: " + reviewId));
		reviewDao.remove(c);
	}

}
