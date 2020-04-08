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

//clase cuyos parámetros permiten establecer una comunicación entre los brokers
//y los equipos de procesamiento
public class Mensaje implements Serializable {
    
    //variable para indicar el tipo de operación requerida o a realizar
    //tanto para broker como para equipo de procesamiento
    //los valores que puede tomar con los siguientes
    
    //1 entregarPaisAEquipo, 2 entregarPaisABroker, 3 entregarRendimiento
    //4 realizarPrimeraComunicacion, 5 distribuirVirus, 6 paísRecibidoParaProcesar
    //7 equipoEntregandoPaisABroker
    private int instrucccion; 
    
    private Pais pais; //país que se está trasladando (nulo si se está realizando otra operación)    
    
    private String ipSender; //ip de la máquina que envía el mensaje
    
    //procesamiento total de la máquina (en caso de que sea equipo de procesamiento)
    private Long procesamientoCPU; 
    
    //procesamiento del agente con menor procesamiento (en caso de que sea equipo de procesamiento)
    private Long procesamientoInferior;
    //procesamiento del agente con mayor procesamiento (en caso de que sea equipo de procesamiento)
    private Long procesamientoSuperior;
    
    //variable para indicar a un equipo qué país retornar al broker, 
    //cuando se va a iniciar el balanceo de cargas
    private int instruccionPais; 
    
    private List<Pais> paisesInicio;

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

    public List<Pais> getPaisesInicio() {
        return paisesInicio;
    }

    public void setPaisesInicio(List<Pais> paisesInicio) {
        this.paisesInicio = paisesInicio;
    }
    
}
