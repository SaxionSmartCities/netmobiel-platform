package eu.netmobiel.banker.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    private BankerUser getCaller() throws NotFoundException {
    	String caller = sessionContext.getCallerPrincipal().getName();
		return userDao.findByManagedIdentity(caller)
				.orElseThrow(() -> new NotFoundException("No such user: " + caller));
    }

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
		BankerUser me = getCaller();
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
     * Settles a single withdrawal request. 
     * @param withdrawalId the withdrawal request.
     * @throws BadRequestException 
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void settleWithdrawalRequest(Long withdrawalId) throws NotFoundException, BadRequestException {
		BankerUser me = getCaller();
    	WithdrawalRequest wrdb = withdrawalRequestDao.find(withdrawalId)
    			.orElseThrow(() -> new IllegalStateException("No such withdrawal request: " + withdrawalId));
		if (wrdb.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Withdrawal request has already reached final state: %d %s", withdrawalId, wrdb.getStatus()));
		}
    	try {
    		settleWithdrawalRequest(me, wrdb, false);
    	} catch (BalanceInsufficientException e) {
    		throw new IllegalStateException("Setlement of withdraw should not cause insufficient funds", e);
    	}
    }

    /**
     * Cancels a withdrawal that should belong to the given account.
     * @param acc the owning account. If there is a mismatch, the process is cancelled. 
     * @param withdrawalId the if of the withdrawal.
     * @param reason the (optional) reason for cancelling
     * @throws NotFoundException
     * @throws BadRequestException
     */
    public void cancelWithdrawalRequest(Account acc, Long withdrawalId, String reason)  throws NotFoundException, BadRequestException {
    	WithdrawalRequest wrdb = withdrawalRequestDao.find(withdrawalId)
    			.orElseThrow(() -> new IllegalStateException("No such withdrawal request: " + withdrawalId));
    	if (! wrdb.getAccount().getId().equals(acc.getId())) {
    		throw new EJBAccessException(String.format("Withdrawal request %d does not belong to account %d %s", withdrawalId, acc.getId(), acc.getNcan()));
    	}
		BankerUser me = getCaller();
		cancelWithdrawalRequest(me, wrdb, reason, false);
    }

    /**
     * Cancels a withdrawal request owned by someone. Only accessible for admin and treasurer roles.
     * @param withdrawalId the if of the withdrawal.
     * @throws NotFoundException
     * @throws BadRequestException
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void cancelWithdrawalRequest(Long withdrawalId, String reason)  throws NotFoundException, BadRequestException {
    	WithdrawalRequest wrdb = withdrawalRequestDao.find(withdrawalId)
    			.orElseThrow(() -> new IllegalStateException("No such withdrawal request: " + withdrawalId));
		BankerUser me = getCaller();
		cancelWithdrawalRequest(me, wrdb, reason, false);
    }

    /**
     * Cancels a withdrawal request owned by someone. This call is intended for the admin or treasurer. 
     * Once a withdrawal is ACTIVE it can only be cancelled by this method with batchCancel set to true. The intention
     * is to allow the treasurer to cancel a faulty withdrawal request (for whatever reason).
     * A cancelled or completed withdrawal cannot be cancelled again. 
     * @param caller the caller for logging purposes
     * @param wr the withdrawal request (must be in persistence context already)
     * @param reason the (optional) reason for cancelling
     * @param batchCancel if set to true, then an active withdrawal is allowed to be cancelled.
     * @throws NotFoundException
     * @throws BadRequestException
     */
    private void cancelWithdrawalRequest(BankerUser caller, WithdrawalRequest wr, String reason, boolean batchCancel)  throws NotFoundException, BadRequestException {
		if (wr.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Withdrawal request has already reached final state: %d %s", wr.getId(), wr.getStatus()));
		}
		if (!batchCancel && wr.getStatus() == PaymentStatus.ACTIVE) {
			throw new EJBAccessException("Withdrawal request is already being processed: " + wr.getId());
		}
    	OffsetDateTime now = OffsetDateTime.now();
		AccountingTransaction tr_r = ledgerService.cancel(wr.getTransaction(), now);
		wr.setTransaction(tr_r);
		wr.setReason(reason);
		wr.setStatus(PaymentStatus.CANCELLED);
		wr.setModificationTime(now.toInstant());
		wr.setModifiedBy(caller);
    }
    
    /**
     * Cancels a withdrawal request owned by someone. This call is intended for the admin or treasurer. 
     * Once a withdrawal is ACTIVE it can only be cancelled by this method with batchCancel set to true. The intention
     * is to allow the treasurer to cancel a faulty withdrawal request (for whatever reason).
     * A cancelled or completed withdrawal cannot be cancelled again. 
     * @param caller the caller for logging purposes
     * @param wr the withdrawal request (must be in persistence context already)
     * @param reason the (optional) reason for cancelling
     * @param batchCancel if set to true, then an active withdrawal is allowed to be cancelled.
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws BalanceInsufficientException 
     */
    private void settleWithdrawalRequest(BankerUser caller, WithdrawalRequest wr, boolean batchCancel)  throws NotFoundException, BadRequestException, BalanceInsufficientException {
		if (wr.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Withdrawal request has already reached final state: %d %s", wr.getId(), wr.getStatus()));
		}
		if (!batchCancel && wr.getStatus() == PaymentStatus.ACTIVE) {
			throw new EJBAccessException("Withdrawal request is already being processed: " + wr.getId());
		}
    	OffsetDateTime now = OffsetDateTime.now();
		AccountingTransaction tr_r = ledgerService.withdraw(wr.getTransaction(), now);
		wr.setTransaction(tr_r);
		wr.setReason(null);
		wr.setStatus(PaymentStatus.COMPLETED);
		wr.setModificationTime(now.toInstant());
		wr.setModifiedBy(caller);
    }
    
    /**
     * Cancels a single withdrawal request that is part of a batch. Internal use only!
     * Once a request is active, it is part of a payment batch. At that point the request cannot be cancelled anymore by the owner. 
     * Only the treasurer executing the batch can cancel (or settle) the withdrawal contained in a batch.
     * @param caller the caller for logging purposes
     * @param wr the withdrawal request (must be in persistence context already)
     * @param reason the (optional) reason for cancelling
     * @throws BadRequestException 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void cancelBatchWithdrawalRequest(BankerUser caller, WithdrawalRequest wr, String reason)  throws NotFoundException, BadRequestException {
    	WithdrawalRequest wrdb = getWithdrawalRequest(wr.getId());
		cancelWithdrawalRequest(caller, wrdb, reason, true);
    }
    
    /**
     * Settles a single withdrawal request that is part of a batch. Internal use only! 
     * Once a request is active, it is part of a payment batch. At that point the request cannot be cancelled anymore by the owner. 
     * Only the treasurer executing the batch can cancel (or settle) the withdrawal contained in a batch.
     * @param caller the caller for logging purposes
     * @param wr the withdrawal request (must be in persistence context already)
     * @throws BadRequestException 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void settleBatchWithdrawalRequest(BankerUser caller, WithdrawalRequest wr)  throws BusinessException {
    	WithdrawalRequest wrdb = getWithdrawalRequest(wr.getId());
		settleWithdrawalRequest(caller, wrdb, true);
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
		BankerUser me = getCaller();
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
		pb.setNrRequests(wdrs.size());
		int sumRequested = wdrs.stream()
				.map(w -> w.getAmountEurocents())
				.reduce(0, Integer::sum);
		pb.setAmountRequestedEurocents(sumRequested);
		pb.setAmountSettledEurocents(0);
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
    	return paymentBatchDao.fetchGraph(id, PaymentBatch.WITHDRAWALS_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such payment batch: " + id));
    }

    private PaymentBatch getPaymentBatchNonFinal(Long paymentBatchId) throws NotFoundException, BadRequestException {
    	PaymentBatch pb = getPaymentBatch(paymentBatchId);
		if (pb.getStatus().isFinal()) {
			throw new BadRequestException(String.format("Payment Batch has already reached final state: %d %s", paymentBatchId, pb.getStatus()));
		}
		return pb;
    }
    
    private static void updatePaymentBatchAttributes(BankerUser caller, PaymentBatch pb) throws NotFoundException, BadRequestException {
		pb.setModificationTime(Instant.now());
    	pb.setModifiedBy(caller);
		int sumSettled = pb.getWithdrawalRequests().stream()
				.filter(w -> w.getStatus() == PaymentStatus.COMPLETED)
				.map(w -> w.getAmountEurocents())
				.reduce(0, Integer::sum);
		pb.setAmountSettledEurocents(sumSettled);
    	// If all are final, the batch is final.
    	if (pb.getWithdrawalRequests().stream().noneMatch(w -> !w.getStatus().isFinal())) {
    		// The batch has been processed successfully 
    		pb.setStatus(PaymentStatus.COMPLETED);
    	}
    }
    /**
     * Updates a specific payment batch: For each withdrawal request either settle or cancel it. 
     * Each withdrawal request gets its own database transaction.
     * @param id the database id of the payment batch.
     * @throws NotFoundException If the object could not be found.
     * @throws BadRequestException 
     * @throws PaymentException 
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void updatePaymentBatch(Long paymentBatchId, List<WithdrawalRequest> withdrawals) throws NotFoundException, UpdateException, BadRequestException {
    	PaymentBatch pb = getPaymentBatchNonFinal(paymentBatchId);
		BankerUser me = getCaller();
    	// Process the withdrawal requests one by one. 
    	// A failure of one should not affect others. Use a new transaction for each withdrawal request.
    	for (WithdrawalRequest wr : pb.getWithdrawalRequests()) {
    		if (wr.getStatus().isFinal()) {
    			continue;
    		}
    		Optional<WithdrawalRequest> optwrupdate = withdrawals.stream().filter(w -> w.getId().equals(wr.getId())).findFirst();
    		if (optwrupdate.isEmpty()) {
    			// Not in the list, ignore
    			continue;
    		}
    		WithdrawalRequest wru = optwrupdate.get();
    		try {
    			if (wru.getStatus() == PaymentStatus.CANCELLED) {
    				sessionContext.getBusinessObject(this.getClass()).cancelBatchWithdrawalRequest(me, wr, wru.getReason());
    			} else if (wru.getStatus() == PaymentStatus.COMPLETED) {
    				sessionContext.getBusinessObject(this.getClass()).settleBatchWithdrawalRequest(me, wr);
    			}
			} catch (Exception e) {
				log.error("Error in payment batch during update of withdrawal request " + wr.getId());
			}
		}
    	// Refresh
    	// Could use the following, but then all refresh cascading must be set properly
    	// paymentBatchDao.refresh(pb);
    	// Just to be sure, refresh the straight away manner
    	paymentBatchDao.clear();
    	pb = getPaymentBatch(pb.getId());
    	updatePaymentBatchAttributes(me, pb);
    }

    /**
     * Cancels specific payment batch: Cancel each active withdrawal request. 
     * @param id the database id of the payment batch.
     * @param reason the (optional) reason for cancelling
     * @throws NotFoundException If the object could not be found.
     * @throws BadRequestException 
     */
    @RolesAllowed({ "admin", "treasurer" })
    public void cancelPaymentBatch(Long paymentBatchId, String reason) throws NotFoundException, UpdateException, BadRequestException {
    	PaymentBatch pb = getPaymentBatchNonFinal(paymentBatchId);
		BankerUser me = getCaller();
    	// Process the withdrawal requests one by one. 
    	// A failure of one should not affect others. Use a new transaction for each withdrawal request.
    	for (WithdrawalRequest wr : pb.getWithdrawalRequests()) {
    		if (wr.getStatus().isFinal()) {
    			continue;
    		}
    		try {
				sessionContext.getBusinessObject(this.getClass()).cancelBatchWithdrawalRequest(me, wr, reason);
			} catch (Exception e) {
				log.error("Error in payment batch during cancel of withdrawal request " + wr.getId());
			}
		}
    	// Refresh, erase 
    	paymentBatchDao.clear();
    	updatePaymentBatchAttributes(me, pb);
		pb.setStatus(PaymentStatus.CANCELLED);
    }
}
