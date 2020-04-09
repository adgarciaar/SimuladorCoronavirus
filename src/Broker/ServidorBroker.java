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
import java.util.Arrays;
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
    private volatile HashMap< String, Integer > paisesProcesandosePorEquipo;
    
    //mapa que guarda duplas < IP de un equipo, Procesamiento total en ese equipo >
    //para conocer estados generales de procesamiento de cada equipo
    private volatile HashMap<String, Long> cargaPorEquipo;
    
    //mapa que guarda duplas < IP de un equipo, Lista de carga de cada país en ese equipo >
    private volatile HashMap< String, long[] > cargaPaisesPorEquipo;
    
    //mapa que guarda duplas < IP de un equipo, Datos de ese equipo >
    //para conocer estados de esos equipos
    private volatile HashMap<String, Equipo> estadosEquipos;
    
    //mapa que guarda duplas <Nombre de un país, Datos de ese país>
    //para realizar el balanceo de cargas
    private volatile HashMap<String, Pais> paisesPorDistribuir;    
    
    //mapa que guarda duplas <Población de un país, IP del equipo al que se va a enviar>
    //para realizar el balanceo de cargas
    private volatile HashMap<Long, String> paisesADistribuirEnEquipos;    
    
    //semáforo para garantizar que las funciones acceden sólo una a la vez
    //a las variables de esta clase, garantizando consistencia
    private Semaphore sem;     
    
    //constructor de la clase, se le pasan el puerto por el que se va a comunicar
    //y un mapa con los equipos precargados con los que se va a comunicar
    public ServidorBroker(int puerto, List<String> equipos) {
        
        this.puerto = puerto;
        this.cargaPorEquipo = new HashMap<>();
        this.estadosEquipos = new HashMap<>();
        this.paisesEnEquipos = new HashMap<>();
        this.paisesProcesandosePorEquipo = new HashMap();
        this.cargaPaisesPorEquipo = new HashMap();        
        this.paisesADistribuirEnEquipos = new HashMap();
        
        String ipEquipo;
        //para cada equipo que el broker conoce se inicializan unas variables
        //para conocer su estado
        for(int i=0; i<equipos.size();i++){
            
            //ipEquipo = entry.getKey();
            ipEquipo = equipos.get(i);
            
            Equipo equipo = new Equipo();
            equipo.setActivo(false); //se inicializa equipo como inactivo
            equipo.setNotificacionReporteCargaEnviada(false);
            equipo.setRespuestaEntregada(false);
            
            this.estadosEquipos.put(ipEquipo, equipo);
            //se inicializa la carga de cada equipo en 0
            this.cargaPorEquipo.put(ipEquipo, 0L);
            this.cargaPaisesPorEquipo.put(ipEquipo, null);
        }        
        
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
            System.out.println("Error al conseguir la dirección IP de la máquina actual");
            System.exit(1);
        }        
        this.ipServidor = inetAddress.getHostAddress();
       
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
        
        for (HashMap.Entry<String, Equipo> entry : this.estadosEquipos.entrySet()) {
            
            ipEquipo = entry.getKey();
            
            Mensaje mensaje = new Mensaje();
            mensaje.setIpSender(this.ipServidor);
            mensaje.setPais(null);
            mensaje.setInstrucccion(4);
            
            SenderBroker sender = new SenderBroker(ipEquipo, this.puerto);
            sender.enviarMensaje( mensaje );      
            System.out.println("Enviado mensaje inicial a "+ipEquipo);
            
            Equipo equipo = this.estadosEquipos.get(ipEquipo);
            equipo.setNotificacionReporteCargaEnviada(true);
            this.estadosEquipos.replace(ipEquipo, equipo);
            
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
        
                for (HashMap.Entry<String, Equipo> entry : estadosEquipos.entrySet()) {

                    ipEquipo = entry.getKey();

                    Mensaje mensaje = new Mensaje();
                    mensaje.setIpSender(ipServidor);
                    mensaje.setPais(null);
                    mensaje.setInstrucccion(3);

                    SenderBroker sender = new SenderBroker(ipEquipo, puerto);
                    sender.enviarMensaje( mensaje );      
                    System.out.println("Enviada solicitud de información de "
                            + "procesamiento a "+ipEquipo);
                    
                    Equipo equipo = estadosEquipos.get(ipEquipo);
                    //equipo.setActivo(false);
                    equipo.setNotificacionReporteCargaEnviada(true);
                    equipo.setRespuestaEntregada(false);
                    estadosEquipos.replace(ipEquipo, equipo);
                }
                
                sem.release();
                
            }
        };

        Timer timer = new Timer();
        //timer.schedule(task, new Date(), 3000);
        
        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 30000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 30000;
        
        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
        
    }
    
    //calcula la desviación estándar poblacional para una lista con la carga de los equipos
    public double calcularDesvEstandarNuevaDistribucion( List<Long> cargasEnEquipos ){
        
        long promedio, suma = 0;
        for(int i=0; i<cargasEnEquipos.size(); i++){
            suma = suma + cargasEnEquipos.get(i);
        }
        promedio = suma/cargasEnEquipos.size();
        
        double sumatoria = 0;
        for(int i=0; i<cargasEnEquipos.size(); i++){
            sumatoria = sumatoria + Math.pow( cargasEnEquipos.get(i) - promedio , 2);
        }
        
        double division = sumatoria/cargasEnEquipos.size();
        
        double desvEstandar = Math.sqrt(division);
        
        return desvEstandar;
    }
    
    //función que envía mensajes a los equipos a los cuales se les solicita
    //enviar un agente al broker, para realizar el balanceo de cargas
    //esta función se ejecuta periódicamente
    public void definirDistribucion(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {              
                               
                try {
                    sem.acquire();
                } catch (InterruptedException ex) {
                    System.out.println("Error al intentar activar semáforo");
                    System.exit(1);
                }
                
                //verificar si la carga de los equipos es igual o diferente
                List<Long> valoresCargaEquipos = new ArrayList<>();
                for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {
                    valoresCargaEquipos.add( entry.getValue() );
                }                        
                Long procesamientoIgual = valoresCargaEquipos.get(0);
                boolean todosTienenMismaCarga = true;
                for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {
                    if( procesamientoIgual != entry.getValue() ){
                        todosTienenMismaCarga = false;
                        break;
                    }
                }
                
                //si todos tienen diferente carga entonces hay que balancear
                if( todosTienenMismaCarga == false ){
                    
                    String equipoConMayorProcesamiento = null;
                    String equipoConMenorProcesamiento = null;
                    Long menorProcesamiento = Long.MAX_VALUE;
                    Long mayorProcesamiento = Long.MIN_VALUE;
                    Equipo equipo;
                
                    //conocer el equipo que tiene mayor y menor carga de procesamiento
                    for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {

                        equipo = estadosEquipos.get(entry.getKey());                   

                        if( equipo.isActivo() == true ){

                            if( entry.getValue() < menorProcesamiento ){
                                menorProcesamiento = entry.getValue();
                                equipoConMenorProcesamiento = entry.getKey();
                            }
                            if( entry.getValue() > mayorProcesamiento 
                                    && paisesProcesandosePorEquipo.get(entry.getKey())>1 ){
                                mayorProcesamiento = entry.getValue();
                                equipoConMayorProcesamiento = entry.getKey();
                            }

                        }                    
                    }
                    
                    if( equipoConMayorProcesamiento.equals(equipoConMenorProcesamiento) ){
                        
                        System.out.println("No se puede balancear más en este momento");
                        
                    }else{
                        
                        System.out.println("Equipo con mayor procesamiento es "+equipoConMayorProcesamiento);
                        System.out.println("Equipo con menor procesamiento es "+equipoConMenorProcesamiento);

                        //tomar el agente con menor procesamiento
                        //del equipo con mayor procesamiento
                        //(cuando ese equipo ejecuta más de 1 agente)
                        //y darselo al equipo con menos procesamiento
                        //pero antes revisar si se disminuye la desviación estándar
                        //de la carga de todos los equipos (más balanceado)

                        long cargaAgenteATransferir;

                        long[] cargasMayor = cargaPaisesPorEquipo.get(equipoConMayorProcesamiento);

                        //la nueva carga del mayor sería la misma restando el agente que se va a transferir
                        long nuevaCargaMayor = mayorProcesamiento - cargasMayor[0];
                        //la nueva carga del menor sería la misma sumando el agente que se va a transferir
                        long nuevaCargaMenor = menorProcesamiento + cargasMayor[0];

                        List<Long> anteriorDistribucionCargas = new ArrayList<>();                    
                        for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {                        
                            anteriorDistribucionCargas.add(entry.getValue());                       
                        }

                        List<Long> nuevaDistribucionCargas = new ArrayList<>();
                        nuevaDistribucionCargas.add(nuevaCargaMayor);
                        nuevaDistribucionCargas.add(nuevaCargaMenor);

                        for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {
                            if( !entry.getKey().equals(equipoConMayorProcesamiento) &&
                                    !entry.getKey().equals(equipoConMenorProcesamiento) ){
                                nuevaDistribucionCargas.add(entry.getValue());
                            }
                        }

                        double desvEstandarAnterior;
                        double desvEstandarNueva;

                        desvEstandarAnterior = calcularDesvEstandarNuevaDistribucion(anteriorDistribucionCargas);                    
                        desvEstandarNueva = calcularDesvEstandarNuevaDistribucion(nuevaDistribucionCargas);

                        //si la nueva desv. estándar es menor, entonces hacer el intercambio
                        if(desvEstandarNueva < desvEstandarAnterior){

                            //usar mapa para saber a dónde distribuir ese país

                            System.out.println("Se va a transferir un país con población "+cargasMayor[0]);

                            paisesADistribuirEnEquipos.put(cargasMayor[0], equipoConMenorProcesamiento);

                            //enviar mensaje para pedir el agente a trasladar

                            String ipEquipo = equipoConMayorProcesamiento;

                            Mensaje mensaje = new Mensaje();
                            mensaje.setIpSender(ipServidor);
                            mensaje.setPais(null);
                            mensaje.setInstrucccion(7);
                            mensaje.setPaisesInicio(null);

                            SenderBroker sender = new SenderBroker(ipEquipo, puerto);
                            sender.enviarMensaje( mensaje );      
                            System.out.println("Enviado mensaje a "+ipEquipo+" "
                                    + ", solicitando país para traslado con población "+
                                    cargasMayor[0]+" que se va a mover a "+equipoConMenorProcesamiento);

                            //actualizar estado

                            //paisesEnEquipos.put(pais, ipEquipo);

                        }
                    
                    }
                    
                }
                
                sem.release();
                
            }
        };

        Timer timer = new Timer();
        //timer.schedule(task, new Date(), 3000);
        
        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 30000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 45000;
        
        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
    }
    
    //función que se encarga de enviar agentes a equipos para que estos sean
    //procesados, consiguiendo balancear las cargas
    //esta función se ejecuta periódicamente
    public void distribuirPaisesEquipoCaido(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                
                    System.out.println("Distribuidor activado");
                    
                    /*
                    try {
                        sem.acquire();
                    } catch (InterruptedException ex) {
                        System.out.println("Error al intentar activar semáforo");
                        System.exit(1);
                    }
                    
                    Equipo equipo;
                    for (HashMap.Entry<String, Equipo> entry : notificacionesEquipos.entrySet()) {
                        equipo = entry.getValue();
                        if( equipo.isActivo()==true){
                            //si al equipo se le envió una notificación y no la respondió
                            //entonces distribuir los países que allí estaban
                            if( equipo.isNotificacionReporteCargaEnviada()==true 
                                    && equipo.isRespuestaEntregada()==false ){
                                System.out.println("REPARTIENDO PAÍSES HUERFANOS");
                            }
                        }
                    }
                    
                    sem.release();*/                
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
            
            Socket socket = null;    
              
            try { 
                
                // socket object to receive incoming client requests 
                socket = ss.accept(); 
                
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                  
                System.out.println("Mensaje entrante proveniente de " + socket);
                
                Mensaje mensaje = (Mensaje)objectInputStream.readObject();
                Pais pais = mensaje.getPais();
                
                String ipSender = null;
                Long procesamientoEquipo;
                int numeroPaisesProcesando;
                Equipo equipo = null;
                
                switch(mensaje.getInstrucccion()) {
                    
                    case 4: //recibiendo primera comunicación con un equipo
                        
                        List<Pais> paisesInicio;
                        
                        System.out.println("Recibiendo respuesta inicial de equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoEquipo = mensaje.getProcesamientoEquipo();
                        paisesInicio = mensaje.getPaisesInicio();
                        numeroPaisesProcesando = mensaje.getNumeroPaisesProcesando();

                        this.sem.acquire();

                        //reemplazar por nuevo valor de procesamiento
                        this.cargaPorEquipo.replace(ipSender, procesamientoEquipo);
                        this.paisesProcesandosePorEquipo.put(ipSender, numeroPaisesProcesando);
                        
                        equipo = this.estadosEquipos.get(ipSender);
                        //se conoce que el equipo se encuentra activo, 
                        //se actualiza dicho estado
                        equipo.setActivo(true);
                        this.estadosEquipos.replace(ipSender, equipo);
                        
                        //añadir a la lista de países del broker
                        //los países del equipo que recién se comunica
                        this.paises.addAll(paisesInicio);
                        for(int i=0; i<paisesInicio.size(); i++){
                            if( this.paisesEnEquipos.get( paisesInicio.get(i).getNombre() )!=null ){
                                System.out.println("Error: se ha duplicado un país "
                                        + "en los equipos de procesamiento.");
                                System.exit(1);
                            }
                            this.paisesEnEquipos.put(paisesInicio.get(i).getNombre()
                                    , ipSender);
                            System.out.println("Agregado a la lista del broker "
                                    + "el país "+paisesInicio.get(i).getNombre());
                        }

                        this.sem.release();                 
                        
                        System.out.println("Actualizada información del equipo");
                        
                      break;
                      
                    case 2: //recibiendo país desde un equipo
                        
                        System.out.println("Recibiendo país desde el equipo para distribuir");
                      
                        this.sem.acquire();
                        
                        this.paisesPorDistribuir.put(pais.getNombre(), pais);
                        
                        String ipEquipoARepartir = this.paisesADistribuirEnEquipos.get(pais.getPoblacion());                        
                        
                        //enviarlo al nuevo equipo donde se va a procesar
                        
                        Mensaje nuevoMensaje = new Mensaje();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setPais(pais);
                        nuevoMensaje.setInstrucccion(1);

                        SenderBroker sender = new SenderBroker(ipEquipoARepartir, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );      
                        System.out.println("Enviado país "+ pais.getNombre() 
                                +" a "+ipEquipoARepartir+" con población "+pais.getPoblacion());

                        this.sem.release();

                        //System.out.println("Recibido el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");
                        
                      break;
                      
                    case 3: //recibiendo reporte de rendimiento
                        
                        System.out.println("Recibiendo información de procesamiento del equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoEquipo = mensaje.getProcesamientoEquipo();
                        numeroPaisesProcesando = mensaje.getNumeroPaisesProcesando();
                        long[] cargaPaises = mensaje.getCargaPaisesPorEquipo();

                        this.sem.acquire();

                        //reemplazar por nuevo valor de uso de procesamiento
                        this.cargaPorEquipo.replace(ipSender, procesamientoEquipo);
                        this.paisesProcesandosePorEquipo.put(ipSender, numeroPaisesProcesando);
                        
                        equipo = this.estadosEquipos.get(ipSender);
                        //equipo.setActivo(true);
                        //se conoce que el reporte fue enviado pero ya se retornó
                        equipo.setNotificacionReporteCargaEnviada(false);
                        this.estadosEquipos.replace(ipSender, equipo);  
                        
                        this.cargaPaisesPorEquipo.put(ipSender, cargaPaises);

                        this.sem.release();
                        
                        System.out.println("Equipo "+ipSender+" tiene carga total "
                                + "de "+procesamientoEquipo+", procesando "
                                +numeroPaisesProcesando+" países");
                        
                        System.out.println(Arrays.toString(cargaPaises));                   
                        
                        System.out.println("Actualizada información del equipo");
                        
                        break;
                        
                    case 6: //confirmación de país recibido para procesar
                        
                        System.out.println("Recibiendo confirmación de procesamiento de país "+pais.getNombre());
                        
                        String paisRecibido = pais.getNombre();
                        
                        this.sem.acquire();
                        
                        //eliminar país que se está ejecutando de la lista de
                        //países que se van a distribuir
                        this.paisesPorDistribuir.remove(paisRecibido);
                        this.paisesADistribuirEnEquipos.remove(pais.getPoblacion());
                        
                        this.sem.release();
                        
                        break;
                   
                }
               
            } 
            catch (Exception e){ 
                try { 
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Problema al cerrar socket");
                    System.exit(1);
                }
                //e.printStackTrace(); 
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
