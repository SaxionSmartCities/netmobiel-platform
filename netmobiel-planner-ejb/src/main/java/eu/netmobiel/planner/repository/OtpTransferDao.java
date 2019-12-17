package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.OtpTransfer;
import eu.netmobiel.planner.model.OtpTransferId;

@ApplicationScoped
@Typed(OtpTransferDao.class)
public class OtpTransferDao extends AbstractDao<OtpTransfer, OtpTransferId> {

	@Inject @PlannerDatabase
    private EntityManager em;

    public OtpTransferDao() {
		super(OtpTransfer.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    
}
