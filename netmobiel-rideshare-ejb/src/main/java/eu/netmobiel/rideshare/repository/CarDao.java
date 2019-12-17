package eu.netmobiel.rideshare.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Car;
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

    public List<Car> findByDriver(User driver) {
    	return em.createQuery("from Car where driver = :driver", Car.class)
    			.setParameter("driver", driver)
    			.getResultList();

    }
    
    public boolean exists(Car c) {
    	return em.createQuery("select count(*) from Car " + 
    			"where driver = :driver and registrationCountry = :country and licensePlate = :plate", Long.class)
    			.setParameter("driver", c.getDriver())
    			.setParameter("country", c.getRegistrationCountry())
    			.setParameter("plate", c.getLicensePlate())
    			.getSingleResult() != 0;
    }
}
