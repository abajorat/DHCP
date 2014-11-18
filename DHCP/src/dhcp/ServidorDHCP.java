/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

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
	InetAddress clientIPaddress;

	// Número de puerto a usar
	int numPuerto;

	// Booleano de servidor activo
	boolean servidorActivo = true;

	// Constantes para los códigos de las opciones DHCP
	static final int SERVER_PORT = 67;

	public static void main(String[] args) throws Exception {
		try {
			
			ServidorDHCP.getInstance().start();
			DHCPDatabase.getInstance().agregarClienteForzado("AC-FD-EC-C1-C8-6F", "192.168.0.11");
		} catch (Exception e) {
			System.out.println("Error principal: " + e.getMessage()+e);
			throw e;
		}
	}

	/**
	 * Constructor de la clase ServidorDHCP
	 */

	protected ServidorDHCP() {
		try {
			principal = new Interfaz();
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

			pack	 = new PaqueteDHCP(messageIn.getData());

			switch (pack.existInOption(53).getIntValue()) {
			case PaqueteDHCP.DISCOVER:
				System.out.println("DISCOVERY "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Recibido DISCOVERY de: ["
						+ pack.getStringHexa(pack.getCHADDR()) + "] || FECHA: "
						+ new Date());
				manejarDiscovery();
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
	private void manejarDiscovery()
			throws UnknownHostException, IOException {
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
		if (DHCPDatabase.getInstance().existeCliente(
				pack.getStringHexa(pack.getCHADDR())))
			tempId = DHCPDatabase.getInstance().getIPdeMAC(
					pack.getStringHexa(pack.getCHADDR()));
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
			int indiceIP = DHCPDatabase.getInstance().buscarIp(tempId);
			if (indiceIP != -1)
				tempId = DHCPDatabase.getInstance().sacarIP(indiceIP,
						pack.getStringHexa(pack.getCHADDR()),
						IPdesdeByte(pack.getGIADDR()));
			else
				tempId = DHCPDatabase.getInstance().getIPLibre(
						pack.getStringHexa(pack.getCHADDR()),
						IPdesdeByte(pack.getGIADDR()));

		} else {
			tempId = DHCPDatabase.getInstance().getIPLibre(
					pack.getStringHexa(pack.getCHADDR()),
					IPdesdeByte(pack.getGIADDR()));
			System.out.println("Nuevo");
		}
		if (tempId == null) {
			// no hay mas direcciones
			System.out.println("No hay mas direcciones");
			return;
		}
		
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
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().mascara.get(DHCPDatabase.getInstance().identificarSubRed(tempId)));// máscara
		opt2[2] = aux[0];
		opt2[3] = aux[1];
		opt2[4] = aux[2];
		opt2[5] = aux[3];
		data.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(3))); // opción router
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().gateway.get(DHCPDatabase.getInstance().identificarSubRed(tempId)));// Gateway
		opt2[2] = aux[0];
		opt2[3] = aux[1];
		opt2[4] = aux[2];
		opt2[5] = aux[3];
		data.addDHCPOPTION(opt2);

		// Primero buscamos si el paquete tiene un requerimiento de lease:

		opt2[0] = (hexToByte(Integer.toHexString(51))); // IP Address Lease Time
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		byte[] leaseTime = fragmentarInt(DHCPDatabase.getInstance().lease);
		opt2[2] = leaseTime[0];
		opt2[3] = leaseTime[1];
		opt2[4] = leaseTime[2];
		opt2[5] = leaseTime[3];
		data.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(6))); // Domain Name server
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().dns.get(DHCPDatabase.getInstance().identificarSubRed(tempId)));
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

		byte[] broadcast = { hexToByte("FF"), hexToByte("FF"), hexToByte("FF"),
				hexToByte("FF") };// Broadcast
		if (!IPdesdeByte(pack.getGIADDR()).equals("0.0.0.0")) {
			broadcast = pack.getGIADDR();

		}

		DatagramPacket ep = new DatagramPacket(data.getData(),
				data.getLengthData(), InetAddress.getByAddress(broadcast), 68);
		socket.send(ep);
		System.out.println("OFFER " + pack.getStringHexa(pack.getCHADDR())
				+ "IP OFRECIDA: " + tempId);
		DHCPlog.reportar("Se envió DHCP_OFFFER a: ["
				+ pack.getStringHexa(pack.getCHADDR()) + "] con la IP: ["
				+ tempId + "] || FECHA: " + new Date());
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
	private void manejarRequest() throws UnknownHostException,
			IOException {
		PaqueteDHCP data = new PaqueteDHCP();
		String TempIP = new String();
		byte[] unicast = null;

		// Ingresamos valores al data para hacer el DHCPACK
		OpcionDHCP opServ = pack.existInOption(54);

		if (opServ.getIntOption() == 54) { // si existe el identificador del
											// servidor es respuesta a un offer
			System.out.println("option 54");
			InetAddress idServidor = InetAddress.getByAddress(opServ
					.getValues());
			if (idServidor.equals(InetAddress.getLocalHost())) {
				System.out.println("richtiger server");
				// SI escogio este servidor, entonces continua
				// Confiamos en que el cliente envia la direccion ofrecida en la
				// opcion IP Request
				// if(DHCPDatabase.getInstance().existeCliente(pack.getStringHexa(pack.getCHADDR())))
				TempIP = DHCPDatabase.getInstance().getIPdeMAC(
						pack.getStringHexa(pack.getCHADDR()));
				// else
				// TempIP =
				// DHCPDatabase.getInstance().getIPLibre(pack.getStringHexa(pack.getCHADDR()));
			} else{
				System.out.println("kein richtiger server");
			// No escogio este servidor entonces se queda callado
			return;}
			
		} else {// No tiene el id del servidor por ende no es respuesta a un
				// offer
			System.out.println("nicht 54");
			String ipPedida = getRequestedIP(pack);

			if (DHCPDatabase.getInstance().existeCliente(
					pack.getStringHexa(pack.getCHADDR()))) {
				System.out.println("kenn ich schon");

				TempIP = DHCPDatabase.getInstance().getIPdeMAC(
						pack.getStringHexa(pack.getCHADDR()));
			} else{
				System.out.println("kennt keine sau");
				return; }// Si no existe en la base de datos se queda callado

			if (ipPedida == null) {// Esta en RENEWING state no vienen ni ip
									// request ni server id
				// Se confia en que tiene la direccion correcta que es la que yo
				// tengo en la base de datos.
				// se hace unicast:
				unicast = pack.getCIADDR();
				System.out.println("renew");

			} else if (!ipPedida.equals(TempIP)) { // esta mal le envio un NAK
				System.out.println("maaloooo");
				PaqueteDHCP nak = generarNak(pack);
				byte[] broadcast = { hexToByte("FF"), hexToByte("FF"),
						hexToByte("FF"), hexToByte("FF") };// Broadcast
				DatagramPacket ep = new DatagramPacket(nak.getData(),
						nak.getLengthData(),
						InetAddress.getByAddress(broadcast), 68);
				socket.send(ep);
				System.out.println("NACK "
						+ pack.getStringHexa(pack.getCHADDR()));
				DHCPlog.reportar("Se envió DHCP_NAK a: ["
						+ pack.getStringHexa(pack.getCHADDR())
						+ "]  || FECHA: " + new Date());
				return;
			} 

		}
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
		 * if(DHCPDatabase.getInstance().existeCliente(pack.getStringHexa(pack.
		 * getCHADDR()))) TempIP =
		 * DHCPDatabase.getInstance().getIPdeMAC(pack.getStringHexa
		 * (pack.getCHADDR())); else TempIP =
		 * DHCPDatabase.getInstance().getIPLibre
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
		opt1[2] = (hexToByte(Integer.toHexString(PaqueteDHCP.ACK))); // DCP ACK
		data.addDHCPOPTION(opt1);

		byte[] opt2 = new byte[6];
		opt2[0] = (hexToByte(Integer.toHexString(1))); // Subnet Mask
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		byte[] aux = new byte[4];
		System.out.println(TempIP);
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().mascara.get(DHCPDatabase.getInstance().identificarSubRed(TempIP)));// máscara
		opt2[2] = aux[0];
		opt2[3] = aux[1];
		opt2[4] = aux[2];
		opt2[5] = aux[3];
		data.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(3))); // opción router
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		System.out.println(DHCPDatabase.getInstance().identificarSubRed(TempIP));
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().gateway.get(DHCPDatabase.getInstance().identificarSubRed(TempIP)));// Gateway
		opt2[2] = aux[0];
		opt2[3] = aux[1];
		opt2[4] = aux[2];
		opt2[5] = aux[3];
		data.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(51))); // IP Address Lease Time
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		byte[] leaseTime = fragmentarInt(DHCPDatabase.getInstance().lease);
		opt2[2] = leaseTime[0];
		opt2[3] = leaseTime[1];
		opt2[4] = leaseTime[2];
		opt2[5] = leaseTime[3];
		data.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(6))); // Domain Name server
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().dns.get(DHCPDatabase.getInstance().identificarSubRed(TempIP)));
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

		byte[] broadcast = { hexToByte("FF"), hexToByte("FF"), hexToByte("FF"),
				hexToByte("FF") };// Broadcast
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
		principal.actualizarTabla(
				DHCPDatabase.getInstance().getCliente(
						pack.getStringHexa(pack.getCHADDR())),
				DHCPDatabase.getInstance().getIndice(
						pack.getStringHexa(pack.getCHADDR())), darHoraInicio(),
				darHoraVencimiento());
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
		DHCPDatabase.getInstance().liberarIP(
				pack.getStringHexa(pack.getCHADDR()), giaddr);
		DHCPlog.reportar("Se liberó conexión de: ["
				+ pack.getStringHexa(pack.getCHADDR())
				+ "] Dirección liberada: ["
				+ DHCPDatabase.getInstance().eliminarCliente(
						pack.getStringHexa(pack.getCHADDR()), giaddr)
				+ "] || FECHA: " + new Date());
	}

	private void manejarDecline() {
		// Como ya se habia creado, la direccion no esta dentro de las
		// disponibles
		// solo se elminia el cliente
		String giaddr = IPdesdeByte(pack.getGIADDR());
		System.out.println("DECLINE " + pack.getStringHexa(pack.getCHADDR()));
		DHCPDatabase.getInstance().elminiarClienteDecline(
				pack.getStringHexa(pack.getCHADDR()));
		DHCPlog.reportar("Se liberó conexión de: ["
				+ pack.getStringHexa(pack.getCHADDR())
				+ "] Dirección declinada: ["
				+ DHCPDatabase.getInstance().eliminarCliente(
						pack.getStringHexa(pack.getCHADDR()), giaddr)
				+ "] || FECHA: " + new Date());
	}

	@SuppressWarnings("static-access")
	private void manejarInform() throws UnknownHostException,
			IOException {

		String clientIP = IPdesdeByte(pack.getCIADDR());
		int indice = DHCPDatabase.getInstance().buscarIp(clientIP);
		String giaddr = IPdesdeByte(pack.getGIADDR());
		if (indice != -1)
			DHCPDatabase.getInstance().
			sacarIP(indice,
					pack.getStringHexa(pack.getCHADDR()), giaddr);
		// Aqui ya quedo agregado el nuevo cliente.
		else
			// En caso de que no este la ip en el grupo de direcciones
			// asignables:
			DHCPDatabase.getInstance().agregarClienteForzado(
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
		opt1[2] = (hexToByte(Integer.toHexString(PaqueteDHCP.ACK))); // DCP ACK
		ack.addDHCPOPTION(opt1);

		byte[] opt2 = new byte[6];
		opt2[0] = (hexToByte(Integer.toHexString(1))); // Subnet Mask
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		byte[] aux = new byte[4];
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().mascara.get(DHCPDatabase.getInstance().identificarSubRed(giaddr)));// máscara
		opt2[2] = aux[0];
		opt2[3] = aux[1];
		opt2[4] = aux[2];
		opt2[5] = aux[3];
		ack.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(3))); // opción router
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().gateway.get(DHCPDatabase.getInstance().identificarSubRed(giaddr)));// Gateway
		opt2[2] = aux[0];
		opt2[3] = aux[1];
		opt2[4] = aux[2];
		opt2[5] = aux[3];
		ack.addDHCPOPTION(opt2);

		opt2[0] = (hexToByte(Integer.toHexString(6))); // Domain Name server
		opt2[1] = (hexToByte(Integer.toHexString(4))); // lenght
		aux = pack.getIPOfStr(DHCPDatabase.getInstance().dns.get(DHCPDatabase.getInstance().identificarSubRed(giaddr)));
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
		principal.actualizarTabla(
				DHCPDatabase.getInstance().getCliente(
						pack.getStringHexa(pack.getCHADDR())),
				DHCPDatabase.getInstance().getIndice(
						pack.getStringHexa(pack.getCHADDR())), darHoraInicio(),
				null);

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

	private String darHoraInicio() {
		Date date = new Date();
		return "" + date.getHours() + ":" + date.getMinutes() + ":"
				+ date.getSeconds();
	}

	private String darHoraVencimiento() {
		Date date = new Date(new Date().getTime()
				+ DHCPDatabase.getInstance().lease * 1000);
		return "" + date.getHours() + ":" + date.getMinutes() + ":"
				+ date.getSeconds();
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
