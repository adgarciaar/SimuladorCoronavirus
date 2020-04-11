/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Arranque;

import EquipoProcesamiento.ServidorEquipo;
import Broker.ServidorBroker;
import Entidades.Pais;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner; // Import the Scanner class to read text files

/**
 *
 * @author adgar
 */

//esta clase se encarga de iniciar el programa en la máquina en que se encuentre
//ya sea como broker o como un equipo de procesamiento
//dependiendo de los datos en el archivo de configuración
public class Inicio {
    
    public static void main(String args[]) {
        
        List<String> instruccionesConfiguracion = new ArrayList<>();
                
        try {
            File myObj = new File("src/Configuracion/configuracionBroker.txt");
            try (Scanner myReader = new Scanner(myObj)) {
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    instruccionesConfiguracion.add(data.trim());
                }
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("Error: No se encontró archivo de configuración");
            System.exit(1);
        } 
        
        //si es un broker
        if(instruccionesConfiguracion.get(0).equals("broker\tsi")){
            
            boolean esArranque= true;
            
            
            List<String> otrosBrokers = new ArrayList<>();
            List<String> equipos = new ArrayList<>();
            
            String[] arrayLinea = instruccionesConfiguracion.get(1).split("\t");
            int puertoEquipos = Integer.parseInt( arrayLinea[1] );
            int puertoBrokers = Integer.parseInt( arrayLinea[2] );
            
            System.out.println("Puerto para comunicación con equipos: "+puertoEquipos);
            System.out.println("Puerto para comunicación con brokers: "+puertoBrokers+"\n");
            
            arrayLinea = instruccionesConfiguracion.get(2).split("\t");
            int numeroOtrosBrokers = Integer.parseInt(arrayLinea[1]);
            
            System.out.println("Número de otros brokers: "+numeroOtrosBrokers+"\n");
            
            String ipSiguienteBroker;
            
            /*System.out.println("desde "+3);
            System.out.println("hasta "+(3+numeroOtrosBrokers-1));*/
            
            if ( numeroOtrosBrokers > 0 ){
                for (int i = 3; i < 3+numeroOtrosBrokers; i++) {

                    ipSiguienteBroker = instruccionesConfiguracion.get(i);

                    //equipos.put(ipSiguienteEquipo, 0L);
                    otrosBrokers.add(ipSiguienteBroker);

                    System.out.println(ipSiguienteBroker);
                }
            }
            
            System.out.print("\n");
            
            
            arrayLinea = instruccionesConfiguracion.get(3+numeroOtrosBrokers).split("\t");
            int numeroEquipos = Integer.parseInt(arrayLinea[1]);
            
            System.out.println("Número de equipos para procesamiento: "+numeroEquipos+"\n");
            
            String ipSiguienteEquipo;
            
            /*System.out.println("desde "+(3+numeroOtrosBrokers+1));
            System.out.println("hasta "+(3+numeroOtrosBrokers+1+numeroEquipos-1));*/
            
            for (int i = 3+numeroOtrosBrokers+1; i < 3+numeroOtrosBrokers+1+numeroEquipos; i++) {
                
                ipSiguienteEquipo = instruccionesConfiguracion.get(i);
                
                //equipos.put(ipSiguienteEquipo, 0L);
                equipos.add(ipSiguienteEquipo);
                
                System.out.println(ipSiguienteEquipo);
            }
            
            System.out.print("\n");
            
            if (esArranque){
                
                ServidorBroker servidorBroker = new ServidorBroker( puertoEquipos, 
                        puertoBrokers, equipos, otrosBrokers);
                servidorBroker.iniciarEscuchaServidor();
                servidorBroker.establecerComunicacionInicialConEquipos();            
                servidorBroker.solicitarCargaEquipos();   
                servidorBroker.iniciarMonitorEquiposActivos();
                servidorBroker.definirDistribucion();  
                
            }else{
                
                ServidorBroker servidorBroker = new ServidorBroker( puertoEquipos, 
                        puertoBrokers);
                servidorBroker.iniciarEscuchaBrokers();
            }            
            
        //si no es un broker
        }else{
            
            List<Pais> paises = null;
            int puerto = 0;
            
            try{
            
                String[] arrayLinea = instruccionesConfiguracion.get(1).split("\t");
                puerto = Integer.parseInt( arrayLinea[1] );

                paises = new ArrayList<>();

                arrayLinea = instruccionesConfiguracion.get(2).split("\t");
                int numeroPaises = Integer.parseInt(arrayLinea[1]);

                System.out.println("Número de países en este precargados en este equipo: "+numeroPaises+"\n");

                Pais pais;
                String[] datosSiguientePais;

                for (int i = 3; i < 3+numeroPaises; i++) {

                    datosSiguientePais = instruccionesConfiguracion.get(i).split("\t");

                    pais = new Pais();
                    pais.setNombre( datosSiguientePais[0] );
                    pais.setPoblacion( Integer.parseInt(datosSiguientePais[1]) );

                    paises.add(pais);

                    System.out.println(Arrays.toString(datosSiguientePais));
                }
            }catch (Exception e) {
            /* This is a generic Exception handler which means it can handle
             * all the exceptions. This will execute if the exception is not
             * handled by previous catch blocks. */
                System.out.println("Error con los datos del archivo. Por favor, revise las condiciones para éste.");
                System.exit(1);
            } 
            
            System.out.print("\n");
            
            ServidorEquipo servidor = new ServidorEquipo(paises, puerto);
            
            //iniciar el servidor del equipo de procesamiento
            //el cual se queda esperando por la comunicación inicial de un broker
            servidor.iniciarEscuchaServidor();
        }
        
    }
    
}
