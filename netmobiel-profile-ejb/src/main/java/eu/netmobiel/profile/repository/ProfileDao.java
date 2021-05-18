package eu.netmobiel.profile.repository;

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

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.UserDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Profile_;
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

	/**
	 * Retrieves the profile according the search criteria.
	 * @param role The user role to select. Role 'both' is always included if driver or passenger is set.
	 * @param maxResults The maximum results in one result set.
	 * @param offset The offset (starting at 0) to start the result set.
	 * @return A pages result. Total count is determined only when maxResults is set to 0.
	 */
	public PagedResult<Long> listProfiles(ProfileFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Profile> profile = cq.from(Profile.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getText() != null) {
        	String text = filter.getText().replace("%","%%").toLowerCase();
        	if (!text.startsWith("%")) {
        		text = "%" + text;
        	}
        	if (!text.endsWith("%")) {
        		text = text + "%";
        	}
	        predicates.add(cb.or(
	        		cb.like(cb.lower(profile.get(Profile_.email)), text), 
	        		cb.like(cb.lower(profile.get(Profile_.phoneNumber)), text),
	        		cb.like(cb.lower(profile.get(Profile_.familyName)), text),
	        		cb.like(cb.lower(profile.get(Profile_.givenName)), text)
	        		));
        }
        if (filter.getUserRole() != null) {
        	Predicate rolePred = cb.equal(profile.get(Profile_.userRole), filter.getUserRole());  
        	// Search for the profile with the specified role, or role 'both'
        	if (filter.getUserRole() == UserRole.DRIVER || filter.getUserRole() == UserRole.PASSENGER) {
		        predicates.add(cb.or(rolePred, cb.equal(profile.get(Profile_.userRole), UserRole.BOTH)));
        	} else {
		        predicates.add(rolePred);
        	}
        }        
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = null;
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(profile.get(Profile_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(profile.get(Profile_.id));
	        cq.orderBy(cb.asc(profile.get(Profile_.id)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, cursor, totalCount);
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
    	TypedQuery<Profile> tq = em.createQuery("from Profile p where "
    			+ "     contains(:pickupLarge, p.homeLocation.point) = true and contains(:dropOffLarge, p.homeLocation.point) = true "
    			+ " and (contains(:pickupSmall, p.homeLocation.point) = true or contains(:dropOffSmall, p.homeLocation.point) = true) "
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
