package eu.netmobiel.communicator.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.Message;

@ApplicationScoped
@Typed(MessageDao.class)
public class MessageDao extends AbstractDao<Message, Long> {

    @Inject @CommunicatorDatabase
    private EntityManager em;

    public MessageDao() {
		super(Message.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

//	@Override
//	public List<Message> fetch(List<Long> ids, String graphName) {
//		// Create an identity map using the generic fetch. Rows are returned, but not necessarily in the same order
//		Map<Long, Message> resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Message::getId, Function.identity()));
//		// Now return the rows in the same order as the ids.
//		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
//	}

}
