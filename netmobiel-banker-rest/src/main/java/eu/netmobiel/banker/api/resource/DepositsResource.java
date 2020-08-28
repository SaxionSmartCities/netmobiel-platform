package eu.netmobiel.banker.api.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.DepositsApi;
import eu.netmobiel.banker.api.mapping.DepositRequestMapper;
import eu.netmobiel.banker.api.model.PaymentEvent;
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.service.LedgerService;

@ApplicationScoped
public class DepositsResource implements DepositsApi {

	@Inject
	private DepositRequestMapper mapper;

    @Inject
    private LedgerService ledgerService;

	@Override
	public Response updateDepositStatus(PaymentEvent paymentEvent) {
		Response rsp = null;
		Optional<DepositRequest> depositRequest = ledgerService.verifyDeposition(paymentEvent.getOrderId());
		if (!depositRequest.isPresent()) {
			throw new NotFoundException("Order id not found");
		}
		rsp = Response.ok(mapper.map(depositRequest.get())).build();
		return rsp;
	}

}
