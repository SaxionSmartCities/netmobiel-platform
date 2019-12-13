package eu.netmobiel.rideshare;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    @Produces
    @PersistenceContext
    private EntityManager em;

//    @Default
//    @Produces
//    public Logger produceLog(InjectionPoint injectionPoint) {
//        return LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
//    }

}
