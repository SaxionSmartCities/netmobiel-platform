package eu.netmobiel.payment.client.model;

public class PaymentLink {

    public PaymentLink(String page, String id) {
        paymentPage = page;
        transactionId = id;
    }

    /**
     * URL of page where payment can be carried out.
     */
    public final String paymentPage;

    /**
     * Unique transaction id of this payment.
     */
    public final String transactionId;

	@Override
	public String toString() {
		return String.format("PaymentLink [page %s,  tid %s]", paymentPage, transactionId);
	}
}
