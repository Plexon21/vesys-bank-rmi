package bank.rmi;

import java.rmi.Naming;

public class RmiServer {
	public static void main(String[] args) throws Exception {
		RmiBank bank = new RmiBankImpl();
		Naming.rebind("rmi://localhost:1099/bank", bank);
		System.out.println("Bank Server started..");
	}
}
