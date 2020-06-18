package eu.netmobiel.payment.client.model;

public class PaymentLinkOptions {

    /**
     * Amount to transfer to merchant's account in euro cents.
     */
    public int euroCents;

    /**
     * Description to show on the payment page.
     */
    public String description;

    /**
     * Payment link will expire after a number of minutes.
     * If negative or zero, the default period is used.
     */
    public int expirationMinutes;

    /**
     * Internal id for merchant to identify transaction.
     */
    public String merchantOrder;

    /**
     * The URL to which the user is redirected when the transaction has been completed.
     */
    public String returnAfterCompletion;

    /**
     * Web hook URL to inform when the transaction changes its status.
     */
    public String informStatusUpdate;

}
