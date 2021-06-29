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
import eu.netmobiel.profile.model.Place;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.repository.PlaceDao;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Bean class for the Place service.  
 */
@Stateless
@Logging
@DeclareRoles({ "admin", "delegate" })
public class PlaceManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private PlaceDao placeDao;

	public Long createPlace(String managedId, Place place) throws BadRequestException, NotFoundException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	place.setProfile(profile);
		placeDao.save(place);
		return place.getId();
	}

	public @NotNull PagedResult<Place> listPlaces(String managedId, Cursor cursor) throws BadRequestException, NotFoundException {
		cursor.validate(MAX_RESULTS, 0);
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	PagedResult<Long> prs = placeDao.listPlaces(profile, Cursor.COUNTING_CURSOR);
    	List<Place> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = placeDao.listPlaces(profile, cursor);
    		results = placeDao.loadGraphs(pids.getData(), null, Place::getId);
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

	public Place getPlace(String managedId, Long placeId) throws NotFoundException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		boolean privileged = sessionContext.isCallerInRole("admin");
		Place place = placeDao.find(placeId)
				.orElseThrow(() -> new NotFoundException("No such place: " + placeId));
		if (! place.getProfile().getId().equals(profile.getId()) && ! privileged) {
			throw new SecurityException("You have no privilege to remove this place: " + placeId);
		}
		return place;
	}
	
	public void updatePlace(String managedId, Long placeId, Place place) throws BadRequestException, NotFoundException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
    	place.setProfile(profile);
		boolean privileged = sessionContext.isCallerInRole("admin");
		Place dbplace = placeDao.find(placeId)
				.orElseThrow(() -> new NotFoundException("No such place: " + place.getId()));
		if (! dbplace.getProfile().getId().equals(profile.getId()) && ! privileged) {
			throw new SecurityException("You have no privilege to update this place: " + place.getId());
		}
		place.setId(placeId);
		placeDao.merge(place);
	}

	public void removePlace(String managedId, Long placeId) throws NotFoundException {
		Place place = getPlace(managedId, placeId);
		placeDao.remove(place);
	}
}
