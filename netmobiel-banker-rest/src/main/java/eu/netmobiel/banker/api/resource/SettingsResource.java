package eu.netmobiel.banker.api.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.SettingsApi;
import eu.netmobiel.banker.api.model.Settings;
import eu.netmobiel.banker.service.LedgerService;

@ApplicationScoped
public class SettingsResource implements SettingsApi {

	@Override
	public Response getSettings() {
		Response rsp = null;
		Settings settings = new Settings();
		settings.setExchangeRate(LedgerService.CREDIT_EXCHANGE_RATE);
		rsp = Response.ok(settings).build();
		return rsp;
		
	}

}
