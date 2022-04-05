package eu.netmobiel.banker.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Asynchronous;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.exception.OverdrawnException;
import eu.netmobiel.banker.filter.RewardFilter;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountPurposeType;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingEntryType;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.model.SettlementOrder;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.repository.AccountDao;
import eu.netmobiel.banker.repository.AccountingEntryDao;
import eu.netmobiel.banker.repository.AccountingTransactionDao;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.LedgerDao;
import eu.netmobiel.banker.repository.RewardDao;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.TokenGenerator;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * Stateless bean for the management of the ledger.
 * Design decision: A regular user has aq running account as well as a premium account. The premium account is used 
 * for incentives, to motivate the user into certian desired behaviour with regard to the use of Netmobiel.
 * We want to show the spending of premium credits explicitly, it must be visible in the statement overview of 
 * the running account (the default statement view). To show the premium spending in the statement overview there are two choices:
 * - Show when reserving the travel fee
 * - Show when charging the travel fee.
 * For now we use the first option, the advantage for the new traveller with premium credits is the ability to travel without
 * depositing first (assuming the premium balance is enough).
 * 
 * The balances are a shared resources for many processes. To prevent too many roll-backs, the system balances are locked with
 * a pessimistic write. To prevent deadlock, system balances are always locked in the same sequence: 
 * banking-reserve, reservations, premiums.
 * 
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@DeclareRoles({ "admin", "treasurer" })
@PermitAll
public class LedgerService {
	/**
	 * The Asset account for Netmobiel. This account reflects the total amount of credits available in Netmobiel.
	 */
	public static final String ACC_REF_BANKING_RESERVE = "banking-reserve";
	public static final String ACC_NAME_BANKING_RESERVE = "De NetMobiel Kluis";
	/**
	 * The Reservation account of netmobiel where all reservations are saved. A reservation amount is in general 
	 * (i.e., always) reserved and released a single sum, as opposed to released in parts.
	 * This is a liability account.
	 */
	public static final String ACC_REF_RESERVATIONS = "reservations";
	public static final String ACC_NAME_RESERVATIONS = "NetMobiel Reserveringen";
	/**
	 * The central account from where each premium is paid to individual users. 
	 */
	public static final String ACC_REF_PREMIUMS = "premiums";
	public static final String ACC_NAME_PREMIUMS = "NetMobiel Premies";
	
	public static final Integer MAX_RESULTS = 10; 
	public static final int CREDIT_EXCHANGE_RATE = 19;	// 1 credit is x euro cent

	public static final List<String> SYSTEM_NCAN = List.of(ACC_REF_BANKING_RESERVE, ACC_REF_RESERVATIONS, ACC_REF_PREMIUMS);
	
	@Inject
    private Logger log;

    @Resource
	private SessionContext sessionContext;

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
    private RewardDao rewardDao;

    private static void expect(Account account, AccountType type) {
    	if (! Account.isOpen.test(account)) {
    		throw new IllegalArgumentException(String.format("Account is not open: %s", account.toString()));
    	}
    	if (account.getAccountType() != type) {
    		throw new IllegalArgumentException(String.format("Expected account type %s, got %s", type.toString(), account.toString()));
    	}
    }

    private static void expect(AccountingTransaction tr, TransactionType purpose, AccountingEntryType entryType) {
    	@SuppressWarnings("unused")
		AccountingEntry ae = tr.lookup(purpose, entryType);
    }

    static void sortAccountsForLocking(List<Account> accounts) {
    	accounts.sort(new Comparator<Account>() {
			@Override
			public int compare(Account a1, Account a2) {
				int cmp = Integer.compare(a1.getPurpose().ordinal(), a2.getPurpose().ordinal());
				if  (cmp == 0) {
					if (a1.getPurpose() == AccountPurposeType.SYSTEM) {
						cmp = Integer.compare(SYSTEM_NCAN.indexOf(a1.getNcan()), SYSTEM_NCAN.indexOf(a2.getNcan()));
					}
				}
				return cmp;
			}
		});
    }

    /**
     * A Netmobiel user deposits credits. The balance of the netmobiel credit system grows: the account of the user gets
     * more credits and the banking reserve of Netmobiel is equally increased.
     * A deposit takes in general place on the running account of a user. The treasurer can opt to deposit credits 
     * to the premium account of the system, intended to reward users.
     * @param acc the netmobiel account to deposit to. 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public AccountingTransaction deposit(Account acc, int amount, OffsetDateTime when, String description, String reference) {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance assetBalance = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE, LockModeType.PESSIMISTIC_WRITE);  
    	expect(assetBalance.getAccount(), AccountType.ASSET);
    	Balance liabilityBalance = balanceDao.findByLedgerAndAccount(ledger, acc, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    	expect(liabilityBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = null;
		try {
			tr = ledger
					.createStartTransaction(description, reference, when.toInstant(), Instant.now())
					.debit(assetBalance, amount, TransactionType.DEPOSIT, liabilityBalance.getAccount())
					.credit(liabilityBalance, amount, TransactionType.DEPOSIT, assetBalance.getAccount())
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
     * A withdrawal can only take place on the running account of a user or a charity.
     * @param acc the netmobiel account to withdraw from. 
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @description the description in the journal.
     */
    public  AccountingTransaction withdraw(Account acc, int amount, OffsetDateTime when, String description, String reference) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance assetBalance = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE, LockModeType.PESSIMISTIC_WRITE);  
    	expect(assetBalance.getAccount(), AccountType.ASSET);
    	Balance liabilityBalance = balanceDao.findByLedgerAndAccount(ledger, acc, LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	expect(liabilityBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createStartTransaction(description, reference, when.toInstant(), Instant.now())
    			.credit(assetBalance, amount, TransactionType.WITHDRAWAL, liabilityBalance.getAccount())
				.debit(liabilityBalance, amount, TransactionType.WITHDRAWAL, assetBalance.getAccount())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    /**
     * A Netmobiel user withdraws credits that have been reserved in a previous step. 
     * The balance of the netmobiel credit system shrinks: the account of the user gets
     * less credits and the banking reserve of Netmobiel is equally decreased.
     * A withdrawal can only take place on the running account of a user or a charity.
     * Note: Netmobiel cannot verify whether the payment to the bank account has really taken place, the treasure
     * is the proxy in this process.
     * @param reservation the earlier reservation. 
     * @param when the accounting time of this financial fact.
     * @throws NotFoundException 
     */
    public AccountingTransaction withdraw(AccountingTransaction reservation, OffsetDateTime when) 
    		throws BalanceInsufficientException, NotFoundException {
    	reservation = lookupTransactionWithEntries(reservation.getId());
    	AccountingTransaction theHead = reservation.getHead() != null ? reservation.getHead() : reservation; 
    	expect(reservation, TransactionType.RESERVATION, AccountingEntryType.DEBIT);
		AccountingEntry userAccEntry = reservation.lookup(TransactionType.RESERVATION, AccountingEntryType.DEBIT);
		final int amount = userAccEntry.getAmount();

		Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance brab = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_BANKING_RESERVE, LockModeType.PESSIMISTIC_WRITE);  
    	expect(brab.getAccount(), AccountType.ASSET);
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS, LockModeType.PESSIMISTIC_WRITE);
    	expect(rb.getAccount(), AccountType.LIABILITY);
    	Balance userAccountBalance = balanceDao.findByLedgerAndAccount(ledger, userAccEntry.getAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	expect(userAccountBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createFollowUpTransactionFromHead(theHead, when.toInstant(), Instant.now())
    	    	.transfer(rb, amount, TransactionType.RELEASE, userAccountBalance)
    			.credit(brab, amount, TransactionType.WITHDRAWAL, userAccountBalance.getAccount())
				.debit(userAccountBalance, amount, TransactionType.WITHDRAWAL, brab.getAccount())
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
     * @param rollback if set then the transfer is a rollback
     * @return the persisted transaction object
     */
    public AccountingTransaction transfer(Account originator, Account beneficiary, int amount, OffsetDateTime when, 
    		String description, String reference, boolean rollback) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance originatorBalance = balanceDao.findByLedgerAndAccount(ledger, originator, LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	expect(originatorBalance.getAccount(), AccountType.LIABILITY);
    	Balance beneficiaryBalance = balanceDao.findByLedgerAndAccount(ledger, beneficiary, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    	expect(beneficiaryBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction tr = ledger
    			.createStartTransaction(description, reference, when.toInstant(), Instant.now())
    			.rollback(rollback)
    			.debit(originatorBalance, amount, TransactionType.PAYMENT, beneficiaryBalance.getAccount())
				.credit(beneficiaryBalance, amount, TransactionType.PAYMENT, originatorBalance.getAccount())
    			.build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

	/**
	 * Create a transaction that reverses this transaction. The opposite accounting entry is created and also the purpose is reversed.
	 * @param head The head of the conversation
	 * @param trToReverse the transaction to reverse (can be equal to the head)
	 * @param accountingTime the accounting time
	 * @param isRollback If true this is a rollback transaction
	 * @return The reversed transaction, not persisted yet.
	 * @throws BalanceInsufficientException In case a debit depletes the balance more than allowed.  
	 */
	private AccountingTransaction reverse(@NotNull AccountingTransaction head, AccountingTransaction trToReverse, Instant accountingTime, boolean isRollback) 
			throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(accountingTime);
    	ledger.expectOpen();
		AccountingTransaction.Builder trb =	ledger
				.createFollowUpTransactionFromHead(head, accountingTime, Instant.now())
				.rollback(isRollback);
		// The locking of the balances should occur in the proper order: System accounts first
		List<Account> accounts = trToReverse.getAccountingEntries().stream()
				.map(ae -> ae.getAccount())
				.collect(Collectors.toList());
		sortAccountsForLocking(accounts);
		Map<Account, Balance> balanceMap = new HashMap<>();
		accounts.forEach(acc -> {
			LockModeType type = acc.getPurpose() == AccountPurposeType.SYSTEM ? LockModeType.PESSIMISTIC_WRITE
					: LockModeType.OPTIMISTIC_FORCE_INCREMENT;
	    	Balance balance = balanceDao.findByLedgerAndAccount(ledger, acc, type);
			balanceMap.put(acc, balance);
		});
		// Iterate the transactions also in opposite sequence, that is for the logic in the list of statements
		ArrayList<AccountingEntry> reversedEntries = new ArrayList<>(trToReverse.getAccountingEntries());
		Collections.reverse(reversedEntries);
		for (AccountingEntry entry : reversedEntries) {
	    	Balance originatorBalance = balanceMap.get(entry.getAccount());
	    	TransactionType reversedPurpose = entry.getPurpose().reverse();
			if (entry.getEntryType() == AccountingEntryType.CREDIT) {
				trb.debit(originatorBalance, entry.getAmount(), reversedPurpose, entry.getCounterparty());
			} else {
				trb.credit(originatorBalance, entry.getAmount(), reversedPurpose, entry.getCounterparty());
			}
		}
		return trb.build();
	}
	

    /**
     * Reserves an amount of credits from one Netmobiel user for a yet to be delivered service. The reservation will try to reserve
     * some premium credits first, and the remaining part from the regular account. This method starts a new transaction conversation. 
     * @param acc the netmobiel account that will be charged
     * @param amount the amount of credits
     * @param premiumAcc the premium account that might be charged
     * @param maxPremiumPercentage The maximum amount in percentage to take from the premium account. Zero means nothing and
     * 							100 means everything, if the balance allows for.
     * @param when the time of this financial fact.
     * @param description the description in the journal.
     * @param reference the contextual reference to a system object in the form of a urn.
     * @param rollback if set then the transfer is a rollback
     * @return the persisted transaction object
     */
    public AccountingTransaction reserve(Account acc, int amount, Account premiumAcc, int maxPremiumPercentage, 
    		OffsetDateTime when, String description, String reference, boolean rollback) throws BalanceInsufficientException {
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS, LockModeType.PESSIMISTIC_WRITE);  
    	expect(rb.getAccount(), AccountType.LIABILITY);
    	Balance userBalance = balanceDao.findByLedgerAndAccount(ledger, acc, LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	expect(userBalance.getAccount(), AccountType.LIABILITY);
    	Balance premiumBalance = null;
    	int premiumAmount = 0;
    	if (maxPremiumPercentage > 0 && premiumAcc != null) {
    		premiumBalance = balanceDao.findByLedgerAndAccount(ledger, premiumAcc, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        	expect(premiumBalance.getAccount(), AccountType.LIABILITY);
    		premiumAmount = Math.min(Math.round((amount * maxPremiumPercentage) / 100.0f), premiumBalance.getEndAmount());
    	}
    	AccountingTransaction.Builder trb = ledger
    			.createStartTransaction(description, reference, when.toInstant(), Instant.now())
    			.rollback(rollback);
    	if (premiumBalance != null && premiumAmount > 0) {
			trb.transfer(premiumBalance, premiumAmount, TransactionType.RELEASE, userBalance);
    	}
    	AccountingTransaction tr = trb.transfer(userBalance, amount, TransactionType.RESERVATION, rb).build();
    	accountingTransactionDao.save(tr);
    	return tr;
    }

    /**
     * Reserves an amount of credits from one Netmobiel user for a yet to be delivered service. The reservation will use 
     * the regular account only. This method starts a new transaction conversation. 
     * @param acc the netmobiel account that will be charged
     * @param amount the amount of credits
     * @param when the time of this financial fact.
     * @param description the description in the journal.
     * @param reference the contextual reference to a system object in the form of a urn.
     * @param rollback if set then the transfer is a rollback
     * @return the persisted transaction object
     */
    public AccountingTransaction reserve(Account acc, int amount, OffsetDateTime when, 
    		String description, String reference, boolean rollback) throws BalanceInsufficientException {
    	return reserve(acc, amount, null, 0, when, description, reference, rollback);
    }

    private AccountingTransaction lookupTransactionWithEntries(Long tid) throws NotFoundException {
    	return accountingTransactionDao.loadGraph(tid, AccountingTransaction.ENTRIES_ENTITY_GRAPH)
    			.orElseThrow(() -> new IllegalArgumentException("No such transaction: " + tid)); 
    }
    
    /**
     * Reserves an amount of credits from a Netmobiel user for a yet to be delivered service. 
     * @param nmuser the netmobiel user to be charged
     * @param amount the amount of credits
     * @param maxPremiumPercentage The maximum amount in percentage to take from the premium account. Zero means nothing and
     * 							100 means everything, if the balance allows for.
     * @param description the description in the journal.
     * @param reference the contextual reference to a system object in the form of a urn.
     * @return the transaction urn
     * @throws BalanceInsufficientException 
     */
    public String reserve(NetMobielUser nmuser, int amount, int maxPremiumPercentage, String description, String reference) throws NotFoundException, BalanceInsufficientException {
    	BankerUser user = lookupUser(nmuser)
    			.orElseThrow(() -> new NotFoundException("No such user: " + nmuser.getManagedIdentity()));
    	AccountingTransaction tr = reserve(user.getPersonalAccount(), amount, user.getPremiumAccount(), 
    			maxPremiumPercentage, OffsetDateTime.now(), description, reference, false);
    	return tr.getTransactionRef();
    }

    /**
     * Cancels a previous reservation. A new transaction will be added to nullify the reservation.
     * The earlier reservation may have used premium credits. The only transaction with knowledge about the use of premium credits is the very first
     * transaction of the conversation. Use that one to release the credits to the proper account.
     * Note that a release is only possible if there are still credits reserved by the transaction conversation. We need to verify that. 
     * @param reservation the reference to the previously made reservation (or refund).  
     * @param when the timestamp of the release.
     * @return the transaction  
     * @throws NotFoundException 
     */
    public AccountingTransaction cancel(AccountingTransaction reservation, OffsetDateTime when) throws NotFoundException {
    	AccountingTransaction headReservation; 
    	if (reservation.getHead() != null) {
    		headReservation = lookupTransactionWithEntries(reservation.getHead().getId());
    	} else {
    		headReservation = lookupTransactionWithEntries(reservation.getId()); 
    	}
    	expect(headReservation, TransactionType.RESERVATION, AccountingEntryType.DEBIT);
    	AccountingTransaction tr = null;
    	try {
    		// A cancel always uses the initial reservation! This is a regular operation (and not a rollback)
        	tr = reverse(headReservation, headReservation, when.toInstant(), false); 
        	accountingTransactionDao.save(tr);
		} catch (BalanceInsufficientException e) {
			throw new IllegalStateException("Panic: Reservation account should never be empty", e);
		}
    	return tr;
    }

    /**
     * Releases a previous reservation. A new transaction will be added to nullify the reservation.
     * The provided transaction is used to retrieve the start of the transaction conversation. Only the head contains the original 
     * reservation and knows the origin of the credits. 
     * @param transactionRef the reference to the previously made reservation.  
     * @param when the timestamp of the release.
     * @return the transaction, but only the flat object.  
     * @throws BadRequestException 
     * @throws NotFoundException 
     */
    public AccountingTransaction cancel(String transactionRef, OffsetDateTime when) throws BadRequestException, NotFoundException {
    	Long tid = UrnHelper.getId(AccountingTransaction.URN_PREFIX, transactionRef);
    	AccountingTransaction reservation = accountingTransactionDao.find(tid).orElseThrow(() -> new IllegalArgumentException("No such transaction: " + transactionRef));
    	return cancel(reservation, when);
    }

    /**
     * Releases a previous reservation. 
     * @param reservationId the earlier reservation transaction
     * @return the transaction urn
     * @throws NotFoundException 
     * @throws BadRequestException 
     */
    public String cancel(String reservationId) throws NotFoundException, BadRequestException {
    	AccountingTransaction tr = cancel(reservationId, OffsetDateTime.now());
    	return tr.getTransactionRef();
    }

    /**
     * Reverses (kind of) an earlier cancel by reserving the same amount again. Mark the entry as a rollback.
     * This will start a new transaction conversation.
     * @param releaseId the earlier release transaction urn
     * @return the persisted transaction
     * @throws BadRequestException
     * @throws BalanceInsufficientException
     * @throws NotFoundException 
     */
    public String uncancel(String releaseId, int maxPremiumPercentage) throws BadRequestException, BalanceInsufficientException, NotFoundException {
    	Long releaseTid = UrnHelper.getId(AccountingTransaction.URN_PREFIX, releaseId);
    	AccountingTransaction release = lookupTransactionWithEntries(releaseTid);
    	expect(release, TransactionType.RELEASE, AccountingEntryType.CREDIT);
    	AccountingEntry userEntry = release.lookup(TransactionType.RELEASE, AccountingEntryType.CREDIT);
    	BankerUser user = userDao.findByPersonalAccount(userEntry.getAccount())
    			.orElseThrow(() -> new NotFoundException("No such user with personal account: " + userEntry.getAccount()));
    	AccountingTransaction tr = reserve(user.getPersonalAccount(), userEntry.getAmount(), user.getPremiumAccount(), maxPremiumPercentage, 
    			OffsetDateTime.now(), release.getDescription(), release.getContext(), true);
    	return tr.getTransactionRef();
    }

    /**
     * Charges an amount of credits from a Netmobiel user, using an earlier reserved fare for a service. 
     * @param nmbeneficiary the netmobiel user to pay the reserved amount of credits.
     * @param reservationId the earlier reservation transaction urn (could also be an reversed transaction).
     * @return the persisted transfer transaction
     * @throws BadRequestException
     * @throws BalanceInsufficientException
     * @throws NotFoundException 
     */
    public String charge(NetMobielUser nmbeneficiary, String reservationId) 
    		throws BalanceInsufficientException, OverdrawnException, BadRequestException, NotFoundException {
    	OffsetDateTime when = OffsetDateTime.now();
    	BankerUser beneficiary = lookupUser(nmbeneficiary)
    			.orElseThrow(() -> new NotFoundException("Beneficiary has no account: " + nmbeneficiary.getManagedIdentity()));
    	Long tid = UrnHelper.getId(AccountingTransaction.URN_PREFIX, reservationId);
    	AccountingTransaction reservation = lookupTransactionWithEntries(tid);
		AccountingEntry userAccEntry = reservation.lookup(TransactionType.RESERVATION, AccountingEntryType.DEBIT);
		// The conversation head is the head of the previous transaction , or the reservation itself. 
		AccountingTransaction head = reservation.getHead() != null ? reservation.getHead() : reservation;
		Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance rb = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_RESERVATIONS, LockModeType.PESSIMISTIC_WRITE);
    	Balance userBalance = balanceDao.findByLedgerAndAccount(ledger, userAccEntry.getAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	Balance beneficiaryBalance = balanceDao.findByLedgerAndAccount(ledger, beneficiary.getPersonalAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	if (rb.getId().equals(userBalance.getId())) {
    		throw new IllegalStateException("Panic: Expected to find a user debit reservation entry in reservation: " + reservationId);
    	}
    	AccountingTransaction tr = ledger
    			.createFollowUpTransactionFromHead(head, when.toInstant(), Instant.now())
    	    	.transfer(rb, userAccEntry.getAmount(), TransactionType.RELEASE, userBalance)
    	    	.transfer(userBalance, userAccEntry.getAmount(), TransactionType.PAYMENT, beneficiaryBalance)
    	    	.build();
    	accountingTransactionDao.save(tr);
    	return tr.getTransactionRef();
    }

    /**
     * Reverses an earlier charge, i.e., a refund. 
     * @param chargeId the charge in question
     * @return the transaction reference of this transaction (which includes a reservation).
     * @throws BalanceInsufficientException
     * @throws OverdrawnException
     * @throws BadRequestException
     * @throws NotFoundException 
     */
    public String uncharge(String chargeId) 
    		throws BalanceInsufficientException, BadRequestException, NotFoundException {
    	Long chargeTid = UrnHelper.getId(AccountingTransaction.URN_PREFIX, chargeId);
    	AccountingTransaction charge = lookupTransactionWithEntries(chargeTid);
    	expect(charge, TransactionType.PAYMENT, AccountingEntryType.DEBIT);
    	expect(charge, TransactionType.RELEASE, AccountingEntryType.DEBIT);
    	// Mark the transaction as a rollback
    	AccountingTransaction theHead = charge.getHead() != null ? charge.getHead() : charge; 
    	AccountingTransaction tr = reverse(theHead, charge, Instant.now(), true); 
       	accountingTransactionDao.save(tr);
    	return tr.getTransactionRef();
    }

    /**
     * Closes the current ledger by setting the endPeriod to <code>newStartPeriod</code>.
     * Move all transactions with accountingTime >= newStartPeriod to the new ledger.
     * Calculate the account balances of the closed ledger and calculate the balances for
     * the new ledger. 
     * @param newStartPeriod
     */
    //TODO finish implementation
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
    	if (maxResults > 0 && totalCount > 0) {
    		// Get the actual data
    		PagedResult<Long> lids = ledgerDao.listLedgers(maxResults, offset);
    		results = ledgerDao.loadGraphs(lids.getData(), null, Ledger::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, totalCount);
    }
    
	/**
	 * Lists the accounts. Filter optionally by criteria.
	 * @param accountName the account name, use '%' for any substring and '_' for any character match. Use '\' to 
	 * 					escape the special characters.  
	 * @param purpose the account purpose type
	 * @param maxResults The maximum results to query. If set to 0 the total number of results is fetched.
	 * @param offset The zero-based paging offset 
	 * @return a paged result of account objects, sorted by account name ascending
	 */
    public PagedResult<Account> listAccounts(String accountName, AccountPurposeType purpose, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	PagedResult<Long> prs = accountDao.listAccounts(accountName, purpose, null, 0, offset);
    	List<Account> results = null;
    	if (maxResults > 0 && prs.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> mids = accountDao.listAccounts(accountName, purpose, null, maxResults, offset);
    		results = accountDao.loadGraphs(mids.getData(), null, Account::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

	/**
	 * Lists accounts with their balance. Filter optionally by account criteria. 
	 * @param accountName the account name, use '%' for any substring and '_' for any character match. Use '\' to 
	 * 					escape the special characters.  
	 * @param purpose the account purpose type
	 * @param ledgerTime the ledger time. Omit for the actual ledger.
	 * @param maxResults The maximum results to query. If set to 0 the total number of results is fetched.
	 * @param offset The zero-based paging offset 
	 * @return a paged result of account objects, sorted by account name ascending
	 */
    public PagedResult<Account> listAccountsWithBalance(String accountName, AccountPurposeType purpose, 
    		OffsetDateTime ledgerTime, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        if (ledgerTime == null) {
        	ledgerTime = OffsetDateTime.now();
        }
		Ledger ledger = ledgerDao.findByDate(ledgerTime.toInstant());
    	PagedResult<Long> prs = balanceDao.listBalances(accountName, purpose, null, ledger, 0, offset);
    	List<Account> accounts = null;
    	if (maxResults > 0 && prs.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = balanceDao.listBalances(accountName, purpose, null, ledger, maxResults, offset);
    		List<Balance> results = balanceDao.loadGraphs(ids.getData(), null, Balance::getId);
        	results.forEach(b -> b.getAccount().setActualBalance(b));
        	accounts = results.stream().map(b -> b.getAccount()).collect(Collectors.toList());
    	}
    	return new PagedResult<>(accounts, maxResults, offset, prs.getTotalCount());
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
    	if (maxResults > 0 && prs.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = balanceDao.listBalances(acc, ledger, maxResults, offset);
    		results = balanceDao.loadGraphs(ids.getData(), null, Balance::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

    public PagedResult<AccountingEntry> listAccountingEntries(String accountReference, Instant since, Instant until, TransactionType purpose, Integer maxResults, Integer offset) 
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
    	PagedResult<Long> prs = accountingEntryDao.listAccountingEntries(accountReference, since, until, purpose, 0, offset);
    	List<AccountingEntry> results = null;
    	if (maxResults > 0 && prs.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = accountingEntryDao.listAccountingEntries(accountReference, since, until, purpose, maxResults, offset);
    		results = accountingEntryDao.loadGraphs(ids.getData(), AccountingEntry.STATEMENT_ENTITY_GRAPH, AccountingEntry::getId);
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
    public Account createAccount(String reference, String name, AccountType type, AccountPurposeType purpose) {
    	Instant now = Instant.now();
    	Ledger ledger = ledgerDao.findByDate(now);
    	Account acc = Account.newInstant(reference, name, type, purpose);
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
    @RolesAllowed({ "admin", "treasurer" })
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
    public void prepareAccount(String ncan, String name, AccountType type, AccountPurposeType purpose) {
    	if (accountDao.findByAccountNumber(ncan).isEmpty()) {
    		createAccount(ncan, name, type, purpose);
    	}
    }

    private Optional<BankerUser> lookupUser(NetMobielUser user) {
    	return Optional.ofNullable(userDao.findByManagedIdentity(user.getManagedIdentity()).orElse(null));
    }

    private Account createAccount(BankerUser dbUser, String suffix, AccountPurposeType purpose) {
		if (! userDao.contains(dbUser)) {
			throw new IllegalStateException("User should be in persistence context");
		}
		// Create a personal liability account. 
		String accRef = createNewAccountNumber("PLA");
		String accName = String.format("%s %s", dbUser.getName(), suffix).trim();
		if (accName.isEmpty()) {
			accName = accRef;
		}
		return createAccount(accRef, accName, AccountType.LIABILITY, purpose);
    }
    /**
     * Adds a personal account to a new user. The ledger service must create and assign a personal monetary account.
     * @param dbUser the new user account. The user record. It must be persistent already.
     */
    public void addPersonalAccount(BankerUser dbUser) {
    	if (dbUser.getPersonalAccount() != null) {
			throw new IllegalStateException("Not a new user, personal account exists already");
    	}    		
		Account acc = createAccount(dbUser, "", AccountPurposeType.CURRENT);
		dbUser.setPersonalAccount(acc);
    }

    /**
     * Adds a premium account to a new user. The ledger service must create and assign a premium monetary account.
     * @param dbUser the new user account. The user record. It must be persistent already.
     */
    public void addPremiumAccount(BankerUser dbUser) {
    	if (dbUser.getPremiumAccount() != null) {
			throw new IllegalStateException("Not a new user, premium account exists already");
    	}    		
		Account acc = createAccount(dbUser, "(Premie)", AccountPurposeType.PREMIUM);
		dbUser.setPremiumAccount(acc);
    }

    /**
     * Event handler for handling new users. The ledger service must create and assign a personal monetary account.
     * @param dbUser the new user account. The user record. It must be persistent already.
     */
    public void onNewUser(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created BankerUser dbUser) {
    	addPersonalAccount(dbUser);
    	addPremiumAccount(dbUser);
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
    			order.getEntryTime().atOffset(ZoneOffset.UTC), order.getDescription(), order.getContext(), false);
    }
    
    /**
     * Event handler for handling new charities. The ledger service must create and assign a personal monetary account.
     * @param charity the new user account. The user record. It must be persistent already.
     */
    public void onNewCharity(final @Observes(during = TransactionPhase.IN_PROGRESS) @Created Charity charity) {
		// Create a charity liability account.
		String accRef = createNewAccountNumber("CLA");
		Account acc = createAccount(accRef, charity.getName(), AccountType.LIABILITY, AccountPurposeType.CURRENT);
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
     * Pay a user an amount of premium credits. The credits are first paid to the current account and then 
     * reserved on the premium account for spending on selected activities.
     * A reward could have a cancelled transaction attached, verify.
     * @param reward
     * @param when
     * @param statementText the text to appear on the statement.
     * @throws BalanceInsufficientException
     * @throws NotFoundException
     */
    public void rewardWithPremium(Reward reward, OffsetDateTime when) throws BalanceInsufficientException, NotFoundException {
    	Reward rewarddb = rewardDao.loadGraph(reward.getId(), Reward.GRAPH_WITH_INCENTIVE_AND_RECIPIENT)
    			.orElseThrow(() -> new NotFoundException("No such reward: " + reward.getId()));
    	BankerUser user = userDao.loadGraph(rewarddb.getRecipient().getId(), BankerUser.GRAPH_WITH_ACCOUNT)  
    			.orElseThrow(() -> new NotFoundException("No such user: " + rewarddb.getRecipient().getManagedIdentity()));
    	int amount = rewarddb.getAmount();
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance maecenasBalance = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_PREMIUMS, LockModeType.PESSIMISTIC_WRITE);  
    	expect(maecenasBalance.getAccount(), AccountType.LIABILITY);
    	Balance personalBalance = balanceDao.findByLedgerAndAccount(ledger, user.getPersonalAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	expect(personalBalance.getAccount(), AccountType.LIABILITY);
    	Balance personalPremiumBalance = balanceDao.findByLedgerAndAccount(ledger, user.getPremiumAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    	expect(personalPremiumBalance.getAccount(), AccountType.LIABILITY);
    	AccountingTransaction head = null; 
    	if (rewarddb.getTransaction() != null) {
        	AccountingTransaction prevTr = lookupTransactionWithEntries(rewarddb.getTransaction().getId());
    		head = prevTr.getHead() != null ? prevTr.getHead() : prevTr;
    		if (prevTr.hasEntry(TransactionType.PAYMENT)) {
    			// Panic! the payment was already made!
    			throw new IllegalStateException("Duplicate pay-out of reward detected:" + rewarddb.getUrn());
    		}
    	}
    	// The system premium is paid to the personal account and then reserved on the premium account of that user.
    	AccountingTransaction tr = ledger
    			.createTransaction(rewarddb.getIncentive().getDescription(), rewarddb.getUrn(), when.toInstant(), Instant.now())
    			.head(head)
    			.transfer(maecenasBalance, amount, TransactionType.PAYMENT, personalBalance)
				.transfer(personalBalance, amount, TransactionType.RESERVATION, personalPremiumBalance)
    			.build();
    	accountingTransactionDao.save(tr);
    	rewarddb.setTransaction(tr);
    	rewarddb.setCancelTime(null);
    	rewarddb.setPaidOut(true);
    }
    
    /**
     * Pay a user an amount of premium credits. The credits are first paid to the current account and then 
     * reserved on the premium account for spending on selected activities.
     * A reward could have a cancelled transaction attached, verify.
     * @param reward
     * @param when
     * @param statementText the text to appear on the statement.
     * @throws BalanceInsufficientException
     * @throws NotFoundException
     */
    public void rewardWithRedemption(Reward reward, OffsetDateTime when) throws BalanceInsufficientException, NotFoundException {
    	Reward rewarddb = rewardDao.loadGraph(reward.getId(), Reward.GRAPH_WITH_INCENTIVE_AND_RECIPIENT)
    			.orElseThrow(() -> new NotFoundException("No such reward: " + reward.getId()));
    	BankerUser user = userDao.loadGraph(rewarddb.getRecipient().getId(), BankerUser.GRAPH_WITH_ACCOUNT)  
    			.orElseThrow(() -> new NotFoundException("No such user: " + rewarddb.getRecipient().getManagedIdentity()));
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	ledger.expectOpen();
    	Balance personalBalance = balanceDao.findByLedgerAndAccount(ledger, user.getPersonalAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);  
    	expect(personalBalance.getAccount(), AccountType.LIABILITY);
    	Balance personalPremiumBalance = balanceDao.findByLedgerAndAccount(ledger, user.getPremiumAccount(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    	expect(personalPremiumBalance.getAccount(), AccountType.LIABILITY);
    	// With redemption you can only redeem what is yours and as far as it goes!
    	int actualAmount = Math.min(rewarddb.getAmount(), personalPremiumBalance.getEndAmount());
    	if (actualAmount > 0) {
    		// There is still some left
        	AccountingTransaction head = null; 
        	if (rewarddb.getTransaction() != null) {
            	AccountingTransaction prevTr = lookupTransactionWithEntries(rewarddb.getTransaction().getId());
        		head = prevTr.getHead() != null ? prevTr.getHead() : prevTr;
        		if (prevTr.hasEntry(TransactionType.PAYMENT)) {
        			// Panic! the payment was already made!
        			throw new IllegalStateException("Duplicate pay-out of reward detected:" + rewarddb.getUrn());
        		}
        	}
        	// The personal premium is release to the personal account.
        	AccountingTransaction tr = ledger
        			.createTransaction(rewarddb.getIncentive().getDescription(), rewarddb.getUrn(), when.toInstant(), Instant.now())
        			.head(head)
        			.transfer(personalPremiumBalance, actualAmount, TransactionType.RELEASE, personalBalance)
        			.build();
        	accountingTransactionDao.save(tr);
        	rewarddb.setTransaction(tr);
        	rewarddb.setCancelTime(null);
        	rewarddb.setPaidOut(true);
        	// If the left-over is not equal to the intended amount, well, then make it look intentional! 
        	if (actualAmount != rewarddb.getAmount()) {
        		rewarddb.setAmount(actualAmount);
        	}
    	} else {
    		throw new BalanceInsufficientException("There is no premium to redeem!");
    	}
    }

    /**
     * Reverse the earlier payment of a reward. This method is primarily added for testing purposes, 
     * but might also be helpful in case of service actions.
     * @param reward
     * @param when
     * @throws BalanceInsufficientException
     * @throws NotFoundException
     */
    public void refundReward(Reward reward, OffsetDateTime when) throws BalanceInsufficientException, NotFoundException {
    	if (reward.getTransaction() == null) {
    		return;
    	}
    	Reward rewarddb = rewardDao.find(reward.getId())
    			.orElseThrow(() -> new NotFoundException("No such reward: " + reward.getId()));
    	AccountingTransaction lastTransaction = lookupTransactionWithEntries(reward.getTransaction().getId());
    	if (lastTransaction.hasEntry(TransactionType.PAYMENT)) {
    		// There is a payment made, make a refund
        	// Mark the transaction as a rollback
        	AccountingTransaction theHead = lastTransaction.getHead() != null ? lastTransaction.getHead() : lastTransaction; 
        	AccountingTransaction tr = reverse(theHead, lastTransaction, when.toInstant(), true); 
           	accountingTransactionDao.save(tr);
           	// Save the transaction reference.
        	rewarddb.setTransaction(tr);
        	rewarddb.setPaidOut(false);
			log.info(String.format("Payment for reward %s refunded in transaction %s: ", reward.getUrn(), tr.getTransactionRef()));
    	} else {
        	// else refund has taken place already, ignore, no reason to panic.
			log.info(String.format("No payment to refund for reward %s: ", reward.getUrn()));
    	}
    }

    /** 
     * Checks whether there are at least some premium credits to pay for rewards. Premium balance is locked!
     *  
     * @param amount
     * @return
     */
    public boolean fiatForPremiumBalance(int amount) {
    	OffsetDateTime when = OffsetDateTime.now();
    	Ledger ledger = ledgerDao.findByDate(when.toInstant());
    	// Try to lock the balance, next step is the acrtual transfer
    	Balance maecenasBalance = balanceDao.findByLedgerAndAccountNumber(ledger, ACC_REF_PREMIUMS, LockModeType.PESSIMISTIC_WRITE);
    	return maecenasBalance.getEndAmount() >= amount;
    }

    /**
     * Checks whether there is premium left to redeem. No guarantee, but just a check to avoid the creation of reward when there is 
     * nothing to redeem. No locking of the balance entity.
     * @param recipient the recipient of the reward, i.e., the person being rewarded. 
     * @return if true then there is something to redeem (at least 1 premium credit).
     * @throws NotFoundException 
     */
    public boolean canRedeemSome(NetMobielUser recipient) throws NotFoundException {
    	BankerUser rcp = userDao.findByManagedIdentity(recipient.getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such user: " + recipient.getManagedIdentity()));
    	Ledger ledger = ledgerDao.findByDate(Instant.now());
    	Balance personalPremiumBalance = balanceDao.findByLedgerAndAccount(ledger, rcp.getPremiumAccount(), LockModeType.OPTIMISTIC);
    	return personalPremiumBalance.getEndAmount() > 0;
    }
    
    /**
     * Attempt to pay-out a newly created Reward.
     * @param reward
     */
    @Asynchronous
    public void onNewReward(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Created Reward reward) {
    	onNewOrUpdatedReward(reward);
    }

    /**
     * Attempt to pay-out a restored Reward.
     * @param reward
     */
    @Asynchronous
    public void onUpdatedReward(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Updated Reward reward) {
    	onNewOrUpdatedReward(reward);
    }

    /**
     * Handles the case of a new reward. 
     * NOTE: This call is made without transaction!! Use the session context to invoke the bean and start a new transaction.
     * @param reward
     */
    private void onNewOrUpdatedReward(Reward reward) {
    	try {
    		var when = OffsetDateTime.now();
    		if (reward.getIncentive().isRedemption()) {
    	    	sessionContext.getBusinessObject(LedgerService.class).rewardWithRedemption(reward, when);
    		} else {
    	    	sessionContext.getBusinessObject(LedgerService.class).rewardWithPremium(reward, when);
    		}
    	} catch (BalanceInsufficientException e) {
    		log.warn("Premium balance is insufficient, reward payment is pending: " + reward.getUrn());
    	} catch (Exception e) {
    		log.error("Error in onNewOrUpdatedReward: " + e);
    	}
    }

    /**
     * For testing: Handle the disposal of a reward by reverting the payment of the reward.
     * This method a synchronous.
     * @param reward
     * @throws BusinessException
     */
    public void onRewardDisposal(@Observes(during = TransactionPhase.IN_PROGRESS) @Removed Reward reward) throws BusinessException {
    	if (reward.getTransaction() != null && reward.isPaidOut()) {
    		refundReward(reward, OffsetDateTime.now());
    	}
    }

    /**
     * Lists the pending rewards, sorted from old to new ascending. Redeemable rewards are normally never pending, but once they are assigned,
     * they are nevertheless paid out.
     * This method is called by the system.  
     * @param cursor the cursor 
     * @return A paged list of pending rewards
     * @throws BadRequestException
     */
    private PagedResult<Long> listPendingRewards(Cursor cursor) throws BadRequestException {
    	RewardFilter filter = new RewardFilter();
        // Redemption type rewards should normally not be be pending, but before the reward is issued a balance check is done
    	// A reward is only created if there is at least some credits to earn.

    	// A cancelled reward can never be pending payment
        filter.setCancelled(false); 
        // A paid-out reward is certainly not pending payment
        filter.setPaid(false);
        // The oldest are paid first
        filter.setSortDir(SortDirection.ASC);
        // Only the reward object itself are needed
        filter.validate();
        cursor.validate(MAX_RESULTS, 0);
    	return rewardDao.listRewards(filter, cursor);
    }

    /**
     * Attempt to pay out a reward. Because all our application exceptions trigger a rollback, this call is executed in its own 
     * transaction.  
     * @param reward
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean attemptToPayoutReward(Reward reward) {
    	boolean paid = false;
    	try {
        	Reward rewarddb = rewardDao.find(reward.getId())
        			.orElseThrow(() -> new NotFoundException("No such reward: " + reward.getId()));
			if (fiatForPremiumBalance(rewarddb.getAmount())) {
		    	rewardWithPremium(reward, OffsetDateTime.now());
		    	paid = true;
			}
    	} catch (BalanceInsufficientException e) {
    		log.warn("Premium balance is insufficient, reward(s) payment remains pending");
    	} catch (NotFoundException e) {
    		log.warn(e.toString());
		}
    	return paid;
    }
    /**
     * Check for unpaid rewards and attempt to pay them. 
     * 
     * Note: redeemable rewards are never pending.
     */
    @Schedule(info = "Reward check", hour = "*/1", minute = "15", second = "0", persistent = false /* non-critical job */)
	public void resolvePendingRewardPayments() {
		try {
			long totalCount = listPendingRewards(Cursor.COUNTING_CURSOR).getTotalCount();
	    	Cursor cursor = new Cursor(10, 0);
			PagedResult<Long> pendingRewardIds = listPendingRewards(cursor);
    		for (Long rewardId: pendingRewardIds.getData()) {
    			boolean paid = sessionContext.getBusinessObject(LedgerService.class).attemptToPayoutReward(rewardDao.getReference(rewardId));
    			if (!paid) {
    				break;
    			}
				totalCount--;
			}
    		if (totalCount > 0) {
    			log.info(String.format("Payment of %d reward(s) pending", totalCount));
    		}
    	} catch (Exception ex) {
			log.error("Error processing pending rewards", ex);
    	}
	}

}
