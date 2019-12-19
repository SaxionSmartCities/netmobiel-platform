package eu.netmobiel.planner.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;

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

    @Produces
    @ApplicationScoped
    public Mapper produceDozenMapper() {
    	Mapper mapper = DozerBeanMapperBuilder.create()
//    			.withMappingBuilder(new BookingMappingBuilder())
//    			.withMappingBuilder(new CarMappingBuilder())
//    			.withMappingBuilder(new RideMappingBuilder())
//    			.withMappingBuilder(new StopMappingBuilder())
//    			.withMappingBuilder(new UserMappingBuilder())
    			.build();
        return mapper;
    }

}
