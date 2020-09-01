package eu.netmobiel.payment.client;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import eu.netmobiel.payment.client.model.PaymentError;
import eu.netmobiel.payment.client.model.PaymentLink;
import eu.netmobiel.payment.client.model.PaymentOrder;
/**
 * Client for the payment provider EMS Pay (by ABNAmro).
 * 
 * The clienty uses Jackson2 for serialization and deserialization of JSON. The Yasson JSON-B has also been tried, 
 * but in the tested vesion (WildFly 17/RestEasy 3.7.0) the bean lookup of the adapter necessary for enum conversion
 * seems to get lost after one test run. After server restart it works again only for one ro two attempts. So, 
 * an intermittent failure, very nasty. So, we use Jackson.
 * 
 * Note: For testing it is necesary to exclude the org.jboss.resteasy.resteasy-json-binding-provider from the 
 * deployment using the jboss-deployment-structure. Otherwise the jackson2 library will be ignored.
 *   
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class PaymentClient {

    private ResteasyClient webClient;

    private static final String PAYMENT_LINKS_RESOURCE = "https://api.online.emspay.eu/v1/paymentlinks";
    private static final String PAYMENT_ORDERS_RESOURCE = "https://api.online.emspay.eu/v1/orders";

    @Resource(lookup = "java:global/paymentClient/emspay/apiKey")
    private String apiKey;

    private String authorizationValue;

    @PostConstruct
    public void createClient() {
        webClient = new ResteasyClientBuilder()
                .connectionPoolSize(200)
                .connectionCheckoutTimeout(5, TimeUnit.SECONDS)
                .maxPooledPerRoute(20)
				.register(new Jackson2ObjectMapperContextResolver())
				.property("resteasy.preferJacksonOverJsonB", true)
                .build();
        authorizationValue = "Basic " + Base64.getEncoder().encodeToString((apiKey + ":").getBytes());
    }

    @PreDestroy
    void cleanup() {
        webClient.close();
    }

    /**
     * Creates a new payment link. The object returned comprises the input properties, the link is and the payment page url.
     * The other fields are not yet initialised by the payment provider.
     * @param input the payment link object: amount, description , merchant order id, return url.
     * @return a payment link object
     */
    public PaymentLink createPaymentLink(PaymentLink input) {
    	PaymentLink link = null;
        try (Response response = webClient.target(PAYMENT_LINKS_RESOURCE).request()
                .header(HttpHeaders.AUTHORIZATION, authorizationValue)
                .post(Entity.entity(input, MediaType.APPLICATION_JSON))) {
	        if (response.getStatusInfo() != Response.Status.CREATED) {
	        	PaymentError error = response.readEntity(PaymentError.class);
	        	throw new WebApplicationException("Cannot create payment link: " + error.toString(), response.getStatus());
	        }
	        // In testing the object returned has only the id and the payment url added. No timestamps and status yet.
	        // Perhaps the API relies on the webhook status updates?
	        // Fetch the rest of the parameters with a GET.
	        link = response.readEntity(PaymentLink.class);
        }
        return link;
    }

    /**
     * Retrieves a payment link object by its id.
     * @param id The payment link id 
     * @return The payment link object.
     * @throws NotFoundException if the object is not found.
     */
    public PaymentLink getPaymentLink(String id) {
    	PaymentLink link = null;
        WebTarget target = webClient.target(PAYMENT_LINKS_RESOURCE)
        		.path("{id}")
        		.resolveTemplate("id", id);
        try (Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authorizationValue)
                .get()) {
        	link = response.readEntity(PaymentLink.class);
        }
        return link;
    }

    /**
     * Retrieves an order object by its id.
     * @param id The order id 
     * @return The order object. Only a few fields are deserialized: Only merchant order id and related payment link id.
     * @throws NotFoundException if the object is not found.
     */
    public PaymentOrder getPaymentOrder(String orderId) {
    	PaymentOrder order= null;
        WebTarget target = webClient.target(PAYMENT_ORDERS_RESOURCE)
        		.path("{id}")
        		.resolveTemplate("id", orderId);
        try (Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authorizationValue)
                .get()) {
        	order = response.readEntity(PaymentOrder.class); 
        }
        return order;
    }
    
    /**
     * Retrieves a payment link object by an underlying order id. An order id is provided when returning from the payment page.
     * @param id The order id 
     * @return The payment link object.
     * @throws NotFoundException if the object is not found.
     */
    public PaymentLink getPaymentLinkByOrderId(String orderId) {
    	PaymentOrder order = getPaymentOrder(orderId); 
        return getPaymentLink(order.relatedPaymentLinkId);
    }

}

