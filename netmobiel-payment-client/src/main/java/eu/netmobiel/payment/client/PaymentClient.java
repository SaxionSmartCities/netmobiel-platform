package eu.netmobiel.payment;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.Test;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PaymentProvider {

    //TODO: It's unclear whether Client is thread-safe!
    private final Client client = new ResteasyClientBuilder()
            .connectionPoolSize(200)
            .connectionCheckoutTimeout(5, TimeUnit.SECONDS)
            .maxPooledPerRoute(20)
            .property("resteasy.preferJacksonOverJsonB", true)
            .build();

    //TODO: Resource injection
    private final URI paymentLinkTarget = URI.create("https://api.online.emspay.eu/v1/paymentlinks");

    //id part is replaced by actual transaction id
    private final URI paymentStatusTarget = URI.create("https://api.online.emspay.eu/v1/paymentlink/id");

    //id part is replaced by actual transaction id
    private final URI orderStatusTarget = URI.create("https://api.online.emspay.eu/v1/orders/id");

    private final String API_KEY = "cd422b152bd94c468da25810debe7fe4";

    private void addAPIKey(Invocation.Builder builder) {
        // authorization header is API key, appended with a colon, in Base64
        byte[] key = (API_KEY + ":").getBytes();
        String authorization = "Basic " + Base64.getEncoder().encodeToString(key);
        builder.header("Authorization", authorization);
    }

    @PreDestroy
    void cleanup() {
        client.close();
    }

    public PaymentLink getPaymentLink(PaymentLinkOptions options) {
        // prepare POST request
        Invocation.Builder builder = client.target(paymentLinkTarget).request();
        addAPIKey(builder);
        // copy options to appropriate JSON request body
        PaymentLinkRequestBody body = new PaymentLinkRequestBody();
        // TODO: validate link options?
        body.merchant_order_id = options.merchantOrder;
        body.amount = options.euroCents;
        body.description = options.description;
        // expiration in ISO 8601 duration
        body.expiration_period = "PT" + options.expirationMinutes + "M";
        body.return_url = options.returnAfterCompletion;
        body.webhook_url = options.informStatusUpdate;
        // send synchronous request to obtain payment link
        PaymentLinkResponseBody response = builder.post(Entity.json(body), PaymentLinkResponseBody.class);
        System.out.println("Payment URL: " + response.payment_url);
        return new PaymentLink(response.payment_url, response.id);
    }

    public PaymentStatus getPaymentStatus(String id) {
        // prepare GET request
        Invocation.Builder builder = client.target(paymentStatusTarget.resolve(id)).request();
        addAPIKey(builder);
        // send synchronous request to obtain payment status
        PaymentStatusResponseBody response = builder.get(PaymentStatusResponseBody.class);
        return new PaymentStatus(
                response.status,
                response.created,
                response.modified,
                response.completed
        );
    }

    public OrderStatus getOrderStatus(String id) {
        return null;
    }

    /**
     * @link https://dev.online.emspay.eu/rest-api/features/payment-link
     * This class must be public accessible! Otherwise JSON conversion will fail with reflection error.
     */
    static public class PaymentLinkRequestBody {
        // mandatory
        public String merchant_order_id;
        public int amount;
        public String currency = "EUR";
        // optional
        public String[] payment_methods = new String[]{"ideal"};
        public String description;
        public String expiration_period;
        public String return_url;
        public String webhook_url;
    }

    /**
     * @link https://dev.online.emspay.eu/rest-api/features/payment-link
     * This class must be public accessible! Otherwise JSON conversion will fail with reflection error.
     */
    static public class PaymentLinkResponseBody {
        public String payment_url;
        public String id;
    }

    /**
     * @link https://dev.online.emspay.eu/rest-api/features/request-order-status
     * This class must be public accessible! Otherwise JSON conversion will fail with reflection error.
     */
    static public class PaymentStatusResponseBody {
        public String status;
        public String created;
        public String modified;
        public String completed;
    }

    @Test
    public void testPaymentLink() {
        PaymentProvider provider = new PaymentProvider();
        PaymentLinkOptions options = new PaymentLinkOptions();
        options.euroCents = 99;
        options.description = "Test payment provider";
        options.expirationMinutes = 15;
        options.merchantOrder = "Unieke NetMobiel transactie id";
        options.returnAfterCompletion = "https://emspay-test.glitch.com/return";
        PaymentLink link = provider.getPaymentLink(options);
        System.out.println(link);
    }
}

