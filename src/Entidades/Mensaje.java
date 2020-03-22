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
public class Mensaje implements Serializable {
    
    private Pais pais;
    private int instrucccion; 
    //1 entregarPaisAEquipo, 2 entregarPaisABroker, 3 entregarRendimiento
    //4 realizarPrimeraComunicacion, 5 distribuirVirus, 6 paísRecibidoParaProcesar
    //7 equipoEntregandoPaisABroker
    
    private String ipSender;
    private Long procesamientoCPU;
    
    private Long procesamientoInferior;
    private Long procesamientoSuperior;
    
    private int instruccionPais; //para saber qué país retornar a broker

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

    public Long getProcesamientoCPU() {
        return procesamientoCPU;
    }

    public void setProcesamientoCPU(Long procesamientoCPU) {
        this.procesamientoCPU = procesamientoCPU;
    }

    public Long getProcesamientoInferior() {
        return procesamientoInferior;
    }

    public void setProcesamientoInferior(Long procesamientoInferior) {
        this.procesamientoInferior = procesamientoInferior;
    }

    public Long getProcesamientoSuperior() {
        return procesamientoSuperior;
    }

    public void setProcesamientoSuperior(Long procesamientoSuperior) {
        this.procesamientoSuperior = procesamientoSuperior;
    }

    public int getInstruccionPais() {
        return instruccionPais;
    }

    public void setInstruccionPais(int instruccionPais) {
        this.instruccionPais = instruccionPais;
    }
    
}
