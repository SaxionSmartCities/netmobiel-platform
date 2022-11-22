package eu.netmobiel.profile.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class GeometryMapper {
    // Location --> GeoLocation
	public  GeoLocation map(eu.netmobiel.profile.api.model.Location source) {
		if (source == null || source.getCoordinates() == null || source.getCoordinates().size() < 2) {
			return null;
		}
		if (source.getCoordinates().get(0) == null || source.getCoordinates().get(1) == null) {
			return null;
		}
    	return new GeoLocation(source.getCoordinates().get(1), source.getCoordinates().get(0));
    }

    // GeoLocation --> Location
	public eu.netmobiel.profile.api.model.Location map(GeoLocation source) {
		if (source == null || source.getLongitude() == null || source.getLatitude() == null) {
			return null;
		}
    	eu.netmobiel.profile.api.model.Location location = new eu.netmobiel.profile.api.model.Location();
    	location.setType("Point");
    	location.getCoordinates().add(source.getLongitude());
    	location.getCoordinates().add(source.getLatitude());
    	return location;
    }
}
