package example2.consumer.synchronous;

import java.util.Hashtable;
import javax.naming.*;
import javax.jms.*;

public class SynchronousJMSConsumer {
	private static InitialContext ctx = null;
	private static QueueConnectionFactory qcf = null;
	private static QueueConnection qc = null;
	private static QueueSession qsess = null;
	private static Queue q = null;
	private static QueueReceiver qReceiver = null;
	private static TextMessage message = null;
	private static final String INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	private static final String DEFAULT_QCF_NAME = "jndi.ConnectionFactoryMisha";
	private static final String DEFAULT_QUEUE_NAME = "jndi.QueueMishaTest";
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";

	public static void retreiveMessage() {
		// create InitialContext
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
		properties.put(Context.PROVIDER_URL, DEFAULT_URL);
		properties.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
		properties.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);

		try {

			System.out.println("Start consumer");
			ctx = new InitialContext(properties);
			qcf = (QueueConnectionFactory) ctx.lookup(DEFAULT_QCF_NAME);
			qc = qcf.createQueueConnection();
			qsess = qc.createQueueSession(false, 0);
			q = (Queue) ctx.lookup(DEFAULT_QUEUE_NAME);
			qReceiver = qsess.createReceiver(q);
			qc.start();
			message = (TextMessage) qReceiver.receive();
			System.out.println("Received: " + message.getText());
			System.out.println("End consumer");

			// Clean
			message = null;
			qReceiver.close();
			qReceiver = null;
			q = null;
			qsess.close();
			qsess = null;
			qc.close();
			qc = null;
			qcf = null;
			ctx = null;

		} catch (NamingException ne) {
			ne.printStackTrace(System.err);
			System.exit(0);
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}

	}

	public static void main(String[] args) {
		retreiveMessage();
	}

}
