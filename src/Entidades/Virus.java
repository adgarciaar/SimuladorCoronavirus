package Entidades;

import java.io.Serializable;

//tiene los datos del virus requeridos para realizar la infección
public class Virus implements Serializable {

    double tasatransmicion;
    double tasaMortalidad;

    public Virus(double tasatransmicion, double tasaMortalidad) {
        super();
        this.tasatransmicion = tasatransmicion;
        this.tasaMortalidad = tasaMortalidad;
    }

    public double getTasatransmicion() {
        return tasatransmicion;
    }

    public void setTasatransmicion(double tasatransmicion) {
        this.tasatransmicion = tasatransmicion;
    }

    public double getTasaMortalidad() {
        return tasaMortalidad;
    }

    public void setTasaMortalidad(double tasaMortalidad) {
        this.tasaMortalidad = tasaMortalidad;
    }

}

