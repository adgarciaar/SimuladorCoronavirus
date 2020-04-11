/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Broker;

import Entidades.MensajeGeneral;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author adgar
 */

//esta clase permite crear un socket con el cual un broker puede enviar un
//objeto de tipo Mensaje a un equipo de procesamiento
public class SenderBroker {
    
    private String ip; //ip del equipo con que se va a comunicar
    private int puerto; //puerto por el que se va a comunicar

    public SenderBroker(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
    }
    
    //función que envía un objeto de tipo Mensaje al equipo especificado
    public void enviarMensaje(MensajeGeneral mensaje){
        
        Socket socket = null;
        
        try {
            //socket = new Socket(this.ip, this.puerto);
            socket = new Socket();
            socket.connect(new InetSocketAddress(this.ip, this.puerto), 2*1000);
            //socket.setSoTimeout(5*1000);
        } catch (IOException ex) {
            System.out.println("Error al abrir socket para comunicación con "+this.ip);
            System.out.println(ex.toString());
            socket = null;
        }
        
        OutputStream outputStream = null;
        
        if(socket != null){
            System.out.println("Socket creado para conectarse con "+this.ip+" por "
                + "medio del puerto "+this.puerto);

            // get the output stream from the socket.            
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException ex) {
                System.out.println("Error abriendo stream para envío");
                outputStream = null;
            }
        }        
        
        ObjectOutputStream objectOutputStream = null;
        
        if (outputStream != null){        
            // create an object output stream from the output stream so we can send an object through it            
            try {
                objectOutputStream = new ObjectOutputStream(outputStream);
            } catch (IOException ex) {
                System.out.println("Error abriendo stream para envío");
                objectOutputStream = null;
            }   
        }
        
        if(objectOutputStream != null){

            System.out.println("Enviando mensaje al equipo");
            try {
                objectOutputStream.writeObject(mensaje); //enviar objeto
            } catch (IOException ex) {
                System.out.println("Error enviando objeto por socket");                
            }
            
            System.out.println("Cerrando socket");
            try {        
                socket.close();
            } catch (IOException ex) {
                System.out.println("Error cerrando socket");
            }
        }
        
    }
    
}
