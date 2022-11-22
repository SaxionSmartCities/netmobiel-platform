package eu.netmobiel.banker;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import eu.netmobiel.banker.annotation.BankerDatabase;

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
	@BankerDatabase
    @Produces
    @PersistenceContext(unitName = "pu-banker")
    private EntityManager em;

}
