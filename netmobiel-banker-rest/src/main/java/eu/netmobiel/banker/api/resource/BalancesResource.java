package eu.netmobiel.banker.api.resource;

import java.time.OffsetDateTime;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.BalancesApi;
import eu.netmobiel.banker.api.mapping.BalanceMapper;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.UserManager;
import eu.netmobiel.commons.model.PagedResult;

@ApplicationScoped
public class BalancesResource implements BalancesApi {

    @EJB(name = "java:app/netmobiel-banker-ejb/UserManager")
    private UserManager userManager;

	@Inject
	private BalanceMapper mapper;

    @Inject
    private LedgerService ledgerService;

	@Override
	public Response listBalances(String userRef, OffsetDateTime period, Integer maxResults, Integer offset) {
		Response rsp = null;
		User user = null;
		if (userRef != null) {
			user = userManager
					.resolveUrn(userRef)
					.orElseThrow(() -> new NotFoundException("No such user: " + userRef));
		}
		Account acc = user != null ? user.getPersonalAccount() : null;
		PagedResult<Balance> result = ledgerService.listBalances(acc, period, maxResults, offset); 
		rsp = Response.ok(mapper.map(result)).build();
		return rsp;
	}

}
