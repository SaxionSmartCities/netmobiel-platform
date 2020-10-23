package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.commons.model.PagedResult;

@RunWith(Arquillian.class)
public class AccountDaoIT extends BankerIntegrationTestBase {

	@Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(AccountDao.class)
        ;
// 		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private AccountDao accountDao;

	@Override
	protected void insertData() throws Exception {
		
	}

    private void dump(String subject, Collection<Account> accounts) {
    	accounts.forEach(m -> log.info(subject + ": " + m.toString()));
    }
    
    @Test
    public void saveAccount() {
		Account account = Account.newInstant("account-1", "Acc 1", AccountType.LIABILITY);
    	accountDao.save(account);
    	List<Account> actual = accountDao.findAll();
    	assertNotNull(actual);
    	assertEquals(1, actual.size());
    	dump("saveAccount", actual);
    }

    @Test
    public void findByReference() {
    	final String accref = "account-1"; 
		Account account= Account.newInstant(accref, "U1", AccountType.LIABILITY);
    	accountDao.save(account);
    	Account actual = accountDao.findByAccountNumber(accref).orElse(null);
    	assertNotNull(actual);
    	assertEquals(accref, actual.getNcan());
    	dump("saveAccount", Collections.singletonList(actual));
    }

    @Test
    public void findByReference_NotFound() {
    	final String accref = "account-X";
		Account actual = accountDao.findByAccountNumber(accref).orElse(null);
		assertNull(actual);
    }

    @Test
    public void listAccounts() {
    	String name1 = "Account 1";
    	String name2 = "Account 2";
    	final String accref1 = "account-1"; 
    	final String accref2 = "account-2"; 
    	accountDao.save(Account.newInstant(accref1, name1, AccountType.LIABILITY));
    	accountDao.save(Account.newInstant(accref2, name2, AccountType.LIABILITY));
    	PagedResult<Long> actual = accountDao.listAccounts(null, 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(2, actual.getTotalCount().intValue());

    
    	actual = accountDao.listAccounts(null, 1, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertEquals(1, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	List<Account> accounts = accountDao.fetch(actual.getData(), null, Account::getId);
    	// sorting by ref asc
    	assertEquals(accref1, accounts.get(0).getNcan());

    	actual = accountDao.listAccounts(null, 1, 1);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertEquals(1, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	accounts = accountDao.fetch(actual.getData(), null, Account::getId);
    	// sorting by id asc
    	assertEquals(accref2, accounts.get(0).getNcan());

    	actual = accountDao.listAccounts(AccountType.LIABILITY, 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(2, actual.getTotalCount().intValue());

    	actual = accountDao.listAccounts(AccountType.LIABILITY, 10, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getCount());
    	accounts = accountDao.fetch(actual.getData(), null, Account::getId);
    	// sorting by ref asc
    	assertEquals(accref1, accounts.get(0).getNcan());

    	actual = accountDao.listAccounts(AccountType.ASSET, 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(0, actual.getTotalCount().intValue());
    }

}
