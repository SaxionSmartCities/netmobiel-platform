package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import javax.ejb.EJB;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.DepositsApi;
import eu.netmobiel.banker.api.UsersApi;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.api.mapping.DepositRequestMapper;
import eu.netmobiel.banker.api.model.Deposit;
import eu.netmobiel.banker.api.model.PaymentEvent;
import eu.netmobiel.banker.api.model.PaymentLink;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.UserManager;
import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.model.PagedResult;

@ApplicationScoped
public class UsersResource implements UsersApi {

    @EJB(name = "java:app/netmobiel-banker-ejb/UserManager")
    private UserManager userManager;

	@Inject
	private AccountingEntryMapper mapper;

    @Inject
    private LedgerService ledgerService;

    protected User resolveUserReference(String userId) {
		User user = null;
		if ("me".equals(userId)) { 
			user = userManager.findCallingUser();
		} else {
			user = userManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }

    @Override
	public Response createDeposit(String userId, Deposit deposit) {
		Response rsp = null;
		User user = resolveUserReference(userId);
		String paymentUrl = ledgerService.createDepositRequest(user.getPersonalAccount(), deposit.getAmountCredits(), deposit.getDescription(), deposit.getReturnUrl());
		PaymentLink plink = new PaymentLink();
		plink.setPaymentUrl(paymentUrl);
		rsp = Response.ok(plink).build();
		return rsp;
	}

	@Override
	public Response getUser(String userId) {
		User user = resolveUserReference(userId);
		try {
			// TODO Mapping required!
			user = userManager.getUserWithBalance(user.getId());
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException("No such user: " + userId); 
		}
		return Response.ok(user).build();
	}

	@Override
	public Response listStatements(String userId, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
			User user = resolveUserReference(userId);
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(user.getPersonalAccount().getReference(), si, ui, maxResults, offset); 
			rsp = Response.ok(mapper.map(result)).build();
		} catch (ApplicationException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
