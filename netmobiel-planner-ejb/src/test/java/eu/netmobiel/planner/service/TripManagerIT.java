package eu.netmobiel.planner.service;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.planner.model.AbsoluteDirection;
import eu.netmobiel.planner.model.GuideStep;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Leg_;
import eu.netmobiel.planner.model.RelativeDirection;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.Trip_;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.planner.test.PlannerIntegrationTestBase;

@RunWith(Arquillian.class)
public class TripManagerIT extends PlannerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(EventListenerHelper.class)
            .addClass(TripDao.class)
            .addClass(TripManager.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @EJB
    private TripManager tripManager;

    @Inject
    private EventListenerHelper eventListenerHelper;

    @Inject
    private Logger log;

    @Override
    protected void insertData() throws Exception {
		eventListenerHelper.reset();
    }
    
    private Trip createShoutOutTrip(String departureTimeIso, String arrivalTimeIso) {
        Trip trip = new Trip();
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant departureTime = departureTimeIso != null ? OffsetDateTime.parse(departureTimeIso).toInstant() : null;
    	Instant arrivalTime = arrivalTimeIso != null ? OffsetDateTime.parse(arrivalTimeIso).toInstant() : null;
//    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	trip.setFrom(fromPlace);
    	trip.setTo(toPlace);
    	trip.setState(TripState.PLANNING);
    	trip.setDepartureTime(departureTime);
    	trip.setArrivalTime(arrivalTime);
    	return trip;
    }

    private Trip createSimpleTrip(String departureTimeIso, String arrivalTimeIso) {
        Trip trip = new Trip();
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
    	Instant departureTime = OffsetDateTime.parse(departureTimeIso).toInstant();
    	Instant arrivalTime = OffsetDateTime.parse(arrivalTimeIso).toInstant();
//    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	trip.setFrom(fromPlace);
    	trip.setTo(toPlace);
    	trip.setState(TripState.PLANNING);
    	trip.setDepartureTime(departureTime);
    	trip.setArrivalTime(arrivalTime);
    	trip.setDuration(Math.toIntExact(Duration.between(departureTime, arrivalTime).getSeconds()));
    	trip.setStops(new ArrayList<>());
    	trip.setLegs(new ArrayList<>());
    	
    	Leg leg1 = new Leg();
    	trip.getLegs().add(leg1);
    	leg1.setAgencyTimeZoneOffset(3600000);
    	leg1.setDistance(Math.toIntExact(Math.round(toPlace.getDistanceTo(fromPlace) * 1000)));
    	leg1.setDuration(trip.getDuration());
    
    	Stop stop1F = new Stop(fromPlace);
    	stop1F.setDepartureTime(departureTime);
    	leg1.setFrom(stop1F);		
    	trip.getStops().add(stop1F);
    	
    	Stop stop1T = new Stop(GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542"));
    	stop1T.setArrivalTime(stop1F.getDepartureTime().plusSeconds(leg1.getDuration()));
    	stop1T.setStopId("NL:1532280");
    	stop1T.setStopCode("44930020");
    	leg1.setTo(stop1T);
    	trip.getStops().add(stop1T);

    	return trip;
    }

    @Test
    public void testCreateShoutOutTrip() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        int nrTripsStart = trips.getData().size();
        
        Trip trip = createShoutOutTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
    	Long id = tripManager.createTrip(traveller, trip, true);
        assertNotNull(id);
        
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        log.info("List trips: #" + trips.getData().size());
        trips.getData().stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> log.debug(t.toString()));
        assertEquals(nrTripsStart + 1, trips.getData().size());
        
		assertEquals(1, eventListenerHelper.getShoutOutRequestedEventCount());
        
    }

    
    @Test
    public void testGetTrip() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
        flush();
        Trip trip = createSimpleTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
    	Long id = tripManager.createTrip(traveller, trip, true);
        assertNotNull(id);
        flush();
    	assertFalse(em.contains(trip));
        
        trip = tripManager.getTrip(id);
        assertNotNull(trip);
        assertEquals(id, trip.getId());
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertTrue(em.contains(trip));
        flush();
    	assertFalse(em.contains(trip));
    	assertTrue(puu.isLoaded(trip, Trip_.LEGS));
    	assertTrue(puu.isLoaded(trip, Trip_.STOPS));
    	assertTrue(puu.isLoaded(trip, Trip_.TRAVELLER));
    	trip.getLegs().forEach(leg -> assertTrue(puu.isLoaded(leg, Leg_.GUIDE_STEPS)));
        assertNotNull(trip.getLegs());
        assertEquals(1, trip.getLegs().size());

    }
    
    private Trip createLargeTrip() {
        Trip trip = new Trip();
    	GeoLocation fromPlace = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	GeoLocation  toPlace = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
    	Instant departureTime = OffsetDateTime.parse("2020-01-07T14:32:56+01:00").toInstant();
    	Instant arrivalTime = OffsetDateTime.parse("2020-01-07T15:32:15+01:00").toInstant();
//    	TraverseMode[] modes = new TraverseMode[] { TraverseMode.CAR, TraverseMode.WALK }; 
    	trip.setFrom(fromPlace);
    	trip.setTo(toPlace);
    	trip.setState(TripState.PLANNING);
    	trip.setDepartureTime(departureTime);
    	trip.setArrivalTime(arrivalTime);
    	trip.setDuration(3559);
    	trip.setTransfers(1);
    	trip.setTransitTime(1980);
    	trip.setWaitingTime(798);
    	trip.setWalkDistance(943);
    	trip.setWalkTime(781);
    	trip.setStops(new ArrayList<>());
    	trip.setLegs(new ArrayList<>());

    	Leg leg1 = new Leg();
    	trip.getLegs().add(leg1);
    	leg1.setAgencyTimeZoneOffset(3600000);
    	leg1.setDistance(153);
    	leg1.setDuration(123);
    	
    	Stop stop1F = new Stop(fromPlace);
    	stop1F.setDepartureTime(OffsetDateTime.parse("2020-01-07T14:32:56+01:00").toInstant());
    	leg1.setFrom(stop1F);		
    	trip.getStops().add(stop1F);
    	
    	Stop stop1T = new Stop(GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542"));
    	stop1T.setArrivalTime(stop1F.getDepartureTime().plusSeconds(leg1.getDuration()));
    	stop1T.setDepartureTime(OffsetDateTime.parse("2020-01-07T14:35:00+01:00").toInstant());
    	stop1T.setStopId("NL:1532280");
    	stop1T.setStopCode("44930020");
    	leg1.setTo(stop1T);
    	trip.getStops().add(stop1T);

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
    	trip.getLegs().add(leg2);
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
    	trip.getStops().add(stop2T);

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
    	trip.getLegs().add(leg3);
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
    	trip.getStops().add(stop3T);

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
    	trip.getLegs().add(leg4);
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
    	trip.getStops().add(stop4T);
    	
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
    	trip.getLegs().add(leg5);
    	leg5.setAgencyTimeZoneOffset(3600000);
    	leg5.setDistance(655);
    	leg5.setDuration(554);

    	Stop stop5F = stop4T;
    	leg5.setFrom(stop5F);		

    	Stop stop5T = new Stop(GeoLocation.fromString("Rabobank Zutphen::52.148125,6.196966"));
    	stop5T.setArrivalTime(stop4F.getDepartureTime().plusSeconds(leg4.getDuration()));
    	leg5.setTo(stop5T);
    	trip.getStops().add(stop5T);

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

    	return trip;
    }

    @Test
    public void testCreateFullTrip() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        int nrTripsStart = trips.getData().size();
        
        Trip trip = createLargeTrip();
        Long id = tripManager.createTrip(traveller, trip, true);
        assertNotNull(id);
        
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        log.info("List trips: #" + trips.getData().size());
        trips.getData().stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> log.debug(t.toString()));
        assertEquals(nrTripsStart + 1, trips.getData().size());
    }

    private Trip createRideshareTrip(User traveller, String rideRef) {
    	TripPlan plan = Fixture.createRidesharePlan(traveller, "2020-01-06T13:30:00Z", Fixture.placeZieuwent, Fixture.placeSlingeland, "2020-01-07T13:30:00Z", false, 60 * 35, "urn:nb:rs:ride:354");
    	Trip trip = Fixture.createTrip(traveller, plan);
		return trip;
    }

    @Test
    public void testCreateRideshareTrip_NoAutoBook() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	String rideRef = "urn:nb:rs:ride:354";
        Trip trip = createRideshareTrip(traveller, rideRef);

		// Set autobook false
    	Long id = tripManager.createTrip(traveller, trip, false);
        assertNotNull(id);
		Trip tripdb = em.createQuery("from Trip where id = :id", Trip.class)
				.setParameter("id", id)
				.getSingleResult();
		assertEquals(TripState.PLANNING, tripdb.getState());
		assertEquals(0, eventListenerHelper.getBookingRequestedEventCount());
    }

    @Test
    public void testCreateRideshareTrip_AutoBook() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        int nrTripsStart = trips.getData().size();
    	String rideRef = "urn:nb:rs:ride:354";
        Trip trip = createRideshareTrip(rideRef);

		// Set autobook true
    	Long id = tripManager.createTrip(traveller, trip, true);
        assertNotNull(id);
		Trip tripdb = em.createQuery("from Trip where id = :id", Trip.class)
				.setParameter("id", id)
				.getSingleResult();
		assertEquals(TripState.BOOKING, tripdb.getState());
		assertEquals(1, eventListenerHelper.getBookingRequestedEventCount());
        
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        log.info("List trips: #" + trips.getData().size());
        trips.getData().stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> log.debug(t.toString()));
        assertEquals(nrTripsStart + 1, trips.getData().size());

    }

    @Test
    public void testAssignRideshareBookingRef() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	String rideRef = "urn:nb:rs:ride:354";
        Trip trip = createRideshareTrip(rideRef);
		Optional<Leg> leg = trip.getItinerary().findLegByTripId(rideRef);
		assertTrue(leg.isPresent());
		assertNull(leg.get().getBookingId());

		// Set autobook true
    	Long id = tripManager.createTrip(traveller, trip, true);
    	flush();
        assertNotNull(id);
		Trip tripdb = em.createQuery("select t from Trip t join fetch t.legs where t.id = :id", Trip.class)
				.setParameter("id", id)
				.getSingleResult();
		leg = tripdb.getItinerary().findLegByTripId(rideRef);
		assertTrue(leg.isPresent());
		assertNull(leg.get().getBookingId());
		flush();

		String bookingRef = "urn:nb:rs:booking:12345";
		// Assign, without confirmation yet
    	tripManager.assignBookingReference(trip.getTripRef(), rideRef, bookingRef, false);
		flush();
		tripdb = em.createQuery("select t from Trip t join fetch t.legs where t.id = :id", Trip.class)
				.setParameter("id", id)
				.getSingleResult();
		assertEquals(TripState.BOOKING, tripdb.getState());
		assertEquals(1, eventListenerHelper.getBookingRequestedEventCount());
		assertTrue(tripdb.getItinerary().findLegByTripId(rideRef).isPresent());
		assertTrue(tripdb.getItinerary().findLegByBookingId(bookingRef).isPresent());
    }

    @Test
    public void testRemoveRideshareTrip_WhileBooking() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	String rideRef = "urn:nb:rs:ride:354";
        Trip trip = createRideshareTrip(rideRef);
		// Set autobook true
    	Long id = tripManager.createTrip(traveller, trip, true);
		String bookingRef = "urn:nb:rs:booking:12345";
		// Assign, without confirmation yet
    	tripManager.assignBookingReference(trip.getTripRef(), rideRef, bookingRef, false);
		eventListenerHelper.reset();
		flush();
		String reason = "Ik ga toch maar niet";
        tripManager.removeTrip(id, reason);
		assertEquals(1, eventListenerHelper.getBookingCancelledEventCount());
		// Trip is not hard removed.
		assertEquals(1, em.createQuery("select count(*) from Trip where id = :id", Long.class).setParameter("id", id).getSingleResult().intValue());
		Trip tripdb = em.createQuery("from Trip where id = :id", Trip.class)
				.setParameter("id", id)
				.getSingleResult();
		assertEquals(TripState.CANCELLED, tripdb.getState());
		assertEquals(reason, tripdb.getCancelReason());
    }

    @Test
    public void testRemoveFullTrip() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        int nrTripsStart = trips.getData().size();
        boolean autobook = false;
        
        Trip trip = createLargeTrip();
        Long id = tripManager.createTrip(traveller, trip, autobook);
        assertNotNull(id);
        
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(nrTripsStart + 1, trips.getData().size());

        tripManager.removeTrip(id, null);
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(nrTripsStart, trips.getData().size());

        // Note it is hard deleted because the autobook is switched off
    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, null, null);
        assertNotNull(trips);
        assertEquals(nrTripsStart, trips.getData().size());
    }

    @Test
    public void testRemoveShoutOutTrip() throws Exception {
    	User traveller = new User();
    	traveller.setId(1L);
    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        int nrTripsStart = trips.getData().size();
        boolean autobook = true;
        
        Trip trip = createShoutOutTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
        Long id = tripManager.createTrip(traveller, trip, autobook);
        assertNotNull(id);
        
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(nrTripsStart + 1, trips.getData().size());

        tripManager.removeTrip(id, null);
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(nrTripsStart, trips.getData().size());

        // Note it is hard deleted because it was still in planning status
    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, null, null);
        assertNotNull(trips);
        assertEquals(nrTripsStart, trips.getData().size());
    }


    @Test
    public void testListTrips() throws Exception {
    	User traveller = new User();
    	traveller.setId(2L);
    	PagedResult<Trip> trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(0, trips.getData().size());
        
        Trip trip1 = createShoutOutTrip("2020-01-07T14:30:00+01:00", "2020-01-07T16:30:00+01:00");
    	Long id1 = tripManager.createTrip(traveller, trip1, true);
        assertNotNull(id1);
        
        Trip trip2 = createShoutOutTrip("2020-01-08T14:30:00+01:00", "2020-01-08T16:30:00+01:00");
    	Long id2 = tripManager.createTrip(traveller, trip2, true);
        assertNotNull(id2);
        
        Trip trip3 = createSimpleTrip("2020-01-09T14:30:00+01:00", "2020-01-09T16:30:00+01:00");
        // Make it only soft-deletable. Auto book the trip so it will be scheduled.
    	Long id3 = tripManager.createTrip(traveller, trip3, true);
        assertNotNull(id3);
        tripManager.removeTrip(id3, "Ik ga toch maar niet op reis");
        
        // List all non-deleted trips
    	trips = tripManager.listTrips(traveller, null, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(2, trips.getData().size());
        
    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, null, null);
        assertNotNull(trips);
        assertEquals(3, trips.getData().size());

        // Check order on departure time
        assertEquals(id1, trips.getData().get(0).getId());
        assertEquals(id2, trips.getData().get(1).getId());
        assertEquals(id3, trips.getData().get(2).getId());

        // Check explicit sorting on departure time
    	trips = tripManager.listTrips(traveller, null, null, null, null, SortDirection.ASC, null, null);
        assertEquals(id1, trips.getData().get(0).getId());
        assertEquals(id2, trips.getData().get(1).getId());
    	trips = tripManager.listTrips(traveller, null, null, null, null, SortDirection.DESC, null, null);
        assertEquals(id2, trips.getData().get(0).getId());
        assertEquals(id1, trips.getData().get(1).getId());
        
    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, 1, 0);
        assertNotNull(trips);
        assertEquals(1, trips.getData().size());
        assertEquals(id1, trips.getData().get(0).getId());

    	trips = tripManager.listTrips(traveller, null, null, null, Boolean.TRUE, null, 1, 1);
        assertNotNull(trips);
        assertEquals(1, trips.getData().size());
        assertEquals(id2, trips.getData().get(0).getId());

    	trips = tripManager.listTrips(traveller, TripState.PLANNING, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(2, trips.getData().size());

    	trips = tripManager.listTrips(traveller, TripState.BOOKING, null, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(0, trips.getData().size());

        Instant since = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
    	trips = tripManager.listTrips(traveller, null, since, null, null, null, null, null);
        assertNotNull(trips);
        assertEquals(1, trips.getData().size());
        assertEquals(trips.getData().get(0).getId(), id2);

        Instant until = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
    	trips = tripManager.listTrips(traveller, null, null, until, null, null, null, null);
        assertNotNull(trips);
        assertEquals(1, trips.getData().size());
        assertEquals(trips.getData().get(0).getId(), id1);

        since = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
        until = OffsetDateTime.parse("2020-01-09T00:00:00+01:00").toInstant();
    	trips = tripManager.listTrips(traveller, null, since, until, null, null, null, null);
        assertNotNull(trips);
        assertEquals(1, trips.getData().size());
        assertEquals(trips.getData().get(0).getId(), id2);

        since = OffsetDateTime.parse("2020-01-08T14:30:00+01:00").toInstant();
        until = OffsetDateTime.parse("2020-01-08T14:30:00+01:00").toInstant().plusMillis(1);
    	trips = tripManager.listTrips(traveller, null, since, until, null, null, null, null);
        assertNotNull(trips);
        assertEquals(1, trips.getData().size());
        assertEquals(trips.getData().get(0).getId(), id2);

        since = OffsetDateTime.parse("2020-01-08T00:00:00+01:00").toInstant();
        until = OffsetDateTime.parse("2020-01-08T14:30:00+01:00").toInstant();
    	trips = tripManager.listTrips(traveller, null, since, until, null, null, null, null);
        assertNotNull(trips);
        assertEquals(0, trips.getData().size());

        try {
        	trips = tripManager.listTrips(traveller, null, null, null, null, null, 101, null);
        	fail("Expected a BadRequest on maxResults too high");
        } catch (BadRequestException ex) {
        	log.debug(ex.toString());
        }
        try {
        	trips = tripManager.listTrips(traveller, null, null, null, null, null, 0, null);
        	fail("Expected a BadRequest on maxResults too low");
        } catch (BadRequestException ex) {
        	log.debug(ex.toString());
        }
        try {
        	trips = tripManager.listTrips(traveller, null, null, null, null, null, 5, -1);
        	fail("Expected a BadRequest on offset too low");
        } catch (BadRequestException ex) {
        	log.debug(ex.toString());
        }

    }
}
