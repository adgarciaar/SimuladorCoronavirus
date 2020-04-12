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
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author adgar
 */
public class ServidorEquipo {
    
    private String ipServidor; //guarda la ip de la máquina en que se ejecuta
    private int puerto; //puerto con el que se comunica esta máquina
    private volatile List<Pais> paises; //lista de países que se están ejecutando en este equipo  
    
    //mapa para guardar las duplas <Nombre país, Hilo de ejecución de ese país>
    private volatile HashMap<String, EjecutorPropagacion> hilos;
    
    //semáforo para garantizar que las funciones acceden sólo una a la vez
    //a las variables de esta clase, garantizando consistencia
    private volatile Semaphore sem; 

    //constructor de la clase, se le pasan los países precargados en el archivo
    //de configuración inicial y el puerto por el que se va a comunicar
    public ServidorEquipo(List<Pais> paises, int puerto){
        
        this.paises = new ArrayList<>(paises);
        this.puerto = puerto;
        
        //se inicializa el semáforo con 1, para que sólo una función pueda
        //acceder a la vez a las variables de la clase
        sem = new Semaphore(1); 
        //hilos = new ArrayList<>(); 
        hilos = new HashMap<>(); 
        
        //se consigue la ip de la máquina en que se está ejecutando esta función
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorBroker.class.getName()).log(Level.SEVERE, null, ex);
        }        
        this.ipServidor = inetAddress.getHostAddress();
        
    }
    
    //esta función inicia un hilo de procesamiento por cada país en la lista 
    //de países precargados en el equipo. 
    //Cada hilo ejecuta un modelo de automátas celulares
    public void ejecutarModeloPaisesPrecargados(){
        
        System.out.println("Iniciando procesamiento de países precargados en este equipo"); 
        
        try {
            // acquiring the lock
            this.sem.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(ServidorEquipo.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (Pais pais : this.paises) {

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
    
    /*
    //esta función se PUEDE encargar de actualizar el estado de cada país    
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
        
    }*/
    
    //esta función se encarga de estar pendiente de todas las comunicaciones
    //entrantes al equipo, en un ciclo infinito, que siempre está activo
    
    //dependiendo del valor de la variable instruccion de cada mensaje, se
    //ejecuta cierto bloque de código para actualizar determinadas variables
    //de esta clase o para retornar respuesta al broker
    
    public void iniciarEscucha(){
        
        ServerSocket serverSocket = null;
        
        try {
            serverSocket = new ServerSocket(puerto);
        } catch (IOException ex) {
            System.out.println("Error al abrir socket");
            System.exit(1);
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
                
                long procesamientoTotal = 0;
                
                switch(mensaje.getInstruccion()) {
                    
                    case 1: //recibiendo país desde broker
                        
                        System.out.println("Recibiendo país para procesar");
                        
                        this.sem.acquire(); 

                        paises.add(pais);

                        System.out.println("Recibido el país "+pais.getNombre()+" con "+pais.getPoblacion()+" habitantes");                        

                        // create a new thread object 
                        EjecutorPropagacion t = new EjecutorPropagacion(pais);
                        //hilos.add(t);
                        hilos.put(pais.getNombre(), t);
                        
                        this.sem.release(); 

                        // Invoking the start() method                         
                        t.start(); 
                        System.out.println("Iniciado nuevo hilo para procesar este país"); 
                        
                        //comunicar al broker que el país se recibió
                        
                        ipSender = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setPais(pais);
                        nuevoMensaje.setInstruccion(6);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );  
                        System.out.println("Enviada confirmación de recepción "
                                + "de país al broker");     
                        
                        break;
                        
                    case 4: //comunicación inicial con broker
                         
                        System.out.println("Recibiendo mensaje inicial del broker");
                        
                        if(this.hilos.isEmpty() == true){
                            this.ejecutarModeloPaisesPrecargados();
                        }
                        //this.activarMonitor();
                         
                        ipSender = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstruccion(4);
                        nuevoMensaje.setPaisesInicio(this.paises);
                        
                        this.sem.acquire();
                        
                        procesamientoTotal = 0;
                        for (Pais paisRevision : paises) {
                            procesamientoTotal = procesamientoTotal + paisRevision.getPoblacion();
                        }
                        
                        this.sem.release();
                        
                        nuevoMensaje.setProcesamientoEquipo(procesamientoTotal);
                        nuevoMensaje.setNumeroPaisesProcesando(this.paises.size());

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );  
                        System.out.println("Enviada respuesta inicial al broker");
                         
                        break;
                        
                    case 3: //reportar rendimiento
                         
                        System.out.println("Recibiendo solicitud para reportar procesamiento");
                         
                        ipSender = mensaje.getIpSender();
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setIpSender(this.ipServidor);
                        
                        this.sem.acquire();
                        
                        long[] cargaPaisesPorEquipo = new long[ this.paises.size() ];
                        
                        procesamientoTotal = 0;
                        
                        int index = 0;
                        for (Pais paisRevision : paises) {                            
                            procesamientoTotal = procesamientoTotal + paisRevision.getPoblacion();                            
                            cargaPaisesPorEquipo[ index ] = paisRevision.getPoblacion();
                            index = index + 1;
                        }
                        
                        this.sem.release();
                        
                        //ordenar de menor a mayor la carga de los países
                        quickSort(cargaPaisesPorEquipo, 0, cargaPaisesPorEquipo.length-1);
                        System.out.println(Arrays.toString(cargaPaisesPorEquipo));
                        
                        nuevoMensaje.setProcesamientoEquipo(procesamientoTotal);
                        nuevoMensaje.setNumeroPaisesProcesando(this.paises.size());
                        nuevoMensaje.setPais(null);
                        nuevoMensaje.setInstruccion(3);      
                        nuevoMensaje.setCargaPaisesPorEquipo(cargaPaisesPorEquipo);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );   
                        System.out.println("Enviada información de procesamiento al broker");
                         
                        break;
                        
                    case 7: //broker solicitando un país
                        
                        //Se va a enviar el país con menor población
                        
                        System.out.println("Recibiendo solicitud para enviar país");
                        ipSender = mensaje.getIpSender();
                        
                        this.sem.acquire();
                        
                        EjecutorPropagacion ejecutor;
                        Pais paisSaliente = null;
                        
                        long menorPoblacion = Long.MAX_VALUE;
                        
                        int i = 0, j = 0;
                        for (Pais paisRevision : paises) {
                            if( paisRevision.getPoblacion() < menorPoblacion ){
                                menorPoblacion = paisRevision.getPoblacion();
                                paisSaliente =  paisRevision;
                                j = i;
                            }
                            i = i + 1;
                        }                        
                        
                        //detener la ejecución del modelo de ese país
                        ejecutor = hilos.get( paisSaliente.getNombre() );
                        ejecutor.doStop();
                        //quitar el país de la lista de países
                        this.paises.remove(j);
                        hilos.remove(paisSaliente.getNombre());
                        
                        System.out.println("Detenida la ejecución del país "
                                +paisSaliente.getNombre()+", y removido de este equipo");
                        
                        nuevoMensaje = new Mensaje();
                        nuevoMensaje.setPais(paisSaliente);
                        nuevoMensaje.setIpSender(this.ipServidor);
                        nuevoMensaje.setInstruccion(2);

                        sender = new SenderEquipo(ipSender, this.puerto);
                        sender.enviarMensaje( nuevoMensaje );   
                        System.out.println("Enviado país "+nuevoMensaje.getPais().getNombre()+" al broker");
                        
                        this.sem.release();
                        
                        break;

                    case 8: //ejecución finaliza sin error
                        
                        System.out.println("La ejecución ha finalizado");
                        System.exit(0);
                        
                        break;
                        
                    case 9: //ejecución finaliza con error
                        
                        System.out.println("La ejecución ha finalizado con un error");
                        System.out.println("Error: "+mensaje.getTexto());
                        System.exit(1);
                        
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
    

