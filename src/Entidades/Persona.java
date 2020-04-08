package Entidades;

public class Persona {
	EstadoEnum estado;
	boolean isolated;
	boolean vulnerable;

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
