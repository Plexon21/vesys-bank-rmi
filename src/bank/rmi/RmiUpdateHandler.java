package bank.rmi;

import java.rmi.Remote;

import bank.BankDriver2.UpdateHandler;

public interface RmiUpdateHandler extends Remote, UpdateHandler {
}
