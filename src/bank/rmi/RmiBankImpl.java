package bank.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import bank.Account;
import bank.Bank;
import bank.BankDriver2.UpdateHandler;
import bank.InactiveException;
import bank.OverdrawException;
import bank.local.Driver;
import bank.local.LocalAccount;

public class RmiBankImpl extends UnicastRemoteObject implements RmiBank {
	private Bank inner;

	protected RmiBankImpl() throws RemoteException {
		super();
		inner = new Driver.Bank();
	}

	private final LinkedList<UpdateHandler> handlers = new LinkedList<>();

	@Override
	public String createAccount(String owner) throws IOException {
		String accountNr = inner.createAccount(owner);
		update(accountNr);
		return accountNr;
	}

	@Override
	public boolean closeAccount(String accountNr) throws IOException {
		boolean end = inner.closeAccount(accountNr);
		if (end)
			update(accountNr);
		return end;
	}

	@Override
	public Set<String> getAccountNumbers() throws IOException {
		return inner.getAccountNumbers();
	}

	@Override
	public Account getAccount(String accountNr) throws IOException {
		return ((inner.getAccount(accountNr) == null) ? null
				: (new RmiAccountImpl(inner.getAccount(accountNr), handlers)));
	}

	@Override
	public void transfer(Account a, Account b, double amount)
			throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
		// TODO Auto-generated method stub

	}

	public void update(String accountNr) {

	}

	@Override
	public void registerUpdateHandler(RmiUpdateHandler u) throws RemoteException {
		// TODO Auto-generated method stub

	}

	public class RmiAccountImpl extends UnicastRemoteObject implements RmiAccount {
		private LinkedList<UpdateHandler> updater;
		private Account inner;

		public RmiAccountImpl(Account owner, LinkedList<UpdateHandler> updater) throws IOException {
			super();
			this.updater = updater;
			this.inner = owner;
		}

		@Override
		public void deposit(double amount) throws InactiveException, IOException {
			inner.deposit(amount);
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException, IOException {
			inner.withdraw(amount);
		}

		@Override
		public String getNumber() throws IOException {
			return inner.getNumber();
		}

		@Override
		public String getOwner() throws IOException {
			return inner.getOwner();
		}

		@Override
		public boolean isActive() throws IOException {
			return inner.isActive();
		}

		@Override
		public double getBalance() throws IOException {
			return inner.getBalance();
		}
	}
}
