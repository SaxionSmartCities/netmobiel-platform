package eu.netmobiel.rideshare.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Booking_;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.User;

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
    	return em.createQuery("from Booking where ride = :ride", Booking.class)
    			.setParameter("ride", ride)
    			.getResultList();

    }
    
    public Boolean hasBookings(Ride ride) {
    	Boolean exists = em.createQuery("select exists(select 1 from Booking where ride = :ride)", Boolean.class)
    			.setParameter("ride", ride)
    			.getSingleResult();
    	return exists != null && exists;
    }
    
    public List<Booking> findByPassenger(User passenger, boolean cancelledToo, String graphName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Booking> cq = cb.createQuery(Booking.class);
        Root<Booking> bookings = cq.from(Booking.class);
        cq.select(bookings);
        Predicate predPassenger = cb.equal(bookings.get(Booking_.passenger), passenger);
        if (cancelledToo) {
        	cq.where(predPassenger);
        } else {
            Predicate predNotCancelled = cb.notEqual(bookings.get(Booking_.state), BookingState.CANCELLED);
        	cq.where(cb.and(predPassenger, predNotCancelled));
        }
        cq.orderBy(cb.desc(bookings.get(Booking_.ride).get(Ride_.departureTime)));
        TypedQuery<Booking> tq = em.createQuery(cq);
        if (graphName != null) {
        	tq.setHint(JPA_HINT_LOAD, em.getEntityGraph(graphName));
        }
        return tq.getResultList();
    }

}
