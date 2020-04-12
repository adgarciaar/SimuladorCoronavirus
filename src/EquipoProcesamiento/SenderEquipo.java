/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EquipoProcesamiento;

import Entidades.Mensaje;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author adgar
 */

//esta clase permite crear un socket con el cual un equipo puede enviar un
//objeto de tipo Mensaje a un broker
public class SenderEquipo{
    
    private String ip; //ip del broker con que se va a comunicar
    private int puerto; //puerto por el que se va a comunicar

    public SenderEquipo(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
    }
    
    //función que envía un objeto de tipo Mensaje al broker especificado
    public void enviarMensaje(Mensaje mensaje){
        
        Socket socket = null;
        
        try {
            //socket = new Socket(this.ip, this.puerto);
            socket = new Socket();
            socket.connect(new InetSocketAddress(this.ip, this.puerto), 2*1000);
            //socket.setSoTimeout(5*1000);
        } catch (IOException ex) {
            System.out.println("Error al abrir socket");
            System.exit(1);
        }
        System.out.println("Socket creado para conectarse con "+this.ip+" por "
                + "medio del puerto "+this.puerto);

        // get the output stream from the socket.
        OutputStream outputStream = null;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException ex) {
            System.out.println("Error abriendo stream para envío");
            System.exit(1);
        }
        // create an object output stream from the output stream so we can send an object through it
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
        } catch (IOException ex) {
            System.out.println("Error abriendo stream para envío");
            System.exit(1);
        }   

        System.out.println("Enviando mensaje al equipo");
        try {
        	System.out.println("entro enviar mensaje");
            objectOutputStream.writeObject(mensaje); //enviar objeto
        } catch (IOException ex) {
            System.out.println("Error enviando objeto por socket "+ ex);
            //System.exit(1);
        }

        System.out.println("Cerrando socket");
        try {        
            socket.close();
        } catch (IOException ex) {
            System.out.println("Error cerrando socket");
            System.exit(1);
        }
        
    }
    
}
