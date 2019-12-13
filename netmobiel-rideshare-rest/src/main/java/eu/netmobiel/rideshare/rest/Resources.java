package eu.netmobiel.rideshare.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;

import eu.netmobiel.rideshare.api.mapping.BookingMappingBuilder;
import eu.netmobiel.rideshare.api.mapping.CarMappingBuilder;
import eu.netmobiel.rideshare.api.mapping.RideMappingBuilder;
import eu.netmobiel.rideshare.api.mapping.StopMappingBuilder;
import eu.netmobiel.rideshare.api.mapping.UserMappingBuilder;

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
    			.withMappingBuilder(new BookingMappingBuilder())
    			.withMappingBuilder(new CarMappingBuilder())
    			.withMappingBuilder(new RideMappingBuilder())
    			.withMappingBuilder(new StopMappingBuilder())
    			.withMappingBuilder(new UserMappingBuilder())
    			.build();
        return mapper;
    }

}
