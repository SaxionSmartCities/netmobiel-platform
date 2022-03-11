package eu.netmobiel.banker.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.AccountsApi;
import eu.netmobiel.banker.api.mapping.AccountMapper;
import eu.netmobiel.banker.api.mapping.PageMapper;
import eu.netmobiel.banker.api.model.PaymentLink;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountPurposeType;
import eu.netmobiel.banker.service.DepositService;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.UrnHelper;

@ApplicationScoped
public class AccountsResource implements AccountsApi {

	@Inject
    private LedgerService ledgerService;
	
	@Inject
    private DepositService depositService;
	
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
	public Response createDeposit(String accountId, eu.netmobiel.banker.api.model.DepositRequest deposit) {
		Response rsp = null;
		try {
        	Long accid = UrnHelper.getId(Account.URN_PREFIX, accountId);
        	Account acc = ledgerService.getAccount(accid);
			String paymentUrl = depositService.createDepositRequest(acc, deposit.getAmountCredits(), deposit.getDescription(), deposit.getReturnUrl());
			PaymentLink plink = new PaymentLink();
			plink.setPaymentUrl(paymentUrl);
			rsp = Response.ok(plink).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
