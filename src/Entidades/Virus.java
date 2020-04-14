package Entidades;

import java.io.Serializable;

//tiene los datos del virus requeridos para realizar la infección
public class Virus implements Serializable {

    double tasaTransmicionVulnerable;
    double tasaMortalidadVulnerable;
    double tasatransmicionNoVulnerable;
    double tasaMortalidadNoVulnerable;

   

    public Virus(double tasaTransmicionVulnerable, double tasaMortalidadVulnerable, double tasatransmicionNoVulnerable, double tasaMortalidadNoVulnerable) {
       super();
       this.tasaTransmicionVulnerable = tasaTransmicionVulnerable;
       this.tasaMortalidadVulnerable = tasaMortalidadVulnerable;
       this.tasatransmicionNoVulnerable = tasatransmicionNoVulnerable;
       this.tasaMortalidadNoVulnerable= tasaMortalidadNoVulnerable;
    }

    public double getTasaTransmicionVulnerable() {
        return tasaTransmicionVulnerable;
    }

    public void setTasaTransmicionVulnerable(double tasaTransmicionVulnerable) {
        this.tasaTransmicionVulnerable = tasaTransmicionVulnerable;
    }

    public double getTasaMortalidadVulnerable() {
        return tasaMortalidadVulnerable;
    }

    public void setTasaMortalidadVulnerable(double tasaMortalidadVulnerable) {
        this.tasaMortalidadVulnerable = tasaMortalidadVulnerable;
    }

    public double getTasatransmicionNoVulnerable() {
        return tasatransmicionNoVulnerable;
    }

    public void setTasatransmicionNoVulnerable(double tasatransmicionNoVulnerable) {
        this.tasatransmicionNoVulnerable = tasatransmicionNoVulnerable;
    }

    public double getTasaMortalidadNoVulnerable() {
        return tasaMortalidadNoVulnerable;
    }

    public void setTasaMortalidadNoVulnerable(double tasaMortalidadNoVulnerable) {
        this.tasaMortalidadNoVulnerable = tasaMortalidadNoVulnerable;
    }

    

}

