/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModeloPropagacion;

import Entidades.EstadoEnum;
import Entidades.Mensaje;
import Entidades.Pais;
import Entidades.Persona;
import EquipoProcesamiento.SenderEquipo;

import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adgar
 */

//clase para ejecutar el modelo de automátas celulares
public class EjecutorPropagacion extends Thread {
    
    private Pais pais; //país para el modelo   
    private boolean doStop = false; //variable para detener 
    private String ipServidorEquipo;
    private String ipBrokerActual;
    private int puerto;
    private Persona[][] habitantes;
    private int columns;
    private int rows;
        
    private boolean band = false;

    public EjecutorPropagacion() {
    }

    public EjecutorPropagacion(Pais pais) {
    	this.pais = pais;
    	int size = (int) Math.sqrt(this.pais.getPoblacion());
        
        this.columns = size+2;
	this.rows = size+2;
        this.habitantes = new Persona[this.rows][this.columns];
        
        for (int x = 0; x < rows ; x++) {
            for (int y = 0; y < columns ; y++) {
				
		if(x==0||x==rows-1||y==0||y==columns-1){
                    this.habitantes[x][y] = new Persona();
                }else {
                    Random r = new Random();
                    boolean isolated = false, vulnerable = false;
				
                    double rand1 = r.nextInt(100); 
                    double rand2 = r.nextInt(100); 
                    if( rand1 < this.pais.getTasaVulnerabilidad() ) {
			vulnerable = true;
                    }
				
                    if( rand2 < this.pais.getTasaAislamiento() ) {
			isolated = true;
                    }
				
                    this.habitantes[x][y] = new Persona(isolated,vulnerable);
		}
			
            }
        }	
		
	int occupiedSpots = 0;
        Random random = new Random();

        while (occupiedSpots < this.pais.getContagiadosCount()) {
            int x = random.nextInt(habitantes.length-2);
            int y = random.nextInt(habitantes[x].length-2);
            if (habitantes[x][y].estado == EstadoEnum.SANO) {
                habitantes[x][y].estado = EstadoEnum.CONTAGIADO;
                occupiedSpots++;
            }
	}
    }
    
    public  void addEnfermos(int cant) {		  
		
	for (int i = 0; i < cant; i++) {
            Random random = new Random();
            int x = random.nextInt(habitantes.length-2);
            int y = random.nextInt(habitantes[x].length-2);
            if (habitantes[x][y].estado == EstadoEnum.SANO) {
		habitantes[x][y].estado = EstadoEnum.CONTAGIADO;
		this.pais.addEnfermo();
            }
        }	
			
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }
    
    public String getIpServidorEquipo() {
        return ipServidorEquipo;
    }

    public void setIpServidorEquipo(String ipServidorEquipo) {
	this.ipServidorEquipo = ipServidorEquipo;
    }
	
    public int getPuerto() {
	return puerto;
    }

    public void setPuerto(int puerto) {
	this.puerto = puerto;
    }

    public String getIpBrokerActual() {
	return ipBrokerActual;
    }

    public void setIpBrokerActual(String ipBrokerActual) {
	this.ipBrokerActual = ipBrokerActual;
    }

	//Funcion encargada de enviar infectados por medio del broker
    public void infectarOtrosPaises() {

        if (this.band == false && this.pais.getContagiadosCount() > (this.pais.getPoblacion() * 0.01)) {
            System.out.println("infectar");
            for (Map.Entry<String, Integer> entry : this.pais.getPaises().entrySet()) {
                Random r = new Random();
                double rand = r.nextInt(100) + r.nextDouble();
                //if( rand < 20) { 
                System.out.println("envio infectados");
                //Creación del mensaje
                Mensaje nuevoMensaje = new Mensaje();
                nuevoMensaje.setIpSender(this.ipServidorEquipo);
                nuevoMensaje.setInstruccion(5);
                nuevoMensaje.setPais(this.pais);
                nuevoMensaje.setTexto(entry.getKey());
                nuevoMensaje.setNumeroPaisesProcesando(10 * entry.getValue());

                //Creacion sender y envio de mensaje
                SenderEquipo sender = new SenderEquipo(this.ipBrokerActual, this.puerto);
                sender.enviarMensaje(nuevoMensaje);

                System.out.println("Enviado infectados desde pais: "
                        + this.pais.getNombre() + " al pais: " + entry.getKey());
                //entry.getKey().addEnfermos(10*entry.getValue());
                //}
            }
            this.band = true;
        }
    }
    
    //función para detener la ejecución del hilo
    public synchronized void doStop() {
        this.doStop = true;        
    }

    //función para conocer si el hilo debe continuar en ejecución 
    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }
    
    //función para pausar la ejecución del hilo
    public void pausar(){
        try {            
            System.out.println("Pausando hilo que ejecuta infección en país "+this.pais.getNombre());
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(EjecutorPropagacion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public  void  generate() {		

	Persona[][] next = habitantes;

        for (int x = 1; x < rows - 1; x++) {
            for (int y = 1; y < columns - 1; y++) {

                int sickNeighbors = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (habitantes[x + i][y + j] != null) {
                            EstadoEnum estado = habitantes[x + i][y + j].estado;

                            if (estado == EstadoEnum.CONTAGIADO && x + i != x && y + j != y) {
                                sickNeighbors++;
                            }
                        }
                    }
                }

                if ((habitantes[x][y].estado == EstadoEnum.SANO) && (sickNeighbors > 0 && sickNeighbors < 6)) {

                    if (habitantes[x][y].isolated == true) {
                        next[x][y].estado = this.pais.contagio(this.pais.getVirus().getTasatransmicion() / 2);
                    } else {
                        next[x][y].estado = this.pais.contagio(this.pais.getVirus().getTasatransmicion());
                    }
                }
                if ((habitantes[x][y].estado == EstadoEnum.SANO) && (sickNeighbors > 6)) {
                    if (habitantes[x][y].isolated == true) {
                        next[x][y].estado = this.pais.contagio(this.pais.getVirus().getTasatransmicion());
                    } else {
                        next[x][y].estado = this.pais.contagio(this.pais.getVirus().getTasatransmicion() * 2);
                    }
                } else if ((habitantes[x][y].estado == EstadoEnum.CONTAGIADO)) {
                    if (habitantes[x][y].vulnerable == true) {
                        next[x][y].estado = this.pais.muerte(this.pais.getVirus().getTasaMortalidad());
                    }
                }
            }

        }

        // Next is now our board
        habitantes = next;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		
    }
    
    //función para ejecutar el modelo de automátas celulares
    @Override
    public void run() {
        
        while(keepRunning()) {   
            generate();
            System.out.println("pais: "+this.pais.getNombre()+" Enfermos -> "+
                    this.pais.getContagiadosCount()+"  Muertos -> "+this.pais.getMuertosCount());        	
            infectarOtrosPaises();            
        }
    }

    public void InfectarPais(int infectados) {
	System.out.println("Llegaron "+infectados+" a "+this.pais.getNombre());
	addEnfermos(infectados);		
    }       
    
}
