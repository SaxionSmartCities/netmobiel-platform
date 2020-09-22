package eu.netmobiel.planner.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Geometry;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.GeoLocation_;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.repository.predicate.WithinPredicate;
import eu.netmobiel.opentripplanner.api.model.TransportationType;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.OtpCluster_;
import eu.netmobiel.planner.model.TraverseMode;

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

	/**
	 * Returns important hubs: All Rail clusters + clusters with 4 or more routes.
	 * @param sortLocation The location to take as reference
	 * @param area The geometry to limit the search: Too far away for a car driver
	 * @param maxResults Tha maximum  number of results to return
	 * @return A list of clusters, ordered from nearby to further away
	 */
    public List<OtpCluster> findImportantHubs(GeoLocation sortLocation, Geometry area, int maxResults) {
    	// Select all train stations and large bus stations
    	TypedQuery<OtpCluster> tq = em.createQuery(
    			"select c from OtpCluster c where contains(:area, c.location.point) = true and " +
    			"(c.nrRoutes >= 4 or mod(c.transportationTypeValues / :ovtypes, 2) = 1) order by distance(:fromPoint, c.location.point)", OtpCluster.class)
    			.setParameter("area", area)
    			.setParameter("fromPoint", sortLocation.getPoint())
    			.setParameter("ovtypes", TransportationType.RAIL.getMask())
    			.setMaxResults(maxResults);
        return tq.getResultList();
    }

	private static Map<TraverseMode, TransportationType> typeMap = new HashMap<>();
    static {
    	typeMap.put(TraverseMode.BUS, 		TransportationType.BUS);
    	typeMap.put(TraverseMode.CABLE_CAR, TransportationType.CABLE_CAR);
    	typeMap.put(TraverseMode.FERRY, 	TransportationType.FERRY);
    	typeMap.put(TraverseMode.FUNICULAR, TransportationType.FUNICULAR);
    	typeMap.put(TraverseMode.GONDOLA, 	TransportationType.GONDOLA);
    	typeMap.put(TraverseMode.RAIL, 		TransportationType.RAIL);
    	typeMap.put(TraverseMode.SUBWAY,	TransportationType.SUBWAY);
    	typeMap.put(TraverseMode.TRAM, 		TransportationType.TRAM);
    }
    
    // NOT TESTED
    public List<OtpCluster> findImportantHubs(GeoLocation fromPlace, GeoLocation toPlace, Geometry area, int minNrRoutes, TraverseMode[] modes, int maxResults) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
	    CriteriaQuery<OtpCluster> cq = cb.createQuery(OtpCluster.class);
	    Root<OtpCluster> clusters = cq.from(OtpCluster.class);
        Predicate spatialAreaPred = new WithinPredicate(cb, clusters.get(OtpCluster_.location).get(GeoLocation_.point), area);
        Predicate manyRoutesPred = cb.greaterThanOrEqualTo(clusters.get(OtpCluster_.nrRoutes), minNrRoutes);
	    List<Predicate> modesPredicates = new ArrayList<>();
	    if (!Arrays.stream(modes).anyMatch(m -> m == TraverseMode.TRANSIT)) {
		    Arrays.stream(modes).forEach(mode -> {
		    	TransportationType tt = typeMap.get(mode);
		    	if (tt != null) {
		    		Expression<Integer> bit0Value = cb.toInteger(cb.quot(clusters.get(OtpCluster_.transportationTypeValues), tt.getMask()));
		    		Expression<Integer> bitSet = cb.mod(bit0Value, 2); 
		    		modesPredicates.add(cb.equal(bitSet, 1));
		    	}
		    });
	    }
	    Predicate typePreds = cb.conjunction();
	    if (! modesPredicates.isEmpty()) {
	    	typePreds = cb.disjunction();
	    	for (Predicate p: modesPredicates) {
	    		typePreds = cb.or(p);
			} 
	    }
	    Predicate andPreds = cb.and(spatialAreaPred, manyRoutesPred, typePreds);
	    cq.where(andPreds);
        cq.select(clusters);
//        cq.orderBy(cb.asc(clusters.get(Trip_.departureTime)));
        TypedQuery<OtpCluster> tq = em.createQuery(cq);
		tq.setMaxResults(maxResults);
		return tq.getResultList();
    }
}
