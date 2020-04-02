package eu.netmobiel.banker.service;

import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.repository.AccountDao;
import eu.netmobiel.banker.repository.AccountingTransactionDao;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.LedgerDao;
import eu.netmobiel.commons.util.Logging;

@Stateless
@Logging
public class LedgerService {
	public static final String ACC_BANKING_RESERVE = "banking-reserve";
	
    @EJB(name = "java:app/netmobiel-banker-ejb/UserManager")
    private UserManager userManager;

    @Inject
    private LedgerDao ledgerDao;
    @Inject
    private AccountDao accountDao;
    @Inject
    private AccountingTransactionDao accountingTransactionDao;
    @Inject
    private BalanceDao balanceDao;
    
    /**
     * A Netmobiel user deposits money. The proportional amount of credits is deposited to the account of the user and
     * the banking reserve of Netmobiel is increased (debited). The balance of Netmobiel grows.
     * @param name the external reference of the netmobiel account 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     */
    public void deposit(String userAccountRef, int amount, OffsetDateTime when, String description) {
        assert userAccountRef.length() > 0 : "Must specify name of account to deposit to";
    	Account acc = accountDao.findByReference(userAccountRef);
        deposit(acc, amount, when, description);
    }

    public void deposit(Account userAccount, int amount, OffsetDateTime when, String description) {
    	if (userAccount.getAccountType() != AccountType.LIABILITY) {
    		throw new IllegalArgumentException("Charge: deposit account must be a liability account");
    	}
    	Account bra = accountDao.findByReference(ACC_BANKING_RESERVE);
    	if (bra.getAccountType() != AccountType.ASSET) {
    		throw new IllegalArgumentException("Charge: banking reserve account must be an asset account");
    	}
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	Balance brab = balanceDao.findByLedgerAndAccount(ledger, bra);  
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, userAccount);  
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when)
    			.debit(brab, amount)
				.credit(userAccountBalance, amount);
    	tr.validate();
    	accountingTransactionDao.save(tr);
    }

    public void withdraw(String userAccountRef, int amount, OffsetDateTime when, String description) {
        assert userAccountRef.length() > 0 : "Must specify name of account to withdraw from";
    	Account acc = accountDao.findByReference(userAccountRef);
        withdraw(acc, amount, when, description);
    }

    public void withdraw(Account userAccount, int amount, OffsetDateTime when, String description) {
    	if (userAccount.getAccountType() != AccountType.LIABILITY) {
    		throw new IllegalArgumentException("Charge: withDrawal account must be a liability account");
    	}
    	Account bra = accountDao.findByReference(ACC_BANKING_RESERVE);
    	if (bra.getAccountType() != AccountType.ASSET) {
    		throw new IllegalArgumentException("Charge: banking reserve account must be an asset account");
    	}
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	Balance brab = balanceDao.findByLedgerAndAccount(ledger, bra);  
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, userAccount);  
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when)
    			.credit(brab, amount)
				.debit(userAccountBalance, amount);
    	tr.validate();
    	accountingTransactionDao.save(tr);
    }

    public void charge(String customer, String provider, int amount, OffsetDateTime when, String description) {
    	Account ca = accountDao.findByReference(customer);
    	Account pa = accountDao.findByReference(provider);
    	charge(ca, pa, amount, when, description);
    }

    public void charge(Account customer, Account provider, int amount, OffsetDateTime when, String description) {
    	// Must be liability accounts
    	if (customer.getAccountType() != AccountType.LIABILITY) {
    		throw new IllegalArgumentException("Charge: customer must be a liability account");
    	}
    	if (provider.getAccountType() != AccountType.LIABILITY) {
    		throw new IllegalArgumentException("Charge: provider must be a liability account");
    	}
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	Balance customerBalance = balanceDao.findByLedgerAndAccount(ledger, customer);  
    	Balance providerBalance = balanceDao.findByLedgerAndAccount(ledger, provider);  
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when)
    			.debit(customerBalance, amount)
				.credit(providerBalance, amount);
    	tr.validate();
    	accountingTransactionDao.save(tr);
    }

}
