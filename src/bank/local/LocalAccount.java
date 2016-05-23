package bank.local;

import java.io.IOException;
import java.rmi.RemoteException;

import bank.InactiveException;
import bank.OverdrawException;

public class LocalAccount implements bank.Account {
	public String number;
	public String owner;
	private double balance;
	public boolean active = true;
	private static int nextNumber = 1;

	public LocalAccount(String owner) {
		this.owner = owner;
		String s = String.format("%09d", nextNumber);
		this.number = "1-" + s + "-" + nextNumber++;

	}

	@Override
	public double getBalance() {
		return balance;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public String getNumber() {
		return number;
	}

	@Override
	public boolean isActive() {
		return active;
	}
	
	@Override
	public void deposit(double amount) throws InactiveException, IOException {
		if (!isActive()) {
			throw new InactiveException("Inactive");
		}
		if (amount < 0) {
			throw new IllegalArgumentException("IllegalArgument");
		}
		balance += amount;
	}

	@Override
	public void withdraw(double amount) throws InactiveException, OverdrawException,IOException {
		if (!isActive())
			throw new InactiveException("Inactive");
		if (amount < 0)
			throw new IllegalArgumentException("IllegalArgument");
		if (amount > balance) {
			throw new OverdrawException("Overdraw");
		}
		balance -= amount;
	}

}
