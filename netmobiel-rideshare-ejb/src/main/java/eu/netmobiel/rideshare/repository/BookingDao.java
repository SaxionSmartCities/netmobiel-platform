package eu.netmobiel.rideshare.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Booking_;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideshareUser;

@ApplicationScoped
@Typed(BookingDao.class)
public class BookingDao extends AbstractDao<Booking, Long> {

    @Inject @RideshareDatabase
    private EntityManager em;

    public BookingDao() {
		super(Booking.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public List<Booking> findByRide(Ride ride) {
    	return findByRide(ride, null, null);
    }
    
    public List<Booking> findByRide(Ride ride, String hintName, Object hintValue) {
    	TypedQuery<Booking> tq = em.createQuery("from Booking where ride = :ride", Booking.class)
    			.setParameter("ride", ride);
    	if (hintName != null) {
    		tq.setHint(hintName, hintValue);
    	}
    	return tq.getResultList();
    }

    public Boolean hasBookings(Ride ride) {
    	Boolean exists = em.createQuery("select exists(select 1 from Booking where ride = :ride)", Boolean.class)
    			.setParameter("ride", ride)
    			.getSingleResult();
    	return exists != null && exists;
    }
    
    public PagedResult<Long> findByPassenger(RideshareUser passenger, Instant since, Instant until, boolean cancelledToo, Integer maxResults, Integer offset) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Booking> bookings = cq.from(Booking.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predPassenger = cb.equal(bookings.get(Booking_.passenger), passenger);
        predicates.add(predPassenger);
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(bookings.get(Booking_.departureTime), since);
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThanOrEqualTo(bookings.get(Booking_.departureTime), until);
	        predicates.add(predUntil);
        }        
        if (!cancelledToo) {
            Predicate predNotCancelled = cb.notEqual(bookings.get(Booking_.state), BookingState.CANCELLED);
            predicates.add(predNotCancelled);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(bookings.get(Booking_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(bookings.get(Booking_.id));
            cq.orderBy(cb.desc(bookings.get(Booking_.departureTime)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

	@Override
	public List<Booking> fetch(List<Long> ids, String graphName) {
		// Create an identity map using the generic fetch. Rows are returned, but not necessarily in the same order
		Map<Long, Booking> resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Booking::getId, Function.identity()));
		// Now return the rows in the same order as the ids.
		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
	}

}
