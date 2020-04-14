/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Arranque;

import EquipoProcesamiento.ServidorEquipo;
import Broker.ServidorBroker;
import Entidades.Pais;
import Entidades.Virus;
import GUI.PantallaBroker;
import GUI.PantallaInicio;
import GUI.PantallaPaises;

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
//esta clase se encarga de iniciar el programa en la máquina en que se encuentre
//ya sea como broker o como un equipo de procesamiento
//dependiendo de los datos en el archivo de configuración
public class Inicio {
    
    List<Pais> paisesList;

    public List<Pais> getPaisesList() {
        return paisesList;
    }

    public void setPaisesList(List<Pais> paisesList) {
        this.paisesList = paisesList;
    }

    public void leerArchivo(String ruta, PantallaInicio pantallaInicio) {
            
        List<String> instruccionesConfiguracion = new ArrayList<>();

        try {
            //File myObj = new File("src/Configuracion/configuracionBroker.txt");
            File myObj = new File(ruta);
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

        try {

            //si es un broker
            if (instruccionesConfiguracion.get(0).equals("broker\tsi")) {

                boolean esArranque = true;

                List<String> otrosBrokers = new ArrayList<>();
                List<String> equipos = new ArrayList<>();

                String[] arrayLinea = instruccionesConfiguracion.get(1).split("\t");
                int puertoEquipos = Integer.parseInt(arrayLinea[1]);
                int puertoBrokers = Integer.parseInt(arrayLinea[2]);

                System.out.println("Puerto para comunicación con equipos: " + puertoEquipos);
                System.out.println("Puerto para comunicación con brokers: " + puertoBrokers + "\n");

                arrayLinea = instruccionesConfiguracion.get(2).split("\t");
                int numeroOtrosBrokers = Integer.parseInt(arrayLinea[1]);

                System.out.println("Número de otros brokers: " + numeroOtrosBrokers + "\n");

                String ipSiguienteBroker;

                /*System.out.println("desde "+3);
            System.out.println("hasta "+(3+numeroOtrosBrokers-1));*/
                if (numeroOtrosBrokers > 0) {
                    for (int i = 3; i < 3 + numeroOtrosBrokers; i++) {

                        ipSiguienteBroker = instruccionesConfiguracion.get(i);

                        //equipos.put(ipSiguienteEquipo, 0L);
                        otrosBrokers.add(ipSiguienteBroker);

                        System.out.println(ipSiguienteBroker);
                    }
                }

                System.out.print("\n");

                arrayLinea = instruccionesConfiguracion.get(3 + numeroOtrosBrokers).split("\t");
                int numeroEquipos = Integer.parseInt(arrayLinea[1]);

                System.out.println("Número de equipos para procesamiento: " + numeroEquipos + "\n");

                String ipSiguienteEquipo;

                /*System.out.println("desde "+(3+numeroOtrosBrokers+1));
            System.out.println("hasta "+(3+numeroOtrosBrokers+1+numeroEquipos-1));*/
                for (int i = 3 + numeroOtrosBrokers + 1; i < 3 + numeroOtrosBrokers + 1 + numeroEquipos; i++) {

                    ipSiguienteEquipo = instruccionesConfiguracion.get(i);

                    //equipos.put(ipSiguienteEquipo, 0L);
                    equipos.add(ipSiguienteEquipo);

                    System.out.println(ipSiguienteEquipo);
                }

                System.out.print("\n");
                
                pantallaInicio.setVisible(false);
                
                PantallaBroker pantallaBroker = new PantallaBroker();
                pantallaBroker.actualizarEquipos(equipos);
                pantallaBroker.actualizarBrokers(otrosBrokers);
                pantallaBroker.setVisible(true);
                   
                pantallaBroker.establecerVariables(puertoEquipos, puertoBrokers, 
                        otrosBrokers, equipos);
                
                /*if (esArranque) {

                    ServidorBroker servidorBroker = new ServidorBroker(puertoEquipos,
                            puertoBrokers, equipos, otrosBrokers);
                    servidorBroker.iniciarEscuchaServidorBrokers();
                    servidorBroker.iniciarEscuchaServidor();
                    servidorBroker.establecerComunicacionInicialConEquipos();
                    servidorBroker.solicitarCargaEquipos();
                    servidorBroker.iniciarMonitorEquiposActivos();
                    servidorBroker.definirDistribucion();

                } else {

                    ServidorBroker servidorBroker = new ServidorBroker(puertoEquipos,
                            puertoBrokers);
                    servidorBroker.iniciarEscuchaServidorBrokers();
                }*/

                //si no es un broker
            } else {

                HashMap<String, Pais> paises = null;
                int puerto = 0;

                String[] arrayLinea = instruccionesConfiguracion.get(1).split("\t");
                puerto = Integer.parseInt(arrayLinea[1]);

                paises = new HashMap<>();

                arrayLinea = instruccionesConfiguracion.get(2).split("\t");

                double tasaTransmicionVulnerable = Double.parseDouble(arrayLinea[1]);
                double tasaMortalidadVulnerable = Double.parseDouble(arrayLinea[2]);
                double tasatransmicionNoVulnerable = Double.parseDouble(arrayLinea[3]);
                double tasaMortalidadNoVulnerable = Double.parseDouble(arrayLinea[4]);

                Virus virus = new Virus(tasaTransmicionVulnerable, tasaMortalidadVulnerable, tasatransmicionNoVulnerable,tasaMortalidadNoVulnerable);

                arrayLinea = instruccionesConfiguracion.get(3).split("\t");

                int numeroPaises = Integer.parseInt(arrayLinea[1]);

                System.out.println("Número de países en este precargados en este equipo: " + numeroPaises + "\n");

                Pais pais;
                String[] datosSiguientePais;
                int fin = 4;
                for (int i = 4; i < 4 + numeroPaises; i++) {

                    datosSiguientePais = instruccionesConfiguracion.get(i).split("\t");

                    int id = Integer.parseInt(datosSiguientePais[0]);
                    String nombre = datosSiguientePais[1];
                    int poblacion = Integer.parseInt(datosSiguientePais[2]);
                    double tasaVulnerabilidad = Double.parseDouble(datosSiguientePais[3]);
                    double tasaAislamiento = Double.parseDouble(datosSiguientePais[4]);
                    int contagiadosCount = Integer.parseInt(datosSiguientePais[5]);

                    pais = new Pais(id, nombre, poblacion, tasaVulnerabilidad, tasaAislamiento, contagiadosCount, virus);

                    /*pais = new Pais();
                    pais.setNombre( datosSiguientePais[0] );
                    pais.setPoblacion( Integer.parseInt(datosSiguientePais[1]) );*/
                    paises.put(nombre, pais);

                    System.out.println(Arrays.toString(datosSiguientePais));
                    fin++;
                }

                arrayLinea = instruccionesConfiguracion.get(fin).split("\t");
                int numeroConexiones = Integer.parseInt(arrayLinea[1]);

                if (numeroConexiones != 0) {
                    String[] datosSiguienteConexion;
                    fin++;
                    for (int j = fin; j < fin + numeroConexiones; j++) {
                        datosSiguienteConexion = instruccionesConfiguracion.get(j).split("\t");
                        String pais1 = datosSiguienteConexion[0];
                        String pais2 = datosSiguienteConexion[1];
                        int conexiones = Integer.parseInt(datosSiguienteConexion[2]);

                        if (paises.get(pais1) != null) {

                            paises.get(pais1).addPais(pais2, conexiones);

                        } else {
                            System.out.println("El país que envía infectados "
                                    + "no está inicializado en este equipo. Por"
                                    + "favor inicialicelo en el equipo correspondiente");
                            System.exit(1);
                            //paises.get(pais2).addPais(pais1, conexiones);
                        }

                    }
                }

                System.out.print("\n");
                List<Pais> paisesList = new ArrayList(paises.values());
                for (int i = 0; i < paisesList.size(); i++) {
                    System.out.println(paisesList.get(i).getNombre() + ", conexiones: ");
                    paisesList.get(i).printpaises();
                }
                
                pantallaInicio.setVisible(false);
                PantallaPaises pantallaPaises = new PantallaPaises();
                pantallaPaises.setVisible(true);
                pantallaPaises.addRowToJTable(paisesList);

                ServidorEquipo servidor = new ServidorEquipo(paisesList, puerto,pantallaPaises);

                //iniciar el servidor del equipo de procesamiento
                //el cual se queda esperando por la comunicación inicial de un broker
                servidor.iniciarEscuchaServidor();
                servidor.actualizarPantalla();
            }

        } catch (Exception e) {
            /* This is a generic Exception handler which means it can handle
                all the exceptions. This will execute if the exception is not
                handled by previous catch blocks. */
            System.out.println("Error con los datos del archivo. Por favor, revise las condiciones para éste.");
            //System.out.println(e.toString());
            System.exit(1);
        }
        
    }

}
