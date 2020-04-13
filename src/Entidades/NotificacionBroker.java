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

//guarda datos para conocer el estado actual de un broker
public class NotificacionBroker {
    
    private boolean disponible;
    private Date horaUltimaNotificacion;
    private boolean notificacionEnviada;
    private boolean respuestaEntregada;

    public NotificacionBroker() {
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public Date getHoraUltimaNotificacion() {
        return horaUltimaNotificacion;
    }

    public void setHoraUltimaNotificacion(Date horaUltimaNotificacion) {
        this.horaUltimaNotificacion = horaUltimaNotificacion;
    }

    public boolean isNotificacionEnviada() {
        return notificacionEnviada;
    }

    public void setNotificacionEnviada(boolean notificacionEnviada) {
        this.notificacionEnviada = notificacionEnviada;
    }

    public boolean isRespuestaEntregada() {
        return respuestaEntregada;
    }

    public void setRespuestaEntregada(boolean respuestaEntregada) {
        this.respuestaEntregada = respuestaEntregada;
    }
    
}
