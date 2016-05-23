package bank.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import bank.BankDriver2.UpdateHandler;;

public class RmiUpdateHandlerImpl extends UnicastRemoteObject implements RmiUpdateHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5748226242162841287L;
	private UpdateHandler handler;

	public RmiUpdateHandlerImpl(UpdateHandler handler) throws RemoteException{
		super();
		this.handler = handler;
	}
	@Override
	public void accountChanged(String id) throws IOException {
		handler.accountChanged(id);
	}

}
