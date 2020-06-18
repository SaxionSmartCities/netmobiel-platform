package eu.netmobiel.payment.client;

import static org.junit.Assert.*;

import eu.netmobiel.payment.client.model.PaymentLink;
import eu.netmobiel.payment.client.model.PaymentLinkOptions;
import org.junit.Test;

public class PaymentClientTest {

    @Test
    public void testPaymentLink() {
        PaymentClient provider = new PaymentClient();
        PaymentLinkOptions options = new PaymentLinkOptions();
        options.euroCents = 99;
        options.description = "Test payment provider";
        options.expirationMinutes = 15;
        options.merchantOrder = "Unieke NetMobiel transactie id";
        options.returnAfterCompletion = "https://emspay-test.glitch.com/ideal/return";
        PaymentLink link = provider.getPaymentLink(options);
        assertNotNull(link.paymentPage);
        assertNotNull(link.transactionId);
    }
}
