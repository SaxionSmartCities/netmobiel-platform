package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.util.List;
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

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.filter.ConversationFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
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
    
    @Test
    public void listConversations_All() throws BadRequestException {
    	ConversationFilter filter = new ConversationFilter();
    	filter.validate();
    	Cursor cursor = new Cursor();
    	cursor.validate(100,  0);

    	PagedResult<Long> cids = dao.listConversations(filter, cursor);
    	List<Conversation> cs = dao.loadGraphs(cids.getData(), null, Conversation::getId);
    	Long expCount = em.createQuery("select count(c) from Conversation c", Long.class).getSingleResult();
    	assertEquals("All conversations present", Math.toIntExact(expCount) , cs.size());
    	assertNull("No total count", cids.getTotalCount());
    	assertEquals("Offset matches", cursor.getOffset().intValue(), cids.getOffset());
    	assertEquals("maxResults matches", cursor.getMaxResults().intValue(), cids.getResultsPerPage());
    	assertEquals("Result count matches", expCount.longValue(), cids.getCount());
    }

    @Test
    public void listConversations_ByOwner() throws BadRequestException {
    	CommunicatorUser owner = userP1;
    	ConversationFilter filter = new ConversationFilter();
    	filter.setOwner(owner);
    	filter.validate();
    	Cursor cursor = new Cursor();
    	cursor.validate(100,  0);
    	
    	PagedResult<Long> cids = dao.listConversations(filter, cursor);
    	List<Conversation> cs = dao.loadGraphs(cids.getData(), Conversation.FULL_ENTITY_GRAPH, Conversation::getId);
    	Long expCount = em.createQuery("select count(c) from Conversation c where c.owner = :owner", Long.class)
    			.setParameter("owner", owner)
    			.getSingleResult();
    	assertEquals("All conversations present", Math.toIntExact(expCount) , cs.size());
    	assertNull("No total count", cids.getTotalCount());
    	assertEquals("Offset matches", cursor.getOffset().intValue(), cids.getOffset());
    	assertEquals("maxResults matches", cursor.getMaxResults().intValue(), cids.getResultsPerPage());
    	assertEquals("Result count matches", expCount.longValue(), cids.getCount());
    	for (Conversation c : cs) {
			assertEquals(c.getOwner(), owner);
		}
    }

    @Test
    public void listConversations_ByOwnerAndContext() throws BadRequestException {
    	CommunicatorUser owner = userP1;
    	String context = "Trip Plan P1.1";
    	ConversationFilter filter = new ConversationFilter();
    	filter.setOwner(owner);
    	filter.setContext(context);
    	filter.validate();
    	Cursor cursor = new Cursor();
    	cursor.validate(100,  0);
    	
    	PagedResult<Long> cids = dao.listConversations(filter, cursor);
    	List<Conversation> cs = dao.loadGraphs(cids.getData(), Conversation.FULL_ENTITY_GRAPH, Conversation::getId);
    	Long expCount = em.createQuery("select count(c) from Conversation c where c.owner = :owner and :context member of c.contexts", Long.class)
    			.setParameter("owner", owner)
    			.setParameter("context", context)
    			.getSingleResult();
    	assertEquals("All conversations present", Math.toIntExact(expCount) , cs.size());
    	assertNull("No total count", cids.getTotalCount());
    	assertEquals("Offset matches", cursor.getOffset().intValue(), cids.getOffset());
    	assertEquals("maxResults matches", cursor.getMaxResults().intValue(), cids.getResultsPerPage());
    	assertEquals("Result count matches", expCount.longValue(), cids.getCount());
    	for (Conversation c : cs) {
			assertEquals(c.getOwner(), owner);
			assertTrue(c.getContexts().contains(context));
		}
    }
}
