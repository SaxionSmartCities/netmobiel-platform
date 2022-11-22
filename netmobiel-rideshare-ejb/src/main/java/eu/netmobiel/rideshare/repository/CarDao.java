package eu.netmobiel.rideshare.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import eu.netmobiel.rideshare.model.RideshareUser;

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

    public List<Car> findByDriver(RideshareUser driver, Boolean deletedToo) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Car> cq = cb.createQuery(Car.class);
        Root<Car> cars = cq.from(Car.class);
        cq.select(cars);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predDriver = cb.equal(cars.get(Car_.driver), driver);
        predicates.add(predDriver);
        if (deletedToo == null || !deletedToo.booleanValue()) {
            Predicate predNotDeleted = cb.or(cb.isNull(cars.get(Car_.deleted)), cb.isFalse(cars.get(Car_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        cq.orderBy(cb.asc(cars.get(Car_.id)));
        TypedQuery<Car> tq = em.createQuery(cq);
        return tq.getResultList();
    }

    public Optional<Car> findByDriverandPlate(RideshareUser driver, String countryCode, String licensePlateRaw) {
    	TypedQuery<Car> tq = em.createQuery(
    			"from Car c where driver = :driver and registrationCountry = :country and licensePlateRaw = :plate", Car.class)
    			.setParameter("driver", driver)
    			.setParameter("country", countryCode)
    			.setParameter("plate", licensePlateRaw);
    	List<Car> cars = tq.getResultList();
    	return cars.isEmpty() ? Optional.empty() : Optional.of(cars.get(0));
    }

    public boolean exists(Car c) {
    	return em.createQuery("select count(*) from Car " + 
    			"where driver = :driver and registrationCountry = :country and licensePlateRaw = :plate", Long.class)
    			.setParameter("driver", c.getDriver())
    			.setParameter("country", c.getRegistrationCountry())
    			.setParameter("plate", c.getLicensePlateRaw())
    			.getSingleResult() != 0;
    }
    
    public Long getNrRideTemplatesAttached(Car car) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select count(t) from RideTemplate t where t.car = :car", Long.class)
    			.setParameter("car", car);
    	return tq.getSingleResult();
    }

    public Long getNrRidesAttached(Car car) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select count(r) from Ride r where r.car = :car", Long.class)
    			.setParameter("car", car);
    	return tq.getSingleResult();
    }

    public boolean isDrivenBy(Car car, RideshareUser driver) {
    	return em.createQuery("select count(*) from Car c " + 
    			"where c = :car and c.driver = :driver", Long.class)
    			.setParameter("car", car)
    			.setParameter("driver", driver)
    			.getSingleResult() == 1;
    }
}
