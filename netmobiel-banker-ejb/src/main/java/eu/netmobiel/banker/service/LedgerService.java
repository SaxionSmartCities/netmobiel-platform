package eu.netmobiel.banker.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.ejb.Schedule;
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
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.SettlementOrder;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.repository.AccountDao;
import eu.netmobiel.banker.repository.AccountingEntryDao;
import eu.netmobiel.banker.repository.AccountingTransactionDao;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.DepositRequestDao;
import eu.netmobiel.banker.repository.LedgerDao;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.TokenGenerator;
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
	public static final String ACC_REF_RESERVATIONS = "reservations";
	public static final String ACC_NAME_RESERVATIONS = "Reservations";
	public static final Integer MAX_RESULTS = 10; 
	public static final Integer DEFAULT_LOOKBACK_DAYS = 90; 
	public static final int PAYMENT_LINK_EXPIRATION_SECS = 15 * 60;
	public static final int CREDIT_EXCHANGE_RATE = 19;	// 1 credit is x euro cent

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

    @Inject
    private DepositRequestDao depositRequestDao;

    @Inject
    private PaymentClient paymentClient;
    
    /**
     * Verify the active deposits every hour. 
     */
	@Schedule(info = "Deposit check", hour = "*/1", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void verifyActiveDepositRequests() {
		// Get the list of active deposit requests
		// For each request:
		//    Check the status at the provider
		List<DepositRequest> activeRequests = depositRequestDao.listByStatus(PaymentStatus.ACTIVE);
		if (! activeRequests.isEmpty()) {
			log.info(String.format("Checking %d active deposit request(s)", activeRequests.size()));
			for (DepositRequest dr : activeRequests) {
		    	try {
			    	PaymentLink plink = paymentClient.getPaymentLink(dr.getPaymentLinkId());
					synchronizeDepositionRequest(dr, plink);
		    	} catch (Exception ex) {
		    		log.error("Error verivying deposit request - " + ex.getMessage());
		    	}
			}
			activeRequests = depositRequestDao.listByStatus(PaymentStatus.ACTIVE);
			log.info(String.format("Now %d active deposit requests", activeRequests.size()));
		}
	}
	
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
     * @param acc the netmobiel account to deposit to. 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    protected void deposit(Account acc, int amount, OffsetDateTime when, String description, String reference) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, acc);
    	Balance brab = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
		try {
			AccountingTransaction tr = ledger
					.createTransaction(TransactionType.DEPOSIT, description, reference, when.toInstant(), Instant.now())
					.debit(brab, amount, userAccountBalance.getAccount().getName())
					.credit(userAccountBalance, amount, null)
					.build();
	    	accountingTransactionDao.save(tr);
		} catch (BalanceInsufficientException e) {
			throw new IllegalStateException("A deposit should never cause an BalanceInsufficientException");
		}
    }

    /**
     * A Netmobiel user withdraws credits. The balance of the netmobiel credit system shrinks: the account of the user gets
     * less credits and the banking reserve of Netmobiel is equally decreased.
     * @param acc the netmobiel account to withdraw from. 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    protected  void withdraw(Account acc, int amount, OffsetDateTime when, String description, String reference) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, acc);  
    	Balance brab = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	expect(brab.getAccount(), AccountType.ASSET);
    	AccountingTransaction tr = ledger
    			.createTransaction(TransactionType.WITHDRAWAL, description, reference, when.toInstant(), Instant.now())
    			.credit(brab, amount, null)
				.debit(userAccountBalance, amount, userAccountBalance.getAccount().getName())
    			.build();
    	accountingTransactionDao.save(tr);
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
    			.debit(originatorBalance, amount, beneficiaryBalance.getAccount().getName())
				.credit(beneficiaryBalance, amount, originatorBalance.getAccount().getName())
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
    protected AccountingTransaction reserve(Account acc, int amount, OffsetDateTime when, String description, String reference) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance userBalance = balanceDao.findByLedgerAndAccount(ledger, acc);  
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS);  
    	expect(userBalance.getAccount(), AccountType.LIABILITY);
    	expect(rb.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createTransaction(TransactionType.RESERVATION, description, reference, when.toInstant(), Instant.now())
    			.debit(userBalance, amount, null)
				.credit(rb, amount, userBalance.getAccount().getName())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    protected AccountingEntry lookupUserEntry(AccountingTransaction tr) {
    	List<AccountingEntry> rs_entries = tr.getAccountingEntries();
    	if (rs_entries.size() > 2) {
    		throw new IllegalStateException("Cannot lookup a user in a transaction with more than 2 entries: " + tr.getTransactionRef());
    	}
    	AccountingEntry userEntry = rs_entries.stream()
    			.filter(entry -> entry.getAccount().getNcan() != ACC_REF_RESERVATIONS)
    			.findFirst()
    			.orElseThrow(() -> new IllegalStateException("No user acount found when looking up transaction: " + tr.getTransactionRef()));
    	return userEntry;
    }

    /**
     * Releases a previous reservation. A new transaction will be added to nullify the reservation.
     * @param transactionRef the reference to the previously made reservation.  
     */
    protected AccountingTransaction release(String transactionRef, OffsetDateTime when) throws BalanceInsufficientException {
    	Long tid = BankerUrnHelper.getId(AccountingTransaction.URN_PREFIX, transactionRef);
    	AccountingTransaction reservation = accountingTransactionDao.find(tid).orElseThrow(() -> new IllegalArgumentException("No such transaction: " + transactionRef));
    	AccountingEntry userEntry = lookupUserEntry(reservation);
    	Ledger ledger = reservation.getLedger();
    	ledger.expectOpen();
    	Balance userBalance = balanceDao.findByLedgerAndAccount(ledger, userEntry.getAccount());  
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS);  
    	expect(userBalance.getAccount(), AccountType.LIABILITY);
    	expect(rb.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createTransaction(TransactionType.RELEASE, reservation.getDescription(), reservation.getContext(), when.toInstant(), Instant.now())
    			.credit(userBalance, userEntry.getAmount(), null)
				.debit(rb, userEntry.getAmount(), userEntry.getAccount().getName())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    public String reserve(NetMobielUser nmuser, int amount, String description, String reference) throws BalanceInsufficientException {
    	Optional<BankerUser> user = lookupUser(nmuser);
    	if (! user.isPresent()) {
    		throw new BalanceInsufficientException("User has no deposits made yet: " + nmuser.getManagedIdentity());
    	}
    	AccountingTransaction tr = reserve(user.get().getPersonalAccount(), amount, OffsetDateTime.now(), description, reference);
    	return tr.getTransactionRef();
    }

    public String release(String reservationId) {
    	AccountingTransaction tr = null;
    	try {
    		tr = release(reservationId, OffsetDateTime.now());
		} catch (BalanceInsufficientException e) {
			throw new IllegalStateException("Reservation account should not be empty", e);
		}
    	return tr.getTransactionRef();
    }

    public String charge(NetMobielUser nmbeneficiary, String reservationId, int actualAmount) throws BalanceInsufficientException, OverdrawnException {
    	Optional<BankerUser> beneficiary = lookupUser(nmbeneficiary);
    	if (! beneficiary.isPresent()) {
    		throw new BalanceInsufficientException("Beneficiary has no account, nothing to transfer to: " + nmbeneficiary.getManagedIdentity());
    	}
    	AccountingTransaction release = release(reservationId, OffsetDateTime.now());
    	int overspent = actualAmount - release.getAccountingEntries().get(0).getAmount(); 
    	if (overspent > 0) {
    		throw new OverdrawnException("Charge exceeds reservation: " + reservationId + " " + overspent);
    	}
    	AccountingEntry userEntry = lookupUserEntry(release);
    	AccountingTransaction charge_tr = transfer(userEntry.getAccount(), beneficiary.get().getPersonalAccount(), actualAmount, OffsetDateTime.now(), release.getDescription(), release.getContext());
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
    
    public PagedResult<Account> listAccounts(Integer maxResults, Integer offset) {
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
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = balanceDao.listBalances(acc, ledger, maxResults, offset);
    		results = balanceDao.fetch(ids.getData(), null, Balance::getId);
    	}
    	return new PagedResult<Balance>(results, maxResults, offset, prs.getTotalCount());
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
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = accountingEntryDao.listAccountingEntries(accountReference, since, until, maxResults, offset);
    		results = accountingEntryDao.fetch(ids.getData(), null, AccountingEntry::getId);
    	}
    	return new PagedResult<AccountingEntry>(results, maxResults, offset, prs.getTotalCount());
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
     * Create an account and connect it through a balance to the active ledger.   
     * @param holder the holder of the account.
     * @param reference the external reference to the account.
     * @param type the account type.
     * @return the account  
     */
    public void prepareAccount(String ncan, String name, AccountType type) {
    	accountDao.findByAccountNumber(ncan)
    		.orElseGet(() -> createAccount(ncan, name, type));
    }

    protected Optional<BankerUser> lookupUser(NetMobielUser user) {
    	return Optional.ofNullable(userDao.findByManagedIdentity(user.getManagedIdentity()).orElse(null));
    }
    
    /**
     * Event handler for handling new users. The ledger service must create and assign a personal monetary account.
     * @param dbUser the new user account. The user record. It must be persistent already.
     */
    public void onNewUser(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created BankerUser dbUser) {
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
     * Event handler for handling new charities. The ledger service must create and assign a personal monetary account.
     * @param charity the new user account. The user record. It must be persistent already.
     */
    public void onNewCharity(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created Charity charity) {
		// Create a charity liability account.
		String accRef = createNewAccountNumber("CLA");
		Account acc = createAccount(accRef, charity.getAccount().getName(), AccountType.LIABILITY);
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
    	plink.merchantOrderId = String.format("%s-%d", acc.getNcan(), Instant.now().getEpochSecond());
    	plink.returnUrl = returnUrl;
    	plink = paymentClient.createPaymentLink(plink);
    	// Only id and payment url are added now by the client. Do not use other fields, they are not yet set!
    	dr.setAccount(acc);
    	dr.setAmountCredits(amountCredits);
    	dr.setAmountEurocents(plink.amount);
    	dr.setMerchantOrderId(plink.merchantOrderId);
    	dr.setDescription(plink.description);
    	dr.setCreationTime(Instant.now());
    	// The development environment of EMS Pay ignores our expiration period 
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
     * @param paymentProjectId The project id as supplied by the payment provider.
     * @param paymentOrderId The order id as supplied by the payment provider.
     * @return true if the deposition of the payment link was added now or earlier. If false then deposition has not taken place (yet), or something
     * 			went wrong.
     */
    public Optional<DepositRequest> verifyDeposition(String paymentProjectId, String paymentOrderId) {
    	DepositRequest dr = null;
    	try {
	    	PaymentLink plink = paymentClient.getPaymentLinkByOrderId(paymentOrderId);
	    	// Possible optimization: 
	    	// First get payment link id, fetch deposit request, then if still ACTIVE get the payment link record.
	    	dr = depositRequestDao.findByPaymentLink(plink.id);
	    	synchronizeDepositionRequest(dr, plink);
    	} catch (Exception ex) {
    		log.error("Error verivying deposition - " + ex.getMessage());
    	}
    	return Optional.ofNullable(dr == null ? null : dr);
    }

    /**
     * Synchronizes the the deposit request with the payment link at the payment provider.
     * @param dr The deposit request to verify.
     * @param paymentLink The payment link object retrieved from the payment provider.
     */
    public void synchronizeDepositionRequest(DepositRequest dr, PaymentLink plink) {
    	if (!plink.id.equals(dr.getPaymentLinkId())) {
    		throw new IllegalArgumentException("Invalid combination of deposit request and payment link");
    	}
    	// Assure the request is in the persistence context
    	DepositRequest dr_db = depositRequestDao.find(dr.getId())
    			.orElseThrow(() -> new IllegalArgumentException("DepositRequest has gone: dr.getId()"));
    	if (dr_db.getStatus() == PaymentStatus.ACTIVE) {
        	if (plink.status == PaymentLinkStatus.COMPLETED) {
        		// Transition to COMPLETED, add transaction to deposit credits in the NetMobiel system
        		dr_db.setCompletedTime(plink.completed.toInstant());
        		deposit(dr_db.getAccount(), dr_db.getAmountCredits(), dr_db.getCompletedTime().atOffset(ZoneOffset.UTC), dr_db.getDescription(), dr_db.getDepositRequestRef());
        		dr_db.setStatus(PaymentStatus.COMPLETED);
        		log.info(String.format("DepositRequest %d has completed", dr_db.getId()));
        	} else if (plink.status == PaymentLinkStatus.EXPIRED) {
        		dr_db.setStatus(PaymentStatus.EXPIRED);
        		log.info(String.format("DepositRequest %d has expired", dr_db.getId()));
        	}
    	}
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
}
