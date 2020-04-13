package Entidades;

import java.io.Serializable;

//tiene los datos de una persona (habitante de un país) requeridos para la infección
public class Persona implements Serializable{
	public EstadoEnum estado;
	public boolean isolated;
	public boolean vulnerable;

	public Persona(boolean isolated,boolean vulnerable) {
		this.estado = EstadoEnum.SANO;
		this.isolated = isolated;
		this.vulnerable = vulnerable; 
	}
	public Persona() {
		this.estado = EstadoEnum.NONE;
	}
	@Override
	public String toString() {
		return "[" + estado + "]";
	}
	
}
