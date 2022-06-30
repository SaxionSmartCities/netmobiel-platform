package eu.netmobiel.to.rideshare.api.resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;

import eu.netmobiel.rideshare.service.RideshareTompService;
import eu.netmobiel.tomp.api.OperatorApi;
import eu.netmobiel.tomp.api.model.AssetClass;
import eu.netmobiel.tomp.api.model.AssetType;
import eu.netmobiel.tomp.api.model.ConditionRequireBookingData;
import eu.netmobiel.tomp.api.model.ConditionRequireBookingData.RequiredFieldsEnum;
import eu.netmobiel.tomp.api.model.ConditionUpfrontPayment;
import eu.netmobiel.tomp.api.model.Endpoint;
import eu.netmobiel.tomp.api.model.Endpoint.MethodEnum;
import eu.netmobiel.tomp.api.model.Endpoint.StatusEnum;
import eu.netmobiel.tomp.api.model.EndpointImplementation;
import eu.netmobiel.tomp.api.model.Fare;
import eu.netmobiel.tomp.api.model.FarePart;
import eu.netmobiel.tomp.api.model.FarePart.PropertyClassEnum;
import eu.netmobiel.tomp.api.model.FarePart.TypeEnum;
import eu.netmobiel.tomp.api.model.FarePart.UnitTypeEnum;
import eu.netmobiel.tomp.api.model.ProcessIdentifiers;
import eu.netmobiel.tomp.api.model.Scenario;
import eu.netmobiel.tomp.api.model.StationInformation;
import eu.netmobiel.tomp.api.model.SystemAlert;
import eu.netmobiel.tomp.api.model.SystemCalendar;
import eu.netmobiel.tomp.api.model.SystemHours;
import eu.netmobiel.tomp.api.model.SystemInformation;
import eu.netmobiel.tomp.api.model.SystemInformation.ProductTypeEnum;
import eu.netmobiel.tomp.api.model.SystemInformation.TypeOfSystemEnum;
import eu.netmobiel.tomp.api.model.SystemPricingPlan;
import eu.netmobiel.tomp.api.model.SystemRegion;

@RequestScoped
public class OperatorResource extends TransportOperatorResource implements OperatorApi {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
 
    @Inject
    private RideshareTompService tompService;

	@Context 
	private HttpServletResponse response;
	
    /**
     * Describes the status of the Transport Operator - whether the APIs are running or not.
     * This is a healthcheck endpoint to see if the TO is up and running perfectly.
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     */
	@Override
	public void operatorPingGet(@NotNull String acceptLanguage) {
		tompService.ping();
		// JR: Spec says it must return 200, but a 204 would be more logical.
		response.setStatus(HttpServletResponse.SC_OK);
		// Can't return a 200 with this method?
	}

    /**
     * Returns a list of available assets.
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @param offset With paging: the offset
     * @param limit The maximum number of asset type to return
     * @param regionId optional id of the region to use in the filter (/operator/regions)
     * @param stationId optional id of the station to use in the filter (/operator/stations)
     * @returns
     */
	@Override
	public List<AssetType> operatorAvailableAssetsGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, @Min(0) Integer offset,
			@Min(0) Integer limit, String regionId, String stationId) {
		List<AssetType> ats = new ArrayList<>();

		AssetType at = new AssetType();
		at.setId("shared-rides");
		at.setAssetClass(AssetClass.CAR);
		at.setAssetSubClass("RIDESHARE");
		
		SystemPricingPlan spp = new SystemPricingPlan();
		spp.setDescription("Rit wordt afgerekend per kilometer van de passagier");
		Fare fare = new Fare();
		fare.setEstimated(false);
		fare.setPropertyClass("FARE");
		FarePart farePart = new FarePart();
		farePart.setAmount(1.0f);
		farePart.setCurrencyCode("CRD");
		farePart.setType(TypeEnum.FLEX);
		farePart.setUnitType(UnitTypeEnum.KM);
		farePart.setPropertyClass(PropertyClassEnum.FARE);
		fare.addPartsItem(farePart);
		spp.setFare(fare);
		at.addApplicablePricingsItem(spp);

		ConditionUpfrontPayment cup = new ConditionUpfrontPayment();
		cup.setConditionType("conditionUpfrontPayment");
		cup.setId("reserve-fare-at-booking");
		at.addConditionsItem(cup);
		ConditionRequireBookingData crbd = new ConditionRequireBookingData();
		crbd.conditionType("conditionRequireBookingData");
		crbd.addRequiredFieldsItem(RequiredFieldsEnum.FROM_ADDRESS);
		crbd.addRequiredFieldsItem(RequiredFieldsEnum.TO_ADDRESS);
		crbd.addRequiredFieldsItem(RequiredFieldsEnum.NAME);
		at.addConditionsItem(crbd);
		
		ats.add(at);

		return ats;
	}

    /**
     * Describes the system including System operator, System location, year implemented, URLs, contact info, time zone.
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param api API description, can be TOMP or maybe other (specific/derived) API definitions
     * @param apiVersion Version of the API.
     * @param maasId The ID of the sending maas operator
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns
     */
	@Override
	public SystemInformation operatorInformationGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo) {
		SystemInformation si = new SystemInformation();
		si.setSystemId("Netmobiel-Rideshare");
		si.setLanguage(List.of("nl-NL"));
		si.setName("Netmobiel Rideshare");
		si.setShortName("NRS");
		si.setOperator("Vereniging Netmobiel");
		si.setUrl("https://www.netmobiel.eu");
		si.purchaseUrl("https://www.netmobiel.eu");
		si.startDate(LocalDate.parse("2022-04-02"));
		si.setEmail("netmobielbeheer@gmail.com");
		si.setFeedContactEmail("netmobielbeheer@gmail.com");
		si.setTimezone("Europe/Amsterdam");
		si.setLicenseUrl(null);
		si.setTypeOfSystem(TypeOfSystemEnum.FREE_FLOATING);
		si.setChamberOfCommerceInfo(null);
		si.setConditions(null);
		si.setProductType(ProductTypeEnum.SHARING);
		si.setAssetClasses(List.of(AssetClass.CAR));
		return si;
	}

	private static Endpoint createEndpoint(MethodEnum method, String path) {
		Endpoint ep = new Endpoint();
		ep.setMethod(method);
		ep.setStatus(StatusEnum.IMPLEMENTED);
		ep.setPath(path);
		return ep;
	}
	
    /**
     * describes the running implementations. All versions that are implemented on this url, are described in the result of this endpoint. 
     * It contains all versions and per version the endpoints, their status and the supported scenarios.
     * @param acceptLanguage A list of the languages/localizations the user would like to see the results in. For user privacy and ease of use 
     * 				on the TO side, this list should be kept as short as possible, ideally just one language tag from the list in operator/information
     * @param maasId The ID of the sending maas operator
     * @param addressedTo The ID of the maas operator that has to receive this message
     * @returns
     */
	@Override
	public List<EndpointImplementation> operatorMetaGet(@NotNull String acceptLanguage, @NotNull String maasId,
			String addressedTo) {
		List<EndpointImplementation> eis = new ArrayList<>();
		EndpointImplementation ei = new EndpointImplementation();
		ei.setBaseUrl("rideshare-to-base-url");
		ei.setVersion("1.0.0");
		
		ei.addEndpointsItem(createEndpoint(MethodEnum.GET, "/operator/available-assets"));
		ei.addEndpointsItem(createEndpoint(MethodEnum.GET, "/operator/information"));
		ei.addEndpointsItem(createEndpoint(MethodEnum.GET, "/operator/meta"));
		ei.addEndpointsItem(createEndpoint(MethodEnum.GET, "/operator/ping"));
		ei.addEndpointsItem(createEndpoint(MethodEnum.POST, "/planning/inquiries"));
		
		ei.addScenariosItem(Scenario.UPFRONT_PAYMENT);

		ProcessIdentifiers pis = new ProcessIdentifiers();
		pis.addOperatorInformationItem("DEFAULT");
		pis.addPlanningItem("PLANNING_BASED");
		pis.addPlanningItem("MANDATORY_DEPARTURE_TIME");
		pis.addPlanningItem("MANDATORY_ARRIVAL_TIME");
		pis.addPlanningItem("MANDATORY_NR_OF_TRAVELERS");
		pis.addPlanningItem("MANDATORY_FROM_ADDRESS");
		pis.addPlanningItem("MANDATORY_TO_ADDRESS");
		pis.addBookingItem("AUTO_COMMIT");
		pis.addTripExecutionItem("USE_PREPARE_TO_INDICATE_START");
		pis.addTripExecutionItem("ETA_NOTIFICATION");
		pis.addTripExecutionItem("PROGRESS_NOTIFICATION");
		pis.addTripExecutionItem("TO_CONTROLLED");
		pis.addTripExecutionItem("OFF_BOARDING_REQUIRED");
		ei.setProcessIdentifiers(pis);
		
		eis.add(ei);

		return eis;
	}

	@Override
	public List<SystemAlert> operatorAlertsGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, @Min(0) Integer offset,
			@Min(0) Integer limit, String regionId, String stationId) {
		throw new UnsupportedOperationException("Endpoint is not implemented");
	}

	@Override
	public List<SystemCalendar> operatorOperatingCalendarGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, String regionId, String stationId) {
		throw new UnsupportedOperationException("Endpoint is not implemented");
	}

	@Override
	public List<SystemHours> operatorOperatingHoursGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, String regionId, String stationId) {
		throw new UnsupportedOperationException("Endpoint is not implemented");
	}

	@Override
	public List<SystemPricingPlan> operatorPricingPlansGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, String regionId, String stationId) {
		throw new UnsupportedOperationException("Endpoint is not implemented");
	}

	@Override
	public List<SystemRegion> operatorRegionsGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, @Min(0) Integer offset,
			@Min(0) Integer limit) {
		throw new UnsupportedOperationException("Endpoint is not implemented");
	}

	@Override
	public List<StationInformation> operatorStationsGet(@NotNull String acceptLanguage, @NotNull String api,
			@NotNull String apiVersion, @NotNull String maasId, String addressedTo, @Min(0) Integer offset,
			@Min(0) Integer limit, String regionId, @DecimalMin("0") Float lon, @DecimalMin("0") Float lat,
			@DecimalMin("0") Float radius) {
		throw new UnsupportedOperationException("Endpoint is not implemented");
	}

}
