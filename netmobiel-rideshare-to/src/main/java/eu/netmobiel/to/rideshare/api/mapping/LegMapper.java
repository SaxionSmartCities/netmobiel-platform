package eu.netmobiel.to.rideshare.api.mapping;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

import com.vividsolutions.jts.geom.Coordinate;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.to.rideshare.api.mapping.annotation.LegMapperQualifier;
import eu.netmobiel.tomp.api.model.GeojsonLine;
import eu.netmobiel.tomp.api.model.GeojsonPoint;

/**
 * This mapper defines the mapping from the domain Booking to the TOMP API Booking as defined by OpenAPI.
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, GeometryMapper.class })
@LegMapperQualifier
public abstract class LegMapper {

  // Translation of the confirmation reason (used in confirmRide)
    @ValueMapping(target = "PENDING", source = "REQUESTED")
    public abstract eu.netmobiel.tomp.api.model.BookingState map(BookingState state);

	// Domain Booking --> Tomp API Booking
	@Mapping(target = "arrivalDelay", ignore = true)
	@Mapping(target = "arrivalTime", source = "to.arrivalTime")
	@Mapping(target = "assetAccessData", ignore = true)
	@Mapping(target = "allAssetAccessData", ignore = true)
	@Mapping(target = "asset", ignore = true)
	@Mapping(target = "assetType.id", constant = "shared-rides")
	@Mapping(target = "assetType.assetClass", constant = "CAR")
	@Mapping(target = "assetType.assetSubClass", constant = "RIDESHARE")
	@Mapping(target = "conditions", ignore = true)
	@Mapping(target = "departureDelay", ignore = true)
	@Mapping(target = "departureTime", source = "from.departureTime")
	@Mapping(target = "from", source = "from.location")
	@Mapping(target = "legSequenceNumber", ignore = true)
	@Mapping(target = "pricing", ignore = true)
	@Mapping(target = "progressGeometry", source = "legGeometryEncoded")
	@Mapping(target = "suboperator", ignore = true)
	@Mapping(target = "ticket", ignore = true)
	@Mapping(target = "travelerReferenceNumbers", ignore = true)
	@Mapping(target = "state", constant = "NOT_STARTED")
	@Mapping(target = "to", source = "to.location")
	public abstract eu.netmobiel.tomp.api.model.Leg map(Leg leg);

	public GeojsonPoint map(Coordinate source) {
		GeojsonPoint point = new GeojsonPoint();
		point.add((float)source.x);
		point.add((float)source.y);
		return point;
	}
	
	public GeojsonLine map(EncodedPolylineBean source) {
		if (source == null) {
			return null;
		}
		return PolylineEncoder.decode(source).stream()
				.map(c -> map(c))
				.collect(Collectors.toCollection(GeojsonLine::new));
	}

	public abstract List<eu.netmobiel.tomp.api.model.Leg> map(List<Leg> source);

}
