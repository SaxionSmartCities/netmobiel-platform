package eu.netmobiel.planner.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Geometry;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.opentripplanner.api.model.TransportationType;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.OtpCluster;

@ApplicationScoped
@Typed(OtpClusterDao.class)
public class OtpClusterDao extends AbstractDao<OtpCluster, String> {

    @Inject
    private Logger log;

	@Inject 
	@PlannerDatabase
    private EntityManager em;

    public OtpClusterDao() {
		super(OtpCluster.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public void updateNrRoutes() {
    	Query q = em.createQuery("update OtpCluster c set c.nrRoutes = " + 
    			"(select count(distinct r) from OtpRoute r join r.stops s join s.cluster cc where cc = c)");
    	int affectedRows = q.executeUpdate();
    	log.debug("updateNrRoutes: Rows updated #" + affectedRows);
    }

//    public void updateNrStops() {
//    	Query q = em.createQuery("update OtpCluster c set c.nrStops = (select count(s) from OtpStop s where s.cluster = c)");
//    	int affectedRows = q.executeUpdate();
//    	log.debug("updateNrRoutes: Rows updated #" + affectedRows);
//    }

    public List<OtpCluster> findImportantHubs(GeoLocation fromPlace, GeoLocation toPlace, Geometry area, int maxResults) {
    	// Select all train stations and large bus stations
    	TypedQuery<OtpCluster> tq = em.createQuery(
    			"select c from OtpCluster c where contains(:area, c.location.point) = true and " +
    			"(c.nrRoutes >= 4 or mod(c.transportationTypeValues / :ovtypes, 2) = 1) order by distance(:fromPoint, c.location.point)", OtpCluster.class)
    			.setParameter("area", area)
    			.setParameter("fromPoint", fromPlace.getPoint())
    			.setParameter("ovtypes", TransportationType.RAIL.getMask())
    			.setMaxResults(maxResults);
        return tq.getResultList();
    }

}
