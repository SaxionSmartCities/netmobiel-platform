package eu.netmobiel.commons.repository;

import java.util.List;
import java.util.Optional;

import eu.netmobiel.commons.model.User;

public abstract class UserDao<T extends User> extends AbstractDao<T, Long> {

    public UserDao(Class<T> entityClass) {
		super(entityClass);
	}

    public Optional<T> findByManagedIdentity(String managedId) {
//    	CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
//        CriteriaQuery<T> cq = cb.createQuery(this.getPersistentClass());
//        Root<T> entry = cq.from(this.getPersistentClass());
//        cq.where(cb.equal(entry.get(AbstractUser_.managedIdentity), managedId));
//        TypedQuery<T> tq = getEntityManager().createQuery(cq);
//		List<T> results = tq.getResultList();
//    	return Optional.ofNullable(results.size() > 0 ? results.get(0) : null); 

    	List<T> users = getEntityManager().createQuery(String.format("from %s where managedIdentity = :identity", 
    			getPersistentClass().getSimpleName()), getPersistentClass())
    			.setParameter("identity", managedId)
    			.getResultList();
    	return Optional.ofNullable(users.size() > 0 ? users.get(0) : null); 
    }

}
