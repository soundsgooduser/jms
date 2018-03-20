package example2.consumer.asynchronous;

import java.util.Hashtable;
import javax.naming.*;
import javax.jms.*;

public class AsynchronousJMSConsumer implements MessageListener {
	private static InitialContext ctx = null;
	private static QueueConnectionFactory qcf = null;
	private static QueueConnection qc = null;
	private static QueueSession qsess = null;
	private static Queue q = null;
	private static QueueReceiver qReceiver = null;
	private static final String INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	private static final String DEFAULT_QCF_NAME = "jndi.ConnectionFactoryMisha";
	private static final String DEFAULT_QUEUE_NAME = "jndi.QueueMishaTest";
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";

	public void retreiveMessage() {
		// create InitialContext
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
		properties.put(Context.PROVIDER_URL, DEFAULT_URL);
		properties.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
		properties.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);

		try {

			System.out.println("Start asynchronous consumer");
			ctx = new InitialContext(properties);
			qcf = (QueueConnectionFactory) ctx.lookup(DEFAULT_QCF_NAME);
			qc = qcf.createQueueConnection();
			qsess = qc.createQueueSession(false, 0);
			q = (Queue) ctx.lookup(DEFAULT_QUEUE_NAME);
			qReceiver = qsess.createReceiver(q);
			qReceiver.setMessageListener(this);
			qc.start();

			// wait for messages
			System.out.print("waiting for messages");
			for (int i = 0; i < 50; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
				System.out.print(".");
			}

			System.out.println("End asynchronous consumer");

		} catch (NamingException ne) {
			ne.printStackTrace(System.err);
			System.exit(0);
		} catch (JMSException jmse) {
			jmse.printStackTrace(System.err);
			System.exit(0);
		}

	}

	public void onMessage(Message message) {
		System.out.println("Start onMessage");
		if (message instanceof TextMessage) {
			try {
				System.out.println(((TextMessage) message).getText());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("not text message");
		}
		System.out.println("End onMessage");
	}

	public static void main(String[] args) {
		new AsynchronousJMSConsumer().retreiveMessage();
	}

}
