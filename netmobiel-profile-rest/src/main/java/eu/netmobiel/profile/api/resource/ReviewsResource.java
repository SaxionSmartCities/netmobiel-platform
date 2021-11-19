package eu.netmobiel.profile.api.resource;

import java.time.Instant;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.ResponseBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.ComplimentsApi;
import eu.netmobiel.profile.api.ReviewsApi;
import eu.netmobiel.profile.api.mapping.ReviewMapper;
import eu.netmobiel.profile.api.model.ReviewResponse;
import eu.netmobiel.profile.filter.ReviewFilter;
import eu.netmobiel.profile.model.Compliments;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.service.ReviewManager;

@RequestScoped
public class ReviewsResource extends BasicResource implements ReviewsApi {

	@Inject
	private ReviewManager reviewManager;

	@Inject
	private ReviewMapper mapper;

	@Context
	private HttpServletRequest request;

    private void validateInput(Review r) throws BadRequestException, NotFoundException {
		if (r.getContext() == null) {
			throw new BadRequestException("Review context is a mandatory parameter");
		} 
		if (r.getReceiver() == null) {
			throw new BadRequestException("Review receiver is a mandatory parameter");
		} 
		final boolean privileged = request.isUserInRole("admin"); 
		String me = securityIdentity.getEffectivePrincipal().getName();
		if (me.equals(r.getReceiver().getManagedIdentity())) {
			throw new BadRequestException("You cannot review yourself");
		}
		if (r.getSender() != null ) {
			if (! privileged) {
				if (!me.equals(r.getSender().getManagedIdentity())) {
					throw new SecurityException("You have no privilege to review on behalf of this user: " + r.getSender().getManagedIdentity());
				}
			}
		} else {
			r.setSender(new Profile(me));
		}
		// Only admin can set published time.
		if (!privileged || r.getPublished() == null) {
			r.setPublished(Instant.now());
		}
    }

	@Override
	public Response createReview(String xDelegator, eu.netmobiel.profile.api.model.Review review) {
		ResponseBuilder rspb = null;
		try {
			Review r = mapper.map(review);
			validateInput(r);
			Optional<Review> currentReview = reviewManager.lookupReview(r.getReceiver().getManagedIdentity(), r.getContext());
			Long id = null;
			if (currentReview.isEmpty()) {
				id = reviewManager.createReview(r); 
				rspb = Response.created(UriBuilder.fromResource(ReviewsApi.class)
						.path(ReviewsApi.class.getMethod("getReview", String.class, String.class)).build(id));
			} else {
				id = currentReview.get().getId();
				reviewManager.updateReview(id, r);
				rspb = Response.noContent();
			}
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rspb.build();
	}

	@Override
	public Response getReview(String xDelegator, String reviewId) {
		Response rsp = null;
		try {
	    	Review r = reviewManager.getReview(Long.parseLong(reviewId));
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && !r.getReceiver().getManagedIdentity().equals(me) && !r.getSender().getManagedIdentity().equals(me)) {
				throw new SecurityException("You have no privilege to inspect a review that is not written by you or attributed to you");
			}
			rsp = Response.ok(mapper.map(r)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getReviews(String xDelegator, String senderId, String receiverId) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor();
			ReviewFilter filter = new ReviewFilter(resolveIdentity(xDelegator, receiverId), resolveIdentity(xDelegator, senderId));
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && filter.getReceiver() != null && !filter.getReceiver().equals(me)) {
				throw new SecurityException("You have no privilege to list reviews received by someone else");
			}
			if (! privileged && filter.getSender() != null && !filter.getSender().equals(me)) {
				throw new SecurityException("You have no privilege to list reviews sent by someone else");
			}
			if (! privileged && filter.getReceiver() == null) {
				filter.setReceiver(me);
			}
	    	PagedResult<Review> results = reviewManager.listReviews(filter, cursor);
	    	ReviewResponse rr = new ReviewResponse();
	    	rr.setReviews(mapper.map(results.getData()));
	    	rr.setMessage("Success");
	    	rr.setSuccess(true);
			rsp = Response.ok(rr).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updateReview(String xDelegator, String reviewId, eu.netmobiel.profile.api.model.Review review) {
		Response rsp = null;
		try {
			Long rid = Long.parseLong(reviewId);
	    	Review r = reviewManager.getReview(rid);
	    	validateInput(r);
	    	r = mapper.map(review);
			reviewManager.updateReview(rid, r);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response deleteReview(String xDelegator, String reviewId) {
		Response rsp = null;
		try {
	    	Review r = reviewManager.getReview(Long.parseLong(reviewId));
			String me = securityIdentity.getEffectivePrincipal().getName();
			final boolean privileged = request.isUserInRole("admin"); 
			if (! privileged && !r.getSender().getManagedIdentity().equals(me)) {
				throw new SecurityException("You have no privilege to remove a review that is not written by you");
			}
	    	reviewManager.removeReview(Long.parseLong(reviewId));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

//	@Override
//	public Response getReviewsNewSkool() {
//		Response rsp = null;
//		try {
//			Cursor cursor = new Cursor();
//			ReviewFilter filter = new ReviewFilter();
//	    	PagedResult<Review> results = profileManager.listReviews(filter, cursor);
//			rsp = Response.ok(mapper.map(results)).build();
//		} catch (IllegalArgumentException e) {
//			throw new BadRequestException(e);
//		} catch (BusinessException e) {
//			throw new WebApplicationException(e);
//		}
//		return rsp;
//		throw new UnsupportedOperationException("Not yet implemented");
//	}

}
