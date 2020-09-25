package eu.netmobiel.banker.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(DonationDao.class)
public class DonationDao extends AbstractDao<Donation, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public DonationDao() {
		super(Donation.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
