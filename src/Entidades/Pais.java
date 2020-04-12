/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entidades;

import java.io.Serializable;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Pais {
	private int id;
	private String nombre;
	private Persona[][] habitantes;
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

	public Pais(int id,String nombre, int poblacion, double tasaVulnerabilidad, double tasaAislamiento, int contagiadosCount, Virus virus) {
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
		this.columns = size+2;
		this.rows = size+2;
		this.habitantes = new Persona[this.rows][this.columns];
		this.virus = virus;
		
		for (int x = 0; x < rows ; x++) {
			for (int y = 0; y < columns ; y++) {
				
				if(x==0||x==rows-1||y==0||y==columns-1){
					this.habitantes[x][y] = new Persona();
				}
				else {
				Random r = new Random();
				boolean isolated = false, vulnerable = false;
				
				double rand1 = r.nextInt(100); 
				double rand2 = r.nextInt(100); 
				if( rand1 < tasaVulnerabilidad ) {
					vulnerable = true;
				}
				
				if( rand2 < tasaAislamiento ) {
					isolated = true;
				}
				
				this.habitantes[x][y] = new Persona(isolated,vulnerable);
				}
			
			}
		}	
		//for (Persona[] row : this.habitantes)
		//	Arrays.fill(row, new Persona());
		
		int occupiedSpots = 0;
		Random random = new Random();
		


		while (occupiedSpots < this.contagiadosCount) {
			//print2D(habitantes);
			int x = random.nextInt(habitantes.length-2);
			int y = random.nextInt(habitantes[x].length-2);
			if (habitantes[x][y].estado == EstadoEnum.SANO) {
				habitantes[x][y].estado = EstadoEnum.CONTAGIADO;
				occupiedSpots++;
			}
		}
		//print2D(habitantes);

	}

	public  void  generate() {
		

		Persona[][] next = habitantes;

		// Loop through every spot in our 2D array and check spots neighbors
		for (int x = 1; x < rows - 1; x++) {
			for (int y = 1; y < columns - 1; y++) {

				// next[x][y] = habitantes[x][y];

				// Add up all the states in a 3x3 surrounding grid
				int sickNeighbors = 0;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						if (habitantes[x + i][y + j] != null) {
							EstadoEnum estado = habitantes[x + i][y + j].estado;

							if (estado == EstadoEnum.CONTAGIADO && x + i != x && y + j != y) {
								sickNeighbors++;
							}
						}
					}

				}

				if ((habitantes[x][y].estado == EstadoEnum.SANO) && (sickNeighbors > 0 && sickNeighbors < 6)) {
					
					if(habitantes[x][y].isolated == true) {
					next[x][y].estado = contagio(this.virus.tasatransmicion/2);
					}else
					next[x][y].estado = contagio(this.virus.tasatransmicion);
				}
				if ((habitantes[x][y].estado == EstadoEnum.SANO) && (sickNeighbors > 6) ) {
					if(habitantes[x][y].isolated == true) {
					next[x][y].estado = contagio(this.virus.tasatransmicion);
					}else
					next[x][y].estado = contagio(this.virus.tasatransmicion*2);
				}
				else if ((habitantes[x][y].estado == EstadoEnum.CONTAGIADO) ) {
					if(habitantes[x][y].vulnerable == true) {
						next[x][y].estado = muerte(this.virus.tasaMortalidad);
					}
				}
			}
			
		}

		// Next is now our board
		habitantes = next;
		//print2D(habitantes);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 
		//System.err.println(this.contagiadosCount);
		

	}

	public static void print2D(Persona mat[][]) {
		// Loop through all rows
		for (Persona[] row : mat)

			// converting each row as string
			// and then printing in a separate line
			System.out.println(Arrays.toString(row));
		System.out.println();
		
	}

	private EstadoEnum muerte(double prob) {
		Random r = new Random();
		double rand = r.nextInt(100) + r.nextDouble();
		if( rand < prob ) { 
			this.contagiadosCount--;
			this.muertosCount++;
			return EstadoEnum.MUERTO;
		} else
			return EstadoEnum.CONTAGIADO;
	}

	private EstadoEnum contagio(double prob) {
		Random r = new Random();
		double rand = r.nextInt(100) + r.nextDouble();
		if( rand < prob ) { 
			this.contagiadosCount++;
			return EstadoEnum.CONTAGIADO;
		} else
			return EstadoEnum.SANO;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Persona[][] getHabitantes() {
		return habitantes;
	}

	public void setHabitantes(Persona[][] habitantes) {
		this.habitantes = habitantes;
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
		this.paises.put(pais,conexiones);
		
	}

	public  void addEnfermos(int cant) {
		  
		
		for (int i = 0; i < cant; i++) {
			Random random = new Random();
			int x = random.nextInt(habitantes.length-2);
			int y = random.nextInt(habitantes[x].length-2);
			if (habitantes[x][y].estado == EstadoEnum.SANO) {
				habitantes[x][y].estado = EstadoEnum.CONTAGIADO;
				this.contagiadosCount++;
			}
		}	
		
		
		
	}

	public void printpaises() {
		for (Map.Entry<String, Integer> entry : this.paises.entrySet()) {
			System.out.println(entry.getKey()+"  ");
		}	 
		
	}
	
	

}
