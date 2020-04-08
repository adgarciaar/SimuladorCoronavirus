package Entidades;

public class Virus {
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
