/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModeloPropagacion;

import Entidades.Pais;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adgar
 */
public class EjecutorPropagacion extends Thread {
    
    private Pais pais;
    private long x=0;
    private boolean doStop = false;

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
    
    public synchronized void doStop() {
        this.doStop = true;        
    }

    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }
    
    public void pausar(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(EjecutorPropagacion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void run() {
        
        while(keepRunning()) {   
            
            try {
                Thread.sleep(3000);            
                this.pais.setPoblacion(this.pais.getPoblacion()+10);
                System.out.println("Incrementando poblacion de "+this.pais.getNombre()+" a "+this.pais.getPoblacion());
            } catch (InterruptedException ex) {
                Logger.getLogger(EjecutorPropagacion.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }       
    
}
