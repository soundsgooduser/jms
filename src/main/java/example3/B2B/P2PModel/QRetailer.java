package example3.B2B.P2PModel;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.jms.TopicConnectionFactory;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

public class QRetailer implements javax.jms.MessageListener {
	
	private javax.jms.QueueConnection qConnect = null;
	private javax.jms.QueueSession qSession = null;
	private javax.jms.QueueSender qSender = null;
	private javax.jms.TopicConnection tConnect = null;
	private javax.jms.TopicSession tSession = null;
	private javax.jms.Topic hotDealsTopic = null;
	private javax.jms.TopicSubscriber tsubscriber = null;
	private static boolean useJNDI = false;
	private static String uname = null;
	
	private static final String INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";	
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";
	private static final String HOTDEAL_TCF_NAME = "ConnectionFactoryTopicHotDeals";
	private static final String HOTDEAL_TOPIC_NAME = "HotDeals";

	public QRetailer() {
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
			tConnect.setClientID("QRETAILER2");
			qConnect.setClientID("QRETAILER_second");
			tSession = tConnect.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			hotDealsTopic = (Topic) jndi.lookup(HOTDEAL_TOPIC_NAME);
			tsubscriber = tSession.createDurableSubscriber(hotDealsTopic,
					"Hot Deals Subscription");
			tsubscriber.setMessageListener(this);
			tConnect.start();
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (javax.naming.NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}

	public void onMessage(javax.jms.Message aMessage) {
		try {
			autoBuy(aMessage);
		} catch (java.lang.RuntimeException rte) {
			rte.printStackTrace();
		}
	}

	private void autoBuy(javax.jms.Message message) {
		try {
			StreamMessage strmMsg = (StreamMessage) message;
			String dealDesc = strmMsg.readString();
			String itemDesc = strmMsg.readString();
			float oldPrice = strmMsg.readFloat();
			float newPrice = strmMsg.readFloat();
			System.out.println("Received Hot Buy: " + dealDesc);
			// If price reduction is greater than 10 percent, buy
			if (newPrice == 0 || oldPrice / newPrice > 1.1) {
				int count = (int) (java.lang.Math.random() * (double) 1000);
				System.out.println("\nBuying " + count + " " + itemDesc);
				TextMessage textMsg = tSession.createTextMessage();
				textMsg.setText(count + " " + itemDesc);
				textMsg.setIntProperty("QTY", count);
				textMsg.setJMSCorrelationID(uname);
				Queue buyQueue = (Queue) message.getJMSReplyTo();
				qSender = qSession.createSender(buyQueue);
				qSender.send(textMsg, javax.jms.DeliveryMode.PERSISTENT,
						javax.jms.Message.DEFAULT_PRIORITY, 1800000);
			} else {
				System.out.println("\nBad Deal. Not buying");
			}
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	private void exit(String s) {
		try {
			if (s != null && s.equalsIgnoreCase("unsubscribe")) {
				tsubscriber.close();
				tSession.unsubscribe("Hot Deals Subscription");
			}
			tConnect.close();
			qConnect.close();
		} catch (javax.jms.JMSException jmse) {
			jmse.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String argv[]) {
		QRetailer retailer = new QRetailer();

		try {
			System.out.println("\nRetailer application started.\n");
			// Read all standard input and send it as a message
			java.io.BufferedReader stdin = new java.io.BufferedReader(
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