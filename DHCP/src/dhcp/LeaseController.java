package dhcp;

import java.util.ArrayList;
import java.util.Date;

public class LeaseController extends Thread {

	private DHCPDatabase database;
	private ArrayList<Clientes> clientes;

	public LeaseController(DHCPDatabase database) {
		this.database = database;
		this.clientes = database.getClientes();
	}

	public void run() {
		Date date;
		while (true) {
			for (int i = 0; i < clientes.size(); i++) {
				date = new Date();
				if (date.getTime() > clientes.get(i).getDateVencimiento()
						.getTime()) {
					clientes.remove(i);
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
