package bank.jms;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Account;
import bank.Bank;
import bank.BankDriver2;
import bank.InactiveException;
import bank.OverdrawException;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;

public class JmsDriver implements BankDriver2 {

	private ObjectMapper mapper;
	private JmsBank bank;
	private JmsUpdateHandler handler;

	@Override
	public void connect(String[] args) throws IOException {
		try {
			bank = new JmsBank();
			handler = new JmsUpdateHandler();
			handler.start();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void disconnect() throws IOException {
		bank = null;
	}

	@Override
	public Bank getBank() {
		return bank;
	}

	@Override
	public void registerUpdateHandler(UpdateHandler handler) throws IOException {
		this.handler.registerUpdateHandler(handler);
	}

	public class JmsBank implements Bank {

		private Context jndiContext;
		private ConnectionFactory factory;
		private Queue queue;
		private final Map<String, Account> accounts = new HashMap<>();

		public JmsBank() throws NamingException {
			jndiContext = new InitialContext();
			factory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
			queue = (Queue) jndiContext.lookup("queue/BANK");
			mapper = new ObjectMapper();
			mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
		}

		@SuppressWarnings("unchecked")
		protected Result request(Command outputCmd) throws IOException {
			try (JMSContext context = factory.createContext()) {

				JMSProducer sender = context.createProducer().setJMSReplyTo(queue);
				JMSConsumer receiver = context.createConsumer(queue);

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				mapper.writeValue(out, outputCmd);
				sender.send(queue, out.toByteArray());
				byte[] msg = receiver.receiveBody(byte[].class);

				String str = new String(msg);
				return (Result) mapper.readValue(msg, Result.class);
			}
		}

		@Override
		public String createAccount(String owner) throws IOException {
			try {
				Result r = request(new Command(CommandName.createAccount, new String[] { owner }));
				return (String) r.resultValue;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public boolean closeAccount(String number) throws IOException {
			try {
				Result r = request(new Command(CommandName.closeAccount, new String[] { number }));
				return (boolean) r.resultValue;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return false;
			}
		}

		@Override
		public Set<String> getAccountNumbers() throws IOException {
			try {
				Result r = request(new Command(CommandName.getAccountNumbers, null));
				return new HashSet<String>((Collection<? extends String>) r.resultValue);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public Account getAccount(String number) throws IOException {
			try {
				Result r = request(new Command(CommandName.getAccount, new String[] { number }));
				ArrayList<String> res = (ArrayList<String>) r.resultValue;
				if (res != null)
					return new JmsAccount(res.get(1), res.get(0));
				else
					return null;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public void transfer(Account from, Account to, double amount)
				throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
			Result r = new Result();
			try {
				r = request(new Command(CommandName.transfer,
						new String[] { from.getNumber(), to.getNumber(), String.valueOf(amount) }));
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			if (r.arguments != null) {
				switch (r.arguments) {
				case "Inactive":
					throw new InactiveException();
				case "IllegalArgument":
					throw new IllegalArgumentException();
				case "Overdraw":
					throw new OverdrawException();
				case "IO":
					throw new IOException();
				}
			}

		}

		public class JmsAccount implements Account {
			private String number;
			private String owner;
			private double balance;

			public JmsAccount() {

			}

			public JmsAccount(String owner, String number) {
				this.owner = owner;
				this.number = number;
			}

			@Override
			public String getNumber() throws IOException {
				return number;
			}

			@Override
			public String getOwner() throws IOException {
				return owner;
			}

			@Override
			public boolean isActive() throws IOException {
				Result r = request(new Command(CommandName.isActive, new String[] { getNumber() }));
				return (boolean) r.resultValue;
			}

			@Override
			public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
				if (isActive()) {
					Result r = request(
							new Command(CommandName.deposit, new String[] { getNumber(), String.valueOf(amount) }));
					if (r.arguments != null) {
						switch (r.arguments) {
						case "Inactive":
							throw new InactiveException();
						case "IllegalArgument":
							throw new IllegalArgumentException();
						case "IO":
							throw new IOException();
						}
					}
				} else
					throw new InactiveException();
			}

			@Override
			public void withdraw(double amount)
					throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
				if (isActive()) {
					Result r = request(
							new Command(CommandName.withdraw, new String[] { getNumber(), String.valueOf(amount) }));
					if (r.arguments != null) {
						switch (r.arguments) {
						case "Inactive":
							throw new InactiveException();
						case "IllegalArgument":
							throw new IllegalArgumentException();
						case "Overdraw":
							throw new OverdrawException();
						case "IO":
							throw new IOException();
						}
					}
				} else
					throw new InactiveException();
			}

			@Override
			public double getBalance() throws IOException {
				Result r = request(new Command(CommandName.getBalance, new String[] { getNumber() }));
				return (double) r.resultValue;
			}
		}
	}
}
