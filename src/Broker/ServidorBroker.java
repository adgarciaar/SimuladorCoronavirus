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
import java.util.Timer;
import java.util.TimerTask;
import javafx.util.Pair;

/**
 *
 * @author adgar
 */
public class ServidorBroker {
    
    private String ipServidor; //guarda la ip de la máquina en que se ejecuta
    private int puerto; //puerto con el que se comunica esta máquina
    private volatile List<Pais> paises; //lista de países que se están ejecutando en las diferentes máquinas
    
    //mapa que guarda tuplas < Nombre de un país, IP del equipo donde se está ejecutando >
    private volatile HashMap<String, String> paisesEnEquipos;
    
    //mapa que guarda duplas < IP de un equipo, Pareja<NúmeroMayorProcesamiento, NúmeroMenorProcesamiento> >
    //para establecer cómo se va a realizar el balanceo de cargas
    private volatile HashMap< String, Pair<Long, Long> > procesamientoEquipos;
    
    //mapa que guarda duplas < IP de un equipo, Procesamiento total en ese equipo >
    //para conocer estados generales de procesamiento de cada equipo
    private volatile HashMap<String, Long> equipos;
    
    //mapa que guarda duplas < IP de un equipo, Datos de ese equipo >
    //para conocer estados de esos equipos
    private volatile HashMap<String, Equipo> notificacionesEquipos;
    
    //mapa que guarda duplas <Nombre de un país, Datos de ese país>
    //para realizar el balanceo de cargas
    private volatile HashMap<String, Pais> paisesPorDistribuir;
    //private volatile HashMap<String, String> equiposADistribuir;
    
    //variable que guarda la IP del equipo al que se va a enviar el próximo agente
    private volatile String equipoADistribuir;
    
    //semáforo para garantizar que las funciones acceden sólo una a la vez
    //a las variables de esta clase, garantizando consistencia
    private Semaphore sem;     
    
    //constructor de la clase, se le pasan el puerto por el que se va a comunicar
    //y un mapa con los equipos precargados con los que se va a comunicar
    public ServidorBroker(int puerto, List<String> equipos) {
        
        this.puerto = puerto;
        this.equipos = new HashMap<>();
        this.notificacionesEquipos = new HashMap<>();
        this.paisesEnEquipos = new HashMap<>();
        
        String ipEquipo;
        //para cada equipo que el broker conoce se inicializan unas variables
        //para conocer su estado
        for(int i=0; i<equipos.size();i++){
            
            //ipEquipo = entry.getKey();
            ipEquipo = equipos.get(i);
            
            Equipo equipo = new Equipo();
            equipo.setActivo(false);
            equipo.setNotificacionReporteCargaEnviada(false);
            
            this.notificacionesEquipos.put(ipEquipo, equipo);
            
            this.equipos.put(ipEquipo, 0L);
        }        
        
        /*for (List.Entry<String, Long> entry : equipos.entrySet()) {             
        }*/
        
        //se inicializa el semáforo con 1, para que sólo una función pueda
        //acceder a la vez a las variables de la clase
        this.sem = new Semaphore(1); 
        
        this.paises = new ArrayList<>();
        this.paisesPorDistribuir = new HashMap<>();
        
        //se consigue la ip de la máquina en que se está ejecutando esta función
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
    
    //función para enviar mensajes de comunicación inicial a cada uno de los
    //equipos que el broker conoce
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
    
    //función que envía mensaje a cada uno de los equipos que el broker conoce
    //para preguntarles su carga de procesamiento
    //esta función se ejecuta periódicamente
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
    
    //función que envía mensajes a los equipos a los cuales se les solicita
    //enviar un agente al broker, para realizar el balanceo de cargas
    //esta función se ejecuta periódicamente
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
    
    //función que se encarga de enviar agentes a equipos para que estos sean
    //procesados, consiguiendo balancear las cargas
    //esta función se ejecuta periódicamente
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

    //esta función se encarga de estar pendiente de todas las comunicaciones
    //entrantes al broker, en un ciclo infinito, que siempre está activo
    
    //dependiendo del valor de la variable instruccion de cada mensaje, se
    //ejecuta cierto bloque de código para actualizar determinadas variables
    //de esta clase
    
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
                        
                        List<Pais> paisesInicio;
                        
                        System.out.println("Recibiendo respuesta inicial de equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoCPU = mensaje.getProcesamientoCPU();
                        paisesInicio = mensaje.getPaisesInicio();

                        this.sem.acquire();

                        //reemplazar por nuevo valor de uso de CPU
                        equipos.replace(ipSender, procesamientoCPU);
                        
                        equipo = this.notificacionesEquipos.get(ipSender);
                        equipo.setActivo(true);
                        this.notificacionesEquipos.replace(ipSender, equipo);
                        
                        pareja = new Pair<>(0L, 0L); 
                        this.procesamientoEquipos.put(ipSender, pareja);
                        
                        this.paises.addAll(paisesInicio);
                        for(int i=0; i<paisesInicio.size(); i++){
                            if( this.paisesEnEquipos.get( paisesInicio.get(i).getNombre() )!=null ){
                                System.out.println("Error: se ha duplicado un país en los equipos de procesamiento.");
                                System.exit(1);
                            }
                            this.paisesEnEquipos.put(paisesInicio.get(i).getNombre()
                                    , ipSender);
                            System.out.println("Agregado a la lista del broker el país "+paisesInicio.get(i).getNombre());
                        }

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

    //esta función se encarga de iniciar la función iniciarEscucha dentro de un hilo
    public void iniciarEscuchaServidor(){
        Runnable task1 = () -> { this.iniciarEscucha();};      
        Thread t1 = new Thread(task1);
        t1.start();
    }
    
}
