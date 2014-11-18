/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;

import java.util.StringTokenizer;

/**
 * Clase que contiene los campos del paquete DHCP
 *
 * @author Daniel Serrano
 */
public class PaqueteDHCP {

	private byte[] data;

	// Constantes de las opciones DHCP

	public static final int DISCOVER = 1;
	public static final int OFFER = 2;
	public static final int REQUEST = 3;
	public static final int DECLINE = 4;
	public static final int ACK = 5;
	public static final int NACK = 6;
	public static final int RELEASE = 7;
	public static final int INFORM = 8;

	/**
	 * Constructores de la clase Paquete DHCP
	 *
	 */

	public PaqueteDHCP() {
	}

	public PaqueteDHCP(byte[] data) {
		this.data = data;
	}

	public PaqueteDHCP(byte OP, byte HTYPE, byte HLEN, byte HOPS, byte[] XID,
			byte[] SECS, byte[] FLAGS, byte[] SIADDR, byte[] GIADDR,
			byte[] CHADDR, byte[] MAGICOOKIE) {
		data = new byte[350];
		this.setOP(OP);
		this.setHTYPE(HTYPE);
		this.setHLEN(HLEN);
		this.setHOPS(HOPS);
		this.setXID(XID);
		this.setSECS(SECS);
		this.setFLAGS(FLAGS);
		this.setSIADDR(SIADDR);
		this.setGIADDR(GIADDR);
		this.setCHADDR(CHADDR);
		this.setMagicCookie(MAGICOOKIE);
	}

	public void setData(byte[] d) {
		data = d;
	}

	public byte[] getData() {
		return data;
	}

	// Agrega el campo OP al datagrama
	public void setOP(byte s) {
		data = new byte[1];
		data[0] = s;
	}

	// Agrega el campo HTYPE al datagrama
	public void setHTYPE(byte s) {
		byte[] d = new byte[2];
		d[0] = data[0];
		d[1] = s;
		data = d;
	}

	// Agrega el campo HLEN al datagrama
	public void setHLEN(byte s) {
		byte[] b = new byte[1];
		b[0] = s;
		data = this.concatDataDeAaB(2, 3, b);
	}

	// Agrega el campo HOPS al datagrama
	public void setHOPS(byte s) {
		byte[] b = new byte[1];
		b[0] = s;
		data = this.concatDataDeAaB(3, 4, b);
	}

	// Agrega el campo XID al datagrama
	public void setXID(byte[] s) {
		data = this.concatDataDeAaB(4, 8, s);
	}

	// Agrega el campo SECS al datagrama
	public void setSECS(byte[] s) {
		data = this.concatDataDeAaB(8, 10, s);
	}

	// Agrega el campo FLAGS al datagrama
	public void setFLAGS(byte[] s) {
		data = this.concatDataDeAaB(10, 12, s);
	}

	// Agrega el campo SIADDR al datagrama
	public void setCIADDR(byte[] s) {
		data = this.concatDataDeAaB(12, 16, s);
	}

	// Agrega el campo GIADDR al datagrama
	public void setYIADDR(byte[] s) {
		data = this.concatDataDeAaB(16, 20, s);
	}

	// Agrega el campo CHADDR al datagrama
	public void setSIADDR(byte[] s) {
		data = this.concatDataDeAaB(20, 24, s);
	}

	public void setGIADDR(byte[] s) {
		data = this.concatDataDeAaB(24, 28, s);
	}

	public void setCHADDR(byte[] s) {
		data = this.concatDataDeAaB(28, 34, s); // cubre los 16 bytes del chaddr
	}

	public void setSName(byte[] s) {
		data = this.concatDataDeAaB(44, 108, s);
	}

	public void setFile(byte[] s) {
		data = this.concatDataDeAaB(108, 236, s);
	}

	public void setMagicCookie(byte[] s) {
		data = this.concatDataDeAaB(236, 240, s); // justo despues del campo
													// file comoenzan las
													// opciones
	}

	public void addDHCPOPTION(byte[] s) {
		if (data.length >= 240) {
			data = this.concatBytes(data, s);
		}
	}

	// Obtiene la longitud del paquete mas no del data
	public int getLengthData() {
		return data.length;
	}

	// Obtiene el String dado un arreglo de bytes en hexadecimal
	public String getStringHexa(byte[] d) {
		byte[] datos2 = d;
		String sdatos2 = new String();
		for (int i = 0; i < datos2.length; i++) {
			sdatos2 = (sdatos2 + String.format("%02X%s", datos2[i],
					(i < datos2.length - 1) ? "-" : ""));
		}
		return sdatos2;
	}

	public String getIPHexa(byte[] d) {
		byte[] datos2 = d;
		String sdatos2 = new String();
		for (int i = 0; i < datos2.length; i++) {
			sdatos2 = (sdatos2 + String.format("%02X%s", datos2[i],
					(i < datos2.length - 1) ? ":" : ""));
		}
		return sdatos2;
	}

	// Obtiene el String dado un byte en hexadecimal
	public String getStringHexa(byte d) {
		byte[] datos2 = new byte[1];
		datos2[0] = d;
		String sdatos2 = new String();
		sdatos2 = (sdatos2 + String.format("%02X%s", datos2[0],
				(0 < datos2.length - 1) ? "" : ""));
		return sdatos2;
	}

	public byte[] getByteData() {
		return data;
	}

	public String getStringData() {
		return getStringHexa(data);
	}

	// Agrega el campo OP al datagrama
	public byte getOP() {
		return data[0];
	}

	// Agrega el campo HTYPE al datagrama
	public byte getHTYPE() {
		return data[1];
	}

	// Agrega el campo HLEN al datagrama
	public byte getHLEN() {
		return data[2];
	}

	// Agrega el campo HOPS al datagrama
	public byte getHOPS() {
		return data[3];
	}

	// Agrega el campo XID al datagrama
	public byte[] getXID() {
		return deAaBdeDATA(4, 8);
	}

	// Agrega el campo SECS al datagrama
	public byte[] getSECS() {
		return deAaBdeDATA(8, 10);
	}

	// Agrega el campo FLAGS al datagrama
	public byte[] getFLAGS() {
		return deAaBdeDATA(10, 12);
	}

	// Agrega el campo SIADDR al datagrama
	public byte[] getCIADDR() {
		return deAaBdeDATA(12, 16);
	}

	// Agrega el campo YIADDR al datagrama
	public byte[] getYIADDR() {
		return deAaBdeDATA(16, 20);
	}

	// Agrega el campo SIADDR al datagrama
	public byte[] getSIADDR() {
		return deAaBdeDATA(20, 24);
	}

	// Agrega el campo GIADDR al datagrama
	public byte[] getGIADDR() {
		return deAaBdeDATA(24, 28);
	}

	// Agrega el campo CHADDR al datagrama
	public byte[] getCHADDR() {
		return deAaBdeDATA(28, 34);
	}

	// Agrega el campo MagicCookie al datagrama
	public byte[] getMagicCookie() {
		return deAaBdeDATA(236, 240);
	}

	// escribe todas las opciones en el data
	public void writeDHCPOptions(OpcionDHCP[] dh) {
		byte[] b = wDHCPOption(dh[0]);
		for (int i = 1; i < dh.length - 2; i++) {
			b = concatBytes(b, wDHCPOption(dh[i]));
		}
		data = concatDataDeAaB(240, (240 + b.length), b);
		finalizarDatagrama();
	}

	public void finalizarDatagrama() {
		// se pone al final de todo el datagrama el byte 0xff
		byte[] f = new byte[3];
		f[0] = (byte) 0xff;
		f[1] = (byte) 0x00;
		f[2] = (byte) 0x00;
		data = concatDataDeAaB(data.length, data.length + 3, f);
	};

	// Devuelve un objeto DHCPoption en bytes.
	private byte[] wDHCPOption(OpcionDHCP dp) {
		return concatBytes(dp.getOption(),
				concatBytes(dp.getLenght(), dp.getValues()));
	}

	public OpcionDHCP[] readDHCPOptions() {
		OpcionDHCP[] op = new OpcionDHCP[50];
		int i = 0, init = 240; // init es el byte en el cual empiezan las
								// opciones
		while (true) {
			op[i] = buscaOptions(init);
			if (op[i].getIntOption() == 255) {
				break;
			}
			init = op[i].next(init, data);
			i++;
		}
		OpcionDHCP[] opciones = new OpcionDHCP[i];
		for (int j = 0; j < i; j++) {
			opciones[j] = op[j];
		}
		return opciones;
	};

	public int lenghtOpciones() {
		return readDHCPOptions().length;
	}

	// Busca las opciones
	private OpcionDHCP buscaOptions(int index) {
		OpcionDHCP o = new OpcionDHCP();
		o.setOption(data[index]);
		o.setLenght(data[index + 1]);
		if (o.getIntLenght() == 1) {
			o.setValues(data[index + 2]);
		} else {
			o.setValues(deAaBdeDATA(index + 2, index + 2 + o.getIntLenght()));
		}
		return o;
	}

	// Rango que va desde A hasta B-1
	private byte[] deAaBdeDATA(int A, int B) {
		byte[] b = new byte[B - A];
		int j = 0;
		for (int i = A; i < B; i++) {
			b[j] = data[i];
			j++;
		}
		return b;
	}

	// Llena en data los valores que esten en el Rango que va desde A hasta B-1
	private void deAaBdeDATA(int A, int B, byte[] b) {
		int j = 0;
		for (int i = A; i < B; i++) {
			data[i] = b[j];
			j++;
		}
	}

	@SuppressWarnings("empty-statement")
	private byte[] concatDataDeAaB(int A, int B, byte[] b) {
		byte[] d = new byte[B];
		int j = 0;
		for (int i = 0; i < B; i++) {
			if (i < A) {
				if (i >= data.length) {
					d[i] = (byte) 0x00;
				} else {
					d[i] = data[i];
				}
			} else {
				d[i] = b[j];
				j++;
			}
		}
		return d;
	}

	@SuppressWarnings("empty-statement")
	public byte[] concatBytes(byte[] A, byte[] B) {
		int c = A.length + B.length;
		int j = 0;
		byte[] d = new byte[c];
		for (int i = 0; i < c; i++) {
			if (i < A.length) {
				d[i] = A[i];
			} else {
				d[i] = B[j];
				j++;
			}
			;
		}
		return d;
	}

	public byte[] concatBytes(byte A, byte[] B) {
		byte[] a = new byte[1];
		a[0] = A;
		return concatBytes(a, B);
	}

	public byte[] concatBytes(byte[] A, byte B) {
		byte[] b = new byte[1];
		b[0] = B;
		return concatBytes(A, b);
	}

	// Busca el valor en option, si no existe retorna 0
	public OpcionDHCP existInOption(int valor) {
		// OpcionDHCP[] o = new OpcionDHCP[this.readDHCPOptions().length];
		OpcionDHCP op = new OpcionDHCP();
		boolean b = false;
		OpcionDHCP[] o = this.readDHCPOptions();
		for (int i = 0; i < o.length - 1; i++) {
			if (o[i].getIntOption() == valor) {
				op = o[i];
				b = true;
				break;
			}
			// if(o[i].getIntOption()==valor){}
		}
		if (!b) {
			op.setOption((byte) 0x00);
			op.setLenght((byte) 0x00);
			op.setValues((byte) 0x00);
		}
		return op;
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

}
