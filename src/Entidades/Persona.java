package Entidades;

import java.io.Serializable;

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
