package eu.netmobiel.communicator.service;

/**
 * Message-Driven Bean implementation class for MessageProcessor.
 */
//@MessageDriven(
//		activationConfig = { 
//				@ActivationConfigProperty(propertyName = "destination", propertyValue = "netmobielMessageProcessor"), 
//				@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
//				@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
//		}, 
//		mappedName = "messageProcessor")
public class MessageProcessor /*implements MessageListener*/ {
//    @Resource(lookup = "java:module/jms/netmobielNotificationTopic")
//    private Topic notificationTopic;
//    @Inject
//    private JMSContext context;
//
//    @Inject
//    private Logger log;
//
//    @Inject
//    private EnvelopeDao envelopeDao;
//    
//	/**
//     * Default constructor. 
//     */
//    public MessageProcessor() {
//    }
//	
//	/**
//     * @see MessageListener#onMessage(Message)
//     */
//    public void onMessage(Message message) {
//    	if (!(message instanceof MapMessage)) {
//    		log.error("Do not understand message of type " + message.getClass().getName());
//    		return;
//    	}
//    	MapMessage mm = (MapMessage) message;
//    	eu.netmobiel.communicator.model.Message msg = new eu.netmobiel.communicator.model.Message();
//    	try {
//			msg.setBody(mm.getString("text"));
//			msg.setContext(mm.getString("context"));
//			msg.setSubject(mm.getString("subject"));
//			msg.setCreationTime(Instant.now());
//			msg.setSender(mm.getString("sender"));
//			String recipientsCsv = mm.getString("recipients");
//			if (msg.getBody() == null || msg.getSender() == null) {
//				log.error(String.format("Message not accepted: %s", msg.toString()));
//			}
//			if (recipientsCsv == null || recipientsCsv.trim().isEmpty()) {
//				log.error(String.format("Message not accepted: %s - no recipient", msg.toString()));
//			}
//			DeliveryMode mode = DeliveryMode.valueOf(mm.getString("DeliveryMode")); 
//			List<Envelope> envelopes = Arrays.stream(recipientsCsv.split(","))
//					.map(rpc -> new Envelope(msg, rpc))
//					.collect(Collectors.toList());
//			if (mode == DeliveryMode.MESSAGE || mode == DeliveryMode.ALL) {
//				envelopeDao.saveAll(envelopes);
//			}
//			if (mode == DeliveryMode.NOTIFICATION || mode == DeliveryMode.ALL) {
//				
//		        context.createProducer().send(notificationTopic, mm);
//			}
//		} catch (Exception e) {
//			log.error("Error processing message " + message.toString());
//		}
//    }
//    
}
