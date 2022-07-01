package eu.netmobiel.planner.repository.mapping;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.vividsolutions.jts.geom.Coordinate;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.TransportOperator;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.tomp.api.model.Asset;
import eu.netmobiel.tomp.api.model.AssetClass;
import eu.netmobiel.tomp.api.model.AssetProperties;
import eu.netmobiel.tomp.api.model.AssetType;
import eu.netmobiel.tomp.api.model.Booking;
import eu.netmobiel.tomp.api.model.Fare;
import eu.netmobiel.tomp.api.model.FarePart;
import eu.netmobiel.tomp.api.model.GeojsonLine;
import eu.netmobiel.tomp.api.model.GeojsonPoint;
import eu.netmobiel.tomp.api.model.Leg;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN,
	uses = { JavaTimeMapper.class, GeometryMapper.class }
)
public abstract class TompBookingMapper {

	public abstract List<Itinerary> mapToItineraries(List<Booking> bookings, @Context TransportOperator operator, @Context TripPlan plan);
	
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "fareInCredits", source = "pricing")
	@Mapping(target = "legs", source = "legs")
	public abstract Itinerary map(Booking booking, @Context TransportOperator operator, @Context TripPlan plan);

	@AfterMapping
    protected void addPostData(Booking source, @MappingTarget Itinerary target, @Context TransportOperator operator, @Context TripPlan plan) {
		target.setTripPlan(plan);
    	Object driverId  = source.getExtraData().get("driverId");
    	if (driverId != null) {
    		String driverRef = UrnHelper.createUrn(NetMobielUser.KEYCLOAK_URN_PREFIX, driverId.toString());
    		target.getLegs().forEach(lg -> lg.setDriverId(driverRef));
    	}
    	target.updateCharacteristics();
    	// Get the ID of the ride, that is now stored in the booking.id field. 
    	// A bit clumsy
    	target.getLegs().forEach(lg -> lg.setTripId(source.getId()));
	}

    @BeanMapping(ignoreByDefault = true)
    // Note: the 'id' fields has completely different meanings 
    @Mapping(target = "distance", source = "distance")
    @Mapping(target = "fareInCredits", source = "pricing")
    @Mapping(target = "from.location", source = "from")
    @Mapping(target = "from.departureTime", source = "departureTime")
    @Mapping(target = "legGeometryEncoded", source = "progressGeometry")
    @Mapping(target = "state", constant = "PLANNING")
    @Mapping(target = "to.location", source = "to")
    @Mapping(target = "to.arrivalTime", source = "arrivalTime")
    public abstract eu.netmobiel.planner.model.Leg mapTompLeg(Leg source, @Context TransportOperator operator );

	@AfterMapping
    protected void addLegData(Leg source, @MappingTarget eu.netmobiel.planner.model.Leg target, @Context TransportOperator operator) {
		AssetType at = source.getAssetType();
		if (at != null && at.getAssetClass() == AssetClass.CAR && "RIDESHARE".equals(at.getAssetSubClass())) {
			target.setTraverseMode(TraverseMode.RIDESHARE);
		}
		if (target.getTraverseMode() == TraverseMode.RIDESHARE && source.getAsset() != null) {
			Asset as = source.getAsset();
			if (as.getLicensePlate() != null) {
				target.setVehicleLicensePlate(as.getLicensePlate());
			}
			if (as.getOverriddenProperties() != null) {
				AssetProperties props = as.getOverriddenProperties();
				target.setVehicleName(
						((props.getBrand() != null ? props.getBrand() : "") + " " + 
						 (props.getModel() != null ? props.getModel() : ""))
						.trim());
			}
		}
		target.setDuration(Math.toIntExact(target.getEndTime().getEpochSecond() - target.getStartTime().getEpochSecond()));
		target.setAgencyTimeZoneOffset(ZoneId.of(operator.getAgencyZoneId()).getRules().getOffset(Instant.now()).getTotalSeconds() * 1000);
		target.setAgencyId(operator.getAgencyId());
		target.setAgencyName(operator.getAgencyName());
		target.setBookingRequired(true);
	}

    public EncodedPolylineBean map(String polyline) {
    	return new EncodedPolylineBean(polyline, null, 0); 
    }
    
    public Integer map(Fare source) {
    	if (source == null) {
    		return null;
    	}
    	if (!"FARE".equals(source.getPropertyClass())) {
    		throw new IllegalStateException("Can only handle FARE classes");
    	}
    	if (source.getParts().size() != 1) {
    		throw new IllegalStateException("Can only handle fares with a single part");
    	}
    	FarePart part = source.getParts().get(0);
    	if (!"CRD".equals(part.getCurrencyCode())) {
    		throw new IllegalStateException("Can only handle netmobiel credits");
    	}
    	if (FarePart.TypeEnum.FIXED != part.getType()) {
    		throw new IllegalStateException("Can only handle a fixed fare");
    	}
    	if (FarePart.PropertyClassEnum.FARE != part.getPropertyClass()) {
    		throw new IllegalStateException("Can only handle a fare part of class FARE");
    	}
    	return Math.round(part.getAmount());
    }

	public GeojsonPoint map(Coordinate source) {
		GeojsonPoint point = new GeojsonPoint();
		point.add((float)source.x);
		point.add((float)source.y);
		return point;
	}
	
	public Coordinate map(GeojsonPoint source) {
		return new Coordinate(source.get(0), source.get(1));
	}

	public EncodedPolylineBean map(GeojsonLine source) {
		if (source == null) {
			return null;
		}
		List<Coordinate> cs = source.stream()
				.map(point -> map(point))
				.collect(Collectors.toList());
		return PolylineEncoder.createEncodings(cs);
	}

}
