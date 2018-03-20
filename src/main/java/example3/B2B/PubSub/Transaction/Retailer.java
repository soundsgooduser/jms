package example3.B2B.PubSub.Transaction;

import java.io.BufferedReader;
import java.util.*;

import javax.jms.*;
import javax.naming.*;

public class Retailer implements MessageListener {
	private TopicConnection connect = null;
	private TopicSession session = null;
	private TopicPublisher publisher = null;
	private Topic hotDealsTopic = null;

	private static final String INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";
	private static final String HOTDEAL_TCF_NAME = "ConnectionFactoryTopicHotDeals";
	private static final String HOTDEAL_TOPIC_NAME = "HotDeals";

	private boolean inRollback;
	private boolean rollbackOnly;

	public Retailer() {

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
			env.put(Context.PROVIDER_URL, DEFAULT_URL);
			env.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
			env.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);
			InitialContext jndi = new InitialContext(env);

			TopicConnectionFactory factory = (TopicConnectionFactory) jndi
					.lookup(HOTDEAL_TCF_NAME);
			connect = factory.createTopicConnection();
			connect.setClientID("DurableRetailer");
			session = connect.createTopicSession(true,
					Session.SESSION_TRANSACTED);
			hotDealsTopic = (Topic) jndi.lookup(HOTDEAL_TOPIC_NAME);
			TopicSubscriber subscriber = session.createDurableSubscriber(
					hotDealsTopic, "Hot Deals Subscription");
			subscriber.setMessageListener(this);
			connect.start();
		} catch (JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}

	public void onMessage(Message aMessage) {
		try {
			autoBuy(aMessage);
		} catch (RuntimeException rte) {
			rte.printStackTrace();
		}
	}

	private void autoBuy(Message message) {
		int count = 1000;
		try {
			boolean redelivered = message.getJMSRedelivered();
			StreamMessage strmMsg = (StreamMessage) message;
			if (redelivered) {
				System.out.println("\nMessage redelivered, inRollback: "
						+ inRollback + " rollbackOnly: " + rollbackOnly);
				strmMsg.reset();
			}
			if (strmMsg.propertyExists("SEQUENCE_MARKER")) {
				System.out.println("=========================");
				String sequence = strmMsg.getStringProperty("SEQUENCE_MARKER");
				if (sequence.equalsIgnoreCase("END_SEQUENCE")) {
					if (redelivered && inRollback) { // At the end, start fresh
						inRollback = false;
						rollbackOnly = false;
						session.commit();
					} else if (rollbackOnly) {
						inRollback = true;
						session.rollback();
					} else
						session.commit();
				}
				return;
			}

			if (rollbackOnly)
				return; // Ignore all other messages while in rollback mode

			String dealDesc = strmMsg.readString();
			String itemDesc = strmMsg.readString();
			float oldPrice = strmMsg.readFloat();
			float newPrice = strmMsg.readFloat();
			System.out.println("Received Hot Buy :" + dealDesc);

			// If price reduction is greater than 10 percent, buy
			if (newPrice == 0 || oldPrice / newPrice > 1.1) {
				System.out.println("\nBuying d" + count + " " + itemDesc);
				TextMessage textMsg = session.createTextMessage();
				textMsg.setText(count + " " + itemDesc);
				Topic buytopic = (Topic) message.getJMSReplyTo();
				publisher = session.createPublisher(buytopic);
				System.out.println("is sessiontransacted: "+session.getTransacted());
				System.out.println("SESSION_TRANSACTED: "+session.SESSION_TRANSACTED);
				textMsg.setJMSCorrelationID("DurableRetailer");
				publisher.publish(textMsg, DeliveryMode.PERSISTENT,
						Message.DEFAULT_PRIORITY, 1800000);
			} else {
				System.out.println("\nBad Deal- Not buying.");
				rollbackOnly = true;
			}
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	private void exit(String s) {
		try {
			if (s != null && s.equalsIgnoreCase("unsubscribe")) {
				// subscriber.close( );
				session.unsubscribe("Hot Deals Subscription");
			}
			connect.close();
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String argv[]) {

		Retailer retailer = new Retailer();
		try {
			System.out.println("\nRetailer application started.\n");
			// Read all standard input and send it as a message.
			BufferedReader stdin = new BufferedReader(
					new java.io.InputStreamReader(System.in));
			while (true) {
				String s = stdin.readLine();
				if (s == null)
					retailer.exit(null);
				else if (s.equalsIgnoreCase("unsubscribe"))
					retailer.exit(s);
			}
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}
	}
}