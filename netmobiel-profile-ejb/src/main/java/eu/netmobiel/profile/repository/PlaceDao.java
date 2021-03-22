package eu.netmobiel.profile.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Address;


@ApplicationScoped
@Typed(AddressDao.class)
public class AddressDao extends AbstractDao<Address, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

    public AddressDao() {
		super(Address.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
