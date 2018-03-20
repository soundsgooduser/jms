package example4.chat;

import javax.jms.*;
import javax.naming.*;

import java.io.*;
import java.util.Hashtable;
import java.util.Properties;

public class Chat implements javax.jms.MessageListener {
	private TopicSession pubSession;
	private TopicSession subSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String username;
	
	public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";	
	public final static String JMS_FACTORY = "jndi.ChatConnectionFactory";
	public final static String TOPIC = "jndi.ChatTopic";
	private static final String DEFAULT_URL = "t3://localhost:7001";
	private static final String DEFAULT_USER = "weblogic";
	private static final String DEFAULT_PASSWORD = "weblogic11";

	/* Constructor. Establish JMS publisher and subscriber */
	public Chat() throws Exception {
		// Obtain a JNDI connection
		
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
		env.put(Context.PROVIDER_URL, DEFAULT_URL);		
		env.put(Context.SECURITY_PRINCIPAL, DEFAULT_USER);
		env.put(Context.SECURITY_CREDENTIALS, DEFAULT_PASSWORD);

		InitialContext jndi = new InitialContext(env);

		// Look up a JMS connection factory
		TopicConnectionFactory conFactory = (TopicConnectionFactory) jndi.lookup(JMS_FACTORY);

		// Create a JMS connection
		TopicConnection connection = conFactory.createTopicConnection();

		// Create two JMS session objects
		TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		// Look up a JMS topic
		Topic chatTopic = (Topic) jndi.lookup(TOPIC);

		// Create a JMS publisher and subscriber
		TopicPublisher publisher = pubSession.createPublisher(chatTopic);
		TopicSubscriber subscriber = subSession.createSubscriber(chatTopic);

		// Set a JMS message listener
		subscriber.setMessageListener(this);

		// Intialize the Chat application
		set(connection, pubSession, subSession, publisher, username);

		// Start the JMS connection; allows messages to be delivered
		connection.start();

	}

	/* Initialize the instance variables */
	public void set(TopicConnection con, TopicSession pubSess, TopicSession subSess, TopicPublisher pub, String username) {
		this.connection = con;
		this.pubSession = pubSess;
		this.subSession = subSess;
		this.publisher = pub;
		this.username = username;
	}

	/* Receive message from topic subscriber */
	public void onMessage(Message message) {
		try {
			TextMessage textMessage = (TextMessage) message;
			String text = textMessage.getText();
			System.out.println("onMessage: "+text);
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	/* Create and send message using topic publisher */
	protected void writeMessage(String text) throws JMSException {
		TextMessage message = pubSession.createTextMessage();
		message.setText(username + " : " + text);
		publisher.publish(message);
	}

	/* Close the JMS connection */
	public void close() throws JMSException {
		connection.close();
	}

	/* Run the Chat client */
	public static void main(String[] args) {
		try {

			Chat chat = new Chat();

			// Read from command line
			BufferedReader commandLine = new java.io.BufferedReader(new InputStreamReader(System.in));

			// Loop until the word "exit" is typed
			while (true) {
				String s = commandLine.readLine();
				if (s.equalsIgnoreCase("exit")) {
					chat.close(); // close down connection
					System.exit(0);// exit program
				} else
					chat.writeMessage(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
