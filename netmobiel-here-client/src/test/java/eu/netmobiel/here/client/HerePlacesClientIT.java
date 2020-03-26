package eu.netmobiel.here.client;


import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import eu.netmobiel.here.places.HerePlacesClient;
import eu.netmobiel.here.places.Jackson2ObjectMapperContextResolver;
import eu.netmobiel.here.places.model.AutosuggestMediaType;
import eu.netmobiel.here.places.model.AutosuggestPlace;

@RunWith(Arquillian.class)
public class HerePlacesClientIT {
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
            .addPackage(AutosuggestPlace.class.getPackage())
            .addClass(HerePlacesClient.class)
            .addClass(Jackson2ObjectMapperContextResolver.class)
            // Arquillian tests need the beans.xml to recognize it as a CDI application
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // Take car of removing the default json provider, because we use jackson everywhere (unfortunately).
        	.addAsWebInfResource("jboss-deployment-structure.xml")
        	.addAsResource("log4j.properties");
//		System.out.println(archive.toString(Formatters.VERBOSE));
        return archive;
    }

    @Inject
    private HerePlacesClient client;

    @Inject
    private Logger log;

    private void dump(String prefix, AutosuggestPlace[] suggestions) {
    	log.debug(prefix + ": " + Arrays.stream(suggestions).map(as -> as.toString()).collect(Collectors.joining("\n")));
    }
    @Test
    public void testAutoSuggest() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	AutosuggestMediaType result = client.listAutosuggestions("en", myLocation, 30000, null, null, null, null);
    	assertNotNull(result);
    	assertNotNull(result.results);
    	dump("testAutoSuggest", result.results);
    }

    @Test
    public void testAutoSuggestSize() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	Integer maxResults = 2;
    	AutosuggestMediaType result = client.listAutosuggestions("en", myLocation, 30000, null, null, null, maxResults);
    	assertNotNull(result);
    	assertNotNull(result.results);
    	assertEquals(maxResults.intValue(), result.results.length);
    }
    
    @Test
    public void testAutoSuggestHighlight() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	String hls = "<span class=\"search-hit\">";
    	String hle = "</span>";
    	AutosuggestMediaType result = client.listAutosuggestions("en", myLocation, 30000, null, hls, hle, null);
    	assertNotNull(result);
    	assertNotNull(result.results);
    	dump("testAutoSuggestHighlight", result.results);
    	log.debug("testAutoSuggestHighlight: " + Arrays.stream(result.results).map(as -> as.highlightedTitle).collect(Collectors.joining("\n")));
    	// Ok, Not all titles are highlighted by HERE. 
    	boolean foundStart = false;
    	boolean foundEnd = false;
    	for (AutosuggestPlace p: result.results) {
//    		assertTrue("Must contain hls", p.highlightedTitle.contains(hls));
//    		assertTrue("Must contain hle", p.highlightedTitle.contains(hle));
    		if (p.highlightedTitle.contains(hls)) {
    			foundStart = true;
    		}
    		if (p.highlightedTitle.contains(hle)) {
    			foundEnd = true;
    		}
    	}
		assertTrue("Must contain hls", foundStart);
		assertTrue("Must contain hle", foundEnd);
    }

    @Test
    public void testAutoSuggestResultTypePlace() throws Exception {
    	GeoLocation myLocation = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
    	String resultTypes = "place,address";
    	Set<String> resultTypeSet = new HashSet<>(Arrays.asList(resultTypes.split(",")));
    	AutosuggestMediaType result = client.listAutosuggestions("en", myLocation, 30000, null, null, null, null);
    	assertNotNull(result);
    	assertNotNull(result.results);
    	Set<String> foundResultTypeSet = new HashSet<>();
    	for (AutosuggestPlace p: result.results) {
    		foundResultTypeSet.add(p.resultType);
    	}
    	result = client.listAutosuggestions("en", myLocation, 30000, resultTypes, null, null, null);
    	Set<String> filteredResultTypeSet = new HashSet<>();
    	for (AutosuggestPlace p: result.results) {
    		filteredResultTypeSet.add(p.resultType);
    	}
    	Set<String> extraResultTypes = new HashSet<>(foundResultTypeSet);
    	extraResultTypes.removeAll(resultTypeSet);
    	assertTrue("Original result should contain multiple result types", extraResultTypes.size() > 0);
    	assertTrue("Filtered set should contain same result types", filteredResultTypeSet.equals(resultTypeSet));
    }
}
