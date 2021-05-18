package eu.netmobiel.profile.service;

import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
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
		if (review.getReceiver() == null) {
			throw new BadRequestException("Review receiver is a mandatory parameter");
		} else {
	    	Profile rcvProfile = profileDao.getReferenceByManagedIdentity(review.getReceiver().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + review.getReceiver().getManagedIdentity()));
	    	review.setReceiver(rcvProfile);
		}
		if (review.getSender() == null) {
			throw new BadRequestException("Review sender is a mandatory parameter");
		} else {
	    	Profile sndProfile = profileDao.getReferenceByManagedIdentity(review.getSender().getManagedIdentity())
	    			.orElseThrow(() -> new NotFoundException("No such profile: " + review.getSender().getManagedIdentity()));
	    	review.setSender(sndProfile);
		}
		if (review.getReview() == null) {
			throw new BadRequestException("Review text is a mandatory parameter");
		}
		reviewDao.save(review);
		return review.getId();
	}

	public @NotNull PagedResult<Review> listReviews(ReviewFilter filter, Cursor cursor) throws BadRequestException {
		cursor.validate(MAX_RESULTS, 0);
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

	public void removeReview(Long reviewId) throws NotFoundException {
		Review c = reviewDao.find(reviewId)
				.orElseThrow(() -> new NotFoundException("No such review: " + reviewId));
		reviewDao.remove(c);
	}

}
