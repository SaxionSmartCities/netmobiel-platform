package eu.netmobiel.communicator.test;

import java.io.File;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.communicator.Resources;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.repository.converter.DeliveryModeConverter;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

public abstract class CommunicatorIntegrationTestBase {
	
    public static WebArchive createDeploymentBase() {
    	File[] deps = Maven.configureResolver()
    			.loadPomFromFile("pom.xml")
    			.importCompileAndRuntimeDependencies() 
    			.resolve()
    			.withTransitivity()
    			.asFile();
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackages(true, CommunicatorDatabase.class.getPackage())
                .addPackages(true, CommunicatorUrnHelper.class.getPackage())
                .addPackages(true, Envelope.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, DeliveryModeConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
                .addClass(Resources.class)
//                .addAsResource("log4j.properties")
    	        .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml");
    }
	
    @PersistenceContext(unitName = "pu-communicator")
    protected EntityManager em;
    
    @Inject
    protected UserTransaction utx;
    
    @Inject
    private Logger log;
    

    private boolean expectFailure;
    
	@Before
	public void prepareTest() throws Exception {
		expectFailure = false;
		clearData();
		prepareData();
		startTransaction();
	}
	
	@After
	public void finishTest() throws Exception {
		try {
			if (!expectFailure) {
				commitTransaction();
			}
		} catch (Exception ex) {
			log.error(String.join("\n", ExceptionUtil.unwindException(ex)));
			throw ex;	
		}
	}

	public void expectFailure() {
		this.expectFailure = true;
	}

	protected void commitTransaction() throws Exception {
		if (em.isJoinedToTransaction()) {
			utx.commit();
		}
	}

	protected void clearData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Dumping old records...");
        em.createQuery("delete from Envelope").executeUpdate();
        em.createQuery("delete from Message").executeUpdate();
        em.createQuery("delete from Conversation").executeUpdate();
        em.createQuery("delete from CommunicatorUser").executeUpdate();
		utx.commit();
	}

	protected void prepareData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Inserting records...");

		insertData();

		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
	}

	protected abstract void insertData() throws Exception;

	protected void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

	protected void flush() {
		try {
			utx.commit();
			// clear the persistence context (first-level cache)
			em.clear();
			utx.begin();
			em.joinTransaction();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}