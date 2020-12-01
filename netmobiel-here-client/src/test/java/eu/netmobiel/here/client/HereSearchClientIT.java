package eu.netmobiel.here.client;


import static org.junit.Assert.*;

import java.io.File;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.here.search.HereSearchClient;
import eu.netmobiel.here.search.Jackson2ObjectMapperContextResolver;
import eu.netmobiel.here.search.model.GeocodeResultItem;
import eu.netmobiel.here.search.model.OpenSearchReverseGeocodeResponse;
import eu.netmobiel.here.search.model.ResultType;

@RunWith(Arquillian.class)
public class HereSearchClientIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
				.asFile();
		Archive<?> archive = ShrinkWrap.create(WebArchive.class, "test.war")
       		.addAsLibraries(deps)
            .addPackage(OpenSearchReverseGeocodeResponse.class.getPackage())
            .addClass(HereSearchClient.class)
            .addClass(Jackson2ObjectMapperContextResolver.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Take care of removing the default json provider, because we use jackson everywhere (unfortunately).
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	.addAsResource("log4j.properties");
//		System.out.println(archive.toString(Formatters.VERBOSE));
        return archive;
    }

    @Inject
    private HereSearchClient client;

	@Inject
    private Logger log;
	 
    @Test
    public void testReverseGeocode() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	OpenSearchReverseGeocodeResponse result = client.getReverseGeocode(myLocation, null);
    	assertNotNull(result);
    	assertNotNull(result.items);
    	assertEquals(1, result.items.length);
    	GeocodeResultItem item = result.items[0];
    	assertEquals("Kennedystraat 8, 7136 LX Zieuwent, Nederland", item.title);
    	assertNotNull(item.id);
    	assertEquals(ResultType.houseNumber, item.resultType);
    	assertEquals("Kennedystraat 8, 7136 LX Zieuwent, Nederland", item.address.label);
    	assertEquals("NLD", item.address.countryCode);
    	assertEquals("Nederland", item.address.countryName);
    	assertEquals("GE", item.address.stateCode);
    	assertEquals("Gelderland", item.address.state);
    	assertEquals("Oost Gelre", item.address.county);
    	assertEquals("Zieuwent", item.address.city);
    	assertEquals("Kennedystraat", item.address.street);
    	assertEquals("7136 LX", item.address.postalCode);
    	assertEquals("8", item.address.houseNumber);
    	assertEquals(52.00431, item.position.lat, 1E-6);
    	assertEquals(6.51775, item.position.lng, 1E-6);
    	assertEquals(17, item.distance.intValue());
    	assertNotNull(item.mapView);
    	assertEquals(6.51663, item.mapView.west, 1E-6);
    	assertEquals(52.00408, item.mapView.south, 1E-6);
    	assertEquals(6.51955, item.mapView.east, 1E-6);
    	assertEquals(52.00423, item.mapView.north, 1E-6);
    }

    @Test
    public void testReverseGeocode_BadFormat() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::452.004166,1006.517835");
    	try {
    		client.getReverseGeocode(myLocation, null);
    		fail("Expected exception");
    	} catch (Exception ex) {
    		log.error("Anticipated exception: " + ex.toString());
    		assertTrue(ex instanceof WebApplicationException);
    		WebApplicationException wex = (WebApplicationException) ex;
    		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), wex.getResponse().getStatus());
    		assertEquals("HERE: Illegal input for parameter 'at' caused by Actual parameter value: '452.004166,1006.517835' - requirement failed: Latitude must be between -90 <= X <= 90, actual: 452.004166", wex.getMessage());
    	}
    }
}
