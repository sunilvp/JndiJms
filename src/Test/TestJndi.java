package Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class TestJndi implements MessageListener{
	
	public static final String JNDI_TOPIC="java:/jms/topic/sunil/Topic";
	
	public void publish(TopicConnection aInTopicConnection,Topic aInTopic, String aInUserName) throws JMSException, IOException
	{
		TopicSession lTopicSession = aInTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		TopicPublisher lTopicPublisher = lTopicSession.createPublisher(aInTopic);
		aInTopicConnection.start();
		BufferedReader lBr = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.println("Enter the data:");
			String lMsgReceived = lBr.readLine();
			if(lMsgReceived.equals("Exit")){
				aInTopicConnection.close();
				System.exit(0);
			}
			else
			{
				TextMessage lTextMessage = lTopicSession.createTextMessage();
				lTextMessage.setText(aInUserName + ":" + lMsgReceived);
				lTopicPublisher.publish(lTextMessage);
			}
		}
		
	}
	
	public void subscribe(TopicConnection aInTopicConnection,Topic aInTopic, TestJndi aInTestJndi) throws JMSException
	{
		TopicSession lTopicSession = aInTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		TopicSubscriber lTopicSubscriber = lTopicSession.createSubscriber(aInTopic);
		lTopicSubscriber.setMessageListener(aInTestJndi);
	}
	
	public Context getInitialContext() throws NamingException
	{
		Hashtable jndiPropsLocal = new Hashtable();
		jndiPropsLocal.put("java.naming.factory.initial", "org.jboss.naming.remote.client.InitialContextFactory");
		jndiPropsLocal.put("java.naming.provider.url", "remote://localhost:4447");
		jndiPropsLocal.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
		jndiPropsLocal.put("java.naming.security.principal", "appuser2");
		jndiPropsLocal.put("java.naming.security.credentials", "appuser2123");	
		return new InitialContext(jndiPropsLocal);
	}
	
	@Override
	public void onMessage(Message aInMessage) {	
		try{
			if(aInMessage instanceof TextMessage){
				TextMessage lTextMessage = (TextMessage) aInMessage;
				System.out.println("The Message Recived is:" + lTextMessage.getText());
			}
		}
		catch(JMSException e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) throws JMSException, NamingException, IOException
	{ 
		if(args.length <=0)
		{
			System.out.println("Fewer arguments");
		}
		else
		{
			String lUserName = args[0];
			Hashtable jndiProps = new Hashtable();	
	
			TestJndi lTestJndi =  new TestJndi();
			Context lContext = lTestJndi.getInitialContext();
			System.out.println("Performing a lookup on the topic object from the context");
			Topic lTopicRetrived = (Topic)lContext.lookup(JNDI_TOPIC);
			System.out.println("Recived the topic");
			System.out.println("Performing a lookup on RemoteConnectionFactory");
			TopicConnectionFactory lTopicConnectionFactory = (TopicConnectionFactory)lContext.lookup("jms/RemoteConnectionFactory");
			System.out.println("Recived RemoteConnectionFactory");
			System.out.println("Performing a lookup on Topic connection");
			TopicConnection lTopicConnection = lTopicConnectionFactory.createTopicConnection();
			System.out.println("Recieved Topic connection");
			lTestJndi.subscribe(lTopicConnection, lTopicRetrived, lTestJndi);
			lTestJndi.publish(lTopicConnection, lTopicRetrived, lUserName);
		}
	}
}
