package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.UsersApi;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.api.mapping.UserMapper;
import eu.netmobiel.banker.api.model.PaymentLink;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;

@RequestScoped
public class UsersResource implements UsersApi {

    @EJB(name = "java:app/netmobiel-banker-ejb/UserManager")
    private BankerUserManager userManager;

	@Inject
	private AccountingEntryMapper accountingEntryMapper;

	@Inject
	private UserMapper userMapper;

	@Inject
    private LedgerService ledgerService;
	
	@Context
	private HttpServletRequest request;
	
    protected BankerUser resolveUserReference(String userId, boolean createIfNeeded) {
		BankerUser user = null;
		if ("me".equals(userId)) {
			user = createIfNeeded ? userManager.registerCallingUser() : userManager.findCallingUser();
		} else {
			user = userManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }

    @Override
	public Response createDeposit(String userId, eu.netmobiel.banker.api.model.DepositRequest deposit) {
		Response rsp = null;
		BankerUser user = resolveUserReference(userId, true);
		String paymentUrl = ledgerService.createDepositRequest(user.getPersonalAccount(), deposit.getAmountCredits(), deposit.getDescription(), deposit.getReturnUrl());
		PaymentLink plink = new PaymentLink();
		plink.setPaymentUrl(paymentUrl);
		rsp = Response.ok(plink).build();
		return rsp;
	}

	@Override
	public Response getUser(String userId) {
		BankerUser user = resolveUserReference(userId, true);
		try {
			user = userManager.getUserWithBalance(user.getId());
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException("No such user: " + userId); 
		}
		return Response.ok(userMapper.map(user)).build();
	}

	@Override
	public Response listStatements(String userId, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
			BankerUser user = resolveUserReference(userId, true);
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(user.getPersonalAccount().getNcan(), si, ui, maxResults, offset); 
			rsp = Response.ok(accountingEntryMapper.map(result)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
