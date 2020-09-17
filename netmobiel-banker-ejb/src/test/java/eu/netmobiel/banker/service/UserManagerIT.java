package eu.netmobiel.banker.service;

import static org.junit.Assert.*;

import java.security.PrivilegedAction;
import java.util.List;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;

@RunWith(Arquillian.class)
public class UserManagerIT extends BankerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
	        .addClass(UserManager.class)
	        .addClass(LedgerService.class)
	        .addClass(BankerStartupService.class)
	        ;
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private UserManager userManager;

    private User driver1;

    @Override
    public boolean isSecurityRequired() {
    	return true;
    }

    @Override
    protected void insertData() throws Exception {
        driver1 = Fixture.createUser(loginContextDriver);
		em.persist(driver1);

    }

    @Test
    public void testListUsers() throws Exception {
        List<User> users = userManager.listUsers();
        assertNotNull(users);
        log.info("List users: #" + users.size());
        users.forEach(u -> log.debug(u.toString()));
        assertEquals(1, users.size());
    }

    //FIXME I can't get this working. How do I pass the right security context to an EJB in the test?
//    @Test
//    public void testRegisterCallingUser() throws Exception {
//    	loginContextDriver.login();
//    	try {
//            Subject.doAs(loginContextDriver.getSubject(), new PrivilegedAction<Object>() {
//                 @Override
//                 public Object run() {
//                      userManager.registerCallingUser();
//                      return null;
//                 }
//            });
//        } finally {
////            loginContextDriver.logout();
//        }
//        flush();
//        List<User> users = em.createQuery("from User", User.class).getResultList();
//        assertNotNull(users);
//        assertEquals(1, users.size());
//        users.forEach(u -> log.debug(u.toString()));
//    }
}
