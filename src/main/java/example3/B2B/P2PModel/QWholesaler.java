package example3.B2B.P2PModel;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.jms.TopicConnectionFactory;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.Session;
import javax.jms.TextMessage;

public class QWholesaler implements javax.jms.MessageListener {
	private javax.jms.TopicConnection tConnect = null;
	private javax.jms.TopicSession tSession = null;
	private javax.jms.TopicPublisher tPublisher = null;
	private javax.jms.QueueConnection qConnect = null;
	private javax.jms.QueueSession qSession = null;
	private javax.jms.Queue receiveQueue = null;
	private javax.jms.Topic hotDealsTopic = null;
	
	private static final String INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";
	private static final String HOTDEAL_TCF_NAME = "ConnectionFactoryTopicHotDeals";
	private static final String HOTDEAL_TOPIC_NAME = "HotDeals";
	private static final String ORDER_QUEUE_NAME = "OrderQueue";

	public QWholesaler() {
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
			env.put(Context.PROVIDER_URL, DEFAULT_URL);
			env.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
			env.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);			
			InitialContext jndi = new InitialContext(env);
			
			TopicConnectionFactory tFactory = (TopicConnectionFactory) jndi.lookup(HOTDEAL_TCF_NAME);
			QueueConnectionFactory qFactory = (QueueConnectionFactory) jndi.lookup(HOTDEAL_TCF_NAME);
			
			tConnect = tFactory.createTopicConnection();
			qConnect = qFactory.createQueueConnection();
			tSession = tConnect.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			hotDealsTopic = (Topic) jndi.lookup(HOTDEAL_TOPIC_NAME);
			receiveQueue = (Queue) jndi.lookup(ORDER_QUEUE_NAME);
			tPublisher = tSession.createPublisher(hotDealsTopic);
			QueueReceiver qReceiver = qSession.createReceiver(receiveQueue);
			qReceiver.setMessageListener(this);
			// Now that setup is complete, start the Connection
			qConnect.start();
			tConnect.start();
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (javax.naming.NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}

	private void publishPriceQuotes(String dealDesc, String username,
			String itemDesc, float oldPrice, float newPrice) {
		try {
			javax.jms.StreamMessage message = tSession.createStreamMessage();
			message.writeString(dealDesc);
			message.writeString(itemDesc);
			message.writeFloat(oldPrice);
			message.writeFloat(newPrice);
			message.setStringProperty("Username", username);
			message.setStringProperty("itemDesc", itemDesc);
			message.setJMSReplyTo(receiveQueue);
			tPublisher.publish(message, javax.jms.DeliveryMode.PERSISTENT,
					javax.jms.Message.DEFAULT_PRIORITY, 1800000);
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	public void onMessage(javax.jms.Message message) {
		try {
			TextMessage textMessage = (TextMessage) message;
			String text = textMessage.getText();
			System.out.println("Order received - " + text + " from "
					+ message.getJMSCorrelationID());
		} catch (java.lang.Exception rte) {
			rte.printStackTrace();
		}
	}

	public void exit() {
		try {
			tConnect.close();
			qConnect.close();
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String argv[]) {		

		QWholesaler wholesaler = new QWholesaler();
		try {
			// Read all standard input and send it as a message
			java.io.BufferedReader stdin = new java.io.BufferedReader(
					new java.io.InputStreamReader(System.in));
			System.out.println("Enter: Item, Old Price, New Price");
			System.out.println("\ne.g. Bowling Shoes, 100.00, 55.00");
			while (true) {
				String dealDesc = stdin.readLine();
				if (dealDesc != null && dealDesc.length() > 0) {
					// Parse the deal description
					StringTokenizer tokenizer = new StringTokenizer(dealDesc,
							",");
					String itemDesc = tokenizer.nextToken();
					String temp = tokenizer.nextToken();
					float oldPrice = Float.valueOf(temp.trim()).floatValue();
					temp = tokenizer.nextToken();
					float newPrice = Float.valueOf(temp.trim()).floatValue();
					wholesaler.publishPriceQuotes(dealDesc, "QWHOLESALER2", itemDesc,
							oldPrice, newPrice);
				} else {
					wholesaler.exit();
				}
			}
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}
	}
}