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
import eu.netmobiel.here.search.api.model.AutosuggestEntityResultItem;
import eu.netmobiel.here.search.api.model.GeocodeResultItem;
import eu.netmobiel.here.search.api.model.OpenSearchAutosuggestResponse;
import eu.netmobiel.here.search.api.model.OpenSearchReverseGeocodeResponse;

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
    	assertNotNull(result.getItems());
    	assertEquals(1, result.getItems().size());
    	GeocodeResultItem item = result.getItems().get(0);
    	assertEquals("Kennedystraat 8, 7136 LX Zieuwent, Nederland", item.getTitle());
    	assertNotNull(item.getId());
    	assertEquals(GeocodeResultItem.ResultTypeEnum.HOUSENUMBER, item.getResultType());
    	assertEquals("Kennedystraat 8, 7136 LX Zieuwent, Nederland", item.getAddress().getLabel());
    	assertEquals("NLD", item.getAddress().getCountryCode());
    	assertEquals("Nederland", item.getAddress().getCountryName());
    	assertEquals("GE", item.getAddress().getStateCode());
    	assertEquals("Gelderland", item.getAddress().getState());
    	assertEquals("Oost Gelre", item.getAddress().getCounty());
    	assertEquals("Zieuwent", item.getAddress().getCity());
    	assertEquals("Kennedystraat", item.getAddress().getStreet());
    	assertEquals("7136 LX", item.getAddress().getPostalCode());
    	assertEquals("8", item.getAddress().getHouseNumber());
    	assertEquals(52.00431, item.getPosition().getLat(), 1E-6);
    	assertEquals(6.51775, item.getPosition().getLng(), 1E-6);
    	assertEquals(17, item.getDistance().intValue());
    	assertNotNull(item.getMapView());
    	assertEquals(6.51663, item.getMapView().getWest(), 1E-6);
    	assertEquals(52.00408, item.getMapView().getSouth(), 1E-6);
    	assertEquals(6.51955, item.getMapView().getEast(), 1E-6);
    	assertEquals(52.00423, item.getMapView().getNorth(), 1E-6);
    }

    @SuppressWarnings("resource")
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
    
    @Test
    public void testAutoSuggest() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	int radius = 150000;
    	OpenSearchAutosuggestResponse result = client.listAutosuggestions("slingeland", myLocation, radius, null, null, null);
/*    	
        {
            "title": "Slingeland Ziekenhuis",
            "id": "here:pds:place:528u1hrx-fcf8abc218384ad990a9238eaa81babf",
            "resultType": "place",
            "address": {
                "label": "Slingeland Ziekenhuis, Kruisbergseweg 25, 7009 Doetinchem, Nederland"
            },
            "position": {
                "lat": 51.97641,
                "lng": 6.28509
            },
            "access": [
                {
                    "lat": 51.97641,
                    "lng": 6.28569
                }
            ],
            "distance": 16233,
            "categories": [
                {
                    "id": "800-8000-0159",
                    "name": "Ziekenhuis",
                    "primary": true
                }
            ],
            "references": [
                {
                    "supplier": {
                        "id": "core"
                    },
                    "id": "50865913"
                }
            ],
            "highlights": {
                "title": [
                    {
                        "start": 0,
                        "end": 10
                    }
                ],
                "address": {
                    "label": [
                        {
                            "start": 0,
                            "end": 10
                        }
                    ]
                }
            }
        },
*/
    	assertNotNull(result);
    	assertNotNull(result.getItems());
    	assertTrue(result.getItems().size() > 0);
    	AutosuggestEntityResultItem item = result.getItems().get(0);
    	assertEquals("Slingeland Ziekenhuis", item.getTitle());
    	assertEquals(AutosuggestEntityResultItem.ResultTypeEnum.PLACE, item.getResultType());
    	assertNotNull(item.getAddress());
    	assertEquals("Slingeland Ziekenhuis, Kruisbergseweg 25, 7009 Doetinchem, Nederland", item.getAddress().getLabel());
    	assertNotNull(item.getPosition());
    	assertEquals(51.97641, item.getPosition().getLat(), 1E-6);
    	assertEquals(6.28509, item.getPosition().getLng(), 1E-6);
    	assertNotNull(item.getAccess());
    	assertTrue(item.getAccess().size() > 0);
    	assertEquals(51.97641, item.getAccess().get(0).getLat(), 1E-6);
    	assertEquals(6.28569, item.getAccess().get(0).getLng(), 1E-6);
    	assertEquals(16233, item.getDistance().intValue());
    	
    }
}
