/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entidadesDatos;

import java.io.Serializable;

/**
 *
 * @author adgar
 */
public class Mensaje implements Serializable {
    
    private Pais pais;
    private int instrucccion; 
    //1 entregarPaisAEquipo, 2 entregarPaisABroker, 3 entregarRendimiento
    //4 realizarPrimeraComunicacion, 5 distribuirVirus
    
    private String ipSender;
    private int procesamientoCPU;

    public Mensaje() {
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public int getInstrucccion() {
        return instrucccion;
    }

    public void setInstrucccion(int instrucccion) {
        this.instrucccion = instrucccion;
    }

    public String getIpSender() {
        return ipSender;
    }

    public void setIpSender(String ipSender) {
        this.ipSender = ipSender;
    }

    public int getProcesamientoCPU() {
        return procesamientoCPU;
    }

    public void setProcesamientoCPU(int procesamientoCPU) {
        this.procesamientoCPU = procesamientoCPU;
    }
    
}
