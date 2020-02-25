package eu.netmobiel.communicator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import eu.netmobiel.communicator.annotation.CommunicatorDatabase;

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
	@CommunicatorDatabase
    @Produces
    @PersistenceContext(unitName = "pu-communicator")
    private EntityManager em;

}
