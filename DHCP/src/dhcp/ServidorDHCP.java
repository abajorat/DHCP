/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

/**
 * Esta clase maneja el protocolo DHCP según el RFC
 * 
 * @author Daniel Serrano
 */
public class ServidorDHCP extends Thread {

	// Interfaz
	private Interfaz principal;

	private PaqueteDHCP pack;

	// Única instancia de este objeto
	private static ServidorDHCP _servidor = null;

	// Socket para la comunicación
	private DatagramSocket socket;

	// Variable para manejar la direccion del cliente
	private InetAddress clientIPaddress;

	// Número de puerto a usar
	private int numPuerto;

	// Booleano de servidor activo
	private boolean servidorActivo = true;

	private static DHCPDatabase database;

	// Constantes para los códigos de las opciones DHCP
	static final int SERVER_PORT = 67;

	public static void main(String[] args) throws Exception {
		try {

			ServidorDHCP.getInstance().start();

		} catch (Exception e) {
			System.out.println("Error principal: " + e.getMessage() + e);
			throw e;
		}
	}

	/**
	 * Constructor de la clase ServidorDHCP
	 */

	protected ServidorDHCP() {
		try {
			database = DHCPDatabase.getInstance();
			LeaseController con = new LeaseController(database);
			con.start();
			database.agregarClienteForzado("AC-FD-EC-C1-C8-6F", "192.168.0.11");
			principal = new Interfaz(database);
			principal.setVisible(true);
			// Crea el socket para enviar y recibir mensajes DHCP
			socket = new DatagramSocket(SERVER_PORT);

		} catch (java.net.BindException e1) {
			System.out.println("Error en atadura de puerto: ");
			System.out.println("Otro proceso está atado al puerto");
			System.out.println("o no tiene acceso a este puerto");
		} catch (Exception e2) {
			System.out.println("ServidorDHCP:Principal: " + e2);
		}

		// Se crea pero no se activa el servidor
		servidorActivo = false;
	}

	/**
	 * Retorna la instancia del servidor
	 *
	 * @return la instancia del servidor
	 */

	public static ServidorDHCP getInstance() {
		if (_servidor == null)
			_servidor = new ServidorDHCP();
		return _servidor;
	}

	/**
	 * Este método solo activa la instancia del servidor
	 */

	public void activarServidor() {
		servidorActivo = true;

		if (servidorActivo) {
			ServidorDHCP servidor = ServidorDHCP.getInstance();
			synchronized (servidor) {
				servidor.notify();
			}
		}
	}

	/**
	 * Método que desactiva el servidor
	 */

	public void desactivarServidor() {
		if (servidorActivo)
			servidorActivo = false;
	}

	/**
	 * Método que determina la situación del servidor
	 *
	 * @return true si el servidor está activo, de lo contrario false
	 */

	public boolean isServidorActivo() {
		return servidorActivo;
	}

	/**
	 * Método que inicia la aplicación
	 *
	 */

	/**
	 * Método que corre el protocolo DHCP
	 */

	public void run() {
		while (true) {
			try {
				if (!servidorActivo) {
					synchronized (this) {
						while (!servidorActivo)
							wait();
					}
				} else {
					runProtocoloDHCP();
				}
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Método que escucha mensajes DHCP los clasifica y maneja sesgún el tipo
	 */

	public void runProtocoloDHCP() {
		try {
			// DHCPMessage messageIn = new DHCPMessage();
			byte[] datos = new byte[1024];
			DatagramPacket messageIn = new DatagramPacket(datos, datos.length);

			socket.receive(messageIn);

			pack = new PaqueteDHCP(messageIn.getData());

			switch (pack.existInOption(53).getIntValue()) {
			case PaqueteDHCP.DISCOVER:
				System.out.println("DISCOVERY "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Recibido DISCOVERY de: ["
						+ pack.getStringHexa(pack.getCHADDR()) + "] || FECHA: "
						+ new Date());

				try {
					manejarDiscovery();
				} catch (NoRouteToHostException e) {
					// TODO Auto-generated catch block
					System.err
							.println("No pudo ser mandado: " + e.getMessage());
				}
				break;
			case PaqueteDHCP.REQUEST:
				System.out.println("REQUEST "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Recibido REQUEST de: ["
						+ pack.getStringHexa(pack.getCHADDR()) + "] || FECHA: "
						+ new Date());
				manejarRequest();
				break;
			case PaqueteDHCP.DECLINE:
				System.out.println("DECLINE "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Recibido DECLINE de: ["
						+ pack.getStringHexa(pack.getCHADDR()) + "] || FECHA: "
						+ new Date());
				manejarDecline();
				break;
			case PaqueteDHCP.RELEASE:
				System.out.println("RELEASE "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Recibido RELEASE de : ["
						+ pack.getStringHexa(pack.getCHADDR()) + "] || FECHA: "
						+ new Date());
				manejarRelease();
				break;
			case PaqueteDHCP.INFORM:
				System.out.println("INFORM "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Recibido INFORM de : ["
						+ pack.getStringHexa(pack.getCHADDR()) + "] || FECHA: "
						+ new Date());
				manejarInform();
				break;
			default:
				System.out.println("Mensaje desconocido");
				DHCPlog.reportar("Mensaje recibido es desconocido... Ignorando...");
				break;
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------------------------\\
	// MÉTODOS ÚTILES PARA LA CLASE \\
	// -------------------------------------------------------------------------------------------\\

	@SuppressWarnings("static-access")
	private void manejarDiscovery() throws UnknownHostException, IOException {

		PaqueteDHCP data = new PaqueteDHCP();
		data.setOP((byte) 0x02);
		data.setHTYPE(pack.getHTYPE());
		data.setHLEN(pack.getHLEN());
		data.setHOPS((byte) 0x00);
		data.setXID(pack.getXID());
		byte[] a = { hexToByte("00"), hexToByte("00") };
		data.setSECS(a);
		data.setFLAGS(pack.getFLAGS());
		byte[] b = { hexToByte("00"), hexToByte("00"), hexToByte("00"),
				hexToByte("00") };
		data.setCIADDR(b);
		String tempId = new String();
		if (database.existeCliente(pack.getStringHexa(pack.getCHADDR())))
			tempId = database.getIPdeMAC(pack.getStringHexa(pack.getCHADDR()));
		else if (pack.existInOption(50).getIntOption() != 0) {

			OpcionDHCP opc = pack.existInOption(50);
			byte[] dir = opc.getValues();
			int[] dirAux = new int[4];
			for (int i = 0; i < 4; i++) {
				if (dir[i] < 0) {
					dir[i] = (byte) (dir[i] & 0x7F);
					dirAux[i] = (int) dir[i];
					dirAux[i] = dirAux[i] | 0x80;
				} else
					dirAux[i] = (int) dir[i];
			}
			tempId = String.format("%d.%d.%d.%d", dirAux[0], dirAux[1],
					dirAux[2], dirAux[3]);

			if (tempId.equals(database.getOwnIp())) {
				sendNak();
				return;
			}

			else if (!database.existeIp(tempId)) {

				database.sacarIP(pack.getStringHexa(pack.getCHADDR()),
						getRequestedIP(pack));

			} else {

				tempId = database.getIPLibre(
						pack.getStringHexa(pack.getCHADDR()),
						IPdesdeByte(pack.getGIADDR()));
			}
		} else {

			tempId = database.getIPLibre(pack.getStringHexa(pack.getCHADDR()),
					IPdesdeByte(pack.getGIADDR()));
		}
		if (tempId == null) {
			// no hay mas direcciones
			System.out.println("No hay mas direcciones");
			return;
		}
		int subred = database.identificarSubRed(tempId);

		if (subred != -1) {

			data.setYIADDR(pack.getIPOfStr(tempId));
			data.setSIADDR(b);
			data.setGIADDR(b);
			data.setCHADDR(pack.getCHADDR());
			data.setMagicCookie(pack.getMagicCookie());

			// Relleno de las opciones del DHCP-OFFER
			byte[] opt1 = new byte[3];
			// ponemos las opciones
			opt1[0] = (hexToByte(Integer.toHexString(53))); // DHCP Message Type
			opt1[1] = (hexToByte(Integer.toHexString(1))); // lenght
			opt1[2] = (hexToByte(Integer.toHexString(PaqueteDHCP.OFFER))); // DHCP-OFFER
			data.addDHCPOPTION(opt1);

			byte[] opt2 = new byte[6];
			opt2[0] = (hexToByte(Integer.toHexString(1))); // Subnet Mask
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			byte[] aux = new byte[4];
			aux = pack.getIPOfStr(database.getMascara().get(
					database.identificarSubRed(tempId)));// máscara
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			data.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(3))); // opción router
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			aux = pack.getIPOfStr(database.getGateway().get(
					database.identificarSubRed(tempId)));// Gateway
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			data.addDHCPOPTION(opt2);

			// Primero buscamos si el paquete tiene un requerimiento de lease:

			opt2[0] = (hexToByte(Integer.toHexString(51))); // IP Address Lease
															// Time
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			byte[] leaseTime = fragmentarInt(database.getLease());
			opt2[2] = leaseTime[0];
			opt2[3] = leaseTime[1];
			opt2[4] = leaseTime[2];
			opt2[5] = leaseTime[3];
			data.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(6))); // Domain Name server
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			aux = pack.getIPOfStr(database.getDns().get(
					database.identificarSubRed(tempId)));
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			data.addDHCPOPTION(opt2);

			String miIp = InetAddress.getLocalHost().getHostAddress();
			byte[] ipServer = pack.getIPOfStr(miIp);
			opt2[0] = (hexToByte(Integer.toHexString(54)));// Server Identifier
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			opt2[2] = ipServer[0]; // Server DHCP
			opt2[3] = ipServer[1]; // Server DHCP
			opt2[4] = ipServer[2]; // Server DHCP
			opt2[5] = ipServer[3]; // Server DHCP
			data.addDHCPOPTION(opt2);
			data.finalizarDatagrama(); // Finalizamoa el datagrama

			byte[] broadcast = { hexToByte("FF"), hexToByte("FF"),
					hexToByte("FF"), hexToByte("FF") };// Broadcast
			if (!IPdesdeByte(pack.getGIADDR()).equals("0.0.0.0")) {
				broadcast = pack.getGIADDR();

			}

			DatagramPacket ep = new DatagramPacket(data.getData(),
					data.getLengthData(), InetAddress.getByAddress(broadcast),
					68);
			socket.send(ep);
			System.out.println("OFFER " + pack.getStringHexa(pack.getCHADDR())
					+ "IP OFRECIDA: " + tempId);
			DHCPlog.reportar("Se envió DHCP_OFFFER a: ["
					+ pack.getStringHexa(pack.getCHADDR()) + "] con la IP: ["
					+ tempId + "] || FECHA: " + new Date());
		} else
			System.out.println("DESCONOCIDO: " + tempId);
	}

	/**
	 * Método que se encarga de configurar y enviar un mensaje DHCP_ACK
	 * 
	 * @param pack
	 *            mensaje recibido
	 * @throws UnknownHostException
	 * @throws IOException
	 */

	@SuppressWarnings("static-access")
	private void manejarRequest() throws UnknownHostException, IOException {
		PaqueteDHCP data = new PaqueteDHCP();
		String TempIP = new String();
		byte[] unicast = null;

		// Ingresamos valores al data para hacer el DHCPACK
		OpcionDHCP opServ = pack.existInOption(54);

		if (opServ.getIntOption() == 54) { // si existe el identificador del
											// servidor es respuesta a un offer
			InetAddress idServidor = InetAddress.getByAddress(opServ
					.getValues());
			if (idServidor.equals(InetAddress.getLocalHost())) {
				// SI escogio este servidor, entonces continua
				// Confiamos en que el cliente envia la direccion ofrecida en la
				// opcion IP Request
				// if(database.existeCliente(pack.getStringHexa(pack.getCHADDR())))
				TempIP = database.getIPdeMAC(pack.getStringHexa(pack
						.getCHADDR()));
				if (TempIP == null) {

					TempIP = getRequestedIP(pack);
				} 
			} else {
				// No escogio este servidor entonces se queda callado
				System.out.println("No pregunta a este servidor sino a "+idServidor.toString());


				return;
			}

		} else {// No tiene el id del servidor por ende no es respuesta a un
				// offer
			String ipPedida = getRequestedIP(pack);

			if (database.existeCliente(pack.getStringHexa(pack.getCHADDR()))) {

				TempIP = database.getIPdeMAC(pack.getStringHexa(pack
						.getCHADDR()));
			} else {
				System.out.println("DESCONOCIDO" + TempIP);
				return;
			}// Si no existe en la base de datos se queda callado

			if (ipPedida == null) {// Esta en RENEWING state no vienen ni ip
									// request ni server id
				// Se confia en que tiene la direccion correcta que es la que yo
				// tengo en la base de datos.
				// se hace unicast:

				unicast = pack.getCIADDR();
			} else if (!ipPedida.equals(TempIP)) { // esta mal le envio un NAK
				sendNak();
				return;
			}

		}
		int subred = database.identificarSubRed(TempIP);
		if (subred != -1) {
			data.setOP((byte) 0x02);
			data.setHTYPE(pack.getHTYPE());
			data.setHLEN(pack.getHLEN());
			data.setHOPS((byte) 0x00);
			data.setXID(pack.getXID());
			byte[] a = { hexToByte("00"), hexToByte("00") };
			data.setSECS(a);
			data.setFLAGS(pack.getFLAGS());
			byte[] b = { hexToByte("00"), hexToByte("00"), hexToByte("00"),
					hexToByte("00") };
			data.setCIADDR(b);
			/*
			 * if(database.existeCliente(pack.getStringHexa(pack. getCHADDR())))
			 * TempIP = database.getIPdeMAC(pack.getStringHexa
			 * (pack.getCHADDR())); else TempIP = database.getIPLibre
			 * (pack.getStringHexa(pack.getCHADDR()));
			 */
			data.setYIADDR(pack.getIPOfStr(TempIP));
			data.setSIADDR(b);
			data.setGIADDR(b);
			data.setCHADDR(pack.getCHADDR());
			data.setMagicCookie(pack.getMagicCookie());

			// Relleno de las opciones del DHCP-ACK
			byte[] opt1 = new byte[3];
			// ponemos las opciones
			opt1[0] = (hexToByte(Integer.toHexString(53))); // DHCP Message Type
			opt1[1] = (hexToByte(Integer.toHexString(1))); // lenght
			opt1[2] = (hexToByte(Integer.toHexString(PaqueteDHCP.ACK))); // DCP
																			// ACK
			data.addDHCPOPTION(opt1);

			byte[] opt2 = new byte[6];
			opt2[0] = (hexToByte(Integer.toHexString(1))); // Subnet Mask
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			byte[] aux = new byte[4];
			aux = pack.getIPOfStr(database.getMascara().get(
					database.identificarSubRed(TempIP)));// máscara
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			data.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(3))); // opción router
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			aux = pack.getIPOfStr(database.getGateway().get(
					database.identificarSubRed(TempIP)));// Gateway
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			data.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(51))); // IP Address Lease
															// Time
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			byte[] leaseTime = fragmentarInt(database.getLease());
			opt2[2] = leaseTime[0];
			opt2[3] = leaseTime[1];
			opt2[4] = leaseTime[2];
			opt2[5] = leaseTime[3];
			data.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(6))); // Domain Name server
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			aux = pack.getIPOfStr(database.getDns().get(
					DHCPDatabase.getInstance().identificarSubRed(TempIP)));
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			data.addDHCPOPTION(opt2);

			String miIp = InetAddress.getLocalHost().getHostAddress();
			byte[] ipServer = pack.getIPOfStr(miIp);
			opt2[0] = (hexToByte(Integer.toHexString(54)));// Server Identifier
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			opt2[2] = ipServer[0]; // Server DHCP
			opt2[3] = ipServer[1]; // Server DHCP
			opt2[4] = ipServer[2]; // Server DHCP
			opt2[5] = ipServer[3]; // Server DHCP
			data.addDHCPOPTION(opt2);
			data.finalizarDatagrama(); // Finalizamoa el datagrama

			byte[] broadcast = { hexToByte("FF"), hexToByte("FF"),
					hexToByte("FF"), hexToByte("FF") };// Broadcast
			if (!IPdesdeByte(pack.getGIADDR()).equals("0.0.0.0")) {
				broadcast = pack.getGIADDR();
			}
			DatagramPacket ep = null;
			if (unicast == null)
				ep = new DatagramPacket(data.getData(), data.getLengthData(),
						InetAddress.getByAddress(broadcast), 68);
			else
				ep = new DatagramPacket(data.getData(), data.getLengthData(),
						InetAddress.getByAddress(unicast), 68);
			socket.send(ep);
			System.out.println("ACK " + pack.getStringHexa(pack.getCHADDR())
					+ "IP OFRECIDA: " + TempIP);
			DHCPlog.reportar("Se envió DHCP_ACK a: ["
					+ pack.getStringHexa(pack.getCHADDR()) + "] con la IP: ["
					+ TempIP + "] || FECHA: " + new Date());

		} else {
			System.out.println("DESCONOCIDO: " + TempIP);
		}
	}

	private void sendNak() throws UnknownHostException, IOException {
		PaqueteDHCP nak = generarNak(pack);
		byte[] broadcast = { hexToByte("FF"), hexToByte("FF"), hexToByte("FF"),
				hexToByte("FF") };// Broadcast
		DatagramPacket ep = new DatagramPacket(nak.getData(),
				nak.getLengthData(), InetAddress.getByAddress(broadcast), 68);
		socket.send(ep);
		System.out.println("NACK " + pack.getStringHexa(pack.getCHADDR()));
		DHCPlog.reportar("Se envió DHCP_NAK a: ["
				+ pack.getStringHexa(pack.getCHADDR()) + "]  || FECHA: "
				+ new Date());

	}

	/**
	 * Método que elimina el cliente del servidor y devuelve la IP usada al
	 * rango de direcciones disponibles
	 * 
	 * @param pack
	 *            mensaje recibido
	 */

	private void manejarRelease() {
		String giaddr = IPdesdeByte(pack.getGIADDR());
		System.out.println("RELEASE " + pack.getStringHexa(pack.getCHADDR()));
		database.liberarIP(pack.getStringHexa(pack.getCHADDR()), giaddr);
		DHCPlog.reportar("Se liberó conexión de: ["
				+ pack.getStringHexa(pack.getCHADDR())
				+ "] Dirección liberada: ["
				+ database.eliminarCliente(
						pack.getStringHexa(pack.getCHADDR()), giaddr)
				+ "] || FECHA: " + new Date());
		// database.getDirDisponibles().add(giaddr);
	}

	private void manejarDecline() {
		// Como ya se habia creado, la direccion no esta dentro de las
		// disponibles
		// solo se elminia el cliente
		String giaddr = IPdesdeByte(pack.getGIADDR());
		System.out.println("DECLINE " + pack.getStringHexa(pack.getCHADDR()));
		database.elminiarClienteDecline(pack.getStringHexa(pack.getCHADDR()));
		DHCPlog.reportar("Se liberó conexión de: ["
				+ pack.getStringHexa(pack.getCHADDR())
				+ "] Dirección declinada: ["
				+ database.eliminarCliente(
						pack.getStringHexa(pack.getCHADDR()), giaddr)
				+ "] || FECHA: " + new Date());
		database.getDirDisponibles().remove(giaddr);
	}

	@SuppressWarnings("static-access")
	private void manejarInform() throws UnknownHostException, IOException {
		String clientIP = IPdesdeByte(pack.getCIADDR());

		int subred = database.identificarSubRed(clientIP);
		String giaddr = database.getGateway().get(subred);
		if (subred != -1) {

			if (database.existeIp(clientIP)) {
				database.sacarIP(pack.getStringHexa(pack.getCHADDR()), clientIP);
			}
			// Aqui ya quedo agregado el nuevo cliente.
			else
				// En caso de que no este la ip en el grupo de direcciones
				// asignables:
				database.agregarClienteForzado(
						pack.getStringHexa(pack.getCHADDR()), clientIP);

			PaqueteDHCP ack = new PaqueteDHCP();
			ack.setOP((byte) 0x02);
			ack.setHTYPE(pack.getHTYPE());
			ack.setHLEN(pack.getHLEN());
			ack.setHOPS((byte) 0x00);
			ack.setXID(pack.getXID());
			ack.setSECS(new byte[2]);
			ack.setFLAGS(pack.getFLAGS());
			ack.setCIADDR(new byte[4]);
			ack.setYIADDR(new byte[4]);
			ack.setSIADDR(new byte[4]);
			ack.setGIADDR(new byte[4]);
			ack.setCHADDR(pack.getCHADDR());
			ack.setMagicCookie(pack.getMagicCookie());

			// opciones:
			byte[] opt1 = new byte[3];
			// ponemos las opciones
			opt1[0] = (hexToByte(Integer.toHexString(53))); // DHCP Message Type
			opt1[1] = (hexToByte(Integer.toHexString(1))); // lenght
			opt1[2] = (hexToByte(Integer.toHexString(PaqueteDHCP.ACK))); // DCP
																			// ACK
			ack.addDHCPOPTION(opt1);

			byte[] opt2 = new byte[6];
			opt2[0] = (hexToByte(Integer.toHexString(1))); // Subnet Mask
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			byte[] aux = new byte[4];
			aux = pack.getIPOfStr(database.getMascara().get(
					database.identificarSubRed(clientIP)));// máscara
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			ack.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(3))); // opción router
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			aux = pack.getIPOfStr(database.getGateway().get(
					database.identificarSubRed(clientIP)));// Gateway
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			ack.addDHCPOPTION(opt2);

			opt2[0] = (hexToByte(Integer.toHexString(6))); // Domain Name server
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			aux = pack.getIPOfStr(database.getDns().get(
					database.identificarSubRed(clientIP)));
			opt2[2] = aux[0];
			opt2[3] = aux[1];
			opt2[4] = aux[2];
			opt2[5] = aux[3];
			ack.addDHCPOPTION(opt2);

			String miIp = InetAddress.getLocalHost().getHostAddress();

			byte[] ipServer = pack.getIPOfStr(miIp);
			opt2[0] = (hexToByte(Integer.toHexString(54)));// Server Identifier
			opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
			opt2[2] = ipServer[0]; // Server DHCP
			opt2[3] = ipServer[1]; // Server DHCP
			opt2[4] = ipServer[2]; // Server DHCP
			opt2[5] = ipServer[3]; // Server DHCP
			ack.addDHCPOPTION(opt2);
			ack.finalizarDatagrama(); // Finalizamos el datagrama

			// finalmente se envia el paquete unicast:
			byte[] unicast = pack.getCIADDR();

			DatagramPacket ep = new DatagramPacket(ack.getData(),
					ack.getLengthData(), InetAddress.getByAddress(unicast), 68);
			socket.send(ep);
			System.out.println("ACK " + pack.getStringHexa(pack.getCHADDR())
					+ "IP INFORMADA: " + clientIP);
			DHCPlog.reportar("Se envió DHCP_ACK unicast a: ["
					+ pack.getStringHexa(pack.getCHADDR()) + "] con la IP: ["
					+ clientIP + "] || FECHA: " + new Date());

		} else
			System.out.println("DESCONOCIDO : " + clientIP);
	}

	private String darHoraInicio() {
		// TODO Auto-generated method stub
		return null;
	}

	private String IPdesdeByte(byte[] dir) {

		int[] dirAux = new int[4];
		for (int i = 0; i < 4; i++) {
			if (dir[i] < 0) {
				dir[i] = (byte) (dir[i] & 0x7F);
				dirAux[i] = (int) dir[i];
				dirAux[i] = dirAux[i] | 0x80;
			} else
				dirAux[i] = (int) dir[i];
		}
		String clientIP = String.format("%d.%d.%d.%d", dirAux[0], dirAux[1],
				dirAux[2], dirAux[3]);

		return clientIP;
	}

	private byte hexToByte(String s) {
		return (byte) Integer.parseInt(s, 16);
	}

	public byte[] fragmentarInt(int x) {
		byte[] arr = new byte[4];
		arr[3] = (byte) x;
		x = x >> 8;
		arr[2] = (byte) x;
		x = x >> 8;
		arr[1] = (byte) x;
		x = x >> 8;
		arr[0] = (byte) x; // byte mas significativo

		return arr;
	}

	private String getRequestedIP(PaqueteDHCP pack) {
		String TempIP = null;
		if (pack.existInOption(50).getIntOption() != 0) {
			OpcionDHCP opc = pack.existInOption(50);
			byte[] dir = opc.getValues();
			int[] dirAux = new int[4];
			for (int i = 0; i < 4; i++) {
				if (dir[i] < 0) {
					dir[i] = (byte) (dir[i] & 0x7F);
					dirAux[i] = (int) dir[i];
					dirAux[i] = dirAux[i] | 0x80;
				} else
					dirAux[i] = (int) dir[i];
			}
			TempIP = String.format("%d.%d.%d.%d", dirAux[0], dirAux[1],
					dirAux[2], dirAux[3]);
			}

		return TempIP;
	}

	private PaqueteDHCP generarNak(PaqueteDHCP pack) {
		PaqueteDHCP nak = new PaqueteDHCP();
		nak.setOP((byte) 0x02);
		nak.setHTYPE((byte) 0x01);
		nak.setHLEN((byte) 0x06);
		nak.setHOPS((byte) 0x00);
		nak.setXID(pack.getXID());
		byte[] a = { hexToByte("00"), hexToByte("00") };
		nak.setSECS(a);
		nak.setFLAGS(pack.getFLAGS());
		byte[] b = { hexToByte("00"), hexToByte("00"), hexToByte("00"),
				hexToByte("00") };
		nak.setCIADDR(b);
		nak.setYIADDR(b);
		nak.setSIADDR(b);
		nak.setGIADDR(b);
		nak.setCHADDR(pack.getCHADDR());
		nak.setMagicCookie(pack.getMagicCookie());

		// Relleno de las opciones del DHCP-NACK
		byte[] opt1 = new byte[3];
		// ponemos las opciones
		opt1[0] = (hexToByte(Integer.toHexString(53))); // DHCP Message Type
		opt1[1] = (hexToByte(Integer.toHexString(1))); // lenght
		opt1[2] = (hexToByte(Integer.toHexString(PaqueteDHCP.NACK))); // DHCP
																		// NACK
		nak.addDHCPOPTION(opt1);

		nak.finalizarDatagrama();

		return nak;
	}

}
