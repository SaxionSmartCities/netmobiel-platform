package eu.netmobiel.banker.api.resource;

import java.util.Optional;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import eu.netmobiel.banker.api.DepositsApi;
import eu.netmobiel.banker.api.mapping.DepositRequestMapper;
import eu.netmobiel.banker.api.model.Deposit;
import eu.netmobiel.banker.api.model.PaymentEvent;
import eu.netmobiel.banker.api.model.PaymentLink;
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.UserManager;

@ApplicationScoped
public class DepositsResource implements DepositsApi {

    @EJB(name = "java:app/netmobiel-banker-ejb/UserManager")
    private UserManager userManager;

	@Inject
	private DepositRequestMapper mapper;

    @Inject
    private LedgerService ledgerService;

	@Override
	public Response createDeposit(Deposit deposit) {
		Response rsp = null;
		User user = userManager.registerCallingUser();
		String paymentUrl = ledgerService.createDepositRequest(user.getPersonalAccount(), deposit.getAmountCredits(), deposit.getDescription(), deposit.getReturnUrl());
		PaymentLink plink = new PaymentLink();
		plink.setPaymentUrl(paymentUrl);
		rsp = Response.ok(plink).build();
		return rsp;
	}

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
