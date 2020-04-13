/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.io.Serializable;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//tiene los datos principales de un país requeridos para la infección
public class Pais implements Serializable {

    private int id;
    private String nombre;
    private int poblacion;
    private double tasaVulnerabilidad;
    private double tasaAislamiento;
    private int sanosCount;
    private int contagiadosCount;
    private int muertosCount;
    private int columns;
    private int rows;
    private Virus virus;
    private HashMap<String, Integer> paises;

    public Pais() {
        super();
    }

    public Pais(int id, String nombre, int poblacion, double tasaVulnerabilidad, double tasaAislamiento, int contagiadosCount, Virus virus) {
        super();
        this.id = id;
        this.paises = new HashMap<>();
        this.nombre = nombre;
        this.poblacion = poblacion;
        this.tasaVulnerabilidad = tasaVulnerabilidad;
        this.tasaAislamiento = tasaAislamiento;
        this.contagiadosCount = contagiadosCount;
        this.muertosCount = 0;
        int size = (int) Math.sqrt(poblacion);
        this.columns = size + 2;
        this.rows = size + 2;
        this.virus = virus;
    }

    //Función encargada de definir si un enfermo muere
    public EstadoEnum muerte(double prob) {
        Random r = new Random();
        double rand = r.nextInt(100) + r.nextDouble();
        if (rand < prob) {
            this.contagiadosCount--;
            this.muertosCount++;
            return EstadoEnum.MUERTO;
        } else {
            return EstadoEnum.CONTAGIADO;
        }
    }

    //Función encargada de definir si una persona se contagia
    public EstadoEnum contagio(double prob) {
        Random r = new Random();
        double rand = r.nextInt(100) + r.nextDouble();
        if (rand < prob) {
            this.contagiadosCount++;
            return EstadoEnum.CONTAGIADO;
        } else {
            return EstadoEnum.SANO;
        }
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getPoblacion() {
        return poblacion;
    }

    public void setPoblacion(int poblacion) {
        this.poblacion = poblacion;
    }

    public double getTasaVulnerabilidad() {
        return tasaVulnerabilidad;
    }

    public void setTasaVulnerabilidad(double tasaVulnerabilidad) {
        this.tasaVulnerabilidad = tasaVulnerabilidad;
    }

    public double getTasaAislamiento() {
        return tasaAislamiento;
    }

    public void setTasaAislamiento(double tasaAislamiento) {
        this.tasaAislamiento = tasaAislamiento;
    }

    public int getSanosCount() {
        return sanosCount;
    }

    public void setSanosCount(int sanosCount) {
        this.sanosCount = sanosCount;
    }

    public int getContagiadosCount() {
        return contagiadosCount;
    }

    public void setContagiadosCount(int contagiadosCount) {
        this.contagiadosCount = contagiadosCount;
    }

    public int getMuertosCount() {
        return muertosCount;
    }

    public void setMuertosCount(int muertosCount) {
        this.muertosCount = muertosCount;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public Virus getVirus() {
        return virus;
    }

    public void setVirus(Virus virus) {
        this.virus = virus;
    }

    public HashMap<String, Integer> getPaises() {
        return paises;
    }

    public void setPaises(HashMap<String, Integer> paises) {
        this.paises = paises;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addPais(String pais, int conexiones) {
        this.paises.put(pais, conexiones);

    }

    public void printpaises() {
        for (Map.Entry<String, Integer> entry : this.paises.entrySet()) {
            System.out.println(entry.getKey() + "  ");
        }
    }

    public void addEnfermo() {
        this.contagiadosCount++;
    }

}
