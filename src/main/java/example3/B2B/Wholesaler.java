package example3.B2B;

import org.apache.activemq.ActiveMQConnection;

import java.io.*;
import java.util.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

public class Wholesaler implements javax.jms.MessageListener {
	private TopicConnection connect = null;
	private TopicSession pubSession = null;
	private TopicSession subSession = null;
	private TopicPublisher publisher = null;
	private TopicSubscriber subscriber = null;
	private Topic hotDealsTopic = null;
	private TemporaryTopic buyOrdersTopic = null;

	private static final String INITIAL_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
	private static final String DEFAULT_URL = ActiveMQConnection.DEFAULT_BROKER_URL;
	private static final String DEFAULT_USER = "admin";
	private static final String DEFAULT_PASSWORD = "admin";
	private static final String HOTDEAL_TCF_NAME = "TopicConnectionFactory";
	private static final String HOTDEAL_TOPIC_NAME = "dynamicTopics/HotDeals";

	public Wholesaler() {
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
			env.put(Context.PROVIDER_URL, DEFAULT_URL);
			env.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
			env.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);
			InitialContext jndi = new InitialContext(env);

			TopicConnectionFactory factory = (TopicConnectionFactory) jndi.lookup(HOTDEAL_TCF_NAME);
			connect = factory.createTopicConnection();

			pubSession = connect.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			subSession = connect.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			hotDealsTopic = (Topic) jndi.lookup(HOTDEAL_TOPIC_NAME);
			publisher = pubSession.createPublisher(hotDealsTopic);
			buyOrdersTopic = subSession.createTemporaryTopic();
			subscriber = subSession.createSubscriber(buyOrdersTopic);
			subscriber.setMessageListener(this);
			connect.start();
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (javax.naming.NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}

	private void publishPriceQuotes(String dealDesc, String username, String itemDesc, float oldPrice, float newPrice) {
		try {
			StreamMessage message = pubSession.createStreamMessage();
			message.writeString(dealDesc);
			message.writeString(itemDesc);
			message.writeFloat(oldPrice);
			message.writeFloat(newPrice);
			message.setStringProperty("Username", username);
			message.setStringProperty("Itemdesc", itemDesc);
			message.setJMSReplyTo(buyOrdersTopic);
			publisher.publish(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, 1800000);
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	public void onMessage(Message message) {
		try {
			TextMessage textMessage = (TextMessage) message;
			String text = textMessage.getText();
			System.out.println("\nOrder received - " + text + " from " + message.getJMSCorrelationID());
		} catch (java.lang.Exception rte) {
			rte.printStackTrace();
		}
	}

	public void exit() {
		try {
			connect.close();
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String argv[]) {
		Wholesaler wholesaler = new Wholesaler();
		try {
			// Read all standard input and send it as a message.
			BufferedReader stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
			System.out.println("Enter: Item, Old Price, New Price");
			System.out.println("\ne.g., Bowling Shoes, 100.00, 55.00");

			while (true) {
				String dealDesc = stdin.readLine();
				if (dealDesc != null && dealDesc.length() > 0) {
					// Parse the deal description
					StringTokenizer tokenizer = new StringTokenizer(dealDesc, ",");
					String itemDesc = tokenizer.nextToken();
					String temp = tokenizer.nextToken();
					float oldPrice = Float.valueOf(temp.trim()).floatValue();
					temp = tokenizer.nextToken();
					float newPrice = Float.valueOf(temp.trim()).floatValue();
					wholesaler.publishPriceQuotes(dealDesc, "WHOLESALER", itemDesc, oldPrice, newPrice);
				} else {
					wholesaler.exit();
				}
			}
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
