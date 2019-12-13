package eu.netmobiel.rideshare.api.mapping;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;

import eu.netmobiel.rideshare.model.Stop;

public class StopMappingBuilder extends BeanMappingBuilder {
	@Override
	protected void configure() {
		mapping(Stop.class, eu.netmobiel.rideshare.api.model.Stop.class); 
	}
}
