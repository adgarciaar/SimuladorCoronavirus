/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.util.Date;

/**
 *
 * @author adgar
 */

//clase que guarda los datos de estado general de cada equipo de procesamiento 
public class Equipo {
    
    private boolean activo;
    private boolean notificacionReporteCargaEnviada;
    private boolean respuestaEntregada;
    private Date horaUltimaNotificacionEnviada;
    private boolean comunicacionInicialExitosa;

    public Equipo() {
    }    

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isNotificacionReporteCargaEnviada() {
        return notificacionReporteCargaEnviada;
    }

    public void setNotificacionReporteCargaEnviada(boolean notificacionReporteCargaEnviada) {
        this.notificacionReporteCargaEnviada = notificacionReporteCargaEnviada;
    }

    public boolean isRespuestaEntregada() {
        return respuestaEntregada;
    }

    public void setRespuestaEntregada(boolean respuestaEntregada) {
        this.respuestaEntregada = respuestaEntregada;
    }

    public Date getHoraUltimaNotificacionEnviada() {
        return horaUltimaNotificacionEnviada;
    }

    public void setHoraUltimaNotificacionEnviada(Date horaUltimaNotificacionEnviada) {
        this.horaUltimaNotificacionEnviada = horaUltimaNotificacionEnviada;
    }

    public boolean isComunicacionInicialExitosa() {
        return comunicacionInicialExitosa;
    }

    public void setComunicacionInicialExitosa(boolean comunicacionInicialExitosa) {
        this.comunicacionInicialExitosa = comunicacionInicialExitosa;
    }
    
}
