/*
  * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;


/**
 * Clase con la información del cliente asociado con
 * dirección MAC
 * dirección IP
 * Máscara de subred
 * DNS
 * tiempo de arrendamiento
 *
 * @author Daniel Serrano
 */
public class Clientes {

    String idCliente;  // direccion MAC
    private String dirIP;
    private String mask;
    private String DNS;
    private String gateway;
    int lease;
    private boolean forzado;

    /**
     * Constructor de la clase cliente
     * @param idCliente identificador del cliente Direccion MAC
     */

    public Clientes (String idCliente, String dirIP){
        this.idCliente = idCliente;
        this.dirIP = dirIP;
        mask = "";
        DNS = "";
        lease = 0;
        forzado = false;
    }


    //----------------------------------------------------------------------------\\
    //                           MÉTODOS GET Y SET                                \\
    //----------------------------------------------------------------------------\\

    

    public String getDNS() {
        return DNS;
    }

    public void setDNS(String DNS) {
        this.DNS = DNS;
    }

    public String getDirIP() {
        return dirIP;
    }

    public void setDirIP(String dirIP) {
        this.dirIP = dirIP;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public int getLease() {
        return lease;
    }

    public void setLease(int lease) {
        this.lease = lease;
    }

    public String getMask() {
        return mask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
    
    
    
    public void setForzado (boolean x){
        this.forzado = x;
    }
    
    public boolean getForzado (){
        return this.forzado;
    }
    
    public void setMask(String mask) {
        this.mask = mask;
    }
    
   

}
