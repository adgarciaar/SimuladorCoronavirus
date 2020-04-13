/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Broker;

import Entidades.NotificacionEquipo;
import Entidades.Pais;
import EquipoProcesamiento.SenderEquipo;
import Entidades.Mensaje;
import Entidades.MensajeBroker;
import Entidades.NotificacionBroker;
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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author adgar
 */
public class ServidorBroker {
    
    private String ipServidor; //guarda la ip de la máquina en que se ejecuta
    private int puertoEquipos; //puertoEquipos con el que se comunica esta máquina
    private int puertoBrokers; //con el que se comunica con otros brokers
    private int numeroBroker;
    
    private volatile List<Pais> paises; //lista de países que se están ejecutando en los equipos
    
    //mapa que guarda tuplas < Nombre de un país, IP del equipo donde se está ejecutando >
    private volatile HashMap<String, String> paisesEnEquipos;
    
    //mapa que guarda tuplas < Nombre de un país, Objeto País >
    private volatile HashMap<String, Pais> conjuntoPaises;
    
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
    private volatile HashMap<String, NotificacionEquipo> estadosEquipos;
    
    //mapa que guarda duplas <Nombre de un país, Datos de ese país>
    //para realizar el balanceo de cargas
    private volatile HashMap<String, Pais> paisesPorDistribuir;    
    
    //mapa que guarda duplas <Población de un país, IP del equipo al que se va a enviar>
    //para realizar el balanceo de cargas
    private volatile HashMap<Long, String> paisesADistribuirEnEquipos;    
    
    //semáforo para garantizar que las funciones acceden sólo una a la vez
    //a las variables sobre equipos de esta clase, garantizando consistencia
    private Semaphore sem;     
    
    //semáforo para garantizar que las funciones acceden sólo una a la vez
    //a las variables sobre brokers de esta clase, garantizando consistencia
    private Semaphore sem2; 
    
    //variable booleana para saber si antes de la primera iteración
    //la totalidad de los equipos no pudieron establecer comunicación inicial
    private boolean primeraIteracion;
    
    //lista que guarda la totalidad de los equipos introducidos al inicio
    private List<String> equiposIniciales;    
    
    //mapa que guarda duplas < IP de un broker, Estado de ese broker >
    private volatile LinkedHashMap<String, String> brokers;
    
    //mapa que guarda duplas < IP de un broker, Estado de ese broker >
    private volatile HashMap<Integer, String> ordenBrokerAnillo;
    
    //mapa que guarda duplas < IP de un equipo, Broker asignado para manejar ese equipo >
    private volatile HashMap<String, String> equiposPorBroker;
    
    //mapa que guarda duplas < IP de un broker, Objecto con datos de notificaciones de ese broker >
    private volatile HashMap<String, NotificacionBroker> notificacionesBrokers;
    
    //constructor de la clase, se le pasan el puertoEquipos por el que se va a comunicar
    //y un mapa con los equipos precargados con los que se va a comunicar
    public ServidorBroker(int puertoEquipos, int puertoBrokers, 
            List<String> equipos, List<String> otrosBrokers) {
        
        this.inicializarBroker(puertoEquipos, puertoBrokers);
        
        this.numeroBroker = 0;
        this.brokers.put(this.ipServidor, "Activo");
        for(int j=0; j<otrosBrokers.size();j++){      
            this.brokers.put(otrosBrokers.get(j), "Esperando");
        }
        
        this.repartirEquiposPorBrokers(equipos);        
        
        this.inicializarEquiposBroker(equipos);
        
        this.establecerComunicacionOtrosBrokers(equipos);
       
    }
    
    public ServidorBroker(int puertoEquipos, int puertoBrokers) {
        this.inicializarBroker(puertoEquipos, puertoBrokers);
    }
    
    public void inicializarBroker(int puertoEquipos, int puertoBrokers){
        //se consigue la ip de la máquina en que se está ejecutando esta función
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            System.out.println("Error al conseguir la dirección IP de la máquina actual");
            System.exit(1);
        }        
        this.ipServidor = inetAddress.getHostAddress();
        
        this.puertoEquipos = puertoEquipos;
        this.puertoBrokers = puertoBrokers;
        
        //se inicializa el semáforo con 1, para que sólo una función pueda
        //acceder a la vez a las variables de la clase
        this.sem = new Semaphore(1); 
        this.sem2 = new Semaphore(1); 
        
        this.paises = new ArrayList<>();
        this.paisesPorDistribuir = new HashMap<>();
        
        this.cargaPorEquipo = new HashMap<>();
        this.estadosEquipos = new HashMap<>();
        this.paisesEnEquipos = new HashMap<>();
        this.paisesProcesandosePorEquipo = new HashMap();
        this.cargaPaisesPorEquipo = new HashMap();        
        this.paisesADistribuirEnEquipos = new HashMap();
        this.brokers = new LinkedHashMap<>();
        this.conjuntoPaises = new HashMap();
        this.primeraIteracion = true;
        this.notificacionesBrokers = new HashMap();
        this.equiposPorBroker = new HashMap();
        this.ordenBrokerAnillo = new HashMap();
    }
    
    public void inicializarEquiposBroker(List<String> equipos){
        
        String ipEquipo;
        //para cada equipo que el broker conoce se inicializan unas variables
        //para conocer su estado
        for(int i=0; i<equipos.size();i++){            
            
            ipEquipo = equipos.get(i);
            if (ipEquipo.equals(this.ipServidor)){
                System.out.println("Error: una de las direcciones de equipo"
                        + " proporcionada es la misma de dirección de esta máquina");
                System.exit(1);
            }
            
            //si a este broker le corresponde ese equipo entonces se le asigna
            if( this.equiposPorBroker.get(ipEquipo).equals(this.ipServidor) ){
            
                NotificacionEquipo equipo = new NotificacionEquipo();
                equipo.setActivo(false); //se inicializa equipo como inactivo
                equipo.setNotificacionReporteCargaEnviada(false);
                equipo.setRespuestaEntregada(false);
                equipo.setComunicacionInicialExitosa(false);
                equipo.setHoraUltimaNotificacionEnviada(null);
                equipo.setPaisesRepartidosPorCaida(false);

                this.estadosEquipos.put(ipEquipo, equipo);
                //se inicializa la carga de cada equipo en 0
                this.cargaPorEquipo.put(ipEquipo, 0L);
                this.cargaPaisesPorEquipo.put(ipEquipo, null);
                this.paisesProcesandosePorEquipo.put(ipEquipo, 0);
            
            }
        }
        
    }
    
    public void repartirEquiposPorBrokers( List<String> equipos ){
        
        /*if( equipos.size() > this.brokers.size() ){
            
            int equiposPorBroker = (int) Math.floor( equipos.size()/this.brokers.size() );
            int equiposUltimoBroker = 0;
            if( equiposPorBroker*this.brokers.size() < equipos.size() ){
                equiposUltimoBroker = equipos.size() - ( equiposPorBroker*this.brokers.size() );
            }
            
            int i = equipos.size();
            while( i > 0 ){
                
            }
            
        }else{
            if( equipos.size() < this.brokers.size() ){
                
            }else{
                //un equipo por broker
            }
        }
        
        int cantidadEquiposPorBroker = equipos.size()/this.brokers.size();*/
        
        //si hay un sólo equipo entonces entregarselo al 1er broker
        if( equipos.size() == 1 ){
            this.equiposPorBroker.put(equipos.get(0), this.ipServidor);
        }
        
        //si sólo hay un broker entonces se le entregan todos los equipos a éste
        if( this.brokers.size() == 1 ){            
            for(int i=0; i<equipos.size(); i++){
                this.equiposPorBroker.put(equipos.get(i), this.ipServidor);                
            }
        }
        
    }
    
    public void establecerComunicacionOtrosBrokers( List<String> equipos ){
        
        System.out.println("\nESTABLECIENDO COMUNICACIÓN INICIAL CON OTROS BROKERS");
        
        String ipBroker;
        int contador = 0;
        for (HashMap.Entry<String, String> entry : this.brokers.entrySet()) {
            
            ipBroker = entry.getKey();
            
            if( !ipBroker.equals(this.ipServidor) ){
            
                MensajeBroker mensaje = new MensajeBroker();
                mensaje.setIpSender(this.ipServidor);
                mensaje.setInstruccion(1);
                //System.out.println("equipos por aca: "+this.equiposPorBroker);
                mensaje.setBrokers(this.brokers);
                mensaje.setEquiposPorBroker(this.equiposPorBroker);
                mensaje.setNumeroBroker(contador);

                SenderBroker sender = new SenderBroker(ipBroker, this.puertoBrokers);
                sender.enviarMensaje( mensaje );      
                System.out.println("Enviado mensaje inicial a "+ipBroker);            
            }
            this.ordenBrokerAnillo.put(contador, ipBroker);
            contador = contador + 1;
        }
        
    }
    
    //función para enviar mensajes de comunicación inicial a cada uno de los
    //equipos que el broker conoce
    public void establecerComunicacionInicialConEquipos(){
        
        System.out.println("\nESTABLECIENDO COMUNICACIÓN INICIAL CON EQUIPOS");
        
        String ipEquipo = null;
        
        try {
            sem.acquire();
        } catch (InterruptedException ex) {
            System.out.println("Error al intentar activar semáforo");
            System.exit(1);
        }
        
        for (HashMap.Entry<String, NotificacionEquipo> entry : this.estadosEquipos.entrySet()) {
            
            ipEquipo = entry.getKey();
            
            Mensaje mensaje = new Mensaje();
            mensaje.setIpSender(this.ipServidor);
            mensaje.setPais(null);
            mensaje.setInstruccion(4);
            
            SenderBroker sender = new SenderBroker(ipEquipo, this.puertoEquipos);
            sender.enviarMensaje( mensaje );      
            System.out.println("Enviado mensaje inicial a "+ipEquipo);
            
            /*
            Equipo equipo = this.estadosEquipos.get(ipEquipo);
            equipo.setNotificacionReporteCargaEnviada(true);
            this.estadosEquipos.replace(ipEquipo, equipo);
            */
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
                
                if(primeraIteracion == true){
                    revisarEquiposConexionInicial();
                }
                primeraIteracion = false;
                
                System.out.println("\nSOLICITADOR DE REPORTE DE CARGA ACTIVADO");
                
                String ipEquipo = null;
                
                try {
                    sem.acquire();
                } catch (InterruptedException ex) {
                    System.out.println("Error al intentar activar semáforo");
                    System.exit(1);
                }
        
                for (HashMap.Entry<String, NotificacionEquipo> entry : estadosEquipos.entrySet()) {

                    ipEquipo = entry.getKey();
                    NotificacionEquipo equipo = estadosEquipos.get(ipEquipo);
                    
                    if(equipo.isComunicacionInicialExitosa() == true){

                        Mensaje mensaje = new Mensaje();
                        mensaje.setIpSender(ipServidor);
                        mensaje.setPais(null);
                        mensaje.setInstruccion(3);

                        SenderBroker sender = new SenderBroker(ipEquipo, puertoEquipos);
                        sender.enviarMensaje( mensaje );   
                        Date horaEnvioMensaje = new Date(); 
                        System.out.println("Enviada solicitud de información de "
                                + "procesamiento a "+ipEquipo);

                        //actualizar estado del equipo
                        if(equipo.isActivo() == true){
                            equipo.setNotificacionReporteCargaEnviada(true);
                            equipo.setRespuestaEntregada(false);
                            equipo.setHoraUltimaNotificacionEnviada(horaEnvioMensaje);
                        }
                        estadosEquipos.replace(ipEquipo, equipo);
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
    
    public static void quickSort(long[] arr, int start, int end){
 
        int partition = partition(arr, start, end);
 
        if(partition-1>start) {
            quickSort(arr, start, partition - 1);
        }
        if(partition+1<end) {
            quickSort(arr, partition + 1, end);
        }
    }
 
    public static int partition(long[] arr, int start, int end){
        int pivot = (int) arr[end];
 
        for(int i=start; i<end; i++){
            if(arr[i]<pivot){
                int temp= (int) arr[start];
                arr[start]=arr[i];
                arr[i]=temp;
                start++;
            }
        }
 
        int temp = (int) arr[start];
        arr[start] = pivot;
        arr[end] = temp;
 
        return start;
    }
    
    public void revisarEquiposConexionInicial(){    
        
        try {
            this.sem.acquire();
        } catch (InterruptedException ex) {
            System.out.println("Error al intentar activar semáforo");
            System.exit(1);
        }
        NotificacionEquipo equipo;
        int contador = 0;
        for (HashMap.Entry<String, NotificacionEquipo> entry : this.estadosEquipos.entrySet()) {                
            equipo = entry.getValue();                   
            if( equipo.isComunicacionInicialExitosa() == false ){
                contador = contador + 1;
            }
        }
        //System.out.println("contador "+contador);
        //System.out.println("estados "+this.estadosEquipos.size());
        if( contador == this.estadosEquipos.size() ){
            System.out.println("No se logró entablar una comunicación inicial"
                    + " con todos los equipos. Se procede a terminar.");
            System.exit(1);
        }
        
        this.sem.release();
        
    }
    
    public void monitorearEquiposActivos(){
        
        NotificacionEquipo equipo;
        String equipoMonitoreado;
        int diferencia;
        Date horaEnvioMensaje, horaActual;
        
        while(true){
            
            try {
                sem.acquire();
            } catch (InterruptedException ex) {
                System.out.println("Error al intentar activar semáforo");
                System.exit(1);
            }
            
            for (HashMap.Entry<String, NotificacionEquipo> entry : this.estadosEquipos.entrySet()) {
                
                equipo = entry.getValue();  
                equipoMonitoreado = entry.getKey();
                
                if( equipo.isNotificacionReporteCargaEnviada() == true 
                        && equipo.isRespuestaEntregada()==false ){
                    
                    horaEnvioMensaje = equipo.getHoraUltimaNotificacionEnviada();
                    horaActual = new Date();
                    
                    diferencia = (int) (horaActual.getTime()-horaEnvioMensaje.getTime())/1000;
                    //si han pasado más de 6 segundos en que el equipo no ha respondido
                    if( diferencia > 6 ){ 
                        //establecer ese equipo como inactivo
                        equipo.setActivo(false);
                        equipo.setNotificacionReporteCargaEnviada(false);                        
                        this.estadosEquipos.replace(entry.getKey(), equipo);
                        System.out.println("El equipo "+entry.getKey()+" está inactivo");
                    }    
                    
                }else{
                    //si equipo es inactivo y pasan más de 45 segundos sin que
                    //se comunique, entonces los países que tenía a cargo se
                    //reparten
                    if( equipo.isActivo() == false && equipo.getHoraUltimaNotificacionEnviada()!=null 
                            && equipo.isPaisesRepartidosPorCaida()==false ){

                        horaEnvioMensaje = equipo.getHoraUltimaNotificacionEnviada();                        
                        horaActual = new Date();

                        diferencia = (int) (horaActual.getTime()-horaEnvioMensaje.getTime())/1000;
                        
                        if(diferencia > 45){
                            
                            equipo.setComunicacionInicialExitosa(false);                        
                            this.estadosEquipos.replace(entry.getKey(), equipo);
                            
                            //enviarle los países al primer equipo activo encontrado
                            //el balanceo de cargas se realiza posteriormente si
                            //es requerido
                            NotificacionEquipo equipoTemp;
                            String equipoAEnviar = null;
                            for (HashMap.Entry<String, NotificacionEquipo> registro : this.estadosEquipos.entrySet()) {
                                equipoTemp = registro.getValue();
                                if (equipoTemp.isActivo() == true){
                                    equipoAEnviar = registro.getKey();
                                    break;
                                }
                            }
                            
                            if(equipoAEnviar != null){
                            
                                String ipEquipoRevision;
                                Pais pais;
                                //List<Pais> paisesAEnviar = new ArrayList<>();

                                System.out.println("REDISTRIBUYENDO PAÍSES DEL "
                                        + "EQUIPO CAÍDO "+equipoMonitoreado);

                                for (HashMap.Entry<String, String> entrada : this.paisesEnEquipos.entrySet()) {

                                    ipEquipoRevision = entrada.getValue();

                                    if ( ipEquipoRevision.equals(equipoMonitoreado) ){

                                        pais = this.conjuntoPaises.get( entrada.getKey() );

                                        Mensaje mensaje = new Mensaje();
                                        mensaje.setIpSender(this.ipServidor);
                                        mensaje.setInstruccion(1);
                                        mensaje.setPais(pais);                                    

                                        SenderBroker sender = new SenderBroker(equipoAEnviar, 
                                                this.puertoEquipos);
                                        sender.enviarMensaje( mensaje );      
                                        System.out.println("Enviado país a "
                                                +pais.getNombre()+" a "+equipoAEnviar);
                                    }

                                }
                                equipo.setPaisesRepartidosPorCaida(true);                        
                                this.estadosEquipos.replace(entry.getKey(), equipo);
                            }
                        }

                    }
                }
            }  
            
            sem.release();
        }        
    }
    
    //esta función se encarga de iniciar la función iniciarEscucha dentro de un hilo
    public void iniciarMonitorEquiposActivos(){
        Runnable task1 = () -> { this.monitorearEquiposActivos(); };      
        Thread t1 = new Thread(task1);
        t1.start();
    }
    
    //función que establece cómo realizar el balanceo de cargas
    //envía mensajes a los equipos a los cuales se les solicita
    //enviar un agente al broker, para realizar el balanceo
    //esta función se ejecuta periódicamente
    public void definirDistribucion(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() {      
                
                System.out.println("\nBALANCEADOR DE CARGA ACTIVADO");
                               
                try {
                    sem.acquire();
                } catch (InterruptedException ex) {
                    System.out.println("Error al intentar activar semáforo");
                    System.exit(1);
                }
                
                //verificar si la carga de los equipos es igual o diferente
                List<Long> valoresCargaEquipos = new ArrayList<>();
                int cantidadEquiposActivos = 0;
                for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {
                    NotificacionEquipo equipo = estadosEquipos.get( entry.getKey() );
                    if(equipo.isActivo() == true){
                        valoresCargaEquipos.add( entry.getValue() );
                        cantidadEquiposActivos = cantidadEquiposActivos + 1;
                    }
                }    
                
                if(cantidadEquiposActivos > 1){
                
                    Long procesamientoIgual = valoresCargaEquipos.get(0);
                    boolean todosTienenMismaCarga = true;
                    for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {
                        NotificacionEquipo equipo = estadosEquipos.get( entry.getKey() );
                        if(equipo.isActivo() == true){
                            if( procesamientoIgual != entry.getValue() ){
                                todosTienenMismaCarga = false;
                                break;
                            }
                        }
                    }

                    //si todos tienen diferente carga entonces hay que balancear
                    if( todosTienenMismaCarga == false ){

                        String equipoConMayorProcesamiento = null;
                        String equipoConMenorProcesamiento = null;
                        Long menorProcesamiento = Long.MAX_VALUE;
                        Long mayorProcesamiento = Long.MIN_VALUE;
                        NotificacionEquipo equipo;

                        boolean continuarDistribucion = true;                    

                        List<String> equiposMayoresUsados = new ArrayList<>();                   

                        while(continuarDistribucion){

                            //conocer el equipo que tiene mayor y menor carga de procesamiento

                            equipoConMayorProcesamiento = null;
                            equipoConMenorProcesamiento = null;
                            menorProcesamiento = Long.MAX_VALUE;
                            mayorProcesamiento = Long.MIN_VALUE;

                            for (HashMap.Entry<String, Long> entry : cargaPorEquipo.entrySet()) {

                                equipo = estadosEquipos.get(entry.getKey());                   

                                if( equipo.isActivo() == true ){

                                    if( entry.getValue() < menorProcesamiento ){
                                        menorProcesamiento = entry.getValue();
                                        equipoConMenorProcesamiento = entry.getKey();
                                    }
                                    if( entry.getValue() > mayorProcesamiento 
                                            && paisesProcesandosePorEquipo.get(entry.getKey())>1 ){

                                            if( !equiposMayoresUsados.contains(entry.getKey() ) ){
                                                mayorProcesamiento = entry.getValue();
                                                equipoConMayorProcesamiento = entry.getKey();
                                            }          
                                    }

                                }                         
                            }

                            if( equipoConMayorProcesamiento == null
                                    || equipoConMayorProcesamiento.equals(equipoConMenorProcesamiento) ){

                                System.out.println("No se puede balancear más en este momento");
                                break;

                            }else{

                                System.out.println("Equipo con mayor procesamiento es "+equipoConMayorProcesamiento);
                                System.out.println("Equipo con menor procesamiento es "+equipoConMenorProcesamiento);

                                //tomar el agente con menor procesamiento
                                //del equipo con mayor procesamiento
                                //(cuando ese equipo ejecuta más de 1 agente)
                                //y darselo al equipo con menos procesamiento
                                //pero antes revisar si se disminuye la desviación estándar
                                //de la carga de todos los equipos (más balanceado)

                                long[] cargasMayor = cargaPaisesPorEquipo.get(equipoConMayorProcesamiento);

                                long cargaAgenteATransferir = cargasMayor[0];

                                //la nueva carga del mayor sería la misma restando el agente que se va a transferir
                                long nuevaCargaMayor = mayorProcesamiento - cargaAgenteATransferir;
                                //la nueva carga del menor sería la misma sumando el agente que se va a transferir
                                long nuevaCargaMenor = menorProcesamiento + cargaAgenteATransferir;

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
                                    mensaje.setInstruccion(7);
                                    mensaje.setPaisesInicio(null);

                                    SenderBroker sender = new SenderBroker(ipEquipo, puertoEquipos);
                                    sender.enviarMensaje( mensaje );      
                                    System.out.println("Enviado mensaje a "+ipEquipo+" "
                                            + ", solicitando país para traslado con población "+
                                            cargasMayor[0]+" que se va a mover a "+equipoConMenorProcesamiento);

                                    cargaPaisesPorEquipo.replace(ipEquipo, cargasMayor);

                                    //actualizar estado de las cargas para continuar el ciclo

                                    long[] cargasAntiguasMayor = cargaPaisesPorEquipo.get(equipoConMayorProcesamiento);
                                    long[] cargasAntiguasMenor = cargaPaisesPorEquipo.get(equipoConMenorProcesamiento);

                                    //actualizar cargas del mayor
                                    //se le quitó el proceso con menor carga (el primero)

                                    long[] cargasNuevasMayor = new long[ cargasAntiguasMayor.length-1 ];

                                    int j = 0;
                                    for(int k=1; k<cargasAntiguasMayor.length; k++){
                                        cargasNuevasMayor[j] = cargasAntiguasMayor[k];
                                        j = j + 1;
                                    }

                                    //actualizar cargas del mayor
                                    //se le adiciona un nuevo agente
                                    //hay que ordenar porque a priori no sé sabe su posición en el arreglo

                                    long[] cargasNuevasMenor = new long[ cargasAntiguasMenor.length+1 ]; ;

                                    for(int m=0; m<cargasAntiguasMayor.length; m++){
                                        cargasNuevasMenor[m] = cargasAntiguasMenor[m];                                    
                                    }                                
                                    cargasNuevasMenor[ cargasAntiguasMenor.length ] = cargaAgenteATransferir;
                                    //ordenarlo
                                    //ordenar de menor a mayor la carga de los países
                                    quickSort(cargasNuevasMenor, 0, cargasNuevasMenor.length-1);

                                    cargaPaisesPorEquipo.replace(equipoConMayorProcesamiento, cargasNuevasMayor);
                                    cargaPaisesPorEquipo.replace(equipoConMenorProcesamiento, cargasNuevasMenor);

                                    cargaPorEquipo.replace(equipoConMayorProcesamiento, nuevaCargaMayor);
                                    cargaPorEquipo.replace(equipoConMenorProcesamiento, nuevaCargaMenor);

                                    int procesadosPorMayorAntes = paisesProcesandosePorEquipo.get(equipoConMayorProcesamiento);
                                    int procesadosPorMenorAntes = paisesProcesandosePorEquipo.get(equipoConMenorProcesamiento);
                                    paisesProcesandosePorEquipo.replace(equipoConMayorProcesamiento, procesadosPorMayorAntes-1);
                                    paisesProcesandosePorEquipo.replace(equipoConMenorProcesamiento, procesadosPorMenorAntes+1);

                                }else{
                                    equiposMayoresUsados.add(equipoConMayorProcesamiento);
                                }

                            }

                        }

                    }else{
                        System.out.println("Todos los equipos ya se encuentran "
                                + "con la misma carga de procesamiento");
                    }
                
                    
                }else{
                    
                    if( cantidadEquiposActivos == 1 ){
                        System.out.println("No se puede balancear más en este momen"
                            + "to. Sólo se tiene un equipo de procesamiento");
                    }else{
                        System.out.println("No se puede balancear más en este momen"
                            + "to. No se tienen equipos de procesamiento activos");
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
    
    public void terminarEjecucionConErrores(String mensajeError){
        
        try {
            this.sem.acquire();
        } catch (InterruptedException ex) {
            System.out.println("Error al intentar activar semáforo");
            System.exit(1);
        }
        String ipEquipo = null;
        for (HashMap.Entry<String, NotificacionEquipo> entry : this.estadosEquipos.entrySet()) {
            ipEquipo = entry.getKey();
            
            Mensaje mensaje = new Mensaje();
            mensaje.setIpSender(this.ipServidor);
            mensaje.setPais(null);
            mensaje.setInstruccion(9);
            mensaje.setTexto(mensajeError);

            SenderBroker sender = new SenderBroker(ipEquipo, this.puertoEquipos);
            sender.enviarMensaje( mensaje );   
                        
            System.out.println("Enviada solicitud de terminación a "+ipEquipo);
        } 
        
        this.sem.release();
        
        System.out.println("Error: "+mensajeError);
        
        System.exit(1);
    }

    //esta función se encarga de estar pendiente de todas las comunicaciones
    //entrantes al broker, en un ciclo infinito, que siempre está activo
    
    //dependiendo del valor de la variable instruccion de cada mensaje, se
    //ejecuta cierto bloque de código para actualizar determinadas variables
    //de esta clase
    
    public void iniciarEscucha(){
        
        ServerSocket ss = null;
        
        try {
            ss = new ServerSocket(puertoEquipos);
        } catch (IOException ex) {
            System.out.println("Error al abrir socket");
            System.exit(1);
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
                NotificacionEquipo equipo = null;
                
                switch(mensaje.getInstruccion()) {
                    
                    case 4: //recibiendo primera comunicación con un equipo
                        
                        List<Pais> paisesInicio;
                        
                        System.out.println("Recibiendo respuesta inicial de equipo");
                        
                        ipSender = mensaje.getIpSender();
                        procesamientoEquipo = mensaje.getProcesamientoEquipo();
                        paisesInicio = mensaje.getPaisesInicio();
                        //numeroPaisesProcesando = mensaje.getNumeroPaisesProcesando();

                        this.sem.acquire();
                        
                        //reemplazar por nuevo valor de procesamiento
                        this.cargaPorEquipo.replace(ipSender, procesamientoEquipo);
                        //this.paisesProcesandosePorEquipo.put(ipSender, numeroPaisesProcesando
                        
                        equipo = this.estadosEquipos.get(ipSender);   
                        //se conoce que el equipo se encuentra activo, 
                        //se actualiza dicho estado
                        equipo.setActivo(true);   
                        equipo.setComunicacionInicialExitosa(true);
                        
                        this.estadosEquipos.replace(ipSender, equipo);
                        
                        //añadir a la lista de países del broker
                        //los países del equipo que recién se comunica
                        this.paises.addAll(paisesInicio);
                        
                        for(int i=0; i<paisesInicio.size(); i++){
                            
                            //System.out.println("Pais nuevo: "+paisesInicio.get(i).toString());
                            
                            if( this.paisesEnEquipos.get( paisesInicio.get(i).getNombre() )!=null ){
                                //System.out.println("Error: se ha duplicado un país "
                                //        + "en los equipos de procesamiento.");
                                this.sem.release(); 
                                this.terminarEjecucionConErrores("se ha duplicado un país "
                                        + "en los equipos de procesamiento.");
                                //System.exit(1);
                            }
                            
                            this.paisesEnEquipos.put(paisesInicio.get(i).getNombre()
                                    , ipSender);
                            
                            this.conjuntoPaises.put(paisesInicio.get(i).getNombre()
                                    ,paisesInicio.get(i));
                            
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
                        nuevoMensaje.setInstruccion(1);

                        SenderBroker sender = new SenderBroker(ipEquipoARepartir, this.puertoEquipos);
                        sender.enviarMensaje( nuevoMensaje );      
                        System.out.println("Enviado país "+ pais.getNombre() 
                                +" a "+ipEquipoARepartir+" con población "+pais.getPoblacion());
                        
                        //actualizar estado

                        this.paisesEnEquipos.put(pais.getNombre(), ipEquipoARepartir);

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
                        
                        equipo = this.estadosEquipos.get(ipSender);
                        //actualizar estado del equipo que envió mensaje                        
                        equipo.setActivo(true);
                        equipo.setNotificacionReporteCargaEnviada(false);
                        equipo.setRespuestaEntregada(true);

                        this.estadosEquipos.replace(ipSender, equipo);

                        //reemplazar por nuevo valor de uso de procesamiento
                        this.cargaPorEquipo.replace(ipSender, procesamientoEquipo);
                        this.paisesProcesandosePorEquipo.put(ipSender, numeroPaisesProcesando);
                        
                        equipo = this.estadosEquipos.get(ipSender);
                        equipo.setActivo(true);
                        //se conoce que el reporte fue enviado pero ya se retornó
                        equipo.setNotificacionReporteCargaEnviada(false);
                        equipo.setRespuestaEntregada(true);
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
                        ipSender = mensaje.getIpSender();
                        
                        this.sem.acquire();
                        
                        //eliminar país que se está ejecutando de la lista de
                        //países que se van a distribuir
                        if(this.paisesPorDistribuir.get(paisRecibido)!=null){
                            this.paisesPorDistribuir.remove(paisRecibido);
                        }
                        if(this.paisesADistribuirEnEquipos.get(pais.getPoblacion())!=null){
                            this.paisesADistribuirEnEquipos.remove(pais.getPoblacion());
                        }
                        //actualizar en qué equipo quedó ahora ese país
                        this.paisesEnEquipos.replace(paisRecibido, ipSender);
                        
                        this.sem.release();
                        
                        break;
                        
                    case 5://recibe infectados de un pais
                    	
                        System.out.println("ENVÍO DE INFECTADOS DE UN PAÍS A OTRO");
                    	System.out.println("Recibiendo infectados desde "+pais.getNombre());
                    	ipSender = mensaje.getIpSender();
                    	
                    	this.sem.acquire();
                    	 
                    	 //Creación del mensaje
         		Mensaje mensajeContagio = new Mensaje();
                        mensajeContagio.setIpSender(this.ipServidor);
         		mensajeContagio.setInstruccion(5);
         		mensajeContagio.setPais(pais);
         		mensajeContagio.setTexto(mensaje.getTexto()); 
         		mensajeContagio.setNumeroPaisesProcesando(mensaje.getNumeroPaisesProcesando());
         				 
         		String ipEquipoPaisContagiar = this.paisesEnEquipos.get(mensaje.getTexto());
         				
         		//Creacion sender y envio de mensaje
         		SenderEquipo senderContagio = new SenderEquipo(ipEquipoPaisContagiar, this.puertoEquipos);
                        senderContagio.enviarMensaje( mensajeContagio );  
         				 
                        System.out.println("Reenviados infectados desde "+pais.getNombre()+" en "+ipSender +" al pais "+mensaje.getTexto()+" en "+ipEquipoPaisContagiar);
                    	 
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
                
            } 
        } 
        
    } 

    //esta función se encarga de iniciar la función iniciarEscucha dentro de un hilo
    public void iniciarEscuchaServidor(){
        Runnable task1 = () -> { this.iniciarEscucha(); };      
        Thread t1 = new Thread(task1);
        t1.start();
    }
    
    public void iniciarEscuchaBrokers(){
        
        ServerSocket ss = null;
        
        try {
            ss = new ServerSocket(this.puertoBrokers);
        } catch (IOException ex) {
            System.out.println("Error al abrir socket");
            System.exit(1);
        }
        
        System.out.println("Iniciada escucha de mensajes entrantes de brokers");
        
        while (true){
            
            Socket socket = null;    
              
            try { 
                
                // socket object to receive incoming client requests 
                socket = ss.accept(); 
                
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                  
                System.out.println("RECIBIENDO MENSAJE PROVENIENTE DE " + socket);
                
                MensajeBroker mensaje = (MensajeBroker)objectInputStream.readObject();                
                
                String ipSender = mensaje.getIpSender();                
                
                switch(mensaje.getInstruccion()) {
				
                    case 1: //recibiendo primera comunicación con un broker
                        
                        System.out.println("Mensaje inicial desde el broker "+socket);
                        
                        try {
                            this.sem2.acquire();
                        } catch (InterruptedException ex) {
                            System.out.println("Error al intentar activar semáforo 2");
                            System.exit(1);
                        }
                        
                        this.brokers = mensaje.getBrokers();
                        this.equiposPorBroker = mensaje.getEquiposPorBroker();
                        this.numeroBroker = mensaje.getNumeroBroker();
                        
                        System.out.println("Número de este broker: "+this.numeroBroker);
                        System.out.println("Equipos iniciales: "+this.equiposPorBroker);
                        System.out.println("Brokers: "+this.brokers);
                        
                        List<String> equiposInicio = new ArrayList<>();
                        for (HashMap.Entry<String, String> entry : this.equiposPorBroker.entrySet()) {
                            equiposInicio.add(entry.getKey());                               
                        }
                        
                        this.inicializarEquiposBroker(equiposInicio);
                        
                        int contador = 0;
                        for (HashMap.Entry<String, String> entry : this.brokers.entrySet()) {
                            
                            this.ordenBrokerAnillo.put(contador, entry.getKey());
                            
                            NotificacionBroker notBroker = new NotificacionBroker();
                            notBroker.setDisponible(true);
                            notBroker.setNotificacionEnviada(false);
                            notBroker.setHoraUltimaNotificacion(null);
                            notBroker.setRespuestaEntregada(false);
                            this.notificacionesBrokers.put(entry.getKey(), notBroker);
                            
                            contador = contador+1;
                        }                        
                        
                        this.revisarBrokerAnterior();
                        this.iniciarMonitoreoBrokerAnterior();
                        
                        this.sem2.release();
					
			break;
                        
                    case 2: //recibiendo pregunta sobre estado
                        
                        System.out.println("Mensaje preguntado estado desde el broker "+socket);
                        
                        try {
                            this.sem2.acquire();
                        } catch (InterruptedException ex) {
                            System.out.println("Error al intentar activar semáforo 2");
                            System.exit(1);
                        }
                        
                        ///enviarlo al nuevo equipo donde se va a procesar
                        
                        MensajeBroker nuevoMensaje = new MensajeBroker();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setInstruccion(3);
                        nuevoMensaje.setNumeroBroker(this.numeroBroker);

                        SenderBroker sender = new SenderBroker(ipSender, this.puertoBrokers);
                        sender.enviarMensaje( nuevoMensaje );      
                        System.out.println("Enviado respuesta a broker "+ipSender);
                        
                        this.sem2.release();
                        
                        break;
                        
                    case 3:
                        
                        try {
                            this.sem2.acquire();
                        } catch (InterruptedException ex) {
                            System.out.println("Error al intentar activar semáforo 2");
                            System.exit(1);
                        }
                        
                        //actualizar estado de notificación
                        NotificacionBroker notBroker;
                        notBroker = notificacionesBrokers.get(ipSender);                        
                        notBroker.setNotificacionEnviada(false);
                        notBroker.setRespuestaEntregada(false);
                        notificacionesBrokers.replace(ipSender, notBroker);
                        
                        this.sem2.release();
                        
                        break;
                              
                }
				
            }catch (Exception e){ 
                try { 
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Problema al cerrar socket");
                    System.exit(1);
                }
                
            }
        }
    }
    
    //esta función se encarga de iniciar la función iniciarEscucha dentro de un hilo
    public void iniciarEscuchaServidorBrokers(){
        Runnable task1 = () -> { this.iniciarEscuchaBrokers(); };      
        Thread t1 = new Thread(task1);
        t1.start();
    }
    
    //función que envía un mensaje al broker anterior en la configuración
    //de anillo para conocer si aún se encuentra activo
    public void revisarBrokerAnterior(){
        
        TimerTask task = new TimerTask() {

            @Override
            public void run() { 
                
                try {
                    sem2.acquire();
                } catch (InterruptedException ex) {
                    System.out.println("Error al intentar activar semáforo 2");
                    System.exit(1);
                }
                
                String ipBrokerAnterior;
                
                ipBrokerAnterior = ordenBrokerAnillo.get(numeroBroker-1);
                
                String estadoBrokenAnterior;
                
                estadoBrokenAnterior = brokers.get(ipBrokerAnterior);
                
                if(estadoBrokenAnterior.equals("Activo")){
                    
                    System.out.println("\nPREGUNTANDO ESTADO A BROKER ANTERIOR");
                
                    MensajeBroker mensaje = new MensajeBroker();
                    mensaje.setIpSender(ipServidor);
                    mensaje.setInstruccion(2);
                    mensaje.setBrokers(null);
                    mensaje.setEquiposPorBroker(null);
                    mensaje.setNumeroBroker(numeroBroker);

                    SenderBroker sender = new SenderBroker(ipBrokerAnterior, puertoBrokers);
                    Date hora = new Date();
                    sender.enviarMensaje( mensaje ); 

                    //actualizar estado de notificación
                    NotificacionBroker notBroker;
                    notBroker = notificacionesBrokers.get(ipBrokerAnterior);
                    notBroker.setHoraUltimaNotificacion(hora);
                    notBroker.setNotificacionEnviada(true);
                    notBroker.setRespuestaEntregada(false);
                    notificacionesBrokers.replace(ipBrokerAnterior, notBroker);
                
                }
                
                sem2.release();
            }
        
        };

        Timer timer = new Timer();
        //timer.schedule(task, new Date(), 3000);

        //tiempo (ms) que dura para ejecutarse cada vez
        int tiempoPeriodicoEjecucion = 10000;
        //tiempo (ms) que dura para ejecutarse la primera vez
        int tiempoInicialEspera = 30000;

        //la tarea se ejecuta cada t segundos
        timer.schedule(task, tiempoInicialEspera, tiempoPeriodicoEjecucion);
    }
    
    public void monitorearRespuestaBrokerAnterior(){
        
        while(true){
            
            try {
                this.sem2.acquire();
            } catch (InterruptedException ex) {
                System.out.println("Error al intentar activar semáforo 2");
                System.exit(1);
            }
            
            String ipBrokerAnterior;
                
            ipBrokerAnterior = this.ordenBrokerAnillo.get(this.numeroBroker-1);
            
            NotificacionBroker notBroker;
            notBroker = this.notificacionesBrokers.get(ipBrokerAnterior);
                
            if( notBroker.isNotificacionEnviada() == true 
                    && notBroker.isRespuestaEntregada() == false 
                    && this.brokers.get(ipBrokerAnterior).equals("Activo") ){                
                
                Date horaActual = new Date();
                Date horaUltimaNotificacion = notBroker.getHoraUltimaNotificacion();
                
                int diferencia = (int) (horaActual.getTime()-horaUltimaNotificacion.getTime())/1000;
                //si han pasado más de 6 segundos en que el broker no ha respondido
                if( diferencia > 6 ){
                    
                    System.out.println("Broker "+ipBrokerAnterior+" está caído. "
                            + "Tomando el lugar de éste");
                    
                    notBroker.setDisponible(false);
                    notBroker.setNotificacionEnviada(false);
                    this.notificacionesBrokers.replace(ipBrokerAnterior, notBroker); 
                    this.brokers.replace(ipBrokerAnterior, "Inactivo");
                    
                    List<String> equiposAEnviar = new ArrayList<>();
                    for (HashMap.Entry<String, String> entry : this.equiposPorBroker.entrySet()) {
                        
                        if( entry.getValue().equals(ipBrokerAnterior) ){
                            equiposAEnviar.add(entry.getKey());
                            this.equiposPorBroker.replace( entry.getKey() , this.ipServidor);
                        }
                        
                    } 
                    
                    //arrancar este broker con los equipos del que se cayó
                    this.inicializarEquiposBroker(equiposAEnviar);
                    this.iniciarEscuchaServidor();
                    this.establecerComunicacionInicialConEquipos();            
                    this.solicitarCargaEquipos();   
                    this.iniciarMonitorEquiposActivos();
                    this.definirDistribucion();  
                    
                }
                    
            }
            this.sem2.release();
        }
        
    }
    
    //esta función se encarga de iniciar la función iniciarEscucha dentro de un hilo
    public void iniciarMonitoreoBrokerAnterior(){
        Runnable task1 = () -> { this.monitorearRespuestaBrokerAnterior(); };      
        Thread t1 = new Thread(task1);
        t1.start();
    }
}
