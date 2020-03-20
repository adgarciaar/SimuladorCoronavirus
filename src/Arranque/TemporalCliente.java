/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Arranque;

import Entidades.Pais;
import Entidades.Mensaje;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author adgar
 */
public class TemporalCliente {
    
    public static void main(String args[]) {
        
        Pais pais = new Pais();
        pais.setNombre("Colombia");
        pais.setPoblacion(1400);
        
        Mensaje mensaje = new Mensaje();        
        mensaje.setPais(pais);
        mensaje.setInstrucccion(2);       
        
        //SenderEquipo cliente = new SenderEquipo("localhost", 7777);
        //cliente.enviarMensaje(mensaje);
    }
    
}
