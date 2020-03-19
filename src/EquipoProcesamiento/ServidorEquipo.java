/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EquipoProcesamiento;

import Broker.ServidorBroker;
import Entidades.Pais;
import Entidades.Mensaje;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.*;
import ModeloPropagacion.EjecutorPropagacion;

/**
 *
 * @author adgar
 */
public class ServidorEquipo {
    
    private String ipServidor;
    private int puerto;
    private volatile List<Pais> paises;
    private volatile List<EjecutorPropagacion> hilos;    
    
    private volatile Semaphore sem; 

    public ServidorEquipo(List<Pais> paises, int puerto){
        
        this.paises = new ArrayList<>(paises);
        this.puerto = puerto;
        sem = new Semaphore(1); 
        hilos = new ArrayList<>(); 
        
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.ipServidor = inetAddress.getHostAddress();
        
    }
    
    public void ejecutarModeloPaisesPrecargados(){
        
        for (Pais pais : paises) {
        
            try {
                // acquiring the lock
                this.sem.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("Received "+pais.getNombre()+" con "+pais.getPoblacion());

            System.out.println("Assigning new thread for this client"); 

            // create a new thread object 
            EjecutorPropagacion t = new EjecutorPropagacion(pais);
            hilos.add(t);

            this.sem.release(); 

            t.start(); 
        }
    }
    
    public void activarMonitor(){
        
        Thread thread = new Thread(){
            public void run(){
                System.out.println("Monitor activado");
                while(true){
                    try {
                        //System.out.println("Mirador waiting for a permit."); 
                        sem.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        for (int i = 0; i < hilos.size(); i++) {
                            EjecutorPropagacion f = hilos.get(i);
                            f.pausar();
                            //f.doStop();
                            Pais pisito = f.getPais();
                            paises.set(0, pisito);
                            //System.out.println(paises.get(0).getPoblacion());
                            System.out.println(paises.get(0).getNombre());
                        }
                        //System.out.println("Mirador Releases the permit.");
                        sem.release();
                    }
            }
          };

        thread.start();
        
    }
    
    public void iniciarEscucha(){
        
        ServerSocket ss = null;
        
        try {
            ss = new ServerSocket(puerto);
        } catch (IOException ex) {
            Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("ServerSocket awaiting connections...");
        
        
        while (true){
            
            Socket s = null;    
              
            try { 
                
                // socket object to receive incoming client requests 
                s = ss.accept(); 
                
                // get the input stream from the connected socket
                InputStream inputStream = s.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                  
                System.out.println("A new client is connected : " + s);
                
                Mensaje mensaje = (Mensaje)objectInputStream.readObject();
                Pais pais = mensaje.getPais();
                
                String ipEquipo = null;
                Mensaje nuevoMensaje = null;
                SenderEquipo sender = null;
                
                switch(mensaje.getInstrucccion()) {
                    
                    case 1: //recibiendo país desde broker
                        
                        sem.acquire(); 

                        paises.add(pais);

                        System.out.println("Received "+pais.getNombre()+" con "+pais.getPoblacion());
                        System.out.println("Assigning new thread for this client"); 

                        // create a new thread object 
                        EjecutorPropagacion t = new EjecutorPropagacion(pais);
                        hilos.add(t);

                        //System.out.println("Principal Releases the permit."); 
                        sem.release(); 

                        // Invoking the start() method 
                        t.start(); 
                        
                        break;
                        
                     case 4: //comunicación inicial con broker
                         
                        ipEquipo = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        mensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstrucccion(4);

                        sender = new SenderEquipo(ipEquipo, this.puerto);
                        sender.enviarMensaje( mensaje );  
                         
                        break;
                        
                     case 3: //reportar rendimiento
                         
                        ipEquipo = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        mensaje.setIpSender(this.ipServidor);
                        mensaje.setProcesamientoCPU(32);
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstrucccion(3);

                        sender = new SenderEquipo(ipEquipo, this.puerto);
                        sender.enviarMensaje( mensaje );    
                         
                        break;
                        
                }
                
            } 
            catch (Exception e){ 
                try { 
                    s.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
                }
                e.printStackTrace(); 
            } 
        } 
        
    }
       
        
}
    

