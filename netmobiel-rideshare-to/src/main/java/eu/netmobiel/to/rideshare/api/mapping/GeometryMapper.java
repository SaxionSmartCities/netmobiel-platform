package eu.netmobiel.to.rideshare.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.tomp.api.model.Coordinates;
import eu.netmobiel.tomp.api.model.Place;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class GeometryMapper {

	// Domain --> API
	public Place map(GeoLocation source) {
		if (source == null || source.getLongitude() == null || source.getLatitude() == null) {
			return null;
		}
    	Place place = new Place();
    	place.setName(source.getLabel());
    	Coordinates cs = new Coordinates();
    	cs.setLat(source.getLatitude().floatValue());
    	cs.setLng(source.getLongitude().floatValue());
    	place.setCoordinates(cs);
    	return place;
    }
}
