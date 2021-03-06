package main.java.juego;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import javax.security.auth.login.LoginException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.tools.StandardLocation;

import main.java.Graficos.Font;
import main.java.Graficos.Pantalla;
import main.java.Graficos.SpriteSheet;
import main.java.entidad.Elfo;
import main.java.entidad.Humano;
import main.java.entidad.Jugador;
import main.java.entidad.Orco;
import main.java.entidad.Personaje;
import main.java.cliente.*;
import main.java.nivel.Nivel;
import main.java.util.JugadorModelo;

public class Juego extends Canvas implements Runnable {

	private static final long serialVersionUID = 1L;

	public static final int WIDTH = 160;
	public static final int HEIGHT = WIDTH / 12 * 9;
	public static final int SCALE = 3;
	public static final String NAME = "Game";

	private JFrame frame;

	public boolean running = false;
	public int tickCount = 0;

	private BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	private int[] colours = new int[6 * 6 * 6]; // 6 diferent shades
												// tonalidades//6*6*6=216

	
	//server
	private Socket socket;
	private int puerto=9999;
	private String host="localhost";
	private Thread hiloEscritura ;
	private Thread hiloLectura;
	
	//server
	// sdprivate SpriteSheet spriteSheet = new SpriteSheet("/sprite_sheet.png");
	private Pantalla screen;

	public InputHandler input;
	public Nivel level;

	public Jugador jugador;
	private int x = 0, y = 0;
	private Personaje personaje;
	public Juego(Personaje personaje,Socket socket) {
		this.personaje=personaje;
		this.socket=socket;
		
		setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));// setea
																		// canvas
		setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));

		frame = new JFrame(NAME);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// solo tira exit
		frame.setLayout(new BorderLayout()); // posisiona la ventana

		frame.add(this, BorderLayout.CENTER);// setea el canvas en el centro del
												// JFRAME
		frame.pack();// setea el frame de acuerdo al size

		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

	public void run() {
		long lastTime = System.nanoTime();// agaarra el tiempo actual en nano
											// segundos
		double nsPerTick = 1000000000D / 60D;// D significa double
		// cuantos nano segundos hay en un tick

		int frames = 0;
		int ticks = 0;

		long lastTimer = System.currentTimeMillis();
		double delta = 0;

		init();
		
		
		// server
		
		
		hiloEscritura = new Thread(new HiloEscrituraJugador(socket, jugador));
		hiloLectura= new Thread(new HiloLecturaJugador(socket,this,jugador));
		hiloEscritura.start();
		hiloLectura.start();
		
		
		//server
		while (running) {// controla de alguna manera los frames
			long now = System.nanoTime();
			delta += (now - lastTime) / nsPerTick;
			lastTime = now;
			boolean shouldRender = true;

			while (delta >= 1) {
				ticks++;
				tick();
				delta -= 1;
				shouldRender = true;
			}

			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (shouldRender) {
				frames++;
				render();
			}

			if (System.currentTimeMillis() - lastTimer >= 1000) {
				lastTimer += 1000;
				frames = 0;
				ticks = 0;
			}
		}

	}
	
	public void setJugadores(ArrayList<JugadorModelo> jugadores){
		jugador.setJugadores(jugadores);
	}
	public void init() {
		int index = 0;
		for (int r = 0; r < 6; r++) {// loopea rgb
			for (int g = 0; g < 6; g++) {
				for (int b = 0; b < 6; b++) {
					int rr = (r * 255 / 5);// red red shades desde 0 to 5
					int gg = (g * 255 / 5);// 255 es color transparente
					int bb = (b * 255 / 5);

					colours[index++] = rr << 16 | gg << 8 | bb;
				}
			}
		}
		// Colours.get(555, 000, 550, color4) ejemplo
		screen = new Pantalla(WIDTH, HEIGHT, new SpriteSheet("/sprite_sheet.png"));
		input = new InputHandler(this);// creas el input handler
		level = new Nivel(64, 64);
		String raza="";
		
		
		jugador=new Jugador(level, x, y, input,personaje);
	

		level.addEntidad(jugador);
		

	}
	
	public synchronized void start() {
		running = true;
		new Thread(this).start();

	}

	private synchronized void stop() {
		running = false;
	}

	public void tick() {// updatea el juego, parecido al update de unity
		tickCount++;
		level.tick();

	}

	public void render() {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(3);// triple buffer, reduce el tearing
			return;
		}
		int xOffset = jugador.x - (screen.width / 2);// player centrado
		int yOffset = jugador.y - (screen.height / 2);

		level.renderTiles(screen, xOffset, yOffset);
		for (int x = 0; x < level.width; x++) {
			int color = Colours.get(-1, -1, -1, 000);
			if (x % 10 == 0 && x != 0) {
				color = Colours.get(-1, -1, -1, 500);
			}

			// Font.render(msg, screen, x, yOffset, color);
		}
		/*
		 * for(int y = 0; y<32;y++){ for(int x = 0; x<32;x++){ //los flpis hacen
		 * q salgan todos desordenados boolean flipX=x%2==1; boolean flipY= y%2
		 * ==1; //hacer mirroring de los sprites
		 * 
		 * screen.render(x<<3, y<<3, 0, Colours.get(555, 505, 055,
		 * 550),flipX,flipY);//x<<3 multiplic por 8 te da el pixel vallue } }
		 */// descomentar si no llego
			// rendereo despues del lvl
	/*	String msj = "A Generic JRPG Game";
		Font.render(msj, screen, screen.xOffset + screen.width / 2 - (msj.length() * 8) / 2, screen.yOffset,
				Colours.get(-1, -1, -1, 000), 1);// 000 negro poner como
													// background(primer colo
													// puesto en -1 para q sea
													// invisible)*/

		level.renderEntidades(screen);
		for (int y = 0; y < screen.height; y++) {
			for (int x = 0; x < screen.width; x++) {
				int colourCode = screen.pixels[x + y * screen.width];
				if (colourCode < 255)
					pixels[x + y * WIDTH] = colours[colourCode];
			}
		}

		Graphics g = bs.getDrawGraphics();

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);

		g.dispose();
		bs.show();
	}

	/*public static void main(String[] args) {

		new Juego().start();

	}*/
}
