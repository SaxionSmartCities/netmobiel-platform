package eu.netmobiel.banker.api.resource;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.AccountsApi;
import eu.netmobiel.banker.api.mapping.AccountMapper;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountPurposeType;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;

@ApplicationScoped
public class AccountsResource implements AccountsApi {

	@Inject
    private LedgerService ledgerService;
	
	@Inject
    private BankerUserManager userManager;

	@Inject
	private AccountMapper accountMapper;

	@Inject
	private PageMapper pageMapper;

	@Override
	public Response listAccounts(String accountName, String purpose, Integer maxResults, Integer offset) {
		Response rsp = null;
		try {
			AccountPurposeType ap = purpose == null ? null : AccountPurposeType.valueOf(purpose);
	    	PagedResult<Account> results = ledgerService.listAccountsWithBalance(accountName, ap, null, maxResults, offset);
			rsp = Response.ok(pageMapper.mapAccountsAll(results)).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

	@Override
	public Response getAccount(String accountId) {
		Response rsp = null;
		try {
        	Long accid = UrnHelper.getId(Account.URN_PREFIX, accountId);
        	Account acc = ledgerService.getAccount(accid);
			rsp = Response.ok(accountMapper.mapAll(acc)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response updateAccount(String accountId, eu.netmobiel.banker.api.model.Account account) {
		Response rsp = null;
		try {
        	Long accid = UrnHelper.getId(Account.URN_PREFIX, accountId);
	    	Account accdom = accountMapper.map(account);
	    	ledgerService.updateAccount(accid, accdom);
    		rsp = Response.noContent().build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

    @Override
	public Response depositToAccount(String accountId, eu.netmobiel.banker.api.model.DepositRequest deposit) {
		Response rsp = null;
		try {
        	Long accid = UrnHelper.getId(Account.URN_PREFIX, accountId);
        	Account acc = ledgerService.getAccount(accid);
        	// What is the context of this action? It is a manual operation by the user, not induced by objects in the system
			BankerUser user = userManager.findOrRegisterCallingUser();
        	final String reference = user.getUrn();
        	AccountingTransaction tr = ledgerService.deposit(acc, deposit.getAmountCredits(), 
        			OffsetDateTime.now(), deposit.getDescription(), reference);
			final String urn = UrnHelper.createUrn(AccountingTransaction.URN_PREFIX, tr.getId());
			rsp = Response.created(URI.create(urn)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

    @Override
    public Response withdrawFromAccount(String accountId, eu.netmobiel.banker.api.model.WithdrawalRequest withdrawalRequest) {
		Response rsp = null;
		try {
        	Long accid = UrnHelper.getId(Account.URN_PREFIX, accountId);
        	Account acc = ledgerService.getAccount(accid);
        	// What is the context of this action? It is a manual operation by the user, not induced by objects in the system
			BankerUser user = userManager.findOrRegisterCallingUser();
        	final String reference = user.getUrn();
        	AccountingTransaction tr = ledgerService.withdraw(acc, withdrawalRequest.getAmountCredits(), 
        			OffsetDateTime.now(), withdrawalRequest.getDescription(), reference);
			final String urn = UrnHelper.createUrn(AccountingTransaction.URN_PREFIX, tr.getId());
			rsp = Response.created(URI.create(urn)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
    }

    @Override
	public Response listAccountStatements(String accountId, OffsetDateTime since, OffsetDateTime until, String purpose, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
        	Long accid = UrnHelper.getId(Account.URN_PREFIX, accountId);
        	Account acc = ledgerService.getAccount(accid);
        	TransactionType trType = purpose == null ? null : TransactionType.valueOf(purpose);
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(acc.getNcan(), si, ui, trType, maxResults, offset); 
			rsp = Response.ok(pageMapper.mapAccountingEntriesShallow(result)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
		return rsp;
	}

}
