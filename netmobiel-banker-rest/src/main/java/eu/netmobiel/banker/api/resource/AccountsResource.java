package eu.netmobiel.banker.api.resource;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.AccountsApi;
import eu.netmobiel.banker.api.mapping.AccountMapper;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.UrnHelper;

@ApplicationScoped
public class AccountsResource implements AccountsApi {

	@Inject
    private LedgerService ledgerService;
	
	@Inject
	private AccountMapper accountMapper;

	@Override
	@RolesAllowed({ "admin" })
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
	@RolesAllowed({ "admin" })
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

}
