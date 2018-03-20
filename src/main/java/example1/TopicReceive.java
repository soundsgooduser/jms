package example1;

import java.util.*;
import javax.naming.*;
import javax.jms.*;


public class TopicReceive implements MessageListener {
	/**
	 * Defines the JNDI context factory.
	 */
	public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	/**
	 * Defines the JMS connection factory for the topic.
	 */
	public final static String JMS_FACTORY = "jndi.ConnectionFactoryTopicMisha";
	/**
	 * Defines the topic.
	 */
	public final static String TOPIC = "jndi.Topic1Misha";
	
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";
	
	private TopicConnectionFactory tconFactory;
	private TopicConnection tcon;
	private TopicSession tsession;
	private TopicSubscriber tsubscriber;
	private Topic topic;
	private boolean quit = false;

	/**
	 * Message listener interface.
	 * 
	 * @param msg
	 *            message
	 * 
	 */	
	public void onMessage(Message msg) {
		try {
			String msgText;
			if (msg instanceof TextMessage) {
				msgText = ((TextMessage) msg).getText();
			} else {
				msgText = msg.toString();
			}
			System.out.println("JMS Message Received: " + msgText);
			if (msgText.equalsIgnoreCase("quit")) {
				synchronized (this) {
					quit = true;
					this.notifyAll(); // Notify main thread to quit
				}
			}
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	/**
	 * Creates all the necessary objects for sending messages to a JMS topic.
	 * 
	 * @param ctx
	 *            JNDI initial context
	 * @param topicNamename
	 *            of topic
	 * @exception NamingExceptionif
	 *                problem occurred with JNDI context interface
	 * @exception JMSException
	 *                if JMS fails to initialize due to internal error
	 */
	public void init(Context ctx) throws NamingException, JMSException {
		tconFactory = (TopicConnectionFactory) ctx.lookup(JMS_FACTORY);
		tcon = tconFactory.createTopicConnection();
		tsession = tcon.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		topic = (Topic) ctx.lookup(TOPIC);
		tsubscriber = tsession.createSubscriber(topic);
		tsubscriber.setMessageListener(this);
		tcon.start();
	}

	/**
	 * Closes JMS objects.
	 * 
	 * @exception JMSException
	 *                if JMS fails to close objects due to internal error
	 */
	public void close() throws JMSException {
		tsubscriber.close();
		tsession.close();
		tcon.close();
	}

	private static InitialContext getInitialContext(String url) throws NamingException {
		Hashtable<String,String> env = new Hashtable<String,String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
		env.put(Context.PROVIDER_URL, DEFAULT_URL);
		env.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
		env.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);		
		return new InitialContext(env);
	}
	
	/**
	 * main() method.
	 * 
	 * @params args WebLogic Server URL
	 * @exception Exception
	 *                if execution fails
	 */
	public static void main(String[] args) throws Exception {

		InitialContext ic = getInitialContext(DEFAULT_URL);
		TopicReceive tr = new TopicReceive();
		tr.init(ic);
		System.out.println("TopicReceive started.");
		System.out.println("JMS Ready To Receive Messages (To quit, send a \"quit\" message).");
		// Wait until a "quit" message has been received.
		synchronized (tr) {
			while (!tr.quit) {
				try {
					tr.wait();
				} catch (InterruptedException ie) {
				}
			}
		}
		tr.close();
	}
}
