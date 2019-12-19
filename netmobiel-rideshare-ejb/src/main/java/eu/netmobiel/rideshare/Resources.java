package eu.netmobiel.rideshare;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import eu.netmobiel.rideshare.annotation.RideshareDatabase;

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
	@RideshareDatabase
    @Produces
    @PersistenceContext(unitName = "pu-rideshare")
    private EntityManager em;

	public void close(@Disposes @RideshareDatabase EntityManager entityManager) {
        entityManager.close();
    }

}
