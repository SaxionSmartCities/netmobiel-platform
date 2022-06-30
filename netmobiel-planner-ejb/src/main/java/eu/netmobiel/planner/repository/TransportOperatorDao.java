package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.TransportOperator;

@ApplicationScoped
@Typed(TransportOperatorDao.class)
public class TransportOperatorDao extends AbstractDao<TransportOperator, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public TransportOperatorDao() {
		super(TransportOperator.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}
}
