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

	private final List<UpdateHandler> handlers = new LinkedList<>();

	@Override
	public String createAccount(String owner) throws IOException {
		String accountNr = inner.createAccount(owner);
		update(accountNr);
		return accountNr;
	}

	@Override
	public boolean closeAccount(String accountNr) throws IOException {
		boolean end = inner.closeAccount(accountNr);
		if (end) {
			update(accountNr);
		}
		return end;
	}

	@Override
	public Set<String> getAccountNumbers() throws IOException {
		Set<String> activeAccounts = new HashSet<>();
		for (Account acc : accounts.values()) {
			if (acc.isActive()) {
				activeAccounts.add(acc.getNumber());
			}
		}
		return activeAccounts;
	}

	@Override
	public Account getAccount(String accountNr) throws IOException {
		return ((inner.getAccount(accountNr)==null)? null:(new RmiAccountImpl(inner.getAccount(accountNr),handlers));
	}

	@Override
	public void transfer(Account a, Account b, double amount)
			throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerUpdateHandler(Update u) throws RemoteException {
		if (u != null)
			handlers.add(u);

	}

	public void update(String accountNr) {
		for (Update ru : handlers) {
			try {
				ru.update(accountNr);
			} catch (RemoteException e) {
				e.printStackTrace();
				handlers.remove(ru);
			}
		}
	}

	public class RmiAccountImpl extends LocalAccount implements RmiAccount {
		private List<UpdateHandler> updater;

		public void setActive(boolean active) {
			this.active = active;
		}

		public RmiAccountImpl(String owner, List<UpdateHandler> updater) {
			super(owner);
			this.updater = updater;
		}

		@Override
		public void deposit(double amount) throws InactiveException, IOException {
			super.deposit(amount);
			updater.update(getNumber());
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException, IOException {
			super.withdraw(amount);
			updater.update(getNumber());
		}
	}

	@Override
	public void registerUpdateHandler(RmiUpdateHandler u) throws RemoteException {
		// TODO Auto-generated method stub

	}
}
