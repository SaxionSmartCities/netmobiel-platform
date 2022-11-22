package eu.netmobiel.profile.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.UserSession;


@ApplicationScoped
@Typed(UserSessionDao.class)
public class UserSessionDao extends AbstractDao<UserSession, String> {

	@Inject @ProfileDatabase
    private EntityManager em;

	public UserSessionDao() {
		super(String.class, UserSession.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Optional<UserSession> findUserSessionBySessionId(String sessionId) {
    	List<UserSession> results = em.createQuery("from UserSession where sessionId = :sessionId", UserSession.class)
			.setParameter("sessionId", sessionId)
			.getResultList();
    	if (results.size() > 1) {
    		throw new IllegalStateException("Duplicate UserSession identifier: " + sessionId);
    	}
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
