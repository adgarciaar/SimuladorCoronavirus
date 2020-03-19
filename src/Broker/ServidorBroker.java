/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Broker;

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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import EquipoProcesamiento.ServidorEquipo;

/**
 *
 * @author adgar
 */
public class ServidorBroker {
    
    private String ipServidor;
    private int puerto;
    private volatile List<Pais> paises;
    private volatile HashMap<String, Integer> equipos;
    private volatile List<Pais> paisesPorDistribuir;
    
    private Semaphore sem; 

    public ServidorBroker(int puerto, HashMap<String, Integer> equipos) {
        
        this.puerto = puerto;
        this.equipos = new HashMap<>(equipos);
        sem = new Semaphore(1); 
        paises = new ArrayList<>();
        
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.ipServidor = inetAddress.getHostAddress();
    }
    
    public void establecerComunicacionInicialConEquipos(){
        
        String ipEquipo = null;
        
        for (HashMap.Entry<String, Integer> entry : equipos.entrySet()) {
            
            ipEquipo = entry.getKey();
            
            Mensaje mensaje = new Mensaje();
            mensaje.setIpSender(this.ipServidor);
            mensaje.setPais(null);
            mensaje.setInstrucccion(4);
            
            SenderBroker sender = new SenderBroker(ipEquipo, this.puerto);
            sender.enviarMensaje( mensaje );            
        }
        
    }
    
    public void iniciarDistribuidor(){
        
        Thread thread = new Thread(){
            @Override
            public void run(){
                System.out.println("Distribuidor activado");
                
                while(true){
                    
                    if(paisesPorDistribuir.size()>0){
                    
                        try {

                            sem.acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        for (int i = 0; i < paisesPorDistribuir.size(); i++) {

                            Pais paisAEnviar = paisesPorDistribuir.get(i);
                            
                            Mensaje mensaje = new Mensaje();
                            mensaje.setIpSender(ipServidor);
                            mensaje.setPais(paisAEnviar);
                            mensaje.setInstrucccion(1);

                            String ipEquipo = null;
                            int menor = 0;

                            for (HashMap.Entry<String, Integer> entry : equipos.entrySet()) {
                                String ip = entry.getKey();
                                Integer procesamientoCPU = entry.getValue();
                                if ( procesamientoCPU < menor ){
                                    menor = procesamientoCPU;
                                    ipEquipo = ip;
                                }
                            }

                            SenderBroker sender = new SenderBroker(ipEquipo, puerto);
                            sender.enviarMensaje( mensaje );
                        }

                        sem.release();
                    
                    }
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
                
                String ipSender = null;
                int procesamientoCPU, anteriorProcesamientoCPU;
                
                switch(mensaje.getInstrucccion()) {
                    
                    case 4: //recibiendo primera comunicación con un equipo
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();

                        sem.acquire();

                        anteriorProcesamientoCPU = equipos.get(ipSender);

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, anteriorProcesamientoCPU, procesamientoCPU);

                        sem.release();
                        
                      break;
                      
                    case 2: //recibiendo país desde un equipo
                      
                        sem.acquire();

                        paisesPorDistribuir.add(pais);

                        sem.release();

                        System.out.println("Received "+pais.getNombre()+" con "+pais.getPoblacion());
                        System.out.println("Assigning new thread for this client"); 
                        
                      break;
                      
                    case 3: //recibiendo reporte de rendimiento
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();

                        sem.acquire();

                        anteriorProcesamientoCPU = equipos.get(ipSender);

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, anteriorProcesamientoCPU, procesamientoCPU);

                        sem.release();
                        
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
