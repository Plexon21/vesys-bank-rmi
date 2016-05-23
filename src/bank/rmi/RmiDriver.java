package bank.rmi;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;

import bank.Bank;
import bank.BankDriver2;

public class RmiDriver implements BankDriver2 {
	RmiBank bank;

	@Override
	public void connect(String[] args) throws IOException {
		try {
			bank = (RmiBank) Naming.lookup("rmi://localhost:1099/bank");
		} catch (NotBoundException e) {
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
		bank.registerUpdateHandler(new RmiUpdateHandlerImpl(handler));
	}

}
