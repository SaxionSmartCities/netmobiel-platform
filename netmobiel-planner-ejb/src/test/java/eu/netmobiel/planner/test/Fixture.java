package eu.netmobiel.planner.test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.ws.rs.core.Response;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.planner.model.AbsoluteDirection;
import eu.netmobiel.planner.model.GuideStep;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerReport;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.RelativeDirection;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.ToolType;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;

public class Fixture {
	public static final GeoLocation placeHengeloStation = GeoLocation.fromString("Hengelo NS Station::52.260977,6.7931087");// Bij metropool
	public static final GeoLocation placeZieuwent = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
	public static final GeoLocation placeSlingeland = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
	public static final GeoLocation placeRaboZutphen = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
	public static final GeoLocation placeRozenkwekerijZutphen = GeoLocation.fromString("Hoveniersweg 9-5 Zutphen::52.146734,6.174644");
	public static final GeoLocation placeZieuwentRKKerk = GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542");
	public static final GeoLocation placeThuisLichtenvoorde  = GeoLocation.fromString("Rapenburgsestraat Lichtenvoorde::51.987757,6.564012");
	public static final GeoLocation placeCentrumDoetinchem = GeoLocation.fromString("Catharina Parkeergarage Doetinchem::51.9670528,6.2894002");

	private Fixture() {
		// No instances allowed
	}

	public static User createUser(String identity, String givenName, String familyName) {
		return new User(identity, givenName, familyName);
	}
	
	public static User createUser(LoginContext loginContext) {
        Subject subject = loginContext.getSubject();
        @SuppressWarnings("rawtypes")
		Set<KeycloakPrincipal> ps = subject.getPrincipals(KeycloakPrincipal.class);
        @SuppressWarnings("unchecked")
		KeycloakPrincipal<KeycloakSecurityContext> p = ps.iterator().next();
        return createUser(p.getKeycloakSecurityContext().getToken());
	}

	public static User createUser(AccessToken token) {
		return new User(token.getSubject(), token.getGivenName(), token.getFamilyName());
	}

	public static User createUser1() {
		return createUser("ID1", "Carla1", "Netmobiel");
	}
	
	public static User createUser2() {
		return createUser("ID2", "Carla2", "Netmobiel");
	}
	public static User createUser3() {
		return createUser("ID3", "Carla3", "Netmobiel");
	}

    public static TripPlan createTransitPlan(User traveller) {
		TripPlan plan = new TripPlan();
		plan.setPlanType(PlanType.REGULAR);
		plan.setRequestTime(OffsetDateTime.parse("2020-01-07T10:00:00+01:00").toInstant());
		plan.setTraveller(traveller);
		plan.setFrom(Fixture.placeZieuwent);
		plan.setTo(Fixture.placeRaboZutphen);
		plan.setTravelTime(OffsetDateTime.parse("2020-01-07T14:00:00+01:00").toInstant());
		plan.setUseAsArrivalTime(false);
		plan.setTraverseModes(new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.TRANSIT, TraverseMode.WALK })));
		plan.setMaxWalkDistance(1000);
		plan.setNrSeats(1);
		
		Itinerary it = new Itinerary();
    	it.setDepartureTime(OffsetDateTime.parse("2020-01-07T14:32:56+01:00").toInstant());
    	it.setDuration(3559);
    	it.setArrivalTime(it.getDepartureTime().plusSeconds(it.getDuration()));
    	it.setTransfers(1);
    	it.setTransitTime(1980);
    	it.setWaitingTime(798);
    	it.setWalkDistance(943);
    	it.setWalkTime(781);
    	it.setStops(new ArrayList<>());
    	it.setLegs(new ArrayList<>());

    	Leg leg1 = new Leg();
    	it.getLegs().add(leg1);
    	leg1.setAgencyTimeZoneOffset(3600000);
    	leg1.setDistance(153);
    	leg1.setDuration(123);
    	
    	Stop stop1F = new Stop(plan.getFrom());
    	stop1F.setDepartureTime(OffsetDateTime.parse("2020-01-07T14:32:56+01:00").toInstant());
    	leg1.setFrom(stop1F);		
    	it.getStops().add(stop1F);
    	
    	Stop stop1T = new Stop(GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542"));
    	stop1T.setArrivalTime(stop1F.getDepartureTime().plusSeconds(leg1.getDuration()));
    	stop1T.setDepartureTime(OffsetDateTime.parse("2020-01-07T14:35:00+01:00").toInstant());
    	stop1T.setStopId("NL:1532280");
    	stop1T.setStopCode("44930020");
    	leg1.setTo(stop1T);
    	it.getStops().add(stop1T);

    	leg1.setState(TripState.PLANNING);
    	leg1.setTraverseMode(TraverseMode.WALK);
    	leg1.setLegGeometry(GeometryHelper.createLegGeometry(new EncodedPolylineBean("_al|Hm_xf@?U?WHe@D_AF{@?QAQEW[iAs@@", null, 11)));

    	leg1.setGuideSteps(new ArrayList<>());
    	GuideStep ws1_1 = new GuideStep();
    	leg1.getGuideSteps().add(ws1_1);
    	ws1_1.setAbsoluteDirection(AbsoluteDirection.EAST);
    	ws1_1.setDistance(124);
    	ws1_1.setLatitude(52.00416334775857);
    	ws1_1.setLongitude(6.517834978465908);
    	ws1_1.setName("Kennedystraat");
    	ws1_1.setRelativeDirection(RelativeDirection.DEPART);
    	GuideStep ws1_2 = new GuideStep();
    	leg1.getGuideSteps().add(ws1_2);
    	ws1_2.setAbsoluteDirection(AbsoluteDirection.NORTH);
    	ws1_2.setDistance(29);
    	ws1_2.setLatitude(52.0042224);
    	ws1_2.setLongitude(6.519548800000001);
    	ws1_2.setName("Dorpsstraat");
    	ws1_2.setRelativeDirection(RelativeDirection.LEFT);

    	
    	Leg leg2 = new Leg();
    	it.getLegs().add(leg2);
    	leg2.setAgencyTimeZoneOffset(3600000);
    	leg2.setDistance(13391);
    	leg2.setDuration(1080);
    	
    	Stop stop2F = stop1T;
    	leg2.setFrom(stop2F);		

    	Stop stop2T = new Stop(GeoLocation.fromString("Ruurlo, Station::52.081233,6.45004"));
    	stop2T.setArrivalTime(stop2F.getDepartureTime().plusSeconds(leg2.getDuration()));
    	stop2T.setDepartureTime(OffsetDateTime.parse("2020-01-07T14:35:00+01:00").toInstant());
    	stop2T.setStopId("NL:205");
    	stop2T.setStopCode("44810170");
    	leg2.setTo(stop2T);
    	it.getStops().add(stop2T);

    	leg2.setState(TripState.PLANNING);
    	leg2.setTraverseMode(TraverseMode.BUS);
    	leg2.setLegGeometry(GeometryHelper.createLegGeometry(new EncodedPolylineBean("acl|Hcjxf@?Ds@Bq@@cEN]Bm@HeAb@i@Xe@\\UVW`@o@fAkApBQVgBdDa@r@iArB_A~BgA|CgCrHsAvDu@rBs@bBa@dAGTYpBMn@s@rJKfAKEJDADK|@_@bCYpAKXsG|KuAlB[\\m@f@k@d@_B~@_CrAaA|@]d@Y`@KRKVaDbH]t@Sb@SRYPXnCKBJCBXFd@PzBFx@FXZtAtAfEn@hBPj@Np@Hr@Hn@Dl@Bp@@x@Ar@SbCeAzHW|AKx@Gj@MbAEz@Av@?r@@f@Bd@Db@Hp@v@vFn@nELtANpAJxANxBLzADz@H`At@tEF^DZH`BH`DDb@Jd@P~@hBrIt@rDd@vBH^NZtCdEvGrJtDtFGNFOX^{CrHO`@yAlDiAjC_@`AO\\wCbHoCrG_@~@OVINOPWL]R_DtACQBPuB~@MJELS^u@zBa@fBIh@Ij@EXMd@_C~GQX}@b@[LuIzCeJ`DwUpIkZzKcE|AmE|AsSrHiKzDQFkJjDaXvJOBaBn@iE`Bo@Pa@P_@H[D]@c@GWEcAo@cLaHeBmAiAiAaAeAuCkDoAoAs@u@{AeASMWIeNkDqFmAeCo@[GsSmFoCi@SA{JnAqBXw@LYJSNY`@iB`DUn@Sl@k@dC_@~A]|@_@v@AD_@l@_@b@c@`@g@^s@Vw@Xw@TyAXiALo@@k@GkAOw@Ak@DW@a@AkACiFq@kBU]ImGyBaA_@MGGEUk@MFy@PODg@LeRpHm@XSLQNOPsI|Ig@f@_@`@Yv@CHEJAHE@C@GAGCUEKCMb@Kd@Kb@C?C?KBEDAFCF]QSK]_@{@{@{@aAWUc@_@UWWSrAaHt@aDJk@FS@O@U?QEMG_@II", null, 273)));
    	leg2.setAgencyName("Arriva");
    	leg2.setRouteType(3);
    	leg2.setRouteId("NL:10");
    	leg2.setRouteLongName("Ruurlo - Aalten");
    	leg2.setRouteShortName("191");
    	leg2.setHeadsign("Ruurlo");

    	Leg leg3 = new Leg();
    	it.getLegs().add(leg3);
    	leg3.setAgencyTimeZoneOffset(3600000);
    	leg3.setDistance(135);
    	leg3.setDuration(104);

    	Stop stop3F = stop2T;
    	leg3.setFrom(stop3F);		

    	Stop stop3T = new Stop(GeoLocation.fromString("Ruurlo::52.0806730545,6.45028352737"));
    	stop3T.setArrivalTime(stop3F.getDepartureTime().plusSeconds(leg3.getDuration()));
    	stop3T.setDepartureTime(OffsetDateTime.parse("2020-01-07T15:08:00+01:00").toInstant());
    	stop3T.setStopId("NL:205");
    	stop3T.setPlatformCode("1");
    	leg3.setTo(stop3T);
    	it.getStops().add(stop3T);

    	leg3.setState(TripState.PLANNING);
    	leg3.setTraverseMode(TraverseMode.WALK);
    	leg3.setLegGeometry(GeometryHelper.createLegGeometry(new EncodedPolylineBean("sb{|Hwwjf@Rz@@LATAFHD\\P??DDJg@CAf@iC", null, 12)));
    	
    	leg3.setGuideSteps(new ArrayList<>());
    	GuideStep ws3_1 = new GuideStep();
    	leg3.getGuideSteps().add(ws3_1);
    	ws3_1.setAbsoluteDirection(AbsoluteDirection.SOUTHWEST);
    	ws3_1.setDistance(39);
    	ws3_1.setLatitude(52.08122495805119);
    	ws3_1.setLongitude(6.4500472005336755);
    	ws3_1.setName("Stationsstraat");
    	ws3_1.setRelativeDirection(RelativeDirection.DEPART);
    	
    	GuideStep ws3_2 = new GuideStep();
    	leg3.getGuideSteps().add(ws3_2);
    	ws3_2.setAbsoluteDirection(AbsoluteDirection.SOUTHWEST);
    	ws3_2.setDistance(42);
    	ws3_2.setLatitude(52.081132200000006);
    	ws3_2.setLongitude(6.449528900000001);
    	ws3_2.setName("path");
    	ws3_2.setBogusName(true);
    	ws3_2.setRelativeDirection(RelativeDirection.LEFT);

    	GuideStep ws3_3 = new GuideStep();
    	leg3.getGuideSteps().add(ws3_3);
    	ws3_3.setAbsoluteDirection(AbsoluteDirection.NORTH);
    	ws3_3.setDistance(54);
    	ws3_3.setLatitude(52.0808466);
    	ws3_3.setLongitude(6.4495762);
    	ws3_3.setName("1");
    	ws3_3.setArea(true);
    	ws3_3.setRelativeDirection(RelativeDirection.LEFT);

    	Leg leg4 = new Leg();
    	it.getLegs().add(leg4);
    	leg4.setAgencyTimeZoneOffset(3600000);
    	leg4.setDistance(21114);
    	leg4.setDuration(900);

    	Stop stop4F = stop3T;
    	leg4.setFrom(stop4F);		

    	Stop stop4T = new Stop(GeoLocation.fromString("Zutphen::52.1450541734,6.19536101818"));
    	stop4T.setArrivalTime(stop4F.getDepartureTime().plusSeconds(leg4.getDuration()));
    	stop4T.setDepartureTime(OffsetDateTime.parse("2020-01-07T15:23:01+01:00").toInstant());
    	stop4T.setStopId("NL:55057");
    	stop4T.setPlatformCode("1b");
    	leg4.setTo(stop4T);
    	it.getStops().add(stop4T);
    	
    	leg4.setState(TripState.PLANNING);
    	leg4.setTraverseMode(TraverseMode.RAIL);
    	leg4.setLegGeometry(GeometryHelper.createLegGeometry(new EncodedPolylineBean("e_{|Hqyjf@}b@lcCk{@n|K}e@|rC{]hfCwIxe@gOxZqoEz|EkKdTgy@laC_Mn_BtHjzB|GzQz~@xqA", null, 14)));
    	leg4.setAgencyName("Arriva");
    	leg4.setRouteType(2);
    	leg4.setRouteId("NL:17752");
    	leg4.setRouteLongName("Zutphen <-> Winterswijk ST30800");
    	leg4.setRouteShortName("Stoptrein");
    	leg4.setHeadsign("Zutphen");

    	Leg leg5 = new Leg();
    	it.getLegs().add(leg5);
    	leg5.setAgencyTimeZoneOffset(3600000);
    	leg5.setDistance(655);
    	leg5.setDuration(554);

    	Stop stop5F = stop4T;
    	leg5.setFrom(stop5F);		

    	Stop stop5T = new Stop(GeoLocation.fromString("Rabobank Zutphen::52.148125,6.196966"));
    	stop5T.setArrivalTime(stop4F.getDepartureTime().plusSeconds(leg4.getDuration()));
    	leg5.setTo(stop5T);
    	it.getStops().add(stop5T);

    	leg5.setState(TripState.PLANNING);
    	leg5.setTraverseMode(TraverseMode.WALK);
    	leg5.setLegGeometry(GeometryHelper.createLegGeometry(new EncodedPolylineBean("arg}Ho_yd@Pj@p@bDCBBHA@EDw@n@{@f@A@KQOa@I]_A_FgB~@QoAc@wCCKCIEIw@gACCG?_Aj@k@XODGw@gA\\Gk@XK", null, 30)));
    	
    	leg5.setGuideSteps(new ArrayList<>());
    	GuideStep ws5_1 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_1);
    	ws5_1.setAbsoluteDirection(AbsoluteDirection.SOUTHWEST);
    	ws5_1.setDistance(80);
    	ws5_1.setLatitude(52.14513155204586);
    	ws5_1.setLongitude(6.195281985854009);
    	ws5_1.setName("Platform 2b");
    	ws5_1.setRelativeDirection(RelativeDirection.DEPART);
    	
    	GuideStep ws5_2 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_2);
    	ws5_2.setAbsoluteDirection(AbsoluteDirection.NORTHWEST);
    	ws5_2.setDistance(2);
    	ws5_2.setLatitude(52.1447967);
    	ws5_2.setLongitude(6.194240000000001);
    	ws5_2.setName("path");
    	ws5_2.setBogusName(true);
    	ws5_2.setRelativeDirection(RelativeDirection.RIGHT);

    	GuideStep ws5_3 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_3);
    	ws5_3.setAbsoluteDirection(AbsoluteDirection.SOUTHWEST);
    	ws5_3.setDistance(82);
    	ws5_3.setLatitude(52.1448115);
    	ws5_3.setLongitude(6.194225800000001);
    	ws5_3.setName("NS Stationstunnel");
    	ws5_3.setRelativeDirection(RelativeDirection.LEFT);

    	GuideStep ws5_4 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_4);
    	ws5_4.setAbsoluteDirection(AbsoluteDirection.NORTHEAST);
    	ws5_4.setDistance(120);
    	ws5_4.setLatitude(52.1454218);
    	ws5_4.setLongitude(6.1936831);
    	ws5_4.setName("Lijmerij");
    	ws5_4.setRelativeDirection(RelativeDirection.RIGHT);

    	GuideStep ws5_5 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_5);
    	ws5_5.setAbsoluteDirection(AbsoluteDirection.NORTH);
    	ws5_5.setDistance(62);
    	ws5_5.setLatitude(52.145932300000005);
    	ws5_5.setLongitude(6.1952143);
    	ws5_5.setName("Statenbolwerk");
    	ws5_5.setRelativeDirection(RelativeDirection.LEFT);

    	GuideStep ws5_6 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_6);
    	ws5_6.setAbsoluteDirection(AbsoluteDirection.EAST);
    	ws5_6.setDistance(218);
    	ws5_6.setLatitude(52.1464538);
    	ws5_6.setLongitude(6.194897500000001);
    	ws5_6.setName("Noorderhavenstraat");
    	ws5_6.setRelativeDirection(RelativeDirection.RIGHT);

    	GuideStep ws5_7 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_7);
    	ws5_7.setAbsoluteDirection(AbsoluteDirection.EAST);
    	ws5_7.setDistance(20);
    	ws5_7.setLatitude(52.1477512);
    	ws5_7.setLongitude(6.1962102);
    	ws5_7.setName("Dreef");
    	ws5_7.setRelativeDirection(RelativeDirection.RIGHT);

    	GuideStep ws5_8 = new GuideStep();
    	leg5.getGuideSteps().add(ws5_8);
    	ws5_8.setAbsoluteDirection(AbsoluteDirection.NORTH);
    	ws5_8.setDistance(72);
    	ws5_8.setLatitude(52.147799400000004);
    	ws5_8.setLongitude(6.1964958);
    	ws5_8.setName("parking aisle");
    	ws5_8.setBogusName(true);
    	ws5_8.setRelativeDirection(RelativeDirection.LEFT);

    	plan.addItineraries(Collections.singletonList(it));
    	PlannerReport report = new PlannerReport();
    	report.setFrom(plan.getFrom());
    	report.setTo(plan.getFrom());
    	report.setExecutionTime(3500);
    	report.setMaxWalkDistance(plan.getMaxWalkDistance());
    	report.setEarliestDepartureTime(plan.getEarliestDepartureTime());
    	report.setLatestArrivalTime(plan.getLatestArrivalTime());
    	report.setNrItineraries(plan.getItineraries().size());
    	report.setNrSeats(plan.getNrSeats());
    	report.setStartPosition(0);
    	report.setTravelTime(plan.getTravelTime());
    	report.setUseAsArrivalTime(plan.isUseAsArrivalTime());
    	report.setStatusCode(Response.Status.OK.getStatusCode());
    	report.setRequestGeometry(GeometryHelper.createLines(plan.getFrom().getPoint().getCoordinate(), 
    			plan.getTo().getPoint().getCoordinate(), null));
    	report.setToolType(ToolType.MANUAL);
    	it.getLegs().forEach(leg -> leg.setPlannerReport(report));
    	
    	plan.addPlannerReport(report);
    	plan.setRequestDuration(report.getExecutionTime());
    	return plan;
    }

	public static TripPlan createShoutOutTripPlan(User traveller, String nowIso, GeoLocation from, GeoLocation to, String travelTimeIso, boolean isArriveBy, Long requestDuration) {
		TripPlan plan = new TripPlan();
		plan.setPlanType(PlanType.SHOUT_OUT);
		plan.setRequestTime(Instant.parse(nowIso));
		plan.setTraveller(traveller);
		plan.setFrom(from);
		plan.setTo(to);
		plan.setTravelTime(Instant.parse(travelTimeIso));
		plan.setUseAsArrivalTime(isArriveBy);
		plan.setTraverseModes(new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.RIDESHARE })));
		plan.setMaxWalkDistance(1000);
		plan.setNrSeats(1);
		plan.setRequestDuration(requestDuration);
		return plan;
	}
    
    public static TripPlan createRidesharePlan(User traveller, String nowIso, GeoLocation from, GeoLocation to, String travelTimeIso, boolean useArriveBy, int tripDuration, String rideRef) {
		TripPlan plan = new TripPlan();
		plan.setPlanType(PlanType.REGULAR);
		plan.setRequestTime(Instant.parse(nowIso));
		plan.setTraveller(traveller);
		plan.setFrom(from);
		plan.setTo(to);
		plan.setTravelTime(Instant.parse(travelTimeIso));
		plan.setUseAsArrivalTime(useArriveBy);
		plan.setTraverseModes(new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.RIDESHARE })));
		plan.setMaxWalkDistance(1000);
		plan.setNrSeats(1);
		
		Itinerary it = new Itinerary();
		if (plan.isUseAsArrivalTime()) {
			it.setArrivalTime(plan.getTravelTime());
			it.setDepartureTime(plan.getTravelTime().minusSeconds(tripDuration));
		} else {
			it.setDepartureTime(plan.getTravelTime());
			it.setArrivalTime(plan.getTravelTime().plusSeconds(tripDuration));
		}
    	it.setDuration(tripDuration);
    	it.setStops(new ArrayList<>());
    	it.setLegs(new ArrayList<>());

    	Leg leg1 = new Leg();
    	it.getLegs().add(leg1);
    	leg1.setAgencyId("NB:RS1234");
    	leg1.setAgencyName("Netmobiel Rideshare Services");
    	leg1.setAgencyTimeZoneOffset(3600000);
    	leg1.setDistance(Math.toIntExact(Math.round(from.getDistanceFlat(to) * 1000)));
    	leg1.setDuration(it.getDuration());
    	
    	Stop stop1F = new Stop(plan.getFrom());
    	stop1F.setDepartureTime(it.getDepartureTime());
    	leg1.setFrom(stop1F);		
    	it.getStops().add(stop1F);
    	
    	Stop stop1T = new Stop(plan.getTo());
    	stop1T.setArrivalTime(it.getArrivalTime());
    	leg1.setTo(stop1T);
    	it.getStops().add(stop1T);

    	leg1.setState(TripState.PLANNING);
    	leg1.setTraverseMode(TraverseMode.RIDESHARE);
    	leg1.setTripId(rideRef);
    	leg1.setDriverId("urn:nb:rs:user:1");
    	leg1.setDriverName("Piet Pietersma");
    	leg1.setVehicleId("urn.nb:rs:car:5");
    	leg1.setVehicleLicensePlate("52-PH-VD");
    	leg1.setVehicleName("Volvo V70");
    	// For rideshare booking is always required
		leg1.setBookingRequired(true);

    	plan.addItineraries(Collections.singletonList(it));
    	PlannerReport report = new PlannerReport();
    	report.setFrom(plan.getFrom());
    	report.setTo(plan.getFrom());
    	report.setExecutionTime(1500);
    	report.setMaxWalkDistance(plan.getMaxWalkDistance());
    	report.setEarliestDepartureTime(plan.getEarliestDepartureTime());
    	report.setLatestArrivalTime(plan.getLatestArrivalTime());
    	report.setNrItineraries(plan.getItineraries().size());
    	report.setNrSeats(plan.getNrSeats());
    	report.setStartPosition(0);
    	report.setTravelTime(plan.getTravelTime());
    	report.setUseAsArrivalTime(plan.isUseAsArrivalTime());
    	report.setStatusCode(Response.Status.OK.getStatusCode());
    	report.setRequestGeometry(GeometryHelper.createLines(plan.getFrom().getPoint().getCoordinate(), 
    			plan.getTo().getPoint().getCoordinate(), null));
    	report.setToolType(ToolType.MANUAL);
    	it.getLegs().forEach(leg -> leg.setPlannerReport(report));
    	
    	plan.addPlannerReport(report);
    	plan.setRequestDuration(report.getExecutionTime());
    	return plan;
    }
    
	public static Trip createTrip(User traveller, TripPlan plan) {
		Itinerary itinerary = plan.getItineraries().iterator().next();
        Trip trip = new Trip();
        trip.setArrivalTimeIsPinned(plan.isUseAsArrivalTime());
        trip.setFrom(plan.getFrom());
        trip.setTo(plan.getTo());
//    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	trip.setTraveller(traveller);
    	trip.setState(TripState.SCHEDULED);
    	trip.setItinerary(itinerary);
    	trip.setNrSeats(plan.getNrSeats());
    	return trip;
    }
    
	public static PlannerResult createShoutOutSolution(GeoLocation from, GeoLocation to, TripPlan shoutOutplan) {
		if (shoutOutplan.isUseAsArrivalTime()) {
			throw new IllegalStateException("Fixture method does not support useAsArrivalTime");
		}
		Itinerary it = new Itinerary();
    	it.setStops(new ArrayList<>());
    	it.setLegs(new ArrayList<>());

    	Leg leg1 = new Leg();
    	it.getLegs().add(leg1);
    	leg1.setDistance(10000);
    	leg1.setDuration(60 * 15);
    	
    	Stop stop1 = new Stop(from);
    	stop1.setDepartureTime(shoutOutplan.getTravelTime());
    	leg1.setFrom(stop1);		
    	it.getStops().add(stop1);
    	
    	Stop stop2 = new Stop(shoutOutplan.getFrom());
    	stop2.setArrivalTime(stop1.getDepartureTime().plusSeconds(leg1.getDuration()));
    	stop2.setDepartureTime(stop2.getArrivalTime());
    	leg1.setTo(stop2);
    	it.getStops().add(stop2);

    	Leg leg2 = new Leg();
    	it.getLegs().add(leg2);
    	leg2.setDistance(30000);
    	leg2.setDuration(60 * 45);
    	leg2.setFrom(stop2);		

    	Stop stop3 = new Stop(shoutOutplan.getTo());
    	stop3.setArrivalTime(stop2.getDepartureTime().plusSeconds(leg2.getDuration()));
    	leg2.setTo(stop3);
    	it.getStops().add(stop3);

    	if (to != null) {
        	stop3.setDepartureTime(stop3.getArrivalTime());
        	Leg leg3 = new Leg();
        	it.getLegs().add(leg3);
        	leg2.setDistance(10000);
        	leg2.setDuration(60 * 15);
        	leg2.setFrom(stop3);		

        	Stop stop4 = new Stop(to);
        	stop4.setArrivalTime(stop3.getDepartureTime().plusSeconds(leg3.getDuration()));
        	leg3.setTo(stop4);
        	it.getStops().add(stop4);
    	}
    	it.getLegs().forEach(leg -> leg.setTraverseMode(TraverseMode.CAR));
    	it.updateCharacteristics();
    	PlannerReport report = new PlannerReport();
    	report.setFrom(from);
    	report.setTo(to);
    	report.setTravelTime(shoutOutplan.getTravelTime());
    	PlannerResult result = new PlannerResult(report);
    	result.addItineraries(Collections.singletonList(it));
    	return result;
	}

}
