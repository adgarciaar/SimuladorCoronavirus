/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author adgar
 */

//clase padre para los mensajes (tanto entre equipos-brokers como brokers-brokers)
public class MensajeGeneral implements Serializable {
    
    private int instruccion;
    
    private String ipSender; //ip de la máquina que envía el mensaje

    public MensajeGeneral() {
    }

    public int getInstruccion() {
        return instruccion;
    }

    public void setInstruccion(int instruccion) {
        this.instruccion = instruccion;
    }

    public String getIpSender() {
        return ipSender;
    }

    public void setIpSender(String ipSender) {
        this.ipSender = ipSender;
    }
    
}
