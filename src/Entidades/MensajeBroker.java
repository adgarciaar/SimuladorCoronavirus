/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author adgar
 */

//clase para los mensajes que se envían entre brokers
public class MensajeBroker extends MensajeGeneral{
    
    //instruccion
    //1 para comunicación inicial entre broker que inicia el sistema y los demás
    //2 para cada broker comunicar su estado a los demás
    
    private LinkedHashMap<String, String> brokers;    
    private volatile HashMap<String, String> equiposPorBroker; 
    private int numeroBroker;

    public MensajeBroker() {
    }

    public LinkedHashMap<String, String> getBrokers() {
        return brokers;
    }

    public void setBrokers(LinkedHashMap<String, String> brokers) {
        this.brokers = brokers;
    }

    public HashMap<String, String> getEquiposPorBroker() {
        return equiposPorBroker;
    }

    public void setEquiposPorBroker(HashMap<String, String> equiposPorBroker) {
        this.equiposPorBroker = equiposPorBroker;
    }

    public int getNumeroBroker() {
        return numeroBroker;
    }

    public void setNumeroBroker(int numeroBroker) {
        this.numeroBroker = numeroBroker;
    }
    
}
