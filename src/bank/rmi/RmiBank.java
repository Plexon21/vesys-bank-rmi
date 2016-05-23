package bank.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import bank.Bank;

public interface RmiBank extends Bank, Remote {
	void registerUpdateHandler(RmiUpdateHandler u) throws RemoteException;
}
