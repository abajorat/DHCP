/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;

/**
 * Clase que maneja las opciones DHCP
 * 
 * @author Daniel Serrano
 */
public class OpcionDHCP {

	private byte OPTION;
	private byte LENGHT;
	private byte[] VALUES;
	private byte VALUE;

	/**
	 * Constructores de la clase OpcionDHCP
	 *
	 */

	public OpcionDHCP() {
	}

	public OpcionDHCP(byte op, byte le, byte[] values) {
		OPTION = op;
		LENGHT = le;
		VALUES = values;
	}

	public OpcionDHCP(byte op, byte le, byte value) {
		OPTION = op;
		LENGHT = le;
		VALUE = value;
	}

	/**
	 * Gets y Sets de la clase
	 *
	 */

	public void setOption(byte option) {
		OPTION = option;
	};

	public void setLenght(byte lenght) {
		LENGHT = lenght;
	};

	public void setValues(byte[] values) {
		VALUES = values;
	};

	public void setValues(byte value) {
		VALUES = new byte[1];
		VALUES[0] = value;
		VALUE = value;
	};

	public byte getOption() {
		return OPTION;
	};

	public int getIntOption() { // Devuelve el valor en formato decimal
		return Integer.parseInt(new PaqueteDHCP().getStringHexa(OPTION), 16);
	};

	public byte getLenght() {
		return LENGHT;
	};

	public int getIntLenght() {// Devuelve el valor en formato decimal
		return (Integer.parseInt(new PaqueteDHCP().getStringHexa(LENGHT), 16));
	};

	public int getIntValue() {// Devuelve el valor en formato decimal
		return (Integer.parseInt(new PaqueteDHCP().getStringHexa(VALUE), 16));
	};

	public String getStrValues() {
		String b = new String();
		for (int i = 0; i < getIntLenght(); i++) {
			b = b + new PaqueteDHCP().getStringHexa(VALUES[i]);
		}
		return b;
	};

	// siguiente opcion
	public int next(int index, byte[] data) {
		int i = 240, l = 0;
		if (index < 240) {
			index = 240;
		} else {
			while (i <= index) {
				i = i
						+ 2
						+ (Integer.parseInt(
								new PaqueteDHCP().getStringHexa(data[i + 1]),
								16));
				if (data.length < i) {
					i = -2;
				}
			}
		}
		return i;
	};

	public byte getValue() {
		return VALUES[0];
	};

	public byte[] getValues() {
		return VALUES;
	}

}
