package eu.netmobiel.planner.api.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.vividsolutions.jts.geom.MultiPoint;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.planner.api.model.EncodedPolyline;

/**
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface GeometryMapper {
    // Location <--> GeoLocation
    @Mapping(target = "point", ignore = true)
    GeoLocation map(eu.netmobiel.planner.api.model.Location source);

	// MultiPoint -> EncodedPolylineBean 
	EncodedPolyline map(EncodedPolylineBean geometry);

	// MultiPoint -> EncodedPolylineBean 
	default EncodedPolyline map(MultiPoint geometry) {
		EncodedPolylineBean bean = PolylineEncoder.createEncodings(geometry);
		return map(bean);
	}

}
