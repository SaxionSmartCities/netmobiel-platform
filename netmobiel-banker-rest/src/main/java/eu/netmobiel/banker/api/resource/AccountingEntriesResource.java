package eu.netmobiel.banker.api.resource;

import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.AccountingEntriesApi;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.model.PagedResult;

@ApplicationScoped
public class AccountingEntriesResource implements AccountingEntriesApi {

	@Inject
	private AccountingEntryMapper mapper;

    @Inject
    private LedgerService ledgerService;

	@Override
	public Response listAccountingEntries(String user, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Response rsp = null;
		PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(user, 
				since != null ? since.toInstant() : null, 
				until != null ? until.toInstant() : null, 
				maxResults, offset); 
		rsp = Response.ok(mapper.map(result)).build();
		return rsp;
	}

}
