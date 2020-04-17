package eu.netmobiel.profile.client;


import static org.junit.Assert.*;

import java.io.File;

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

@RunWith(Arquillian.class)
public class ProfileClientIT {
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
            .addClass(ProfileClient.class)
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
    private ProfileClient client;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;

    /**
     * Managed Identity of net@netmobiel.net.
     */
    private static final String testManagedIdentity = "5fd8defe-848e-4b66-8e6f-8a7d3b7ee485";
    /**
     * FCM token of net@netmobiel.net.
     */
    private static final String testFcmToken = "eFxxs0F4uEadoiqHu54Byt:APA91bHJHwXFxH3jOSUybFs7iRw48kpIHPsGM31BpzHJZPGsaa37c6SXhjjC-FiJyNcGKowwKJiySKl6AjGT0QDA0K-yjlnrqfHudiEt6wvUHYCeDC6JqR7Tcc-Ns5qPK_J5n8D3dwci";

    @Test
    public void testGetFcmToken() throws Exception {
		String token = client.getFirebaseToken(testManagedIdentity);
    	assertNotNull(token);
    	assertEquals(testFcmToken, token);
    }

}
