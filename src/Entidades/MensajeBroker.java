/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author adgar
 */
public class MensajeBroker extends MensajeGeneral{
    
    private LinkedHashMap<String, String> brokers;    
    private List<String> equipos; 
    private int numeroBroker;

    public MensajeBroker() {
    }

    public LinkedHashMap<String, String> getBrokers() {
        return brokers;
    }

    public void setBrokers(LinkedHashMap<String, String> brokers) {
        this.brokers = brokers;
    }

    public List<String> getEquipos() {
        return equipos;
    }

    public void setEquipos(List<String> equipos) {
        this.equipos = equipos;
    }

    public int getNumeroBroker() {
        return numeroBroker;
    }

    public void setNumeroBroker(int numeroBroker) {
        this.numeroBroker = numeroBroker;
    }
    
}
