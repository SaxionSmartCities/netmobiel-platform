package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.Conversation_;
import eu.netmobiel.communicator.test.CommunicatorIntegrationTestBase;

@RunWith(Arquillian.class)
public class ConversationDaoIT extends CommunicatorIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
    	try {
	        WebArchive archive = createDeploymentBase()
	            .addClass(ConversationDao.class);
	//		System.out.println(archive.toString(true));
			return archive;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
    }

    @Inject
    private ConversationDao dao;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;
    
    @Test
    public void findConversation() throws Exception {
    	Optional<Conversation> optCv = dao.findByContextAndOwner("Trip Plan P1.1", userP1);
    	assertTrue(optCv.isPresent());
    	assertEquals(convP1_1.getId(), optCv.get().getId());
    	
    	optCv = dao.findByContextAndOwner("Trip Plan P1.1000", userP1);
    	assertFalse(optCv.isPresent());

    	optCv = dao.findByContextAndOwner("Trip Plan P1.1", userP2);
    	assertFalse(optCv.isPresent());

    	optCv = dao.findByContextAndOwner("Trip P1.1", userP1);
    	assertTrue(optCv.isPresent());
    	assertEquals(convP1_1.getId(), optCv.get().getId());
    }

    @Test
    public void loadConversation_Plain() {
    	Optional<Conversation> optCv = dao.find(convP1_1.getId());
    	assertTrue(optCv.isPresent());
    	Conversation cv = optCv.get();
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertTrue(em.contains(cv));
    	assertFalse(puu.isLoaded(cv, Conversation_.CONTEXTS));
    	assertFalse(puu.isLoaded(cv, Conversation_.OWNER));
    }

    @Test
    public void loadConversation_Default() {
    	Optional<Conversation> optCv = dao.loadGraph(convP1_1.getId(), Conversation.DEFAULT_ENTITY_GRAPH);
    	assertTrue(optCv.isPresent());
    	Conversation cv = optCv.get();
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertTrue(em.contains(cv));
    	assertTrue(puu.isLoaded(cv, Conversation_.CONTEXTS));
    	assertFalse(puu.isLoaded(cv, Conversation_.OWNER));
    }

    @Test
    public void loadConversation_Full() {
    	Optional<Conversation> optCv = dao.loadGraph(convP1_1.getId(), Conversation.FULL_ENTITY_GRAPH);
    	assertTrue(optCv.isPresent());
    	optCv = dao.loadGraph(optCv.get().getId(), Conversation.FULL_ENTITY_GRAPH);
    	assertTrue(optCv.isPresent());
    	Conversation cv = optCv.get();
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertTrue(em.contains(cv));
    	assertTrue(puu.isLoaded(cv, Conversation_.CONTEXTS));
    	assertTrue(puu.isLoaded(cv, Conversation_.OWNER));
    }
}
