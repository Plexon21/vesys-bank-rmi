package bank.jms;

import java.util.LinkedList;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import bank.BankDriver2;

public class JmsUpdateHandler implements MessageListener {
	private List<BankDriver2.UpdateHandler> handlers = new LinkedList<>();
	private Context jndiContext;
	private ConnectionFactory factory;
	private Topic topic;
	
	public JmsUpdateHandler() throws NamingException {
		jndiContext = new InitialContext();
		factory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
		topic = (Topic) jndiContext.lookup("topic/BANK");
	}
	public void registerUpdateHandler(BankDriver2.UpdateHandler handler) {
		handlers.add(handler);
	}
	public void start() {
		JMSContext context = factory.createContext();
		context.start();
		JMSConsumer subscriber = context.createConsumer(topic);
		subscriber.setMessageListener(this);
	}
	
	@Override
	public void onMessage(Message arg0) {
		try {
			String nr = arg0.getBody(String.class);
			for(BankDriver2.UpdateHandler handler : handlers){
				handler.accountChanged(nr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
