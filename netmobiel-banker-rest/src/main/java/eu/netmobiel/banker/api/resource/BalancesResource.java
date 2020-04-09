package eu.netmobiel.banker.api.resource;

import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.BalancesApi;
import eu.netmobiel.banker.api.mapping.BalanceMapper;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.model.PagedResult;

@ApplicationScoped
public class BalancesResource implements BalancesApi {

	@Inject
	private BalanceMapper mapper;

    @Inject
    private LedgerService ledgerService;

	@Override
	public Response listBalances(String userRef, OffsetDateTime period, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<Balance> result = ledgerService.listBalances(userRef, null, period, maxResults, offset); 
		rsp = Response.ok(mapper.map(result)).build();
		return rsp;
	}

}
