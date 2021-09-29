package eu.netmobiel.banker.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.exception.OverdrawnException;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.model.SettlementOrder;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.repository.AccountDao;
import eu.netmobiel.banker.repository.AccountingEntryDao;
import eu.netmobiel.banker.repository.AccountingTransactionDao;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.LedgerDao;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.TokenGenerator;
import eu.netmobiel.commons.util.UrnHelper;

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
@DeclareRoles({ "admin" })
@PermitAll
public class LedgerService {
	public static final String ACC_REF_BANKING_RESERVE = "banking-reserve";
	public static final String ACC_NAME_BANKING_RESERVE = "De NetMobiel Kluis";
	public static final String ACC_REF_RESERVATIONS = "reservations";
	public static final String ACC_NAME_RESERVATIONS = "NetMobiel Reserveringen";
	public static final Integer MAX_RESULTS = 10; 
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
    private BankerUserDao userDao;

    private static void expect(Account account, AccountType type) {
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
     * @param acc the netmobiel account to deposit to. 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public AccountingTransaction deposit(Account acc, int amount, OffsetDateTime when, String description, String reference) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, acc);
    	Balance brab = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = null;
		try {
			tr = ledger
					.createTransaction(TransactionType.DEPOSIT, description, reference, when.toInstant(), Instant.now())
					.debit(brab, amount, userAccountBalance.getAccount())
					.credit(userAccountBalance, amount, brab.getAccount())
					.build();
	    	accountingTransactionDao.save(tr);
		} catch (BalanceInsufficientException e) {
			throw new IllegalStateException("A deposit should never cause an BalanceInsufficientException");
		}
		return tr;
    }

    /**
     * A Netmobiel user withdraws credits. The balance of the netmobiel credit system shrinks: the account of the user gets
     * less credits and the banking reserve of Netmobiel is equally decreased.
     * @param acc the netmobiel account to withdraw from. 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public  AccountingTransaction withdraw(Account acc, int amount, OffsetDateTime when, String description, String reference) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, acc);  
    	Balance brab = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = ledger
    			.createTransaction(TransactionType.WITHDRAWAL, description, reference, when.toInstant(), Instant.now())
    			.credit(brab, amount, userAccountBalance.getAccount())
				.debit(userAccountBalance, amount, brab.getAccount())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    /**
     * Transfers credits from one Netmobiel account to another. The balance of the netmobiel credit system does not change. The originator 
     * account is decreased and the beneficiary account is equally increased.
     * Both accounts are expected to be liability accounts.
     * @param originator the netmobiel account that is debited (pays).
     * @param beneficiary the netmobiel account that is credited (receives).
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @param description the description in the journal.
     * @param reference the contextual reference to a system object in the form of a urn.
     */
    public AccountingTransaction transfer(Account originator, Account beneficiary, int amount, OffsetDateTime when, String description, String reference) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance originatorBalance = balanceDao.findByLedgerAndAccount(ledger, originator);  
    	Balance beneficiaryBalance = balanceDao.findByLedgerAndAccount(ledger, beneficiary);
    	expect(originatorBalance.getAccount(), AccountType.LIABILITY);
    	expect(beneficiaryBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createTransaction(TransactionType.PAYMENT, description, reference, when.toInstant(), Instant.now())
    			.debit(originatorBalance, amount, beneficiaryBalance.getAccount())
				.credit(beneficiaryBalance, amount, originatorBalance.getAccount())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    /**
     * Reserves an amount of credits from one Netmobiel user for a yet to be delivered service. 
     * @param acc the netmobiel account that will be charged
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @param description the description in the journal.
     * @param reference the contextual reference to a system object in the form of a urn.
     */
    public AccountingTransaction reserve(Account acc, int amount, OffsetDateTime when, String description, String reference) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userBalance = balanceDao.findByLedgerAndAccount(ledger, acc);  
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS);  
    	expect(userBalance.getAccount(), AccountType.LIABILITY);
    	expect(rb.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createTransaction(TransactionType.RESERVATION, description, reference, when.toInstant(), Instant.now())
    			.debit(userBalance, amount, rb.getAccount())
				.credit(rb, amount, userBalance.getAccount())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    /**
     * Releases a previous reservation. A new transaction will be added to nullify the reservation.
     * @param transactionRef the reference to the previously made reservation.  
     * @param when the timestamp of the release.
     * @return the transaction  
     */
    public AccountingTransaction release(String transactionRef, OffsetDateTime when) {
    	Long tid = UrnHelper.getId(AccountingTransaction.URN_PREFIX, transactionRef);
    	AccountingTransaction reservation = accountingTransactionDao.find(tid).orElseThrow(() -> new IllegalArgumentException("No such transaction: " + transactionRef));
    	return release(reservation, when);
    }

    /**
     * Releases a previous reservation. A new transaction will be added to nullify the reservation.
     * @param transactionRef the reference to the previously made reservation.  
     * @param when the timestamp of the release.
     * @return the transaction  
     */
    public AccountingTransaction release(AccountingTransaction reservation, OffsetDateTime when) {
    	Ledger ledger = reservation.getLedger();
    	ledger.expectOpen();
    	AccountingEntry userEntry = reservation.lookupByCounterParty(ACC_REF_RESERVATIONS);
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS);  
    	Balance userBalance = balanceDao.findByLedgerAndAccount(ledger, userEntry.getAccount());  
    	expect(rb.getAccount(), AccountType.LIABILITY);
    	expect(userBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = null;
    	try {
        	tr = ledger
        			.createTransaction(TransactionType.RELEASE, reservation.getDescription(), reservation.getContext(), when.toInstant(), Instant.now())
        			.credit(userBalance, userEntry.getAmount(), rb.getAccount())
    				.debit(rb, userEntry.getAmount(), userEntry.getAccount())
        			.build();
        	accountingTransactionDao.save(tr);
		} catch (BalanceInsufficientException e) {
			throw new IllegalStateException("Reservation account should not be empty", e);
		}
    	return tr;
    }

    public String reserve(NetMobielUser nmuser, int amount, String description, String reference) throws BalanceInsufficientException {
    	BankerUser user = lookupUser(nmuser)
    			.orElseThrow(() -> new BalanceInsufficientException("User has no deposits made yet: " + nmuser.getManagedIdentity()));
    	AccountingTransaction tr = reserve(user.getPersonalAccount(), amount, OffsetDateTime.now(), description, reference);
    	return tr.getTransactionRef();
    }

    public String release(String reservationId) {
    	AccountingTransaction tr = release(reservationId, OffsetDateTime.now());
    	return tr.getTransactionRef();
    }

    public String charge(NetMobielUser nmbeneficiary, String reservationId, int actualAmount) throws BalanceInsufficientException, OverdrawnException {
    	BankerUser beneficiary = lookupUser(nmbeneficiary)
    			.orElseThrow(() -> new BalanceInsufficientException("Beneficiary has no account, nothing to transfer to: " + nmbeneficiary.getManagedIdentity()));
    	AccountingTransaction release = release(reservationId, OffsetDateTime.now());
    	int overspent = actualAmount - release.getAccountingEntries().get(0).getAmount(); 
    	if (overspent > 0) {
    		throw new OverdrawnException("Charge exceeds reservation: " + reservationId + " " + overspent);
    	}
    	AccountingEntry userEntry = release.lookupByCounterParty(ACC_REF_RESERVATIONS);
    	AccountingTransaction charge_tr = transfer(userEntry.getAccount(), beneficiary.getPersonalAccount(), actualAmount, OffsetDateTime.now(), release.getDescription(), release.getContext());
    	return charge_tr.getTransactionRef();
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
		Long totalCount = ledgerDao.listLedgers(0, offset).getTotalCount();
    	if (maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> lids = ledgerDao.listLedgers(maxResults, offset);
    		results = ledgerDao.loadGraphs(lids.getData(), null, Ledger::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, totalCount);
    }
    
    public PagedResult<Account> listAccounts(Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	PagedResult<Long> prs = accountDao.listAccounts(null, 0, offset);
    	List<Account> results = null;
    	if (maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> mids = accountDao.listAccounts(null, maxResults, offset);
    		results = accountDao.loadGraphs(mids.getData(), null, Account::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

    public PagedResult<Balance> listBalances(Account acc, OffsetDateTime period, Integer maxResults, Integer offset) {
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
    	PagedResult<Long> prs = balanceDao.listBalances(acc, ledger, 0, offset);
    	List<Balance> results = null;
    	if (maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = balanceDao.listBalances(acc, ledger, maxResults, offset);
    		results = balanceDao.loadGraphs(ids.getData(), null, Balance::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

    public PagedResult<AccountingEntry> listAccountingEntries(String accountReference, Instant since, Instant until, Integer maxResults, Integer offset) 
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
    	if (maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = accountingEntryDao.listAccountingEntries(accountReference, since, until, maxResults, offset);
    		results = accountingEntryDao.loadGraphs(ids.getData(), null, AccountingEntry::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

    public Ledger createLedger(Instant when) {
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
    	acc.setActualBalance(bal);
    	return acc;
    }

    /**
     * Retrieves the specified account, including the actual balance.
     * @param id
     * @return
     * @throws NotFoundException
     */
    @RolesAllowed({ "admin" })
    public Account getAccount(Long id) throws NotFoundException {
    	Account acc = accountDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such Account: " + id));
		Balance balance = balanceDao.findActualBalance(acc);
		acc.setActualBalance(balance);
		return acc;
    }

    /**
     * Updates the specified account. Only name, iban and iban holder can be modified.
     * @param accId the account id
     * @param acc the updated parameters.
     * @throws NotFoundException
     */
    @RolesAllowed({ "admin" })
    public void updateAccount(Long accId, Account acc) throws NotFoundException {
    	Account accdb = accountDao.find(accId)
    			.orElseThrow(() -> new NotFoundException("No such Account: " + accId));
    	accdb.setIban(acc.getIban());
    	accdb.setIbanHolder(acc.getIbanHolder());
    	accdb.setName(acc.getName());
    }

    /**
     * Create an account and connect it through a balance to the active ledger.   
     * @param holder the holder of the account.
     * @param reference the external reference to the account.
     * @param type the account type.
     * @return the account  
     */
    public void prepareAccount(String ncan, String name, AccountType type) {
    	if (accountDao.findByAccountNumber(ncan).isEmpty()) {
    		createAccount(ncan, name, type);
    	}
    }

    private Optional<BankerUser> lookupUser(NetMobielUser user) {
    	return Optional.ofNullable(userDao.findByManagedIdentity(user.getManagedIdentity()).orElse(null));
    }
    
    /**
     * Adds a personal account to a new user. The ledger service must create and assign a personal monetary account.
     * @param dbUser the new user account. The user record. It must be persistent already.
     */
    public void addPersonalAccount(BankerUser dbUser) {
    	if (dbUser.getPersonalAccount() != null) {
			throw new IllegalStateException("Not a new user, personal account exists already");
    	}    		
		if (! userDao.contains(dbUser)) {
			throw new IllegalStateException("User should be in persistence context");
		}
		// Create a personal liability account. The reference id is directly related to the user primary key.
		String accRef = createNewAccountNumber("PLA");
		String accName = String.format("%s %s", dbUser.getGivenName() != null ? dbUser.getGivenName() : "", dbUser.getFamilyName() != null ? dbUser.getFamilyName() : "").trim();
		if (accName.isEmpty()) {
			accName = accRef;
		}
		Account acc = createAccount(accRef, accName, AccountType.LIABILITY);
		dbUser.setPersonalAccount(acc);
    }

    /**
     * Event handler for handling new users. The ledger service must create and assign a personal monetary account.
     * @param dbUser the new user account. The user record. It must be persistent already.
     */
    public void onNewUser(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created BankerUser dbUser) {
    	addPersonalAccount(dbUser);
    }
    
    public AccountingEntry getAccountingEntry(Long entryId) throws NotFoundException {
    	return accountingEntryDao.find(entryId)
    			.orElseThrow(() -> new NotFoundException("No such AccountingEntry: " + entryId));
    }

    /**
     * Event handler for handling a settlement order.
     */
    public void onNewSettlementOrder(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created SettlementOrder order) throws BalanceInsufficientException {
    	transfer(order.getOriginator(), order.getBeneficiary(), order.getAmount(), 
    			order.getEntryTime().atOffset(ZoneOffset.UTC), order.getDescription(), order.getContext());
    }
    
    /**
     * Event handler for handling new charities. The ledger service must create and assign a personal monetary account.
     * @param charity the new user account. The user record. It must be persistent already.
     */
    public void onNewCharity(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created Charity charity) {
		// Create a charity liability account.
		String accRef = createNewAccountNumber("CLA");
		Account acc = createAccount(accRef, charity.getName(), AccountType.LIABILITY);
		charity.setAccount(acc);
    }

    //FIXME Try to save the account and retry on a constraint violation
    private String createNewAccountNumber(String prefix) {
    	String ncan = null;
    	int tries = 10;
    	while (true) {
    		ncan = String.format("%s-%s", prefix, TokenGenerator.createSecureToken());
    		if (! accountDao.findByAccountNumber(ncan).isPresent()) {
    			break;
    		}
    		--tries;
    		if (tries == 0) {
    			throw new IllegalStateException("Unable to create a NCAN");
    		}
    	}
    	return ncan;
    }
    
}
