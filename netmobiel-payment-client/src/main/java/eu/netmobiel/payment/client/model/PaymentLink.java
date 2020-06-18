package eu.netmobiel.payment.client.model;

public class PaymentLink {

    public PaymentLink(String page, String id) {
        paymentPage = page;
        transactionId = id;
    }

    public final String paymentPage;

    public final String transactionId;
}
