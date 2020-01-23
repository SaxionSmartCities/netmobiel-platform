package eu.netmobiel.rideshare.repository;

import java.util.ArrayList;
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
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.Car_;
import eu.netmobiel.rideshare.model.User;

@ApplicationScoped
@Typed(CarDao.class)
public class CarDao extends AbstractDao<Car, Long> {

    @Inject @RideshareDatabase
    private EntityManager em;

    public CarDao() {
		super(Car.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public List<Car> findByDriver(User driver, Boolean deletedToo) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Car> cq = cb.createQuery(Car.class);
        Root<Car> cars = cq.from(Car.class);
        cq.select(cars);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predDriver = cb.equal(cars.get(Car_.driver), driver);
        predicates.add(predDriver);
        if (deletedToo == null || !deletedToo.booleanValue()) {
            Predicate predNotDeleted = cb.not(cb.isTrue(cars.get(Car_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        cq.orderBy(cb.asc(cars.get(Car_.id)));
        TypedQuery<Car> tq = em.createQuery(cq);
        return tq.getResultList();
    }
    
    public boolean exists(Car c) {
    	return em.createQuery("select count(*) from Car " + 
    			"where driver = :driver and registrationCountry = :country and licensePlate = :plate", Long.class)
    			.setParameter("driver", c.getDriver())
    			.setParameter("country", c.getRegistrationCountry())
    			.setParameter("plate", c.getLicensePlate())
    			.getSingleResult() != 0;
    }
    
    public Long getNrRideTemplatesAttached(Car car) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select count(t) from RideTemplate t where t.car = :car", Long.class)
    			.setParameter("car", car);
    	return tq.getSingleResult();
    }

}
