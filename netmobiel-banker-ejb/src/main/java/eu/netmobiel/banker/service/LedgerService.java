package eu.netmobiel.banker.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.repository.AccountDao;
import eu.netmobiel.banker.repository.AccountingEntryDao;
import eu.netmobiel.banker.repository.AccountingTransactionDao;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.DepositRequestDao;
import eu.netmobiel.banker.repository.LedgerDao;
import eu.netmobiel.banker.repository.UserDao;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.payment.client.PaymentClient;
import eu.netmobiel.payment.client.model.PaymentLink;
import eu.netmobiel.payment.client.model.PaymentLinkStatus;

/**
 * Stateless bean for the management of the ledger.
 * 
 * TODO: Security
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
public class LedgerService {
	public static final String ACC_REF_BANKING_RESERVE = "banking-reserve";
	public static final String ACC_NAME_BANKING_RESERVE = "Banking Reserve";
	public static final Integer MAX_RESULTS = 10; 
	public static final Integer DEFAULT_LOOKBACK_DAYS = 90; 
	public static final int PAYMENT_LINK_EXPIRATION_SECS = 15 * 60;
	public static final int CREDIT_EXCHANGE_RATE = 19;	// 1 credit is x euro cent

    @SuppressWarnings("unused")
    @Inject
    private Logger log;
    
	@Inject
    private LedgerDao ledgerDao;
    @Inject
    private AccountingTransactionDao accountingTransactionDao;
    @Inject
    private BalanceDao balanceDao;
    @Inject
    private AccountingEntryDao accountingEntryDao;
    @Inject
    private AccountDao accountDao;
    @Inject
    private UserDao userDao;

    @Inject
    private DepositRequestDao depositRequestDao;

    @Inject
    private PaymentClient paymentClient;
    
    protected void expect(Account account, AccountType type) {
    	if (! Account.isOpen.test(account)) {
    		throw new IllegalArgumentException(String.format("Account is not open: %s", account.toString()));
    	}
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
    	Balance brab = balanceDao.findByLedgerAndAccountReference(ledger, ACC_REF_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when.toInstant(), Instant.now())
    			.debit(brab, amount)
				.credit(userAccountBalance, amount)
				.build();
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
    	Balance brab = balanceDao.findByLedgerAndAccountReference(ledger, ACC_REF_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when.toInstant(), Instant.now())
    			.credit(brab, amount)
				.debit(userAccountBalance, amount)
    			.build();
    	accountingTransactionDao.save(tr);
    }

    /**
     * Transfers credits from one Netmobiel user to another. The balance of the netmobiel credit system does not change. The account of 
     * one user is decreased and the account of the other is equally increased.
     * Both account are expected to be liability accounts, i.e. user accounts.
     * @param customer the external reference of the netmobiel account that pays for something
     * @param beneficiary the external reference of the netmobiel account that will receive the credits.
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public void charge(String customer, String beneficiary, int amount, OffsetDateTime when, String description) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance customerBalance = balanceDao.findByLedgerAndAccountReference(ledger, customer);  
    	Balance providerBalance = balanceDao.findByLedgerAndAccountReference(ledger, beneficiary);  
    	expect(customerBalance.getAccount(), AccountType.LIABILITY);
    	expect(providerBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createTransaction(description, when.toInstant(), Instant.now())
    			.debit(customerBalance, amount)
				.credit(providerBalance, amount)
    			.build();
    	accountingTransactionDao.save(tr);
    }

    /**
     * Closes the current ledger by setting the endPeriod to <code>newStartPeriod</code>.
     * Move all transactions with accountingTime >= newStartPeriod to the new ledger.
     * Calculate the account balances of the closed ledger and calculate the balances for
     * the new ledger. 
     * @param newStartPeriod
     */
    public void closeLedger(OffsetDateTime newStartPeriod) {
    	// Find the active ledger
    	@SuppressWarnings("unused")
		Ledger ledger = ledgerDao.findByDate(newStartPeriod.toInstant());
    	
    	Ledger newLedger = new Ledger();
    	newLedger.setStartPeriod(newStartPeriod.toInstant());
    	newLedger.setName(String.format("%d", newStartPeriod.getYear()));

    	// Move all transactions with accounting time equal or beyond newStartPeriod to the new ledger
    	
    	// Calculate the final balance of all accounts of the old ledger
    	// No transactions are required to transfer balances. All accounts are balance accounts (as opposed
    	// to revenue and expenses accounts in real life). [Is that true? You to transfer from one ledger to the next. 
    	// Hmmm ok, yes might work, start amount of balance].
    	// Calculate the new balance of all accounts of the new ledger
    	// We need a flag for each balance to notify it is dirty
    	// Should we also mark the ledger as being in maintenance?
    	// At startup we need to check for maintenance and finish whatever was started.
    	throw new UnsupportedOperationException("closeLedger is not yet implemented");
    }
    
    public PagedResult<Ledger> listLedgers(Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	List<Ledger> results = null;
		Long totalCount = 0L;
    	if (maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> lids = ledgerDao.listLedgers(maxResults, offset);
    		results = ledgerDao.fetch(lids.getData(), null, Ledger::getId);
    		totalCount = Long.valueOf(results.size());
    	}
    	if (maxResults == 0 || results.size() >= maxResults) {
    		// We only want to know the total count, or we have potential more data available then we can guess now
    		// Determine the total count
        	totalCount = ledgerDao.listLedgers(0, offset).getTotalCount();
    	}
    	return new PagedResult<Ledger>(results, maxResults, offset, totalCount);
    }
    
    public PagedResult<Account> listAccounts(String holderIdentity, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	PagedResult<Long> prs = accountDao.listAccounts(null, 0, offset);
    	List<Account> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> mids = accountDao.listAccounts(null, maxResults, offset);
    		results = accountDao.fetch(mids.getData(), null, Account::getId);
    	}
    	return new PagedResult<Account>(results, maxResults, offset, prs.getTotalCount());
    }

    public PagedResult<Balance> listBalances(String holder, String accountReference, OffsetDateTime period, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        if (period == null) {
        	period = OffsetDateTime.now();
        }
		Ledger ledger = ledgerDao.findByDate(period.toInstant());
    	PagedResult<Long> prs = balanceDao.listBalances(accountReference, ledger, 0, offset);
    	List<Balance> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = balanceDao.listBalances(accountReference, ledger, maxResults, offset);
    		results = balanceDao.fetch(ids.getData(), null, Balance::getId);
    	}
    	return new PagedResult<Balance>(results, maxResults, offset, prs.getTotalCount());
    }

    public PagedResult<AccountingEntry> listAccountingEntries(String holder, String accountReference, Instant since, Instant until, Integer maxResults, Integer offset) 
    		throws BadRequestException {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        if (since != null && until != null && until.isBefore(since)) {
        	throw new BadRequestException("Until must be after since");
        }
    	PagedResult<Long> prs = accountingEntryDao.listAccountingEntries(accountReference, since, until, 0, offset);
    	List<AccountingEntry> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = accountingEntryDao.listAccountingEntries(accountReference, since, until, maxResults, offset);
    		results = accountingEntryDao.fetch(ids.getData(), null, AccountingEntry::getId);
    	}
    	return new PagedResult<AccountingEntry>(results, maxResults, offset, prs.getTotalCount());
    }

    protected Ledger createLedger(Instant when) {
    	Ledger newLedger = new Ledger();
    	newLedger.setStartPeriod(when);
    	newLedger.setName(String.format("%d", when.atOffset(ZoneOffset.UTC).getYear()));
    	ledgerDao.save(newLedger);
    	return newLedger; 
    }
    
    /**
     * Create an account and connect it through a balance to the active ledger.   
     * @param holder the holder of the account.
     * @param reference the external reference to the account.
     * @param type the account type.
     * @return the account  
     */
    public Account createAccount(String reference, String name, AccountType type) {
    	Instant now = Instant.now();
    	Ledger ledger = ledgerDao.findByDate(now);
    	Account acc = Account.newInstant(reference, name, type);
    	accountDao.save(acc);
    	Balance bal = new Balance(ledger, acc, 0);
    	balanceDao.save(bal);
    	return acc;
    }

    /**
     * Event handler for handling new users. The ledger service must create and assign a personal monetary account.
     * @param newUser the new user account. The user record. It must be persistent already.
     */
    public void onNewUser(final @Observes(during = TransactionPhase.IN_PROGRESS) User newUser) {
    	if (newUser.getPersonalAccount() == null) {
    		if (newUser.getId() == null) {
    			throw new IllegalStateException("User must have a persistent identifier");
    		}
    		User dbUser = newUser;
    		if (! userDao.contains(dbUser)) {
    			dbUser = userDao.find(newUser.getId())
    					.orElseThrow(() -> new IllegalStateException("No such user: " + newUser.getId()));
    		}
    		// Create a personal liability account. The reference id is directly related to the user primary key.
    		String accRef = String.format("PLA-%d", dbUser.getId());
    		String accName = String.format("%s %s", dbUser.getGivenName() != null ? dbUser.getGivenName() : "", dbUser.getFamilyName() != null ? dbUser.getFamilyName() : "").trim();
    		if (accName.isEmpty()) {
    			accName = accRef;
    		}
    		Account acc = createAccount(accRef, accName, AccountType.LIABILITY);
    		dbUser.setPersonalAccount(acc);
    		newUser.setPersonalAccount(acc);
    	}
    }

    /**
     * Creates a payment link request at the payment provider of netmobiel
     * @param account account to deposit credits to.
     * @param amounbtCredits the number of credits to deposit. 
     * @param description the description to use on the payment page
     * @param returnUrl the url to use to return to. The payment provider will add query parameters.
     * 			The parameter object_id must be passed on to the method verifyDeposition.   
     * @return the url to the payment page for the client to redirect the browser to.
     */
    public String createDepositRequest(Account acc, int amountCredits, String description, String returnUrl) {
    	DepositRequest dr = new DepositRequest();
    	
    	PaymentLink plink = new PaymentLink();
    	plink.amount = amountCredits * CREDIT_EXCHANGE_RATE;
    	plink.description = description;
    	plink.expirationPeriod = Duration.ofSeconds(PAYMENT_LINK_EXPIRATION_SECS);
    	plink.merchantOrderId = String.format("NB-%d-%d", acc.getId(), Instant.now().getEpochSecond());
    	plink.returnUrl = returnUrl;
    	plink = paymentClient.createPaymentLink(plink);

    	dr.setAccount(acc);
    	dr.setAmountCredits(amountCredits);
    	dr.setAmountEurocents(plink.amount);
    	dr.setMerchantOrderId(plink.merchantOrderId);
    	dr.setDescription(plink.description);
    	dr.setCreationTime(plink.created.toInstant());
    	dr.setExprationTime(dr.getCreationTime().plusSeconds(plink.expirationPeriod.getSeconds()));
    	dr.setPaymentLinkId(plink.id);
    	dr.setStatus(PaymentStatus.ACTIVE);
    	depositRequestDao.save(dr);
    	return plink.paymentUrl;
    }

    /**
     * Verifies the deposition of credits by the order id supplied by the payment provider.
     * The order id is one of the parameters added to the return url after completing the payment at the payment page of
     * the payment provider. In addition, the order id is also part of the payment webhook. 
     * @param paymentOrderId The order id as supplied by the payment provider.
     * @return true if the deposition of the payment link was added now or earlier. If false then deposition has not taken place (yet), or something
     * 			went wrong.
     */
    public Optional<DepositRequest> verifyDeposition(String paymentOrderId) {
    	DepositRequest dr = null;
    	try {
	    	PaymentLink plink = paymentClient.getPaymentLinkByOrderId(paymentOrderId);
	    	dr = depositRequestDao.findByPaymentLink(plink.id);
	    	if (dr.getStatus() == PaymentStatus.ACTIVE) {
	        	if (plink.status == PaymentLinkStatus.COMPLETED) {
	        		// Transition to COMPLETED, add transaction to deposit credits in the NetMobiel system
	        		dr.setCompletedTime(plink.completed.toInstant());
	        		deposit(dr.getAccount().getReference(), dr.getAmountCredits(), dr.getCompletedTime().atOffset(ZoneOffset.UTC), dr.getDescription());
	        		dr.setStatus(PaymentStatus.COMPLETED);
	        	} else if (plink.status == PaymentLinkStatus.EXPIRED) {
	        		dr.setStatus(PaymentStatus.EXPIRED);
	        	}
	    	}
    	} catch (Exception ex) {
    		log.error("Error verivying deposition - " + ex.getMessage());
    	}
    	return Optional.ofNullable(dr == null ? null : dr);
    }
}
