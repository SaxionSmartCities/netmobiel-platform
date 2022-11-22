package eu.netmobiel.profile;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import eu.netmobiel.profile.annotation.ProfileDatabase;

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
	@ProfileDatabase
    @Produces
    @PersistenceContext(unitName = "pu-profilesvc")
    private EntityManager em;

}
