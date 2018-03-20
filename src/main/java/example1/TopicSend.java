package example1;

import java.io.*;
import java.util.*;
import javax.naming.*;
import javax.jms.*;

public class TopicSend {
	/**
	 * Defines the JNDI context factory.
	 */
	public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
	/**
	 * Defines the JMS connection factory.
	 */
	public final static String JMS_FACTORY = "jndi.ConnectionFactoryTopicMisha";
	/**
	 * Defines the topic.
	 */
	public final static String TOPIC = "jndi.Topic1Misha";
	
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";
	
	protected TopicConnectionFactory tconFactory;
	protected TopicConnection tcon;
	protected TopicSession tsession;
	protected TopicPublisher tpublisher;
	protected Topic topic;
	protected TextMessage msg;
	
	public void init(Context ctx) throws NamingException, JMSException {
		tconFactory = (TopicConnectionFactory) ctx.lookup(JMS_FACTORY);
		tcon = tconFactory.createTopicConnection();
		tsession = tcon.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		topic = (Topic) ctx.lookup(TOPIC);
		tpublisher = tsession.createPublisher(topic);
		msg = tsession.createTextMessage();
		tcon.start();
	}

	/**
	 * Sends a message to a JMS topic.
	 * 
	 * @params message message to be sent
	 * @exception JMSException
	 *                if JMS fails to send message due to internal error
	 * 
	 */
	public void send(String message) throws JMSException {
		msg.setText(message);		
		tpublisher.publish(msg);
	}

	/**
	 * Closes JMS objects.
	 * 
	 * @exception JMSException
	 *                if JMS fails to close objects due to internal error
	 */
	public void close() throws JMSException {
		tpublisher.close();
		tsession.close();
		tcon.close();
	}

	/**
	 * Prompts, reads, and sends a message.
	 * 
	 * @param ts
	 *            TopicSend
	 * @exception IOException
	 *                if problem occurs during read/write operation
	 * @exception JMSException
	 *                if JMS fails due to internal error
	 */
	protected static void readAndSend(TopicSend ts) throws IOException, JMSException {
		BufferedReader msgStream = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		do {
			System.out.print("Enter message (\"quit\" to quit): ");
			line = msgStream.readLine();
			if (line != null && line.trim().length() != 0) {
				ts.send(line);
				System.out.println("JMS Message Sent: " + line + "\n");
			}
		} while (line != null && !line.equalsIgnoreCase("quit"));
	}

	/**
	 * Get initial JNDI context.
	 * 
	 * @params url Weblogic URL.
	 * @exception NamingException
	 *                if problem occurs with JNDI context interface
	 */
	protected static InitialContext getInitialContext() throws NamingException {
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
		env.put(Context.PROVIDER_URL, DEFAULT_URL);		
		env.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
		env.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);
		return new InitialContext(env);
	}	

	public static void main(String[] args) throws Exception {	
	
		InitialContext ic = getInitialContext();
		TopicSend ts = new TopicSend();
		ts.init(ic);
		readAndSend(ts);
		ts.close();
	}
}
