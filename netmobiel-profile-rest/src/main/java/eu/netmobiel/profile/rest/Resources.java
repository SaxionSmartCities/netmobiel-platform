package eu.netmobiel.profile.rest;

import javax.enterprise.context.ApplicationScoped;

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

//    @Produces
//    public Logger produceLog(InjectionPoint injectionPoint) {
//        return LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
//    }

}
