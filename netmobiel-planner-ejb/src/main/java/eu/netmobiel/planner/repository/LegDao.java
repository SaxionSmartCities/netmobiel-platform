package eu.netmobiel.planner.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.ModalityUsage;
import eu.netmobiel.planner.model.PlannerUser;

@ApplicationScoped
@Typed(LegDao.class)
public class LegDao extends AbstractDao<Leg, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public LegDao() {
		super(Leg.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public int updateDriverId(String oldId, String newId) {
    	return em.createQuery("update Leg lg set lg.driverId = :newDriverId where lg.driverId = :oldDriverId")
    			.setParameter("oldDriverId", oldId)
    			.setParameter("newDriverId", newId)
    			.executeUpdate();
	}
	
    /**
     * Report on the usage of the modalities of a user as a passenger. The count is the number of a completed trip in which a modality is used
     * at least once. A multi-legged trip with a single modality counts as one.
     * @param user The user to report about
     * @return A list of ModalityCount objects.
     */
    public List<ModalityUsage> reportModalityUsageAsPassenger(PlannerUser user) {
    	List<ModalityUsage> report = em.createQuery(
    			"select new eu.netmobiel.planner.model.ModalityUsage(lg.traverseMode, count(distinct t.id)) from Trip t " +
    			" join t.itinerary it, in(it.legs) lg " +
    			" where t.traveller = :user and t.state = eu.netmobiel.planner.model.TripState.COMPLETED " +
    			" group by lg.traverseMode " +
    			" order by lg.traverseMode asc", ModalityUsage.class)
    			.setParameter("user", user)
    			.getResultList();
    	return report;
    	
    }
 
    /**
     * Report on the usage of the modalities of a user as a driver. The count is the number of a completed trips in which a modality is used
     * at least once. A multi-legged trip with a single modality counts as one.
     * The user is identified as the driver by the keycloak identity in the driverId field of a leg.
     * @param user The user to report about
     * @return A list of ModalityCount objects.
     */
    public List<ModalityUsage> reportModalityUsageAsDriver(PlannerUser user) {
    	List<ModalityUsage> report = em.createQuery(
    			"select new eu.netmobiel.planner.model.ModalityUsage(lg.traverseMode, count(distinct t.id)) from Trip t " +
    			" join t.itinerary it, in(it.legs) lg " +
    			" where lg.driverId = :managedIdentityUrn and t.state = eu.netmobiel.planner.model.TripState.COMPLETED " +
    			" group by lg.traverseMode " +
    			" order by lg.traverseMode asc", ModalityUsage.class)
    			.setParameter("managedIdentityUrn", user.getKeyCloakUrn())
    			.getResultList();
    	return report;
    	
    }
}