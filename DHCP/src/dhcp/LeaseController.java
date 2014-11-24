package dhcp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class LeaseController extends Thread {

	private DHCPDatabase database;
	private HashMap<String, Clientes> clientes;

	public LeaseController(DHCPDatabase database) {
		this.database = database;
		this.clientes = database.getClientes();
	}

	public void run() {
		Date date;
		while (true) {
			for (Clientes c : new ArrayList<Clientes>(clientes.values())) {
				date = new Date();
				if (date.getTime() > c.getDateVencimiento()
						.getTime()) {
					synchronized(clientes){
						clientes.remove(c.getIdCliente());
					}
					
					database.modelChanged();

				}
			}
			try {
				sleep(DHCPDatabase.getLease()*500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
