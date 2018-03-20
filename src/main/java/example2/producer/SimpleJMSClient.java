package example2.producer;

import java.util.Hashtable;
import javax.naming.*;
import javax.jms.*;

public class SimpleJMSClient {

	private static InitialContext ctx = null;
	private static QueueConnectionFactory qcf = null;
	private static QueueConnection qc = null;
	private static QueueSession qsess = null;
	private static Queue q = null;
	private static QueueSender qsndr = null;
	private static TextMessage message = null;
	private static final String INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	private static final String DEFAULT_QCF_NAME = "jndi.ConnectionFactoryMisha";
	private static final String DEFAULT_QUEUE_NAME = "jndi.QueueMishaTest";
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";

	public static void sendMessage(String messageText) {
		// create InitialContext
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
		properties.put(Context.PROVIDER_URL, DEFAULT_URL);
		properties.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
		properties.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);

		try {
			ctx = new InitialContext(properties);
		} catch (NamingException ne) {
			ne.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Start producer ");
		System.out.println("Got InitialContext " + ctx.toString());

		// create QueueConnectionFactory
		try {
			qcf = (QueueConnectionFactory) ctx.lookup(DEFAULT_QCF_NAME);
		} catch (NamingException ne) {
			ne.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Got QueueConnectionFactory " + qcf.toString());

		// create QueueConnection
		try {
			qc = qcf.createQueueConnection();
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Got QueueConnection " + qc.toString());

		// create QueueSession
		try {
			qsess = qc.createQueueSession(false, 0);
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Got QueueSession " + qsess.toString());

		// lookup Queue
		try {
			q = (Queue) ctx.lookup(DEFAULT_QUEUE_NAME);
		} catch (NamingException ne) {
			ne.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Got Queue " + q.toString());

		// create QueueSender
		try {
			qsndr = qsess.createSender(q);
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Got QueueSender " + qsndr.toString());

		// create TextMessage
		try {
			message = qsess.createTextMessage();
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Got TextMessage " + message.toString());

		// set message text in TextMessage
		try {
			message.setText(messageText);
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Set text in TextMessage " + message.toString());

		// send message
		try {
			qsndr.setDeliveryMode(DeliveryMode.PERSISTENT);			
			qsndr.send(message);
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}
		System.out.println("Sent message ");

		// clean up
		try {
			message = null;
			qsndr.close();
			qsndr = null;
			q = null;
			qsess.close();
			qsess = null;
			qc.close();
			qc = null;
			qcf = null;
			ctx = null;
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
		}
		System.out.println("End producer ");
	}

	// main program
	public static void main(String[] args) {
		sendMessage("=================MY NEW MESSAGE 1100=======================");
	}

}
