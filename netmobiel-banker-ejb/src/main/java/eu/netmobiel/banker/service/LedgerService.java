package eu.netmobiel.banker.service;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.repository.AccountingTransactionDao;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.LedgerDao;
import eu.netmobiel.commons.model.PagedResult;
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
    private AccountingTransactionDao accountingTransactionDao;
    @Inject
    private BalanceDao balanceDao;
    
    protected void expect(Account account, AccountType type) {
    	if (account.getAccountType() != type) {
    		throw new IllegalArgumentException(String.format("Expected account type %s, got %s", type.toString(), account.toString()));
    	}
    }

    /**
     * A Netmobiel user deposits credits. The balance of the netmobiel credit system grows: the account of the user gets
     * more credits and the banking reserve of Netmobiel is equally increased.
     * @param name the external reference of the netmobiel account 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public void deposit(String userAccountRef, int amount, OffsetDateTime when, String description) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccountReference(ledger, userAccountRef);
    	Balance brab = balanceDao.findByLedgerAndAccountReference(ledger, ACC_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when)
    			.debit(brab, amount)
				.credit(userAccountBalance, amount);
    	tr.validate();
    	accountingTransactionDao.save(tr);
    }

    /**
     * A Netmobiel user withdraws credits. The balance of the netmobiel credit system shrinks: the account of the user gets
     * less credits and the banking reserve of Netmobiel is equally decreased.
     * @param name the external reference of the netmobiel account 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public void withdraw(String userAccountRef, int amount, OffsetDateTime when, String description) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccountReference(ledger, userAccountRef);  
    	Balance brab = balanceDao.findByLedgerAndAccountReference(ledger, ACC_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when)
    			.credit(brab, amount)
				.debit(userAccountBalance, amount);
    	tr.validate();
    	accountingTransactionDao.save(tr);
    }

    /**
     * Transfers credits from one Netmobiel user to another. The balance of the netmobiel credit system does not change. The account of 
     * one user is decreased and the account of the other is equally increased.
     * Both account are expected to be liability accounts, i.e. user accounts.
     * @param customer the external reference of the netmobiel account that pays for something
     * @param provider the external reference of the netmobiel account that provides some service.
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public void charge(String customer, String provider, int amount, OffsetDateTime when, String description) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance customerBalance = balanceDao.findByLedgerAndAccountReference(ledger, customer);  
    	Balance providerBalance = balanceDao.findByLedgerAndAccountReference(ledger, provider);  
    	expect(customerBalance.getAccount(), AccountType.LIABILITY);
    	expect(providerBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when)
    			.debit(customerBalance, amount)
				.credit(providerBalance, amount);
    	tr.validate();
    	accountingTransactionDao.save(tr);
    }

    /**
     * Closes the current ledger by setting the endPeriod to <code>newStartPeriod</code>.
     * Move all transactions with accountingTime >= newStartPeriod to the new ledger.
     * Calculate the account balances of the closed ledger and calculate the balances for
     * the new ledger. 
     * @param whenNewLedger
     */
    public void closeLedger(OffsetDateTime newStartPeriod) {
    	// Find the active ledger
    	Ledger ledger = ledgerDao.findByDate(newStartPeriod.toInstant());
    	
    	Ledger newLedger = new Ledger();
    	newLedger.setStartPeriod(newStartPeriod.toInstant());
    	newLedger.setName(String.format("%d", newStartPeriod.getYear()));

    	// Move all transactions with accounting time equal or beyond newStartPeriod to the new ledger
    	
    	// Calculate the final balance of all accounts of the old ledger
    	// No transactions are required to transfer balances. All accounts are balance accounts (as oppposed
    	// to revenue and expenses accounts in real life).
    	// Calculate the new balance of all accounts of the new ledger
    	// We need a flag for each balance to notify it is dirty
    	// Should we also mark the ledger as being in maintenance?
    	// At startup we need to check for maintenance and finish whatever was started.
    	

    }
    
    public PagedResult<Ledger> listLedgers() {
        return null;
    }
    
    public PagedResult<Account> listAccounts() {
        return null;
    }

    public PagedResult<Balance> listBalances() {
        return null;
    }

    public PagedResult<AccountingEntry> listAccountingEntries() {
        return null;
    }

}
