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
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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
        this.sem = new Semaphore(1); 
        this.paises = new ArrayList<>();
        this.paisesPorDistribuir = new ArrayList<>();
        
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.ipServidor = inetAddress.getHostAddress();
    }
    
    public void establecerComunicacionInicialConEquipos(){
        
        System.out.println("Estableciendo comunicación inicial con equipos");
        
        String ipEquipo = null;
        
        for (HashMap.Entry<String, Integer> entry : equipos.entrySet()) {
            
            ipEquipo = entry.getKey();
            
            Mensaje mensaje = new Mensaje();
            mensaje.setIpSender(this.ipServidor);
            mensaje.setPais(null);
            mensaje.setInstrucccion(4);
            
            SenderBroker sender = new SenderBroker(ipEquipo, this.puerto);
            sender.enviarMensaje( mensaje );      
            System.out.println("Enviado mensaje inicial a "+ipEquipo);
        }
        
        //this.monitorearCargaEquipos();
        
    }
    
    public void solicitarCargaEquipos(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                
                System.out.println("Monitor de carga activado");
                
                String ipEquipo = null;
        
                for (HashMap.Entry<String, Integer> entry : equipos.entrySet()) {

                    ipEquipo = entry.getKey();

                    Mensaje mensaje = new Mensaje();
                    mensaje.setIpSender(ipServidor);
                    mensaje.setPais(null);
                    mensaje.setInstrucccion(3);

                    SenderBroker sender = new SenderBroker(ipEquipo, puerto);
                    sender.enviarMensaje( mensaje );      
                    System.out.println("Enviada solicitud de información de "
                            + "procesamiento a "+ipEquipo);
                }
                
            }
        };

        Timer timer = new Timer();
        //timer.schedule(task, new Date(), 3000);
        
        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 20000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 10000;
        
        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
        
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
                            System.out.println("Enviado país "+paisAEnviar.getNombre()+" a "+ipEquipo);
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
        
        System.out.println("Iniciada escucha de mensajes entrantes");
        
        while (true){
            
            Socket s = null;    
              
            try { 
                
                // socket object to receive incoming client requests 
                s = ss.accept(); 
                
                // get the input stream from the connected socket
                InputStream inputStream = s.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                  
                System.out.println("Mensaje entrante proveniente de " + s);
                
                Mensaje mensaje = (Mensaje)objectInputStream.readObject();
                Pais pais = mensaje.getPais();
                
                String ipSender = null;
                int procesamientoCPU, anteriorProcesamientoCPU;
                
                switch(mensaje.getInstrucccion()) {
                    
                    case 4: //recibiendo primera comunicación con un equipo
                        
                        System.out.println("Recibiendo respuesta inicial de equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();

                        this.sem.acquire();

                        anteriorProcesamientoCPU = equipos.get(ipSender);

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, anteriorProcesamientoCPU, procesamientoCPU);

                        this.sem.release();                 
                        
                        System.out.println("Actualizada información del equipo");
                        
                      break;
                      
                    case 2: //recibiendo país desde un equipo
                        
                        System.out.println("Recibiendo país desde el equipo para distribuir");
                      
                        this.sem.acquire();

                        paisesPorDistribuir.add(pais);

                        this.sem.release();

                        System.out.println("Recibido el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");
                        
                      break;
                      
                    case 3: //recibiendo reporte de rendimiento
                        
                        System.out.println("Recibiendo información de procesamiento del equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();

                        this.sem.acquire();

                        anteriorProcesamientoCPU = equipos.get(ipSender);

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, anteriorProcesamientoCPU, procesamientoCPU);

                        this.sem.release();
                        
                        System.out.println("Actualizada información del equipo");
                        
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

    public void iniciarEscuchaServidor(){
        Runnable task1 = () -> { this.iniciarEscucha();};      
        Thread t1 = new Thread(task1);
        t1.start();
    }
    
}
