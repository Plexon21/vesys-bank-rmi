package bank.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiServer {
	public static void main(String[] args) throws IOException {
		RmiBank bank = new RmiBankImpl();
		Registry registry;
		try{
			System.out.println("create registry");
			registry = LocateRegistry.createRegistry(1099);
		}catch(RemoteException e){
			System.err.println("failed to create");
			registry = LocateRegistry.getRegistry();			
		}
		registry.rebind("Bank", bank);
	}
}
