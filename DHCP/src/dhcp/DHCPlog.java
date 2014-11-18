/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dhcp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Esta clase se encarga de escribir la actividad del servidor en el archivo de
 * texto LogServidor.txt
 * @author Daniel Serrano
 */
public class DHCPlog {


    public static synchronized void reportar(String mensaje){
        File f = new File("LogServidor.txt");
        try{
            PrintWriter fileOut = new PrintWriter(new FileWriter(f,true));
            fileOut.println(mensaje);
            fileOut.close();
        }catch (IOException e){
            System.out.println("Error de escritura:  "+e);

        }
    }

    

}
