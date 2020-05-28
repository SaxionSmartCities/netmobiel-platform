package eu.netmobiel.rideshare.test.example;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

//@Stateless
public class ExampleService {

//    @PersistenceContext(unitName = "bookshelfManager")
    private EntityManager entityManager;

    @RolesAllowed({ "User", "Manager" })
    public void addExample(Example book) {
        entityManager.persist(book);
    }

    @RolesAllowed({ "Manager" })
    public void deleteExample(Example book) {
        entityManager.remove(book);
    }

    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Example> getExamples() {
        TypedQuery<Example> query = entityManager.createQuery("SELECT b from Example as b", Example.class);
        return query.getResultList();
    }
}