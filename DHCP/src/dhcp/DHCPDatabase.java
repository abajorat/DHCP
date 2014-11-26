/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Clase que se encarga de cargar la configuración del servidor
 * 
 * @author Daniel Serrano Pardo
 */
public class DHCPDatabase extends Observable {

	// Única instancia de DHCPDatabase
	private static DHCPDatabase _database = null;

	private static ArrayList<String>[] direcciones;

	private static ArrayList<String> net;
	static ArrayList<String> mascara;
	private static ArrayList<String> dns;
	private static ArrayList<String> gateway;
	private static int lease;
	private static String ownIp;

	public static HashMap<String,Clientes> getClientes() {
		return clientes;
	}

	private static ArrayList<String> dirDisponibles;

	private static HashMap<String,Clientes> clientes = new HashMap<String,Clientes>();

	private DHCPDatabase() {
		cargarConfiguracion();
	}

	public static DHCPDatabase getInstance() {
		if (_database == null)
			_database = new DHCPDatabase();

		return _database;
	}

	/**
	 * Método que lee el archivo config.properties para cargar
	 */

	public static void cargarConfiguracion() {

		Properties propiedades = new Properties();
		net = new ArrayList<String>();
		mascara = new ArrayList<String>();
		dns = new ArrayList<String>();
		gateway = new ArrayList<String>();
		InputStream in = null;
		dirDisponibles = new ArrayList<String>();
		try {

			in = new FileInputStream("config.properties");
			propiedades.load(in);

			lease = Integer.parseInt(propiedades.getProperty("lease"));
			int numberSubnets = Integer.parseInt(propiedades
					.getProperty("numberSubnets"));
			direcciones = new ArrayList[numberSubnets];
			net.add(propiedades.getProperty("ownNet"));
			mascara.add(propiedades.getProperty("ownMask"));
			dns.add(propiedades.getProperty("ownDns"));
			gateway.add(propiedades.getProperty("ownGateway"));
			direcciones[0] = obtenerDirecciones(0);
			
			

			for (int i = 0; i < numberSubnets - 1; i++) {
				net.add(propiedades.getProperty("net" + i));
				mascara.add(propiedades.getProperty("mascara" + i));
				dns.add(propiedades.getProperty("dns" + i));
				gateway.add(propiedades.getProperty("gateway" + i));
				direcciones[i + 1] = obtenerDirecciones(i + 1);
			}
			dirDisponibles.remove(ownIp=propiedades.getProperty("ownIp"));
			
			direcciones[0].remove(ownIp);

		} catch (IOException e) {
			System.err.println("Error al cargar el archivo: " + e);
		}
	}

	public static String getOwnIp() {
		return ownIp;
	}

	/**
	 * Método que crea un arreglo con todas las direcciones IP a ofrecer
	 * 
	 * @param inicio
	 *            dirección inicial
	 * @param termino
	 *            dirección final
	 * @return arreglo con las direcciones IP
	 */

	private static ArrayList<String> obtenerDirecciones(int j) {
		ArrayList<String> retorno = new ArrayList<String>();
		String temp = net.get(j);

		StringTokenizer st;
		st = new StringTokenizer(temp, ".");
		int primero = Integer.parseInt(st.nextToken());
		int segundo = Integer.parseInt(st.nextToken());
		int tercero = Integer.parseInt(st.nextToken());
		int cuarto = Integer.parseInt(st.nextToken()) + 1;

		for (int i = 1; i < computeSize(j); i++) {

			cuarto++;
			if (cuarto == 256) {
				cuarto = 0;
				tercero++;
				if (tercero == 256) {
					tercero = 0;
					segundo++;
					if (segundo == 256) {
						segundo = 0;
						primero++;
					}
				}
			}
			temp = String.valueOf(primero) + "." + String.valueOf(segundo)
					+ "." + String.valueOf(tercero) + "."
					+ String.valueOf(cuarto);
			retorno.add(temp);
			dirDisponibles.add(temp);
		}
		return retorno;
	}

	private static int computeSize(int i) {
		StringTokenizer st = new StringTokenizer(mascara.get(i), ".");
		int primero = Integer.parseInt(st.nextToken());
		int segundo = Integer.parseInt(st.nextToken());
		int tercero = Integer.parseInt(st.nextToken());
		int cuarto = Integer.parseInt(st.nextToken());
		return 256 * 256 * 256 * 256 - primero * 256 * 256 * 256 - segundo
				* 256 * 256 - tercero * 256 - cuarto - 2;

	}

	public static void imprimirDireccionesDisponibles() {
		for (int i = 0; i < dirDisponibles.size(); i++)
			System.out.println(dirDisponibles.get(i));

	}

	public static void imprimirDirecciones(int j) {
		for (int i = 0; i < direcciones[j].size(); i++)
			System.out.println(direcciones[j].get(i));

	}

	/**
	 * Método que verifica si un cliente ya existe en la base de datos
	 * 
	 * @param MAC
	 *            Mac del cliente buscado
	 * @return true si existe, false si no existe
	 */

	public boolean existeCliente(String MAC) {
		if(clientes.containsKey(MAC)){
			clientes.get(MAC).renovar();
			dirDisponibles.remove(clientes.get(MAC).getDirIP());
			setChanged();
			notifyObservers();
			return true;
		}
		
		return false;
	}

	/**
	 * Método que retorna la dirección IP del cliente que está buscando
	 * 
	 * @param MAC
	 *            del cliente
	 * @return direccion del cliente
	 */

	public String getIPdeMAC(String MAC) {
		if(!clientes.containsKey(MAC)){
			return null;
		}
		return clientes.get(MAC).getDirIP();
	}

	/**
	 * Método que retorna una IP que no esté en uso y la asocia a un cliente
	 * 
	 * @return dirección IP libre
	 */

	public String getIPLibre(String MAC, String giaddr) {

		int i = identificarSubRed(giaddr);

		String mask = null, gate = null, doNaSe = null;
		int leaseNr = 0;

		if (i == -1) {
			System.out.println("El array List es nulo");
			return null;

		}

		mask = mascara.get(i);
		gate = gateway.get(i);
		leaseNr = lease;
		doNaSe = dns.get(i);

		if (!dirDisponibles.isEmpty()) {
			int j = 0;
			while(!dirDisponibles.contains(direcciones[i].get(j))){
				System.out.println(direcciones[i].get(j));
				j++;
			};
			Clientes nuevo = new Clientes(MAC, direcciones[i].get(j));
			synchronized(clientes){
				clientes.put(MAC,nuevo);
				nuevo.setDNS(doNaSe);
				nuevo.setLease(lease);
				nuevo.setMask(mask);
				nuevo.setGateway(gate);
			}
			
			dirDisponibles.remove(direcciones[i].get(j));
			setChanged();
			notifyObservers();
			System.out.println("getlibre sagt: " + direcciones[i].get(j));
			return direcciones[i].get(j);
		} else {
			System.out.println("El array list esta vacio");
			return null;

		}
	}

	public int identificarSubRed(String giaddr) {
		if (giaddr.equals("0.0.0.0")) {
			return 0;
		}
		for (int i = 0; i < direcciones.length; i++) {
			if (direcciones[i].contains(giaddr)) {
				return i;
			}
		}
		return -1;
	}

	public byte[] getIPOfStr(String s) {
		int j = 0;
		StringTokenizer ax = new StringTokenizer(s, ".");
		byte[] b = new byte[4];
		b[0] = hexToByte(Integer.toHexString(Integer.parseInt(ax.nextToken())));
		b[1] = hexToByte(Integer.toHexString(Integer.parseInt(ax.nextToken())));
		b[2] = hexToByte(Integer.toHexString(Integer.parseInt(ax.nextToken())));
		b[3] = hexToByte(Integer.toHexString(Integer.parseInt(ax.nextToken())));
		return b;
	}

	private byte hexToByte(String s) {
		return (byte) Integer.parseInt(s, 16);
	}

	public String eliminarCliente(String MAC, String giaddr) {
		synchronized(clientes){
			clientes.remove(MAC);
			dirDisponibles.add(giaddr);
		}
		setChanged();
		notifyObservers();
		return giaddr;
	}

	public Clientes getCliente(String MAC) {
		
		for (Clientes temp : new ArrayList<Clientes>(clientes.values()) )
			if (MAC.equalsIgnoreCase(temp.getIdCliente()))
				return temp;
		return null;
	}

	public boolean existeIp(String ip) {
		int n = identificarSubRed(ip);
		if (n == -1)
			return false;
		if(dirDisponibles.contains(ip)){
			return false;
		}

		return true;
	}

	public void sacarIP(String MAC, String ciaddr) {

		Clientes nuevo = new Clientes(MAC, ciaddr);
		
		synchronized(clientes){
			clientes.put(MAC,nuevo);
			
		}
		
		setChanged();
		notifyObservers();
		
	}

	public void liberarIP(String MAC, String giaddr) {
		synchronized(clientes){
			clientes.remove(MAC);
			dirDisponibles.add(giaddr);
		}
		setChanged();
		notifyObservers();

	}

	public void elminiarClienteDecline(String MAC) {
		synchronized(clientes){
			clientes.remove(MAC);
			//dirDisponibles.add(giaddr);
		}
		//TODO poruqe no a�adirlo a los disponibles ?
		setChanged();
		notifyObservers();
	}

	public void agregarClienteForzado(String MAC, String IP) {
		Clientes nuevo = new Clientes(MAC, IP);
		nuevo.setForzado(true);
		synchronized(clientes){
			clientes.put(MAC,nuevo);
		}
		setChanged();
		notifyObservers();
	}
	public void modelChanged(){
		setChanged();
		notifyObservers();
	}

	public static ArrayList<String>[] getDirecciones() {
		return direcciones;
	}

	public static ArrayList<String> getNet() {
		return net;
	}

	public static ArrayList<String> getMascara() {
		return mascara;
	}

	public static ArrayList<String> getDns() {
		return dns;
	}

	public static ArrayList<String> getGateway() {
		return gateway;
	}

	public static int getLease() {
		return lease;
	}

	public static ArrayList<String> getDirDisponibles() {
		return dirDisponibles;
	}

}
