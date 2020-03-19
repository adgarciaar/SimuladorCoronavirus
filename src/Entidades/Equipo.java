/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.io.Serializable;

/**
 *
 * @author adgar
 */
public class Equipo implements Serializable {
    
    private String ip;
    private int usoProcesador;

    public Equipo() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getUsoProcesador() {
        return usoProcesador;
    }

    public void setUsoProcesador(int usoProcesador) {
        this.usoProcesador = usoProcesador;
    }
    
}
