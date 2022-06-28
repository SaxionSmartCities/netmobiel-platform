package eu.netmobiel.to.rideshare.api.mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.to.rideshare.api.mapping.annotation.BookingMapperQualifier;
import eu.netmobiel.tomp.api.model.Fare;
import eu.netmobiel.tomp.api.model.FarePart;
import eu.netmobiel.tomp.api.model.FarePart.PropertyClassEnum;
import eu.netmobiel.tomp.api.model.FarePart.TypeEnum;

/**
 * This mapper defines the mapping from the domain Booking to the TOMP API Booking as defined by OpenAPI.
 * @author Jaap Reitsma
 *
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN, 
	uses = { JavaTimeMapper.class, GeometryMapper.class })
@BookingMapperQualifier
public abstract class BookingMapper {

  // Translation of the confirmation reason (used in confirmRide)
    @ValueMapping(target = "PENDING", source = "REQUESTED")
    public abstract eu.netmobiel.tomp.api.model.BookingState map(BookingState state);

	// Domain Booking --> Tomp API Booking
	@Mapping(target = "callbackUrl", ignore = true)
	@Mapping(target = "customer", ignore = true)
	@Mapping(target = "extraData", ignore = true)
	@Mapping(target = "from", source = "pickup")
	@Mapping(target = "legs", ignore = true)
	@Mapping(target = "mainAssetType", ignore = true)
	@Mapping(target = "pricing", ignore = true)
	@Mapping(target = "to", source = "dropOff")
	public abstract eu.netmobiel.tomp.api.model.Booking mapSearch(Booking source);

	@AfterMapping
    protected void mapSearchPostMapping(Booking source, @MappingTarget eu.netmobiel.tomp.api.model.Booking target) {
		Map<String, Object> extraData = new LinkedHashMap<>();
		RideshareUser driver = source.getRide().getDriver();
		Car car = source.getRide().getCar();
		extraData.put("driverName", driver.getName());
		extraData.put("driverId", driver.getManagedIdentity());
		extraData.put("vehicleId", car.getUrn());
		extraData.put("vehicleLicensePlate", car.getLicensePlate());
		extraData.put("vehicleName", String.format("%s %s", car.getBrand(), car.getModel()));
		target.setExtraData(extraData);
		
		Fare fare = new Fare();
		fare.setEstimated(false);
		fare.setPropertyClass("FARE");
		FarePart farePart = new FarePart();
		farePart.setAmount(source.getFareInCredits().floatValue());
		farePart.setCurrencyCode("CRD");
		farePart.setType(TypeEnum.FIXED);
		farePart.setPropertyClass(PropertyClassEnum.FARE);
		fare.addPartsItem(farePart);
		target.setPricing(fare);
	}
	
	public abstract List<eu.netmobiel.tomp.api.model.Booking> mapSearch(List<Booking> source);

}
