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

//clase para los mensajes enviados entre equipos y brokers
public class Mensaje extends MensajeGeneral {
    
    //variable para indicar el tipo de operación requerida o a realizar
    //tanto para broker como para equipo de procesamiento
    //los valores que puede tomar con los siguientes
    
    //1 entregarPaisAEquipo, 2 entregarPaisABroker, 3 entregarRendimiento
    //4 realizarPrimeraComunicacion, 5 distribuirVirus, 6 paísRecibidoParaProcesar
    //7 equipoEntregandoPaisABroker //8 terminación sin error //9 terminación con error
    //private int instruccion; 
    
    private Pais pais; //país que se está trasladando (nulo si se está realizando otra operación)    
    
    //private String ipSender; //ip de la máquina que envía el mensaje
    
    //procesamiento total de la máquina (en caso de que sea equipo de procesamiento)
    private Long procesamientoEquipo; 
    
    private List<Pais> paisesInicio;
    
    private int numeroPaisesProcesando;
    
    private long[] cargaPaisesPorEquipo;
    
    private String texto;

    public Mensaje() {
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public Long getProcesamientoEquipo() {
        return procesamientoEquipo;
    }

    public void setProcesamientoEquipo(Long procesamientoEquipo) {
        this.procesamientoEquipo = procesamientoEquipo;
    }

    public int getNumeroPaisesProcesando() {
        return numeroPaisesProcesando;
    }

    public void setNumeroPaisesProcesando(int numeroPaisesProcesando) {
        this.numeroPaisesProcesando = numeroPaisesProcesando;
    }

    public long[] getCargaPaisesPorEquipo() {
        return cargaPaisesPorEquipo;
    }

    public void setCargaPaisesPorEquipo(long[] cargaPaisesPorEquipo) {
        this.cargaPaisesPorEquipo = cargaPaisesPorEquipo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public List<Pais> getPaisesInicio() {
        return paisesInicio;
    }

    public void setPaisesInicio(List<Pais> paisesInicio) {
        this.paisesInicio = paisesInicio;
    }
    
}
