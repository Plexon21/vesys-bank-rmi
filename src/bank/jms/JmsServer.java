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
	private ConnectionFactory factory;
	private Queue queue;
	private Topic topic;
	private ObjectMapper mapper;
	private Result r;

	public JmsServer(Bank localBank) throws NamingException {

		this.bank = localBank;
		commandAcceptor = new Thread(this);
		mapper = new ObjectMapper();
		mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
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
			while (running) {
				Message request = consumer.receive();
				try {
					JsonParser parser = mapper.getFactory()
							.createParser(new ByteArrayInputStream(request.getBody(byte[].class)));
					Command c = mapper.readValue(parser, Command.class);
					executeCommand(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void executeCommand(Command c) throws Exception {
		r = new Result();

		switch (c.name) {
		case deposit: {
			String number = c.arguments[0];
			double amount = Double.parseDouble(c.arguments[1]);
			Account acc = bank.getAccount(number);
			try {
				acc.deposit(amount);
				accountChanged(number);
				r = new Result(CommandName.deposit, null, null);
			} catch (InactiveException e) {
				r = new Result(CommandName.deposit, "Inactive", null);
			} catch (IllegalArgumentException e) {
				r = new Result(CommandName.deposit, "IllegalArgument", null);
			} catch (NullPointerException e) {
				r = new Result(CommandName.deposit, "IO", null);
			}
			break;
		}
		case withdraw: {
			String number = c.arguments[0];
			double amount = Double.parseDouble(c.arguments[1]);
			Account acc = bank.getAccount(number);
			try {
				acc.withdraw(amount);
				accountChanged(number);
				r = new Result(CommandName.withdraw, null, null);
			} catch (InactiveException e) {
				r = new Result(CommandName.withdraw, "Inactive", null);
			} catch (IllegalArgumentException e) {
				r = new Result(CommandName.withdraw, "IllegalArgument", null);
			} catch (OverdrawException e) {
				r = new Result(CommandName.withdraw, "Overdraw", null);
			} catch (NullPointerException e) {
				r = new Result(CommandName.withdraw, "IO", null);
			}
			break;
		}
		case transfer: {
			Account from = bank.getAccount(c.arguments[0]);
			Account to = bank.getAccount(c.arguments[1]);
			double amount = Double.parseDouble(c.arguments[2]);
			try {
				bank.transfer(from, to, amount);
				accountChanged(c.arguments[0]);
				accountChanged(c.arguments[1]);
				r = new Result(CommandName.transfer, null, null);
			} catch (InactiveException e) {
				r = new Result(CommandName.transfer, "Inactive", null);
			} catch (IllegalArgumentException e) {
				r = new Result(CommandName.transfer, "IllegalArgument", null);
			} catch (OverdrawException e) {
				r = new Result(CommandName.transfer, "Overdraw", null);
			} catch (NullPointerException e) {
				r = new Result(CommandName.transfer, "IO", null);
			}
			break;
		}
		case createAccount: {
			String owner = c.arguments[0];
			try {
				String number = bank.createAccount(owner);
				r = new Result(CommandName.createAccount, null, number);
			} catch (NullPointerException e) {
				r = new Result(CommandName.createAccount, "IO", null);
			}
			break;
		}
		case closeAccount: {
			String number = c.arguments[0];
			try {
				Boolean result = bank.closeAccount(number);
				accountChanged(number);
				accountChanged(number);
				r = new Result(CommandName.closeAccount, null, result);
			} catch (NullPointerException e) {
				r = new Result(CommandName.closeAccount, "IO", null);
			}
			break;
		}
		case getAccountNumbers: {
			Set<String> result = bank.getAccountNumbers();
			r = new Result(CommandName.getAccountNumbers, null, result);
			break;
		}
		case getAccount: {
			String number = c.arguments[0];
			Account acc = bank.getAccount(number);
			String[] res = new String[2];
			if (acc != null) {
				res[0] = number;
				res[1] = bank.getAccount(number).getOwner();
			} else
				res = null;
			r = new Result(CommandName.getAccount, null, res);
			break;
		}
		case getBalance: {
			String number = c.arguments[0];
			try {
				r = new Result(CommandName.getBalance, null, bank.getAccount(number).getBalance());
			} catch (NullPointerException e) {
				r = new Result(CommandName.getBalance, "IO", null);
			}
			break;
		}
		case isActive: {
			try {
				String number = c.arguments[0];
				r = new Result(CommandName.isActive, null, bank.getAccount(number).isActive());
			} catch (NullPointerException e) {
				r = new Result(CommandName.isActive, "IO", null);
			}
			break;
		}
		default:

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
