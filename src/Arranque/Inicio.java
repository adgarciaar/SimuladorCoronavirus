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
import java.util.HashMap;
import java.util.List;
import java.util.Scanner; // Import the Scanner class to read text files

/**
 *
 * @author adgar
 */
public class Inicio {
    
    public static void main(String args[]) {
        
        List<String> instruccionesConfiguracion = new ArrayList<>();
                
        try {
            File myObj = new File("././configuracion.txt");
            try (Scanner myReader = new Scanner(myObj)) {
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    instruccionesConfiguracion.add(data.trim());
                }
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("Error: No se encontró archivo de configuración");
            System.exit(0);
        }   
        
        /*instruccionesConfiguracion.forEach((linea) -> {
            System.out.println(linea);
        });*/
        
        //si es un broker
        if(instruccionesConfiguracion.get(0).equals("broker\tsi")){
            
            HashMap<String, Integer> equipos = new HashMap<>();
            
            String[] arrayLinea = instruccionesConfiguracion.get(1).split("\t");
            int puerto = Integer.parseInt( arrayLinea[1] );
            
            arrayLinea = instruccionesConfiguracion.get(2).split("\t");
            int numeroEquipos = Integer.parseInt(arrayLinea[1]);
            
            System.out.println("Número de equipos para procesamiento: "+numeroEquipos+"\n");
            
            String ipSiguienteEquipo;
            
            for (int i = 3; i < 3+numeroEquipos; i++) {
                
                ipSiguienteEquipo = instruccionesConfiguracion.get(i);
                
                equipos.put(ipSiguienteEquipo, 0);
                
                System.out.println(ipSiguienteEquipo);
            }
            
            System.out.print("\n");
            
            ServidorBroker servidorBroker = new ServidorBroker( puerto,  equipos);
            servidorBroker.iniciarEscuchaServidor();
            //servidorBroker.establecerComunicacionInicialConEquipos();
            servidorBroker.monitorearCargaEquipos();
        
            
        //si no es un broker
        }else{
            
            String[] arrayLinea = instruccionesConfiguracion.get(1).split("\t");
            int puerto = Integer.parseInt( arrayLinea[1] );
            
            List<Pais> paises = new ArrayList<>();
            
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
            
            System.out.print("\n");
            
            ServidorEquipo servidor = new ServidorEquipo(paises, puerto);
            
            servidor.iniciarEscuchaServidor();
        }
        
    }
    
}
