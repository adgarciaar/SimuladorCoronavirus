/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Broker;

import Entidades.Equipo;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javafx.util.Pair;

/**
 *
 * @author adgar
 */
public class ServidorBroker {
    
    private String ipServidor;
    private int puerto;
    private volatile List<Pais> paises;
    
    private volatile HashMap< String, Pair<Long, Long> > procesamientoEquipos;
    
    private volatile HashMap<String, Long> equipos;
    private volatile HashMap<String, Equipo> notificacionesEquipos;
    
    private volatile HashMap<String, Pais> paisesPorDistribuir;
    //private volatile HashMap<String, String> equiposADistribuir;
    
    private volatile String equipoADistribuir;
    
    private Semaphore sem; 

    public ServidorBroker(int puerto, HashMap<String, Long> equipos) {
        
        this.puerto = puerto;
        this.equipos = new HashMap<>(equipos);
        this.notificacionesEquipos = new HashMap<>();
        
        String ipEquipo;
        
        for (HashMap.Entry<String, Long> entry : equipos.entrySet()) {  
            
            ipEquipo = entry.getKey();
            
            Equipo equipo = new Equipo();
            equipo.setActivo(false);
            equipo.setNotificacionReporteCargaEnviada(false);
            
            this.notificacionesEquipos.put(ipEquipo, equipo);
        }
        
        this.sem = new Semaphore(1); 
        this.paises = new ArrayList<>();
        this.paisesPorDistribuir = new HashMap<>();
        
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.ipServidor = inetAddress.getHostAddress();
        
        this.procesamientoEquipos = new HashMap();
        
        this.equipoADistribuir = null;
        
        //this.equiposADistribuir = new HashMap();
    }
    
    public void establecerComunicacionInicialConEquipos(){
        
        System.out.println("Estableciendo comunicación inicial con equipos");
        
        String ipEquipo = null;
        
        try {
            sem.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (HashMap.Entry<String, Long> entry : equipos.entrySet()) {
            
            ipEquipo = entry.getKey();
            
            Mensaje mensaje = new Mensaje();
            mensaje.setIpSender(this.ipServidor);
            mensaje.setPais(null);
            mensaje.setInstrucccion(4);
            
            SenderBroker sender = new SenderBroker(ipEquipo, this.puerto);
            sender.enviarMensaje( mensaje );      
            System.out.println("Enviado mensaje inicial a "+ipEquipo);
            
            Equipo equipo = this.notificacionesEquipos.get(ipEquipo);
            equipo.setNotificacionReporteCargaEnviada(true);
            this.notificacionesEquipos.replace(ipEquipo, equipo);
            
        }
        
        sem.release();
        
    }
    
    public void solicitarCargaEquipos(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                
                System.out.println("Monitor de carga activado");
                
                String ipEquipo = null;
                
                try {
                    sem.acquire();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
                }
        
                for (HashMap.Entry<String, Long> entry : equipos.entrySet()) {

                    ipEquipo = entry.getKey();

                    Mensaje mensaje = new Mensaje();
                    mensaje.setIpSender(ipServidor);
                    mensaje.setPais(null);
                    mensaje.setInstrucccion(3);

                    SenderBroker sender = new SenderBroker(ipEquipo, puerto);
                    sender.enviarMensaje( mensaje );      
                    System.out.println("Enviada solicitud de información de "
                            + "procesamiento a "+ipEquipo);
                    
                    Equipo equipo = notificacionesEquipos.get(ipEquipo);
                    equipo.setActivo(false);
                    equipo.setNotificacionReporteCargaEnviada(true);
                    notificacionesEquipos.replace(ipEquipo, equipo);
                }
                
                sem.release();
                
            }
        };

        Timer timer = new Timer();
        //timer.schedule(task, new Date(), 3000);
        
        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 30000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 10000;
        
        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
        
    }
    
    public void solicitarPaisesParaDistribuir(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                
                String ipEquipo = null;
                String ipEquipoMenor = null, ipEquipoMayor = null;
                long procesamiento;                
                long menor = Long.MAX_VALUE, mayor = 0;
                
                try {
                    sem.acquire();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                for (HashMap.Entry<String, Long> entry : equipos.entrySet()) {

                    ipEquipo = entry.getKey();
                    procesamiento = entry.getValue();
                    if( procesamiento<menor && notificacionesEquipos.get(ipEquipo).isActivo() ){
                        ipEquipoMenor = ipEquipo;
                        menor = procesamiento;
                    }
                    if( procesamiento>menor && notificacionesEquipos.get(ipEquipo).isActivo() ){
                        ipEquipoMenor = ipEquipo;
                        mayor = procesamiento;
                    }   
                    
                }
                
                
                
                if(ipEquipoMenor != null && ipEquipoMayor != null){
                    
                    //revisar qué países pedir
                    
                    //Pair<Long, Long> parejaMenor = procesamientoEquipos.get(ipEquipoMenor);
                    Pair<Long, Long> parejaMayor = procesamientoEquipos.get(ipEquipoMayor);
                    
                    //long menorDelEquipoMenor = parejaMenor.getKey();
                    //long mayorDelEquipoMenor = parejaMenor.getValue();
                    long menorDelEquipoMayor = parejaMayor.getKey();
                    long mayorDelEquipoMayor = parejaMayor.getValue();
                    
                    long nuevoProcesamientoMenor1 = menor + mayorDelEquipoMayor;
                    long nuevoProcesamientoMenor2 = menor + menorDelEquipoMayor;
                    
                    long nuevoProcesamientoMayor1 = mayor - mayorDelEquipoMayor;
                    long nuevoProcesamientoMayor2 = mayor - menorDelEquipoMayor;
                    
                    if( Math.abs(nuevoProcesamientoMayor1 - nuevoProcesamientoMenor1) < Math.abs(mayor-menor)
                            && Math.abs(nuevoProcesamientoMayor1 - nuevoProcesamientoMenor1) < 
                            Math.abs(nuevoProcesamientoMayor2 - nuevoProcesamientoMenor2) ){
                        
                        Mensaje mensaje = new Mensaje();
                        mensaje.setIpSender(ipEquipoMayor);
                        mensaje.setPais(null);
                        mensaje.setInstrucccion(7);
                        mensaje.setInstruccionPais(1); //para obtener el país con mayor procesamiento
                        
                        equipoADistribuir = ipEquipoMenor;         

                        SenderBroker sender = new SenderBroker(ipEquipoMayor, puerto);
                        sender.enviarMensaje( mensaje );
                        System.out.println("Enviada solicitud para solicitar país desde equipo "+ipEquipoMayor);
                        
                    }else{
                        
                        if( Math.abs(nuevoProcesamientoMayor2 - nuevoProcesamientoMenor2) < (mayor-menor) 
                            && Math.abs(nuevoProcesamientoMayor2 - nuevoProcesamientoMenor2) < 
                            Math.abs(nuevoProcesamientoMayor1 - nuevoProcesamientoMenor1) ){
                            
                            Mensaje mensaje = new Mensaje();
                            mensaje.setIpSender(ipEquipoMayor);
                            mensaje.setPais(null);
                            mensaje.setInstrucccion(7);
                            mensaje.setInstruccionPais(0); //para obtener el país con menor procesamiento
                            
                            equipoADistribuir = ipEquipoMenor;

                            SenderBroker sender = new SenderBroker(ipEquipoMayor, puerto);
                            sender.enviarMensaje( mensaje );
                            System.out.println("Enviada solicitud para solicitar país desde equipo "+ipEquipoMayor);
                        
                        }                        
                        
                        equipoADistribuir = null;
                        
                    }
                   
                sem.release();
                    
                }
                
            }
        };

        Timer timer = new Timer();
        //timer.schedule(task, new Date(), 3000);
        
        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 30000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 20000;
        
        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
    }
    
    public void realizarDistribucion(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                
                    System.out.println("Distribuidor activado");
                    
                    if(paisesPorDistribuir.size()>0){
                    
                        try {

                            sem.acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        Pais paisAEnviar = null; 
                        
                        for (HashMap.Entry<String, Pais> entry : paisesPorDistribuir.entrySet()) {

                            paisAEnviar = entry.getValue();
                            
                        }
                        
                        Mensaje mensaje = new Mensaje();
                        
                        if(equipoADistribuir == null){//entonces decidir aleatoriamente                            
                            
                            //Random rand = new Random();
                            //int cpuUsage = rand.nextInt(equipos.size());
                            
                            String equipoAUsar = null;
                            long menorProcesamiento = Long.MAX_VALUE;
                            for (HashMap.Entry<String, Long> entrada : equipos.entrySet()) {
                                String ipEquipoTemp = entrada.getKey();
                                if( entrada.getValue() < menorProcesamiento && notificacionesEquipos.get(ipEquipoTemp).isActivo() ) {
                                    menorProcesamiento = entrada.getValue();
                                    equipoAUsar = ipEquipoTemp;
                                }
                            }
                            
                            equipoADistribuir = equipoAUsar;                            
                            
                        }else{
                            mensaje.setIpSender(equipoADistribuir);
                        }
                        
                        if(equipoADistribuir != null){
                            
                            mensaje.setIpSender(equipoADistribuir);
                            mensaje.setPais(paisAEnviar);
                            mensaje.setInstrucccion(1);

                            SenderBroker sender = new SenderBroker(equipoADistribuir, puerto);
                            sender.enviarMensaje( mensaje );
                            System.out.println("Enviado país "+paisAEnviar.getNombre()+" a "+equipoADistribuir);

                            equipoADistribuir = null;
                        
                        }
                        
                        /*
                        
                        Pais paisAEnviar = null; 
                        
                        for (HashMap.Entry<String, Pais> entry : paisesPorDistribuir.entrySet()) {

                            paisAEnviar = entry.getValue();
                            
                            Mensaje mensaje = new Mensaje();
                            mensaje.setIpSender(ipServidor);
                            mensaje.setPais(paisAEnviar);
                            mensaje.setInstrucccion(1);

                            String ipEquipo = null;
                            Long menor = 0L;

                            for (HashMap.Entry<String, Long> entrada : equipos.entrySet()) {
                                String ip = entrada.getKey();
                                Long procesamientoCPU = entrada.getValue();
                                if ( procesamientoCPU < menor && notificacionesEquipos.get(ip).isActivo() ){
                                    menor = procesamientoCPU;
                                    ipEquipo = ip;
                                }
                            }
                            
                            if(ipEquipo != null){
                                SenderBroker sender = new SenderBroker(ipEquipo, puerto);
                                sender.enviarMensaje( mensaje );
                                System.out.println("Enviado país "+paisAEnviar.getNombre()+" a "+ipEquipo);
                            }
                    
                        }

                        */

                        sem.release();
                    
                    }
                
            }
          };

        Timer timer = new Timer();

        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 30000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 30000;

        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
        
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
                Long procesamientoCPU;
                Equipo equipo = null;
                
                Pair <Long, Long> pareja;
                
                switch(mensaje.getInstrucccion()) {
                    
                    case 4: //recibiendo primera comunicación con un equipo
                        
                        System.out.println("Recibiendo respuesta inicial de equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();

                        this.sem.acquire();

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, procesamientoCPU);
                        
                        equipo = this.notificacionesEquipos.get(ipSender);
                        equipo.setActivo(true);
                        this.notificacionesEquipos.replace(ipSender, equipo);
                        
                        pareja = new Pair<>(0L, 0L); 
                        this.procesamientoEquipos.put(ipSender, pareja);

                        this.sem.release();                 
                        
                        System.out.println("Actualizada información del equipo");
                        
                      break;
                      
                    case 2: //recibiendo país desde un equipo
                        
                        System.out.println("Recibiendo país desde el equipo para distribuir");
                      
                        this.sem.acquire();
                        
                        paisesPorDistribuir.put(pais.getNombre(), pais);

                        this.sem.release();

                        System.out.println("Recibido el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");
                        
                      break;
                      
                    case 3: //recibiendo reporte de rendimiento
                        
                        System.out.println("Recibiendo información de procesamiento del equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();

                        this.sem.acquire();

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, procesamientoCPU);
                        
                        equipo = this.notificacionesEquipos.get(ipSender);
                        equipo.setActivo(true);
                        this.notificacionesEquipos.replace(ipSender, equipo);                        
                        
                        pareja = new Pair <> (mensaje.getProcesamientoInferior(), mensaje.getProcesamientoSuperior()); 
                        this.procesamientoEquipos.replace(ipSender, pareja);

                        this.sem.release();
                        
                        System.out.println("Actualizada información del equipo");
                        
                        break;
                        
                    case 6: //confirmación de país recibido para procesar
                        
                        System.out.println("Recibiendo confirmación de procesamiento de país "+pais.getNombre());
                        
                        String paisRecibido = pais.getNombre();
                        
                        this.sem.acquire();
                        
                        //eliminar país que se está ejecutando de la lista de
                        //países que se van a distribuir
                        this.paisesPorDistribuir.remove(paisRecibido);
                        
                        this.sem.release();
                        
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
