package eu.netmobiel.profile.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.repository.UserDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.UserRole;


@ApplicationScoped
@Typed(ProfileDao.class)
public class ProfileDao extends UserDao<Profile> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
    public ProfileDao() {
		super(Profile.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	
	@Override
	public void remove(Profile profile) {
		profile.getAddresses().forEach(addr -> getEntityManager().remove(addr));
		super.remove(profile);
	}

	/**
	 * Search for drivers that are eligible to drive a potential passenger to his/her destination.
	 * @param pickup the pickup location of the passenger. 
	 * @param dropOff the drop-off location of the passenger.
	 * @param driverMaxRadiusMeter The radius of the circles that limits the eligibility of the the driver 
	 * 			by demanding his living location to be in both the two large circles around the pickup and drop-off location. 
	 * @param driverNeighbouringRadiusMeter The radius of the circles that limits the eligibility of the the driver 
	 * 			by demanding his living location to be in the neighbourhood of one of the pickup or drop-off locations.
	 * @return A list of profiles of potential drivers, possibly empty.
	 * @throws BusinessException In case of trouble.
	 */
    public List<Profile> searchShoutOutProfiles(GeoLocation pickup, GeoLocation dropOff, int driverMaxRadiusMeter, int driverNeighbouringRadiusMeter) {
    	if (driverMaxRadiusMeter < driverNeighbouringRadiusMeter) {
    		throw new IllegalArgumentException("driverMaxRadiusMeter must be equal or greater than driverNeighbouringRadiusMeter");
    	}
    	Polygon pickupLargeCircle = EllipseHelper.calculateCircle(pickup.getPoint(), driverMaxRadiusMeter);
    	Polygon dropOffLargeCircle = EllipseHelper.calculateCircle(dropOff.getPoint(), driverMaxRadiusMeter);
    	Polygon pickupSmallCircle = EllipseHelper.calculateCircle(pickup.getPoint(), driverNeighbouringRadiusMeter);
    	Polygon dropOffSmallCircle = EllipseHelper.calculateCircle(dropOff.getPoint(), driverNeighbouringRadiusMeter);
//    	if (logger.isDebugEnabled()) {
//    		logger.debug("Pickup large circle: " + GeometryHelper.createWKT(pickupLargeCircle));
//    		logger.debug("Drop-off large circle: " + GeometryHelper.createWKT(dropOffLargeCircle));
//    		logger.debug("Pickup small circle: " + GeometryHelper.createWKT(pickupSmallCircle));
//    		logger.debug("Drop-off small circle: " + GeometryHelper.createWKT(dropOffSmallCircle));
//    	}
    	GeometryHelper.createWKT(dropOffSmallCircle);
    	TypedQuery<Profile> tq = em.createQuery("from Profile p where contains(:pickupLarge, p.homeAddress.location.point) = true and contains(:dropOffLarge, p.homeAddress.location.point) = true "
    			+ " and (contains(:pickupSmall, p.homeAddress.location.point) = true or contains(:dropOffSmall, p.homeAddress.location.point) = true) "
    			+ " and p.userRole != :passengerRole "
    			+ "order by id asc ", Profile.class)
    			.setParameter("pickupLarge", pickupLargeCircle)
    			.setParameter("dropOffLarge", dropOffLargeCircle)
    			.setParameter("pickupSmall", pickupSmallCircle)
    			.setParameter("dropOffSmall", dropOffSmallCircle)
    			.setParameter("passengerRole", UserRole.PASSENGER);
    	return tq.getResultList();
    }
}
