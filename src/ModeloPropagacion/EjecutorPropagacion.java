/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModeloPropagacion;

import Entidades.Mensaje;
import Entidades.Pais;
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

    public EjecutorPropagacion() {
    }

    public EjecutorPropagacion(Pais pais) {
        this.pais = pais;
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
    public void infectarOtrosPaises()
    {
    	if (this.pais.getContagiadosCount()<(this.pais.getPoblacion()*0.001)) {
    		for (Map.Entry<String, Integer> entry : this.pais.getPaises().entrySet()) {
    			Random r = new Random();
    			double rand = r.nextInt(100) + r.nextDouble();
    			if( rand < 20) { 
    				
    				//Creacion del mensaje
    				Mensaje nuevoMensaje = new Mensaje();
    				nuevoMensaje.setIpSender(this.ipServidorEquipo);
    				nuevoMensaje.setInstruccion(5);
    				nuevoMensaje.setPais(this.pais);
    				nuevoMensaje.setTexto(entry.getKey()); 
    				
    				//Creacion sender y envio de mensaje
    				SenderEquipo sender = new SenderEquipo(this.ipBrokerActual, this.puerto);
                    sender.enviarMensaje( nuevoMensaje );   
                    
                    
                    System.out.println("Enviado infectados desde pais: "+this.pais.getNombre()+" al pais: "+entry.getKey());
    				//entry.getKey().addEnfermos(10*entry.getValue());
    			}
			}
    			
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
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(EjecutorPropagacion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //función para ejecutar el modelo de automátas celulares
    @Override
    public void run() {
        
    	
    	
        while(keepRunning()) {   
        	pais.generate();
        	System.out.println("pais: "+this.pais.getNombre()+" Enfermos -> "+this.pais.getContagiadosCount()+"  Muertos -> "+this.pais.getMuertosCount());
        	
        	infectarOtrosPaises();
        	
            
            /*try {
            	
                //Thread.sleep(3000);            
                //this.pais.setPoblacion(this.pais.getPoblacion()+10);
            
            	
                //System.out.println("Incrementando poblacion de "+this.pais.getNombre()+" a "+this.pais.getPoblacion());
            } catch (InterruptedException ex) {
                Logger.getLogger(EjecutorPropagacion.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            
        }
    }       
    
}
