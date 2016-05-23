package bank.rmi;

import java.io.IOException;
import java.rmi.Naming;

public class RmiServer {
	public static void main(String[] args) throws IOException {
		RmiBank bank = new RmiBankImpl();
		Naming.rebind("rmi://localhost:1099/bank", bank);
	}
}
