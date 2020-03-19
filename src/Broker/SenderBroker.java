/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Broker;

import Entidades.Mensaje;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import EquipoProcesamiento.SenderEquipo;

/**
 *
 * @author adgar
 */
public class SenderBroker {
    
    private String ip;
    private int puerto;

    public SenderBroker(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
    }
    
    public void enviarMensaje(Mensaje mensaje){
        
        Socket socket = null;
        
        try {
            socket = new Socket(this.ip, this.puerto);
        } catch (IOException ex) {
            Logger.getLogger(SenderEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Connected!");

        // get the output stream from the socket.
        OutputStream outputStream = null;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException ex) {
            Logger.getLogger(SenderEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }
        // create an object output stream from the output stream so we can send an object through it
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
        } catch (IOException ex) {
            Logger.getLogger(SenderEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }   

        System.out.println("Sending messages to the ServerSocket");
        try {
            objectOutputStream.writeObject(mensaje); //enviar objeto
        } catch (IOException ex) {
            Logger.getLogger(SenderEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Closing socket and terminating program.");
        try {        
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(SenderEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
