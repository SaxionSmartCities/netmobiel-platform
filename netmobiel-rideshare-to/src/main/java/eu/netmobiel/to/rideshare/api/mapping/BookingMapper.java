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
import eu.netmobiel.to.rideshare.api.mapping.annotation.LegMapperQualifier;
import eu.netmobiel.tomp.api.model.Asset;
import eu.netmobiel.tomp.api.model.AssetProperties;
import eu.netmobiel.tomp.api.model.AssetProperties.EnergyLabelEnum;
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
	uses = { LegMapper.class, JavaTimeMapper.class, GeometryMapper.class })
@BookingMapperQualifier
public abstract class BookingMapper {

    @ValueMapping(target = "PENDING", source = "REQUESTED")
    public abstract eu.netmobiel.tomp.api.model.BookingState map(BookingState state);

	// Domain Booking --> Tomp API Booking
	@Mapping(target = "callbackUrl", ignore = true)
	@Mapping(target = "customer", ignore = true)
	@Mapping(target = "extraData", ignore = true)
	@Mapping(target = "from", source = "pickup")
	@Mapping(target = "legs", source = "legs", qualifiedBy = LegMapperQualifier.class)
	@Mapping(target = "legGeometry", ignore = true)
	@Mapping(target = "mainAssetType", ignore = true)
//	@Mapping(target = "mainAssetType.id", constant = "shared-rides")
//	@Mapping(target = "mainAssetType.assetClass", constant = "CAR")
//	@Mapping(target = "mainAssetType.assetSubClass", constant = "RIDESHARE")
	@Mapping(target = "pricing", source = "fareInCredits")
	@Mapping(target = "to", source = "dropOff")
	// We also need a reference to the original ride. Where to put it? Use the booking id(!)
	// So, now the booking id is actually the ride id, required to book a ride. 
	// After booking we have a booking id.
	@Mapping(target = "id", source = "urn")
	public abstract eu.netmobiel.tomp.api.model.Booking map(Booking source);

	public Fare map(Integer fareInCredits) {
		if (fareInCredits == null) {
			return null;
		}
		Fare fare = new Fare();
		fare.setEstimated(false);
		fare.setPropertyClass("FARE");
		FarePart farePart = new FarePart();
		farePart.setAmount((float) fareInCredits);
		farePart.setCurrencyCode("CRD");
		farePart.setType(TypeEnum.FIXED);
		farePart.setPropertyClass(PropertyClassEnum.FARE);
		fare.addPartsItem(farePart);
		return fare;
	}
	
	@AfterMapping
    protected void mapSearchPostMapping(Booking source, @MappingTarget eu.netmobiel.tomp.api.model.Booking target) {
		Map<String, Object> extraData = new LinkedHashMap<>();
		RideshareUser driver = source.getRide().getDriver();
		Car car = source.getRide().getCar();
//		extraData.put("driverName", driver.getName());
		extraData.put("driverId", driver.getManagedIdentity());
		extraData.put("knownIdentifierProvider", "Netmobiel Keycloak");
//		extraData.put("vehicleId", car.getUrn());
		target.setExtraData(extraData);
		
		Asset asset = new Asset();
		asset.setLicensePlate(car.getLicensePlate());
		AssetProperties aps = new AssetProperties();
		if (car.getCo2Emission() != null) {
			aps.setCo2PerKm((float)car.getCo2Emission());
		} else {
			aps.setEnergyLabel(EnergyLabelEnum.E);
		}
		aps.setBrand(car.getBrand());
		aps.setModel(car.getModel());
		asset.setOverriddenProperties(aps);
		target.getLegs().forEach(lg -> lg.setAsset(asset));
	}
	
	public abstract List<eu.netmobiel.tomp.api.model.Booking> mapBookings(List<Booking> source);
	
}
