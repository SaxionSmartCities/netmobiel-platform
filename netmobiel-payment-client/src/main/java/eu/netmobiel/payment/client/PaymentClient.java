package eu.netmobiel.payment.client;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import eu.netmobiel.payment.client.model.OrderStatus;
import eu.netmobiel.payment.client.model.PaymentLink;
import eu.netmobiel.payment.client.model.PaymentLinkOptions;
import eu.netmobiel.payment.client.model.PaymentStatus;

@ApplicationScoped
public class PaymentClient {

    //TODO: It's unclear whether Client is thread-safe!
    private ResteasyClient webClient;

    //TODO: Resource injection, should be a string probably
//    @Resource(lookup = "java:global/paymentClient/abn/paymentLinkTarget")
    private final URI paymentLinkTarget = URI.create("https://api.online.emspay.eu/v1/paymentlinks");

    //id part is replaced by actual transaction id
//    @Resource(lookup = "java:global/paymentClient/abn/paymentStatusTarget")
    private final URI paymentStatusTarget = URI.create("https://api.online.emspay.eu/v1/paymentlink/id");

    // API key for test account (this should not be in GIT repo!)
//  @Resource(lookup = "java:global/paymentClient/abn/apiKey")
    private final String apiKey = "cd422b152bd94c468da25810debe7fe4";

    private String authorizationValue;

    @PostConstruct
    public void createClient() {
        webClient = new ResteasyClientBuilder()
                .connectionPoolSize(200)
                .connectionCheckoutTimeout(5, TimeUnit.SECONDS)
                .maxPooledPerRoute(20)
                .build();
        authorizationValue = "Basic " + Base64.getEncoder().encodeToString((apiKey + ":").getBytes());
    }

    @PreDestroy
    void cleanup() {
        webClient.close();
    }

    public PaymentLink getPaymentLink(PaymentLinkOptions options) {
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
        WebTarget target = webClient.target(paymentLinkTarget);
        PaymentLinkResponseBody response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authorizationValue)
                .post(Entity.json(body), PaymentLinkResponseBody.class);
        return new PaymentLink(response.payment_url, response.id);
    }

    public PaymentStatus getPaymentStatus(String id) {
        // prepare GET request
        WebTarget target = webClient.target(paymentStatusTarget.resolve(id));
        // send synchronous request to obtain payment status
        PaymentStatusResponseBody response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authorizationValue)
                .get(PaymentStatusResponseBody.class);
        // convert status string to enumerated value
        PaymentStatus.Current current = PaymentStatus.Current.valueOf(response.status.toUpperCase());
        // parse timestamps (with timezone info)
        OffsetDateTime
                createdTimestamp = response.created == null ? null : OffsetDateTime.parse(response.created),
                modifiedTimestamp = response.modified == null ? null : OffsetDateTime.parse(response.modified),
                completedTimestamp = response.completed == null ? null : OffsetDateTime.parse(response.completed);
        return new PaymentStatus(response.status, createdTimestamp, modifiedTimestamp, completedTimestamp);
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

}

