package eu.netmobiel.planner.repository;

import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.planner.test.PlannerIntegrationTestBase;

@RunWith(Arquillian.class)
public class OtpClusterDaoIT extends PlannerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(OtpClusterDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private OtpClusterDao dao;

    @Inject
    private Logger log;

    @Override
    protected void insertData() throws Exception {
    }
    
    @Test
    public void testListNearbyClusters() throws Exception {
    	GeoLocation from = Fixture.placeZieuwent;
    	GeoLocation to = Fixture.placeSlingeland;
    	EligibleArea ea = EllipseHelper.calculateEllipse(from.getPoint(), to.getPoint(), null, 0.5f);
        List<OtpCluster> clusters = dao.findImportantHubs(from, ea.eligibleAreaGeometry, 10);
        assertNotNull(clusters);
        assertEquals(10, clusters.size());
        log.debug("testListNearbyClusters: \n\t" + clusters.stream().map(c -> c.toString()).collect(Collectors.joining("\n\t")));
    }
}
