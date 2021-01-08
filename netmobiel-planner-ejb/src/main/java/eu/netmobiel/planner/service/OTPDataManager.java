package eu.netmobiel.planner.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.slf4j.Logger;

import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.OtpRoute;
import eu.netmobiel.planner.model.OtpStop;
import eu.netmobiel.planner.model.OtpTransfer;
import eu.netmobiel.planner.model.OtpTransferId;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.planner.repository.OtpRouteDao;
import eu.netmobiel.planner.repository.OtpStopDao;
import eu.netmobiel.planner.repository.OtpTransferDao;

/**
 * This bean has long running operations which are intended to run asynchronously. 
 * @author Jaap Reitsma
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OTPDataManager {
	
    @Inject
    private Logger log;

    @Inject
    private OtpStopDao otpStopDao;
    @Inject
    private OtpClusterDao otpClusterDao;
    @Inject
    private OtpRouteDao otpRouteDao;
    @Inject
    private OtpTransferDao otpTransferDao;

    public void prepareUpdateStops() {
    	otpStopDao.markAllStale();
    }

    @TransactionTimeout(1800)
    public void finishUpdateStops() {
    	otpStopDao.removeAllStale();
    }

    public void bulkUpdateStops(List<OtpStop> stops) {
    	for (OtpStop otpStop : stops) {
			otpStopDao.find(otpStop.getId()).map(s -> {
				otpStop.setCluster(s.getCluster());
				otpStop.getTransportationTypes().clear();
				return otpStopDao.merge(otpStop);
			}).orElseGet(() -> {
				return otpStopDao.save(otpStop);
			});
		}
    }

    public void prepareUpdateClusters() {
    	otpClusterDao.markAllStale();
    }
    
    @TransactionTimeout(1800)
    public void finishUpdateClusters() {
    	otpClusterDao.removeAllStale();
    }

    public void bulkUpdateClusters(List<OtpCluster> clusters) {
    	for (OtpCluster otpCluster : clusters) {
    		otpCluster.setNrStops(otpCluster.getStops().size());
			OtpCluster cluster = otpClusterDao.find(otpCluster.getId()).map(c -> {
				otpCluster.getTransportationTypes().clear();
				return otpClusterDao.merge(otpCluster);
			}).orElseGet(() -> {
				return otpClusterDao.save(otpCluster);
			});
			for (OtpStop otpStop : otpCluster.getStops()) {
				otpStopDao.find(otpStop.getId()).map(s -> {
					s.setCluster(cluster);
					return s;
				});
			}
		}
    }

    public void prepareUpdateRoutes() {
    	otpRouteDao.markAllStale();
    }
    
    @TransactionTimeout(1800)
    public void finishUpdateRoutes() {
    	otpRouteDao.removeAllStale();
    }
    
    public void bulkUpdateRoutes(List<OtpRoute> routes) {
    	for (OtpRoute otpRoute : routes) {
			OtpRoute route = otpRouteDao.find(otpRoute.getId()).map(c -> {
				return otpRouteDao.merge(otpRoute);
			}).orElseGet(() -> {
				return otpRouteDao.save(otpRoute);
			});
			for (OtpStop otpStop: route.getStops()) {
				otpStopDao.find(otpStop.getId()).ifPresent(s -> {
					s.getTransportationTypes().set(otpRoute.getType());
					s.getCluster().getTransportationTypes().set(otpRoute.getType());
				});
			}
		}
    }

    // Obsoleted transfers are automatically removed with the stops
    
    public void bulkUpdateTransfers(List<OtpTransfer> transfers) {
    	for (OtpTransfer otpTransfer : transfers) {
    		OtpTransferId tpk = new OtpTransferId(otpTransfer.getFromStop().getId(), otpTransfer.getToStop().getId()); 
			otpTransferDao.find(tpk).map(t -> {
				return otpTransferDao.merge(otpTransfer);
			}).orElseGet(() -> {
				otpTransfer.setFromStop(otpStopDao.find(otpTransfer.getFromStop().getId()).orElse(null));
				otpTransfer.setToStop(otpStopDao.find(otpTransfer.getToStop().getId()).orElse(null));
				return otpTransferDao.save(otpTransfer);
			});
		}
    }
    
    @TransactionTimeout(1800)
    public void bulkUpdateClusterRoutes() {
    	log.info("Update cluster nr routes");
    	otpClusterDao.updateNrRoutes();
    }

    @TransactionTimeout(1800)
    public void bulkUpdateStopRoutes() {
    	log.info("Update stop nr routes");
    	otpStopDao.updateNrRoutes();
    }
    
}
