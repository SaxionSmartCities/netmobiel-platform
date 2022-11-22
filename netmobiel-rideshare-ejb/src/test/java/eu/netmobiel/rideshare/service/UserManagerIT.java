package eu.netmobiel.rideshare.service;

import static org.junit.Assert.*;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.test.Fixture;
import eu.netmobiel.rideshare.test.RideshareIntegrationTestBase;

@RunWith(Arquillian.class)
public class UserManagerIT extends RideshareIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
                .addPackages(true, RideDao.class.getPackage())
	            .addClass(RideshareUserManager.class);
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private RideshareUserManager userManager;

    private RideshareUser driver1;
    private Car car1;

    @Override
    public boolean isSecurityRequired() {
    	return true;
    }

    @Override
    protected void insertData() throws Exception {
        driver1 = Fixture.createUser(loginContextDriver);
		em.persist(driver1);

		car1 = Fixture.createCarFordThunderbird(driver1);
		em.persist(car1);
    }

    @Test
    public void testListUsers() throws Exception {
        List<RideshareUser> users = userManager.listUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        log.info("List users: #" + users.size());
        users.forEach(u -> log.debug(u.toString()));
    }

}
