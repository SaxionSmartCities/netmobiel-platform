package eu.netmobiel.banker.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(AccountingTransactionDao.class)
public class AccountingTransactionDao extends AbstractDao<AccountingTransaction, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public AccountingTransactionDao() {
		super(AccountingTransaction.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
