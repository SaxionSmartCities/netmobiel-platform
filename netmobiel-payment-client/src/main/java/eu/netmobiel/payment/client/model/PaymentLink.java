package eu.netmobiel.payment;

public class PaymentLink {

    public PaymentLink(String page, String id) {
        paymentPage = page;
        transactionId = id;
    }

    public final String paymentPage;

    public final String transactionId;
}
