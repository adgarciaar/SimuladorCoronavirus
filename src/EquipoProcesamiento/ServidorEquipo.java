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
        
        System.out.println("Iniciando procesamiento de países precargados en este equipo"); 
        
        for (Pais pais : paises) {
        
            try {
                // acquiring the lock
                this.sem.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("Recibido localmente el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");

            // create a new thread object 
            EjecutorPropagacion t = new EjecutorPropagacion(pais);
            hilos.add(t);

            this.sem.release(); 

            t.start(); 
            System.out.println("Iniciado nuevo hilo para procesar este país"); 
        }
    }
    
    public void activarMonitor(){
        
        Thread thread = new Thread(){
            @Override
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
        
        ServerSocket serverSocket = null;
        
        try {
            serverSocket = new ServerSocket(puerto);
        } catch (IOException ex) {
            Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Iniciada escucha de mensajes entrantes");        
        
        while (true){
            
            Socket socket = null;    
              
            try { 
                
                // socket object to receive incoming client requests 
                socket = serverSocket.accept(); 
                
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                  
                System.out.println("Mensaje entrante del broker: " + socket);
                
                Mensaje mensaje = (Mensaje)objectInputStream.readObject();
                Pais pais = mensaje.getPais();
                
                String ipSender = null;
                Mensaje nuevoMensaje = null;
                SenderEquipo sender = null;
                
                switch(mensaje.getInstrucccion()) {
                    
                    case 1: //recibiendo país desde broker
                        
                        System.out.println("Recibiendo país para procesar");
                        
                        this.sem.acquire(); 

                        paises.add(pais);

                        System.out.println("Recibido el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");                        

                        // create a new thread object 
                        EjecutorPropagacion t = new EjecutorPropagacion(pais);
                        hilos.add(t);

                        //System.out.println("Principal Releases the permit."); 
                        this.sem.release(); 

                        // Invoking the start() method                         
                        t.start(); 
                        System.out.println("Iniciado nuevo hilo para procesar este país"); 
                        
                        break;
                        
                     case 4: //comunicación inicial con broker
                         
                        System.out.println("Recibiendo mensaje inicial del broker");
                        
                        this.ejecutarModeloPaisesPrecargados();
                        this.activarMonitor();
                         
                        ipSender = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstrucccion(4);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );  
                        System.out.println("Enviada respuesta inicial al broker");
                         
                        break;
                        
                     case 3: //reportar rendimiento
                         
                        System.out.println("Recibiendo solicitud para reportar procesamiento");
                         
                        ipSender = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        mensaje.setIpSender(this.ipServidor);
                        mensaje.setProcesamientoCPU(32);
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstrucccion(3);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( mensaje );   
                        System.out.println("Enviada información de procesamiento al broker");
                         
                        break;
                        
                }
                
            } 
            catch (Exception e){ 
                try { 
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
                }
                e.printStackTrace(); 
            } 
        } 
        
    }
       
    public void iniciarEscuchaServidor(){
        Runnable task1 = () -> { this.iniciarEscucha();};      
        Thread t1 = new Thread(task1);
        t1.start();
    }
        
}
    

