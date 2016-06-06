package bank.jms;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSProducer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;
import bank.local.Driver.Bank;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;

public class JmsServerHandler implements Runnable {
	private Destination destination;
	private Command inputCmd;
	private Bank bank;
	private ObjectMapper mapper;
	private JMSProducer sender;

	public JmsServerHandler( JMSProducer sender, Destination destination, Command inputCmd, Bank localBank,ObjectMapper mapper) {
		this.destination = destination;
		this.inputCmd = inputCmd;
		this.bank = localBank;
		this.sender = sender;
	}

	@Override
	public void run() {
		try {
			executeCommand(inputCmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void executeCommand(Command c) throws Exception {
		Result r = new Result();

		switch (c.name) {
		case deposit: {
			String number = c.arguments[0];
			double amount = Double.parseDouble(c.arguments[1]);
			Account acc = bank.getAccount(number);
			try {
				acc.deposit(amount);
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
			System.out.println("fail");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mapper.writeValue(out, r);
		System.out.println(new String(out.toByteArray()));
		sender.send(destination, out.toByteArray());
	}

}
