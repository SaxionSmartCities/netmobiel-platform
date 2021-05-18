package eu.netmobiel.profile.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.profile.api.ComplimentsApi;
import eu.netmobiel.profile.api.ReviewsApi;
import eu.netmobiel.profile.api.mapping.ReviewMapper;
import eu.netmobiel.profile.api.model.ReviewResponse;
import eu.netmobiel.profile.filter.ReviewFilter;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.service.ReviewManager;

@ApplicationScoped
public class ReviewsResource implements ReviewsApi {

	@Inject
	private ReviewManager reviewManager;

	@Inject
	private ReviewMapper mapper;


	@Override
	public Response createReview(eu.netmobiel.profile.api.model.Review review) {
		Response rsp = null;
		try {
			Review r = mapper.map(review);
	    	Long id = reviewManager.createReview(r);
			rsp = Response.created(UriBuilder.fromResource(ComplimentsApi.class)
					.path(ReviewsApi.class.getMethod("getReview", String.class)).build(id)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getReview(String reviewId) {
		Response rsp = null;
		try {
	    	Review result = reviewManager.getReview(Long.parseLong(reviewId));
			rsp = Response.ok(mapper.map(result)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getReviews(String senderId, String receiverId) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor();
			ReviewFilter filter = new ReviewFilter(receiverId, senderId);
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
	public Response updateReview(String reviewId, eu.netmobiel.profile.api.model.Review review) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Response deleteReview(String reviewId) {
		Response rsp = null;
		try {
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
