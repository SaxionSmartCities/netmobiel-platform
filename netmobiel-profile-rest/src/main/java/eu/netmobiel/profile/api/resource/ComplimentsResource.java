package eu.netmobiel.profile.api.resource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import eu.netmobiel.profile.api.mapping.ComplimentMapper;
import eu.netmobiel.profile.api.model.ComplimentTypesResponse;
import eu.netmobiel.profile.filter.ComplimentFilter;
import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.ComplimentType;
import eu.netmobiel.profile.service.ProfileManager;

@ApplicationScoped
public class ComplimentsResource implements ComplimentsApi {

	@Inject
	private ProfileManager profileManager;

	@Inject
	private ComplimentMapper mapper;

	@Override
	public Response createCompliment(eu.netmobiel.profile.api.model.Compliment compliment) {
		Response rsp = null;
		try {
			Compliment c = mapper.map(compliment);
	    	Long id = profileManager.createCompliment(c);
			rsp = Response.created(UriBuilder.fromResource(ComplimentsApi.class)
					.path(ComplimentsApi.class.getMethod("getCompliment", String.class)).build(id)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException | NoSuchMethodException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getCompliment(String complimentId) {
		Response rsp = null;
		try {
	    	Compliment result = profileManager.getCompliment(Long.parseLong(complimentId));
			rsp = Response.ok(mapper.map(result)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getCompliments(String senderId, String receiverId) {
		Response rsp = null;
		try {
			Cursor cursor = new Cursor();
			ComplimentFilter filter = new ComplimentFilter(receiverId, senderId);
	    	PagedResult<Compliment> results = profileManager.listCompliments(filter, cursor);
			rsp = Response.ok(mapper.map(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response updateCompliment(String complimentId, eu.netmobiel.profile.api.model.Compliment compliment) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Response deleteCompliment(String complimentId) {
		Response rsp = null;
		try {
	    	profileManager.removeCompliment(Long.parseLong(complimentId));
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

	@Override
	public Response getComplimentTypes() {
		ComplimentTypesResponse ctr = new ComplimentTypesResponse();
		List<String> types = Stream.of(ComplimentType.values())
				.map(dt -> mapper.map(dt).value())
				.collect(Collectors.toList());
		ctr.getComplimentTypes().addAll(types);
		return Response.ok(ctr).build();
	}

	@Override
	public Response getComplimentsOldskool() {
		throw new UnsupportedOperationException("To be removed");
	}

}
