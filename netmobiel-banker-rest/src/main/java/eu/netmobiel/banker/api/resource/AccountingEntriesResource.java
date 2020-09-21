package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.AccountingEntriesApi;
import eu.netmobiel.banker.api.mapping.AccountingEntryMapper;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.PagedResult;

@ApplicationScoped
public class AccountingEntriesResource implements AccountingEntriesApi {

	@Inject
    private BankerUserManager userManager;

	@Inject
	private AccountingEntryMapper mapper;

    @Inject
    private LedgerService ledgerService;

	@Override
	public Response listAccountingEntries(String userRef, OffsetDateTime since, OffsetDateTime until, Integer maxResults, Integer offset) {
		Instant si = since != null ? since.toInstant() : null;
		Instant ui = until != null ? until.toInstant() : null;
		Response rsp = null;
		try {
			BankerUser user = null;
			if (userRef != null) {
				user = userManager
						.resolveUrn(userRef)
						.orElseThrow(() -> new NotFoundException("No such user: " + userRef));
			}
			PagedResult<AccountingEntry> result = ledgerService.listAccountingEntries(user.getPersonalAccount().getNcan(), si, ui, maxResults, offset); 
			rsp = Response.ok(mapper.map(result)).build();
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

}
