package bank.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import bank.Account;

public interface RmiAccount extends Account, Remote {
	void setActive(boolean active) throws RemoteException;
}
