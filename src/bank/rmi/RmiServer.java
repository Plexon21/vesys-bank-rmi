package bank.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;
import ch.fhnw.ds.rmi.quotes.QuoteListener;

public class RmiServer {


	public static void main(String[] args) {
		RmiBank bank = new RmiBankImpl();
		Naming.rebind("rmi://localhost:1099/bank", bank);
	}
}