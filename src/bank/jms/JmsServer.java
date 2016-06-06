package bank.jms;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Account;
import bank.BankDriver2;
import bank.InactiveException;
import bank.OverdrawException;
import bank.local.Driver.Bank;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;

public class JmsServer implements Runnable, BankDriver2.UpdateHandler {

	private Bank bank;
	private Thread commandAcceptor;
	private boolean running = false;
	private Context jndiContext;
	private ExecutorService executors;
	private ConnectionFactory factory;
	private Queue queue;
	private Topic topic;
	private ObjectMapper mapper;
	private Result r;

	public JmsServer(Bank localBank) throws NamingException {

		this.bank = localBank;
		commandAcceptor = new Thread(this);
		mapper = new ObjectMapper();
		executors = Executors.newFixedThreadPool(20);
		mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
		mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
		jndiContext = new InitialContext();
		factory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
		queue = (Queue) jndiContext.lookup("/queue/BANK");
		topic = (Topic) jndiContext.lookup("/topic/BANK");
	}

	public static void main(String[] args) throws NamingException {
		Bank b = new Bank();
		JmsServer server = new JmsServer(b);
		try {
			server.start();
			System.out.println("Server started");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {
		commandAcceptor.start();
		running = true;
	}

	@Override
	public void run() {
		try (JMSContext context = factory.createContext()) {
			JMSConsumer consumer = context.createConsumer(queue);
			JMSProducer sender = context.createProducer();
			while (running) {
				Message request = consumer.receive();
				try {
					JsonParser parser = mapper.getFactory()
							.createParser(new ByteArrayInputStream(request.getBody(byte[].class)));
					Command c = mapper.readValue(parser, Command.class);
					executors.submit(new JmsServerHandler(sender, request.getJMSReplyTo(), c, bank,mapper));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	@Override
	public void accountChanged(String id) throws IOException {
		try (JMSContext context = factory.createContext()) {
			JMSProducer publisher = context.createProducer();
			publisher.send(topic, id);
		}
	}
}
