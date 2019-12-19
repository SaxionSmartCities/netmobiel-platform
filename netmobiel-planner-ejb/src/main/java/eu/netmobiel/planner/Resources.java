package eu.netmobiel.planner;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import eu.netmobiel.planner.annotation.PlannerDatabase;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 *
 * <p>
 * Example injection on a managed bean field:
 * </p>
 *
 * <pre>
 * &#064;Inject
 * private EntityManager em;
 * </pre>
 */
@ApplicationScoped
public class Resources {
    @PlannerDatabase
    @Produces
    @PersistenceContext(unitName = "pu-planner")
    private EntityManager em;

	public void close(@Disposes @PlannerDatabase EntityManager entityManager) {
        entityManager.close();
    }
}
