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
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author adgar
 */
public class ServidorEquipo {
    
    private String ipServidor;
    private int puerto;
    private volatile List<Pais> paises;
    //private volatile List<EjecutorPropagacion> hilos;   
    
    private volatile HashMap<String, EjecutorPropagacion> hilos;
    
    private volatile Pais paisMayorProcesamiento;
    private volatile Pais paisMenorProcesamiento;
    
    private volatile Semaphore sem; 

    public ServidorEquipo(List<Pais> paises, int puerto){
        
        this.paises = new ArrayList<>(paises);
        this.puerto = puerto;
        sem = new Semaphore(1); 
        //hilos = new ArrayList<>(); 
        hilos = new HashMap<>(); 
        
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.ipServidor = inetAddress.getHostAddress();
        
        this.paisMayorProcesamiento = null;
        this.paisMenorProcesamiento = null;
        
    }
    
    public void ejecutarModeloPaisesPrecargados(){
        
        System.out.println("Iniciando procesamiento de países precargados en este equipo"); 
        
        try {
                // acquiring the lock
                this.sem.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        for (Pais pais : paises) {

            System.out.println("Recibido localmente el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");

            // create a new thread object 
            EjecutorPropagacion t = new EjecutorPropagacion(pais);
            //hilos.add(t);
            hilos.put(pais.getNombre(), t);

            t.start(); 
            System.out.println("Iniciado nuevo hilo para procesar este país"); 
        }
        
        this.sem.release(); 
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
                            /*EjecutorPropagacion f = hilos.get(i);
                            f.pausar();
                            //f.doStop();
                            Pais pisito = f.getPais();
                            paises.set(0, pisito);
                            //System.out.println(paises.get(0).getPoblacion());
                            System.out.println(paises.get(0).getNombre());*/
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
                        //hilos.add(t);
                        hilos.put(pais.getNombre(), t);

                        //System.out.println("Principal Releases the permit."); 
                        this.sem.release(); 

                        // Invoking the start() method                         
                        t.start(); 
                        System.out.println("Iniciado nuevo hilo para procesar este país"); 
                        
                        //comunicar al broker que el país se recibió
                        
                        ipSender = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setPais(pais);
                        nuevoMensaje.setInstrucccion(6);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );  
                        System.out.println("Enviada confirmación de recepción "
                                + "de país al broker");     
                        
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
                        nuevoMensaje.setIpSender(this.ipServidor);
                        
                        //POR AHORA CON UN RANDOM
                        //Random rand = new Random();
                        //int cpuUsage = rand.nextInt(100);
                        //mensaje.setProcesamientoCPU(new Long(cpuUsage));     
                        
                        long menorProcesamiento = Long.MAX_VALUE;
                        long mayorProcesamiento = 0;
                        
                        this.sem.acquire();
                        
                        for (Pais paisRevision : paises) {
                            if( paisRevision.getPoblacion() < menorProcesamiento ){
                                menorProcesamiento = paisRevision.getPoblacion();
                                this.paisMenorProcesamiento = paisRevision;
                            }
                            if( paisRevision.getPoblacion() > mayorProcesamiento ){
                                mayorProcesamiento = paisRevision.getPoblacion();
                                this.paisMayorProcesamiento = paisRevision;
                            }
                        }
                        
                        this.sem.release();
                        
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstrucccion(3);
                        nuevoMensaje.setProcesamientoInferior(menorProcesamiento);
                        nuevoMensaje.setProcesamientoSuperior(mayorProcesamiento);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );   
                        System.out.println("Enviada información de procesamiento al broker");
                         
                        break;
                        
                    case 7: //broker solicitando un país
                        
                        System.out.println("Recibiendo solicitud para enviar país");
                        
                        this.sem.acquire();
                        
                        EjecutorPropagacion ejecutor;
                        Pais paisSaliente = null;
                        
                        if(mensaje.getInstruccionPais() == 0){
                            
                            ejecutor = hilos.get(this.paisMenorProcesamiento.getNombre());
                            //f.pausar();                                                        
                            //paises.set(0, paisSaliente);                            
                            
                        }else{
                            ejecutor = hilos.get(this.paisMayorProcesamiento.getNombre());                            
                            //f.doStop();
                        }
                        
                        ejecutor.doStop();
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setPais(paisSaliente);
                        
                        this.paisMenorProcesamiento = null;
                        this.paisMayorProcesamiento = null;
                        
                        nuevoMensaje.setInstrucccion(2);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );   
                        System.out.println("Enviado país "+nuevoMensaje.getPais().getNombre()+" al broker");
                        
                        hilos.remove(paisSaliente.getNombre());
                        
                        this.sem.release();
                        
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
    

