/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Arranque;

import Entidades.Pais;
import Entidades.Mensaje;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
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
        
                Thread thread = new Thread(){
            @Override
            public void run(){
                    while(true){
                        int x = 1;
                    }
                }
          };

        thread.start();
        long idThread = thread.getId();
            
        
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        
        while(true){
                    long initialUptime = runtimeMxBean.getUptime();
                    long upTime = runtimeMxBean.getUptime();
                    long cpuTime = threadMxBean.getThreadCpuTime(idThread);
                    
                    long initialCPU = cpuTime;
                    long elapsedTime = (upTime - initialUptime);
                    
                    long nrCPUs = 2;
                    
                    long elapsedCpu = cpuTime - initialCPU;
                    float cpuUsage = elapsedCpu / (elapsedTime * 1000000F * nrCPUs);
                    
                        System.out.println(cpuUsage);
                    }
        
        //SenderEquipo cliente = new SenderEquipo("localhost", 7777);
        //cliente.enviarMensaje(mensaje);
    }
    
}
