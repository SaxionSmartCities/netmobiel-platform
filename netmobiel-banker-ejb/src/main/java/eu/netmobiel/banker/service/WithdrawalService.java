package eu.netmobiel.banker.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.repository.AccountDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.banker.repository.PaymentBatchDao;
import eu.netmobiel.banker.repository.WithdrawalRequestDao;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.PaymentException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;

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
@DeclareRoles({ "admin", "treasurer" })
@PermitAll
public class WithdrawalService {
	public static final Integer MAX_RESULTS = 10; 

    @Inject
    private Logger log;
    @Resource
	protected SessionContext sessionContext;

    @Inject
    private AccountDao accountDao;
    @Inject
    private BankerUserDao userDao;

    @Inject
    private WithdrawalRequestDao withdrawalRequestDao;

    @Inject
    private PaymentBatchDao paymentBatchDao;

    @Inject
    private LedgerService ledgerService;

    /*  ============================================================== */ 
    /*  ========================= WITHDRAWAL ========================= */ 
    /*  ============================================================== */ 

    /**
	 * Lists the withdrawal requests as a paged result set according the filter parameters. Supply null as values when
	 * a parameter is don't care. The security restriction are handled in a higher layer. 
	 * @param accountName the account name, use '%' for any substring and '_' for any character match. Use '\' to 
	 * 					escape the special characters.  
	 * @param since the first date to take into account for creation time.
	 * @param until the last date (exclusive) to take into account for creation time.
	 * @param status the status to filter on. 
	 * @param maxResults The maximum number of results per page. Only if set to 0 the total number of results is returned. 
	 * @param offset the zero-based offset in the result set.
	 * @return A paged result with 0 or more results. The results are ordered by creation time descending and then by id descending.
	 */
    public PagedResult<WithdrawalRequest> listWithdrawalRequests(String accountName, Instant since, Instant until, PaymentStatus status, Integer maxResults, Integer offset) 
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
    	PagedResult<Long> prs = withdrawalRequestDao.list(accountName, since, until, status, 0, offset);
    	List<WithdrawalRequest> results = null;
    	if (maxResults > 0 && prs.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = withdrawalRequestDao.list(accountName, since, until, status, maxResults, offset);
    		results = withdrawalRequestDao.fetchGraphs(ids.getData(), WithdrawalRequest.LIST_GRAPH, WithdrawalRequest::getId);
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

    /**
     * Creates a withdrawal request.
     * @param account account to deposit credits to, not necessarily owned by the caller. 
     * @param amountCredits the number of credits to deposit. 
     * @param description the description to use on the payment page
     * @return the id of the withdrawal object.
     * @throws BalanceInsufficientException 
     * @throws NotFoundException 
     * @throws BadRequestException 
     */
    public Long createWithdrawalRequest(Account acc, int amountCredits, String description) throws BalanceInsufficientException, NotFoundException, BadRequestException {
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	OffsetDateTime now = OffsetDateTime.now();
    	WithdrawalRequest wr = new WithdrawalRequest();
    	wr.setCreationTime(now.toInstant());
		wr.setCreatedBy(me);
		wr.setModificationTime(now.toInstant());
		wr.setModifiedBy(me);
    	wr.setAccount(acc);
    	wr.setAmountCredits(amountCredits);
    	wr.setAmountEurocents(amountCredits * LedgerService.CREDIT_EXCHANGE_RATE);
    	wr.setDescription(description);
    	wr.setStatus(PaymentStatus.REQUESTED);
    	if (wr.getAccount().getIban() == null || wr.getAccount().getIban().isBlank()) {
    		throw new BadRequestException("Account has no IBAN: " + wr.getAccount().getName());
    	}
    	if (wr.getAccount().getIbanHolder() == null || wr.getAccount().getIbanHolder().isBlank()) {
    		throw new BadRequestException("Account has no IBAN Holder: " + wr.getAccount().getName());
    	}
    	wr.setIban(wr.getAccount().getIban());
    	wr.setIbanHolder(wr.getAccount().getIbanHolder());
    	withdrawalRequestDao.save(wr);
    	// Assure PostPersist is called.
    	withdrawalRequestDao.flush();
    	// Save the request first, otherwise we cannot insert the reference in the transaction. 
    	// Regular reserve, you cannot reserve premium credits for withdrawal
    	AccountingTransaction tr = ledgerService.reserve(acc, amountCredits, now, description, wr.getUrn(), false);
    	wr.setTransaction(tr);
    	return wr.getId();
    }

    /**
     * Finds a specific withdrawal request.
     * @param id the database id of the object.
     * @return The object.
     * @throws NotFoundException If the object could not be found.
     */
    public WithdrawalRequest getWithdrawalRequest(Long id) throws NotFoundException {
    	return withdrawalRequestDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such withdrawal request: " + id));
    }

    /**
     * Settles a single withdrawal request. This method start a new transaction.
     * @param withdrawalId the withdrawal request.
     * @throws BadRequestException 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RolesAllowed({ "admin", "treasurer" })
    public void settleWithdrawalRequest(Long withdrawalId) throws NotFoundException, BadRequestException {
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	WithdrawalRequest wrdb = withdrawalRequestDao.find(withdrawalId)
    			.orElseThrow(() -> new IllegalStateException("No such withdrawal request: " + withdrawalId));
		if (wrdb.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Withdrawal request has already reached final state: %d %s", withdrawalId, wrdb.getStatus()));
		}
    	OffsetDateTime now = OffsetDateTime.now();
    	try {
    		// Withdraw the earlier reserved amount of credits
    		AccountingTransaction tr_w = ledgerService.withdraw(wrdb.getTransaction(), now);
    		wrdb.setTransaction(tr_w);
    		wrdb.setStatus(PaymentStatus.COMPLETED);
    		wrdb.setModificationTime(now.toInstant());
    		wrdb.setModifiedBy(me);
    	} catch (BalanceInsufficientException e) {
    		throw new IllegalStateException("Withdraw after release cannot cause insufficient funds", e);
    	}
    }

    /**
     * Cancels a withdrawal that should belong to the given account.
     * @param acc the woning account. If there is a mismatch, the process is cancelled. 
     * @param withdrawalId the if of the withdrawal.
     * @throws NotFoundException
     * @throws BadRequestException
     */
    public void cancelWithdrawalRequest(Account acc, Long withdrawalId)  throws NotFoundException, BadRequestException {
    	WithdrawalRequest wrdb = withdrawalRequestDao.find(withdrawalId)
    			.orElseThrow(() -> new IllegalStateException("No such withdrawal request: " + withdrawalId));
    	if (! wrdb.getAccount().getId().equals(acc.getId())) {
    		throw new EJBAccessException(String.format("Withdrawal request %d does not belong to account %d %s", withdrawalId, acc.getId(), acc.getNcan()));
    	}
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
		cancelWithdrawalRequest(me, wrdb, false);
    }

    /**
     * Cancels a withdrawal that should belong to the given account. Only accessible for admin and treasurer roles.
     * @param withdrawalId the if of the withdrawal.
     * @throws NotFoundException
     * @throws BadRequestException
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void cancelWithdrawalRequest(Long withdrawalId)  throws NotFoundException, BadRequestException {
    	WithdrawalRequest wrdb = withdrawalRequestDao.find(withdrawalId)
    			.orElseThrow(() -> new IllegalStateException("No such withdrawal request: " + withdrawalId));
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
		cancelWithdrawalRequest(me, wrdb, false);
    }

    protected void cancelWithdrawalRequest(BankerUser caller, WithdrawalRequest wr, boolean batchCancel)  throws NotFoundException, BadRequestException {
		if (wr.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Withdrawal request has already reached final state: %d %s", wr.getId(), wr.getStatus()));
		}
		if (!batchCancel && wr.getStatus() == PaymentStatus.ACTIVE) {
			throw new EJBAccessException("Withdrawal request is already being processed: " + wr.getId());
		}
    	OffsetDateTime now = OffsetDateTime.now();
		AccountingTransaction tr_r = ledgerService.cancel(wr.getTransaction(), now);
		wr.setTransaction(tr_r);
		wr.setStatus(PaymentStatus.CANCELLED);
		wr.setModificationTime(now.toInstant());
		wr.setModifiedBy(caller);
    }
    
    /**
     * Cancels a single withdrawal request that is part of a batch. 
     * Once a request is active, it is part of a payment batch. At that point the request cannot be cancelled anymore.
     * @param withdrawalId the withdrawal request to cancel
     * @param batchCancel if true then the cancel is part of the cancellation of the batch where it is part of
     * @throws BadRequestException 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void cancelBatchWithdrawalRequest(BankerUser caller, WithdrawalRequest wr)  throws NotFoundException, BadRequestException {
    	withdrawalRequestDao.refresh(wr);
		cancelWithdrawalRequest(caller, wr, true);
    }
    
    /*  ============================================================= */ 
    /*  ======================= PAYMENT BATCH ======================= */ 
    /*  ============================================================= */ 

    /**
	 * Lists the payment batches as a paged result set according the filter parameters. Supply null as values when
	 * a parameter is don't care.
	 * @param since the first date to take into account for creation time.
	 * @param until the last date (exclusive) to take into account for creation time.
	 * @param status the status to filter on. If omitted then list any. 
	 * @param maxResults The maximum number of results per page. Only if set to 0 the total number of results is returned. 
	 * @param offset the zero-based offset in the result set.
	 * @return A paged result with 0 or more results. The results are ordered by creation time descending and then by id descending.
	 */
    @RolesAllowed({ "admin", "treasurer" })
    public PagedResult<PaymentBatch> listPaymentBatches(Instant since, Instant until, PaymentStatus status, Integer maxResults, Integer offset) 
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
    	PagedResult<Long> prs = paymentBatchDao.list(since, until, status, 0, offset);
    	List<PaymentBatch> results = null;
    	if (maxResults > 0 && prs.getTotalCount() > 0) {
    		// Get the actual data
    		PagedResult<Long> ids = paymentBatchDao.list(since, until, status, maxResults, offset);
    		results = paymentBatchDao.loadGraphs(ids.getData(), PaymentBatch.LIST_GRAPH, PaymentBatch::getId);
    		Map<Long, Integer> counts = paymentBatchDao.fetchCount(ids.getData());
    		results.forEach(pb -> pb.setCount(counts.get(pb.getId())));
    	}
    	return new PagedResult<>(results, maxResults, offset, prs.getTotalCount());
    }

    /**
     * Creates a payment batch.
     * @param requester the user requesting the batch.
     * @return The ID of the new payment batch.
     * @throws BusinessException a NotFoundException is thrown when there are no pending withdrawal requests.
     */
    @RolesAllowed({ "admin", "treasurer" })
    public Long createPaymentBatch() throws BusinessException {
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	// Are there any pending withdrawal requests
    	List<WithdrawalRequest> wdrs = withdrawalRequestDao.findPendingRequests();
    	if (wdrs.isEmpty()) {
    		throw new NotFoundException("No active withdrawal requests");
    	}
    	Account originator = accountDao.findByAccountNumber(LedgerService.ACC_REF_BANKING_RESERVE)
    			.orElseThrow(() -> new IllegalStateException("No originator account found"));
    	PaymentBatch pb = new PaymentBatch();
    	pb.setOriginatorAccount(originator);
    	if (originator.getIban() == null || originator.getIban().isBlank()) {
    		throw new BadRequestException("Originator account has no IBAN: " + originator.getName());
    	}
    	if (originator.getIbanHolder() == null || originator.getIbanHolder().isBlank()) {
    		throw new BadRequestException("Originator account has no IBAN Holder: " + originator.getName());
    	}
    	pb.setOriginatorIban(originator.getIban());
    	pb.setOriginatorIbanHolder(originator.getIbanHolder());
    	pb.setCreatedBy(me);
    	pb.setCreationTime(Instant.now());
    	pb.setModifiedBy(me);
    	pb.setModificationTime(Instant.now());
		pb.setStatus(PaymentStatus.ACTIVE);
    	wdrs.forEach(wr -> pb.addWithdrawalRequest(wr));
    	paymentBatchDao.save(pb);
    	// Assure PostPersist is called.
    	paymentBatchDao.flush();
    	// WithdrawalRequests are automatically updated by JPA
    	return pb.getId();
    }

    /**
     * Finds a specific payment batch.
     * @param id the database id of the object.
     * @return The object.
     * @throws NotFoundException If the object could not be found.
     */
    @RolesAllowed({ "admin", "treasurer" })
    public PaymentBatch getPaymentBatch(Long id) throws NotFoundException {
//		boolean adminView = sessionContext.isCallerInRole("admin");
//		if (!adminView) {
//			throw new EJBAccessException("You are not allowed to view a payment batch");
//		}
    	return paymentBatchDao.fetchGraph(id, PaymentBatch.WITHDRAWALS_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such payment batch: " + id));
    }

    /**
     * Settles a specific payment batch: For each withdrawal request in the batch transfer the credits from reserved to not reserved and then 
     * withdraw the credits. Each withdrawal request is settled in its own database transaction.
     * @param id the database id of the payment batch.
     * @throws NotFoundException If the object could not be found.
     * @throws BadRequestException 
     * @throws PaymentException 
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void settlePaymentBatch(Long paymentBatchId) throws NotFoundException, UpdateException, BadRequestException {
//		boolean adminView = sessionContext.isCallerInRole("admin");
//		if (!adminView) {
//			throw new EJBAccessException("You are not allowed to view a payment batch");
//		}
    	PaymentBatch pb = paymentBatchDao.loadGraph(paymentBatchId, PaymentBatch.WITHDRAWALS_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such payment batch: " + paymentBatchId));
		if (pb.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Payment Batch has already reached final state: %d %s", paymentBatchId, pb.getStatus()));
		}
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	// Process the withdrawal requests one by one. 
    	// A failure of one should not affect others. Use a new transaction for each withdrawal request.
    	// Flag the batch as completed if there are no failures.
    	List<Exception> errors = new ArrayList<>();
    	for (WithdrawalRequest wr : pb.getWithdrawalRequests()) {
    		if (wr.getStatus().isFinal()) {
    			continue;
    		}
    		try {
				sessionContext.getBusinessObject(this.getClass()).settleWithdrawalRequest(wr.getId());
			} catch (Exception e) {
				errors.add(e);
				log.error("Error settling withdrawal request " + wr.getId(), e);
			}
		}
		pb.setModificationTime(Instant.now());
    	pb.setModifiedBy(me);
    	if (errors.isEmpty()) {
    		// The batch has been processed successfully 
    		pb.setStatus(PaymentStatus.COMPLETED);
    	} else {
    		throw new UpdateException(String.format("Failure to settle payment batch (%d errors)", errors.size()), errors.get(0));
    	}
    }

    /**
     * Cancels specific payment batch: Cancel each active withdrawal request. 
     * @param id the database id of the payment batch.
     * @throws NotFoundException If the object could not be found.
     * @throws BadRequestException 
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void cancelPaymentBatch(Long paymentBatchId) throws NotFoundException, UpdateException, BadRequestException {
//		boolean adminView = sessionContext.isCallerInRole("admin");
//		if (!adminView) {
//			throw new EJBAccessException("You are not allowed to view a payment batch");
//		}
    	PaymentBatch pb = paymentBatchDao.loadGraph(paymentBatchId, PaymentBatch.WITHDRAWALS_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such payment batch: " + paymentBatchId));
		if (pb.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Payment Batch has already reached final state: %d %s", paymentBatchId, pb.getStatus()));
		}
    	String caller = sessionContext.getCallerPrincipal().getName();
		BankerUser me = userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    	// Process the withdrawal requests one by one. 
    	// A failure of one should not affect others. Use a new transaction for each withdrawal request.
    	// Flag the batch as completed if there are no failures.
    	List<Exception> errors = new ArrayList<>();
    	for (WithdrawalRequest wr : pb.getWithdrawalRequests()) {
    		if (wr.getStatus().isFinal()) {
    			continue;
    		}
    		try {
				sessionContext.getBusinessObject(this.getClass()).cancelBatchWithdrawalRequest(me, wr);
			} catch (Exception e) {
				errors.add(e);
				log.error("Error cancellingwithdrawal request " + wr.getId(), e);
			}
		}
		pb.setModificationTime(Instant.now());
    	pb.setModifiedBy(me);
    	if (errors.isEmpty()) {
    		// The batch has been processed successfully 
    		pb.setStatus(PaymentStatus.CANCELLED);
    	} else {
    		throw new UpdateException(String.format("Failure to cancel payment batch (%d errors)", errors.size()), errors.get(0));
    	}
    }
}
