package bank.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Set;

import bank.Account;
import bank.Bank;
import bank.BankDriver2.UpdateHandler;
import bank.InactiveException;
import bank.OverdrawException;
import bank.local.Driver;

public class RmiBankImpl extends UnicastRemoteObject implements RmiBank {

	private static final long serialVersionUID = 649713363348103036L;
	private final Bank inner;

	protected RmiBankImpl() throws RemoteException {
		super();
		inner = new Driver.Bank();
	}

	private final LinkedList<UpdateHandler> handlers = new LinkedList<>();

	@Override
	public String createAccount(String owner) throws IOException {
		String accountNr = inner.createAccount(owner);
		if (accountNr != null) {
			update(accountNr);
			return accountNr;
		} else
			return null;
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
		return ((inner.getAccount(accountNr) == null) ? null : (new RmiAccountImpl(inner.getAccount(accountNr))));
	}

	@Override
	public void transfer(Account a, Account b, double amount)
			throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
		inner.transfer(a, b, amount);
		update(a.getNumber());
		update(b.getNumber());
	}

	public void update(String accountNr) throws IOException {
		for (UpdateHandler u : handlers) {
			u.accountChanged(accountNr);
		}
	}

	@Override
	public void registerUpdateHandler(RmiUpdateHandler u) throws RemoteException {
		handlers.add(u);
	}

	public class RmiAccountImpl extends UnicastRemoteObject implements RmiAccount {

		private static final long serialVersionUID = -520467193761734515L;

		private final Account inner;

		public RmiAccountImpl(Account owner) throws IOException {
			super();
			this.inner = owner;
		}

		@Override
		public void deposit(double amount) throws InactiveException, IOException {
			inner.deposit(amount);
			update(inner.getNumber());
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException, IOException {
			inner.withdraw(amount);
			update(inner.getNumber());
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
