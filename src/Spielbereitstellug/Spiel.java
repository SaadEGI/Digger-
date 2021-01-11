package Spielbereitstellug;

import Spielverlauf.*;
import org.json.JSONObject;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


public class Spiel extends JPanel implements Runnable {

	//dev stuff

	boolean devFrames = false;

	// game setup

	boolean isMultiplayer;
	boolean isHost;

	// System-/ Filestructure

	private final String skinName = "original_skin"; // Skinnname
	private final String levelfolder_name = "bin/level/"; // ./level/level-01.json ...
	private final String skinfolder_name = "bin/skins/"; // ./skin/sink_original.json,...

	// static content
	private final Skin current_skin;
	private final ArrayList<Map> mapChain;
	int current_map = 0;
	private final double[] border = {0.4, 0.2}; // Wandstärke x,y
	private final double topbarHeight = 1; // Faktor von Feldgröße

	// temp. Att.

	private Level aktuelles_level;
	private int spielstand;

	private int field_size;
	private int old_field_size;

	// Spieler

	private Spieler sp1;
	private Spieler sp2;

	// Testing

	private int way = 5;
	private boolean down = true;

    int[] PixelToInt(int[] pixelPos) {
        int[]fp = new int[2];
        int[] borderOffset = getBorderOffset();
        fp[0] = ((pixelPos[0]-(field_size/2)-borderOffset[0])/field_size)+1;
        fp[1] = ((pixelPos[1]-(field_size/2)-borderOffset[1])/field_size)+1;
        return fp;
    }
	public Spiel(int[] panel_size, boolean isHost, boolean isMultiplayer) {

    	// initalisiere game setup

		this.isHost = isHost;
		this.isMultiplayer = isMultiplayer;

		// initialisiere Skin
		current_skin = new Skin(new File(skinfolder_name), skinName); // Loades original_skin.png and original.json from skins/

		// initialisiere Mapchain

		String[] maps = new File(levelfolder_name).list(); // read Level from Folder

		// create Map and add it to chain

		System.out.println("Anzahl der gelesenen Karten: " + maps.length);

		mapChain = new ArrayList<>();

		for (String map : maps) {

			// read Level-File
			JSONObject obj = null;
			try {
				obj = new JSONObject(new String(Files.readAllBytes(Paths.get(levelfolder_name + map))));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// create Map and add to chain
			mapChain.add(new Map(obj, current_skin));
		}

		// add Player

		sp1 = null;
		sp2 = null;

		aktuelles_level = createNextLevel();

		// refresh sizing

		refreshSizing();


		// TESTING


		// setze Monster ein
		ArrayList<Monster> monster = aktuelles_level.getMap().getMonster();

		monster.add(new Nobbin(getCenterOf(aktuelles_level.getMap().getSpawn_monster()), current_skin));
		monster.add(new Nobbin(getCenterOf(aktuelles_level.getMap().getSpawn_monster()), current_skin));
		monster.add(new Nobbin(getCenterOf(aktuelles_level.getMap().getSpawn_monster()), current_skin));

		int[] fp = {1,1};
		int[] pos = getCenterOf(fp);

		aktuelles_level.getMap().setzeHobbin(pos);

		// setze Kirsche ein

		aktuelles_level.getMap().showKirsche();

		// Ausgaben, Infos
		System.out.println("Anzahl Nobbins: " + aktuelles_level.getMap().getNobbins().size());
		System.out.println("Anzahl Hobbins: " + aktuelles_level.getMap().getHobbins().size());

	}

	private void refreshSizing() {

		// Speichere Änderung
		old_field_size = field_size;

		// setze Feldgröße

		Dimension d = this.getSize();

		int felderX = aktuelles_level.getMap().getPGSize()[0];
		int felderY = aktuelles_level.getMap().getPGSize()[1];

		if(d.width == 0 || d.height == 0)
			d = new Dimension(500, 500);

		int w_temp_size = (int)((double)d.width / ( (double)felderX + ( 2*border[0]) ));
		int h_temp_size = (int)((double)d.height / ( (double)felderY + ( 2*border[1]) + topbarHeight ));

		field_size = Math.min(w_temp_size, h_temp_size);

		// berechne neue Pixelpositionen

		if(field_size != old_field_size && old_field_size != 0) {
			double factor = (double) field_size / (double) old_field_size;

			if(sp1 != null)
				for (int i = 0; i <= sp1.getPosition().length - 1; i++)
					sp1.getPosition()[i] *= factor;

			if(sp2 != null)
				for (int i = 0; i <= sp2.getPosition().length - 1; i++)
					sp2.getPosition()[i] *= factor;

			for (Monster m : aktuelles_level.getMap().getMonster())
				for (int i = 0; i <= m.getPosition().length - 1; i++)
					m.getPosition()[i] *= factor;
		}

	}

	private int getTopBarHeight(){
		return (int)(field_size*topbarHeight);
	}

	public int[] getFieldOf(int[] pos){

		int[] borderOffest = getBorderOffset();

		int[] fp = new int[2];

		if((pos[0]-borderOffest[0]) < 0)
			fp[0] = -1;
		else
			fp[0] = ((pos[0]-borderOffest[0])/field_size) + 1;

		if((pos[1]-borderOffest[1]-getTopBarHeight()) < 0)
			fp[1] = -1;
		else
			fp[1] = ((pos[1]-borderOffest[1]-getTopBarHeight())/field_size) + 1;

		return fp;
	}

	/**
	 *	Spiellogik, die Positionen prüft und Ereignisse aufruft.
	 * @return false if plaer dead; else true if game schoud be contiued
	 */

	private boolean loop() {

		// Take Time, set period
		long DELAY_PERIOD = 10;
		long beginTime = System.currentTimeMillis();

		if(isHost) {
			// link current lists
			ArrayList<Diamant> diamants = aktuelles_level.getMap().getDiamonds();
			ArrayList<Monster> monsters = aktuelles_level.getMap().getMonster();
			ArrayList<Hobbin> hobbins = aktuelles_level.getMap().getHobbins();
			ArrayList<Nobbin> nobbins = aktuelles_level.getMap().getNobbins();
			ArrayList<Geldsack> geldsacke = aktuelles_level.getMap().getGeldsaecke();
			ArrayList<Geld> gelds = aktuelles_level.getMap().getGeld();
			ArrayList<Tunnel> tunnels = aktuelles_level.getMap().getTunnel();
			ArrayList<Feuerball> feuerballs = aktuelles_level.getMap().getFeuerball();


			/// Prüfroutinen

			ArrayList<Spieler> spielers = new ArrayList<>();
			if (sp2 != null && sp2.isAlive())
				spielers.add(sp2);

			if (sp1 != null && sp1.isAlive())
				spielers.add(sp1);

			for (Iterator<Spieler> spIterator = spielers.iterator(); spIterator.hasNext(); ) {

				Spieler sp = spIterator.next();

				// alle Diamanten gesammel?
				if (aktuelles_level.getMap().getDiamonds().size() == 0) {
					// dann nächstes Level
					aktuelles_level = createNextLevel();

					//reset players position
					sp.setPosition(getCenterOf(aktuelles_level.getMap().getSpawn_SP1()));

					// vervolständigen zB von Scores
				}


				// Spieler trifft Diamant
				for (Iterator<Diamant> iterator = diamants.iterator(); iterator.hasNext(); ) {
					Diamant single_item = iterator.next();
					if (Arrays.equals(single_item.getField(), getFieldOf(sp.getPosition()))) {
						iterator.remove();
						spielstand += single_item.getValue();
					}
				}

				// Spieler trifft Monster
		/*
				for (Iterator<Monster> iterator = monsters.iterator(); iterator.hasNext();) {
					Monster m = iterator.next();
					if(Arrays.equals(getFieldOf(m.getPosition()),( getFieldOf(sp.getPosition())))){
						if(sp.isAlive()) {
							sp.decrementLife();
							sp.setPosition(getCenterOf(aktuelles_level.getMap().getSpawn_SP1()));
						}
					}
				}
		*/
				// Spieler trifft Boden
				int[] fpSp = getFieldOf(sp.getPosition());
				DIRECTION dirSp = sp.getMoveDir();

				ArrayList<Tunnel> tt = aktuelles_level.getMap().getTunnel(fpSp);

				if (tt.size() == 1) {
					TUNNELTYP ttyp = tt.get(0).getTyp();

					if (((dirSp == DIRECTION.UP || dirSp == DIRECTION.DOWN) && ttyp == TUNNELTYP.HORIZONTAL) ||
							((dirSp == DIRECTION.RIGHT || dirSp == DIRECTION.LEFT) && ttyp == TUNNELTYP.VERTICAL)) {

						TUNNELTYP arrangement;

						if (dirSp == DIRECTION.UP || dirSp == DIRECTION.DOWN)
							arrangement = TUNNELTYP.VERTICAL;
						else
							arrangement = TUNNELTYP.HORIZONTAL;

						aktuelles_level.getMap().addTunnel(new Tunnel(fpSp, arrangement, current_skin));
					}
				} else if (tt.size() == 0) {
					if (dirSp == DIRECTION.RIGHT || dirSp == DIRECTION.LEFT)
						aktuelles_level.getMap().addTunnel(new Tunnel(fpSp, TUNNELTYP.HORIZONTAL, current_skin));
					else if (dirSp == DIRECTION.UP || dirSp == DIRECTION.DOWN)
						aktuelles_level.getMap().addTunnel(new Tunnel(fpSp, TUNNELTYP.VERTICAL, current_skin));
				}


				// Spieler 1 trifft Geld
				for (Iterator<Geld> iterator = gelds.iterator(); iterator.hasNext(); ) {
					Geld gd = iterator.next();
					if (Arrays.equals(gd.getField(), getFieldOf(sp.getPosition()))) {

						iterator.remove();
						spielstand += gd.getValue();
					}
				}

				/*--------------------------------------------------------------------------------------------------------------------*/

				///Geldsack:

				//Geldsack trifft Tunnel // Geldscak trifft Spieler 1 // Geldscak trifft Spieler 2 //Geld erstellen
				for (Iterator<Geldsack> iterator = geldsacke.iterator(); iterator.hasNext(); ) {
					Geldsack gs = iterator.next();


					// nach l/r bewegen
					if (Arrays.equals(gs.getField(), getFieldOf(sp.getPosition()))) {
						int[] PGSize = aktuelles_level.getMap().getPGSize();
						int[] newField1 = gs.getField();
						if (sp.getMoveDir() == DIRECTION.RIGHT) {
							if (newField1[0] < PGSize[0])
								gs.addFieldPosOff(1, 0);
						} else if (sp.getMoveDir() == DIRECTION.LEFT) {
							if (1 < newField1[0])
								gs.addFieldPosOff(-1, 0);
						}
					}

					// Geldsack trifft Geldsack
					for (Iterator<Geldsack> it = geldsacke.iterator(); it.hasNext(); ) {
						Geldsack g2 = it.next();
						int[] PGSize = aktuelles_level.getMap().getPGSize();
						int[] newField1 = gs.getField();
						int[] newField2 = g2.getField();
						if (gs != g2) {
							if (Arrays.equals(gs.getField(), g2.getField())) {
								if (newField1[0] + newField2[0] < PGSize[0]) {
									g2.addFieldPosOff(1, 0);
								} else if (1 < newField2[0] + newField1[0]) {
									g2.addFieldPosOff(-1, 0);
								}
							}
						}
					}

					// Geldsack trifft auf Boden
					int[] current_field = gs.getField();
					int[] check_field = current_field.clone();
					check_field[1]++;

					if (aktuelles_level.getMap().getTunnel(check_field).size() > 0) {
						gs.addFieldPosOff(0, 1);
						gs.setFalling(true);
						gs.incFallHeight();

					} else if (gs.getFalling()) {
						if (gs.getFallHeight() > 1) {
							aktuelles_level.getMap().addGeld(new Geld(gs.getField(), current_skin));
							iterator.remove();
						} else
							gs.resetFallHeight();
					}

					// Geldsack fällt auf Spieler

					if (Arrays.equals(getFieldOf(sp.getPosition()), gs.getField()) && gs.getFalling()) {
						if (sp.isAlive()) {
							sp.decrementLife();
							sp.setPosition(getCenterOf(aktuelles_level.getMap().getSpawn_SP1()));
						}
					}
				}

				//Entferne Geld nach x Sek

				for (Iterator<Geld> iterator = gelds.iterator(); iterator.hasNext(); ) {
					Geld g = iterator.next();

					if (g.outOfTime())
						iterator.remove();
					else
						g.decRemainingTime(DELAY_PERIOD);
				}

				//Feuerball trifft Monster
				for (Iterator<Feuerball> iterator = feuerballs.iterator(); iterator.hasNext(); ) {
					Feuerball fb = iterator.next();
					for (Iterator<Monster> iter = monsters.iterator(); iter.hasNext(); ) {
						Monster m = iter.next();
						if (Arrays.equals(getFieldOf(fb.getPosition()), getFieldOf(m.getPosition()))) {
							iterator.remove();
							iter.remove();
							break;
						}
					}

					//Feuerball trifft Geldsack
					for (Iterator<Geldsack> it = geldsacke.iterator(); it.hasNext(); ) {
						Geldsack gs = it.next();
						if (Arrays.equals(getFieldOf(fb.getPosition()), gs.getField())) {
							iterator.remove();
						}
					}

					if (fb.getMovDir()==DIRECTION.UP){
						fb.addPosOff(0,-1);
					}
					if (fb.getMovDir()==DIRECTION.DOWN){
						fb.addPosOff(0,1);
					}
					if (fb.getMovDir()==DIRECTION.RIGHT){
						fb.addPosOff(1,0);
					}
					if (fb.getMovDir()==DIRECTION.LEFT){
						fb.addPosOff(-1,0);
					}
				}
				//Feuerball trifft Wand
				//Feuerball trifft Boden

				///Bonsmodus aktivieren:
				// Spieler 1 trifft Kirsche ->
				for (Iterator<Monster> iterator = monsters.iterator(); iterator.hasNext(); ) {
					Monster m = iterator.next();
					if (aktuelles_level.getMap().getKirsche().getVisible()) {
						if (Arrays.equals(aktuelles_level.getMap().getKirsche().getField(), getFieldOf(sp.getPosition()))) {
							aktuelles_level.getMap().hideKirsche();
							spielstand += aktuelles_level.getMap().getKirsche().getValue();
							if (Arrays.equals(sp.getPosition(), m.getPosition())) {
								iterator.remove();
							}
						}
					}
				}

				///Monster:

				// Hobbin trifft Diamant
				for (Iterator<Diamant> iterator = diamants.iterator(); iterator.hasNext(); ) {
					Diamant d = iterator.next();
					for (Iterator<Hobbin> it = hobbins.iterator(); it.hasNext(); ) {
						Hobbin h = it.next();
						if (Arrays.equals(d.getField(), getFieldOf(h.getPosition()))) {
							iterator.remove();
						}
					}
				}

				// Monster trifft Geld
				for (Iterator<Geld> iterator = gelds.iterator(); iterator.hasNext(); ) {
					Geld g = iterator.next();
					for (Iterator<Monster> it = monsters.iterator(); it.hasNext(); ) {
						Monster h = it.next();
						if (Arrays.equals(g.getField(), getFieldOf(h.getPosition()))) {
							iterator.remove();
						}
					}
				}

				// Monster trifft Geldsack
				for (Iterator<Geldsack> iterator = geldsacke.iterator(); iterator.hasNext(); ) {
					Geldsack g = iterator.next();
					for (Iterator<Monster> it = monsters.iterator(); it.hasNext(); ) {
						Monster mo = it.next();
						int[] newField = g.getField();
						int[] PGSize = aktuelles_level.getMap().getPGSize();
						if (Arrays.equals(g.getField(), getFieldOf(mo.getPosition()))) {
							if (mo.getMoveDir() == DIRECTION.RIGHT) {
								if (newField[0] < PGSize[0])
									g.addFieldPosOff(1, 0);
							} else if (mo.getMoveDir() == DIRECTION.LEFT) {
								if (1 < newField[0])
									g.addFieldPosOff(-1, 0);
							}
						}
					}
				}

/*
				// Nobbin trifft Nobbin && Hobbin setzen
				int[] MSpoint = aktuelles_level.getMap().getSpawn_monster();
				int Max_Monster = aktuelles_level.getMaxMonster();
					for (Iterator<Nobbin> iter = nobbins.iterator(); iter.hasNext(); ) {
						Nobbin n1 = iter.next();
						for (Iterator<Nobbin> it = nobbins.iterator(); it.hasNext(); ) {
							Nobbin n2 = it.next();
							if (n1 != n2) {
								if (monsters.size() <= Max_Monster && !Arrays.equals(n1.getPosition(), MSpoint)
											&& !Arrays.equals(n2.getPosition(), MSpoint)) {
									if (Arrays.equals(n1.getPosition(), n2.getPosition())) {
										aktuelles_level.getMap().setzeHobbin(n1.getPosition());
										iter.remove();
									}
								}
							}
						}
					}
*/


				/*
				if(aktuelles_level.getMap().getMonster().size() == 0) {
					// dann nächstes Level
					aktuelles_level = createNextLevel();

					// vervolständigen zB von Scores

				}
				*/

				// Monster verfolgt Spieler
				for (Iterator<Monster> iterator = monsters.iterator(); iterator.hasNext();) {
					Monster m = iterator.next();

					int[] m_pos = m.getPosition();
					int[] s_pos = sp.getPosition();
					int x_off = 0;
					int y_off = 0;

					if (m_pos[0] > s_pos[0])
						x_off = -1;
						// m.addPosOff(-1,0);
					else
						// m.addPosOff(1,0);
						x_off = 1;

					if (m_pos[1] > s_pos[1])
						// m.addPosOff(0,-1);
						y_off = -1;
					else
						// m.addPosOff(0,1);
						y_off = 1;


					if (!aktuelles_level.getMap().getTunnel(PixelToInt(new int[]{x_off + m_pos[0], y_off + m_pos[1]})).isEmpty())//check if nextpos is a tunnel or no, and then choose to execute the move or no
						m.addPosOff(x_off, y_off);
					else if (!aktuelles_level.getMap().getTunnel(PixelToInt(new int[]{m_pos[0], y_off + m_pos[1]})).isEmpty())
						m.addPosOff(0, y_off);
					else if (!aktuelles_level.getMap().getTunnel(PixelToInt(new int[]{x_off + m_pos[0], m_pos[1]})).isEmpty())
						m.addPosOff(x_off, 0);

				}


					/*
					Monster m = aktuelles_level.getMap().getMonster().get(0);
					int[] m_pos = m.getPosition();
					int[] s_pos = sp.getPosition();
					int[] monsternext = m_pos;
					double dx1 = s_pos[0] - m_pos[0];
					double dy1 = s_pos[1] - m_pos[1];
					double norm = Math.sqrt(dx1*dx1+dy1*dy1);
					dx1 /= norm;
					dy1 /= norm;

					dx1 *= 2;
					dy1 *= 2;
					m.addPosOff(0, 0);



					|||
					Monster m = aktuelles_level.getMap().getMonster().get(0);
					int [] m_pos = m.getPosition();
					int[] s_pos = sp.getPosition();


					int  x_off = 0;
					int y_off = 0;
					int[] monsternext = m_pos;


					if (m_pos[0] > s_pos[0]){
						monsternext[0] =  m_pos[0] - 1;
						for (Tunnel num : tunnels) {
							if(num.getField() != monsternext){
								x_off=0;
								break;
							}
							else{
								x_off = -1;}

					}

					}
					else
					{
						monsternext[0] =  m_pos[0] + 1;
						for (Tunnel num : tunnels) {
						if(num.getField()!=monsternext){
							x_off=0;
							break;
						}
						else{
							x_off = 1;}

						}}
					if (m_pos[1] > s_pos[1])
					{
						monsternext[1] =  m_pos[1]  - 1;
						for (Tunnel num : tunnels) {
							if(num.getField()!=monsternext){
								y_off=0;
								break;
							}

						else{
							y_off = -1;}

						}}
					else{
						monsternext[1] =  m_pos[1] + 1;
						for (Tunnel num : tunnels) {
							if(num.getField()!=monsternext){

								y_off=0;
								break;
							}

						else{
							y_off = 1;}

						}}
				m.addPosOff(x_off, y_off);

						*/


				// Hobbin trifft Boden
				/*
				for (Iterator<Hobbin> it = hobbins.iterator(); it.hasNext();){
						Hobbin h = it.next();

					 int[] fph = getFieldOf(h.getPosition());
				DIRECTION dirHp = h.getMoveDir();

				//ArrayList<Tunnel> tt = aktuelles_level.getMap().getTunnel(fph);

				if(tt.size() == 1){
					TUNNELTYP ttyp = tt.get(0).getTyp();


					if( ((dirHp == DIRECTION.UP || dirHp == DIRECTION.DOWN) && ttyp == TUNNELTYP.HORIZONTAL) ||
						((dirHp == DIRECTION.RIGHT || dirHp == DIRECTION.LEFT) && ttyp == TUNNELTYP.VERTICAL) ){

						TUNNELTYP arrangement;

						if(dirHp == DIRECTION.UP || dirHp == DIRECTION.DOWN)
							arrangement = TUNNELTYP.VERTICAL;
						else
							arrangement = TUNNELTYP.HORIZONTAL;

						aktuelles_level.getMap().addTunnel( new Tunnel(fph, arrangement) );
					}
				}}*/

				// Monster trifft Wand // Not yet done
				// for (Iterator<Monster> it = monsters.iterator(); it.hasNext();){
				// 	Monster h = it.next();
				// 	int[] m_pos1 = h.getPosition();
				// 	int[] newField = getFieldOf(m_pos1);
				// 	int[] pgSize = aktuelles_level.getMap().getPGSize();
				// 	if( newField[0] <= 0)
				// 		h.addPosOff(1,0);
				// 	if(newField[0] >= pgSize[0] )
				// 		h.addPosOff(-1,0);
				// 	if( newField[1] <= 0 )
				// 		h.addPosOff(0, 1);
				// 	if(newField[1] >= pgSize[1])
				// 		h.addPosOff(0,-1);
				// }
				/*--------------------------------------------------------------------------------------------------------------------*/

				// Bewegung werden durch Algorithmus oder Tastatuseingabe oder Netzwerksteuerung direkt im Mapobjekt geändert und durch repaint übernommen
				/// in jedem Fall wir true zurückgegeben. Andernfalls beendet sich die loop() und das spiel gilt als beendet. Darauf folgt dann die eintragung der ergebnisse ect.

			}

			if (!sp1.isAlive() && !sp2.isAlive()) {
				System.out.println("Beide tot. Loop beendet");
				return false; // Spiel beendet
			}
			if(spielers.size() == 0) {
				System.out.println("kein Spieler gespawnt. Loop beendet");
				return false;
			}

		}

		// calculate Time
		long timeTaken = System.currentTimeMillis() - beginTime;
		long sleepTime = DELAY_PERIOD - timeTaken;

		if (sleepTime >= 0) {
			try{Thread.sleep(sleepTime);} catch(InterruptedException e){}
		}
		this.repaint();
		return true;

	}

	int[] getCenterOf(int[] fp) {

		int x_field = fp[0] - 1;
		int y_field = fp[1] - 1;

		int[] borderOffset = getBorderOffset();

		int[] pixelPos = new int[2];

		pixelPos[0] = x_field * field_size + (field_size / 2) + borderOffset[0];
		pixelPos[1] = y_field * field_size + (field_size / 2) + borderOffset[1] + getTopBarHeight();

		return pixelPos;
	}

	public void spawnSpieler() {

		int[] pixelPos = getCenterOf(aktuelles_level.getMap().getSpawn_SP1());

		ArrayList<Animation> an = new ArrayList<>();

		an.add(current_skin.getAnimation("digger_red_right"));
		an.add(current_skin.getAnimation("digger_red_left"));
		an.add(current_skin.getAnimation("digger_red_up"));
		an.add(current_skin.getAnimation("digger_red_down"));

		sp1 = new Spieler(pixelPos[0], pixelPos[1], an);

		if(isMultiplayer) {
			an.clear();
			an.add(current_skin.getAnimation("digger_gre_right"));
			an.add(current_skin.getAnimation("digger_gre_left"));
			an.add(current_skin.getAnimation("digger_gre_up"));
			an.add(current_skin.getAnimation("digger_gre_down"));

			pixelPos = getCenterOf(aktuelles_level.getMap().getSpawn_SP2());
			sp2 = new Spieler(pixelPos[0], pixelPos[1], an);
		}

	}

	public Spieler getSP1(){
		return sp1;
	}

	public void spawnFeuerball(DIRECTION dir, int[] pos) {
		aktuelles_level.getMap().addFeuerball(new Feuerball(pos, dir, current_skin));
	}


	// creates next Level, increases speed and decrease regtime

	private Level createNextLevel() {

		int new_s;
		int new_r;
		int new_mm;
		Map nextMap;
		current_map = (current_map+1)%mapChain.size();
		System.out.println("create next Level with Map: " + current_map);
		nextMap = new Map(mapChain.get(current_map)); // nächste Map als KOPIE!!! einsetzen. Sonst wird die Mapchain manipuliert und Folgelevel sind verändert.

		if (aktuelles_level != null) {

			new_s = aktuelles_level.getSpeed() + 1;
			new_r = aktuelles_level.getRegenTime() + 1;
			new_mm = aktuelles_level.getMaxMonster() + 1;
		}
		else{
			new_s = 0;
			new_r = 0;
			new_mm = 4;
		}

		return new Level(new_s, new_r, new_mm, nextMap);

	}

	public void beenden() {
		// TODO - implement Spiel.beenden
		throw new UnsupportedOperationException();
	}

	public void pausieren() {
		// TODO - implement Spiel.pausieren
		throw new UnsupportedOperationException();
	}

	// GUI handling

	protected void paintComponent(Graphics g) {

		super.paintComponent(g);

		refreshSizing();

		// Prepering

		int[] borderOffset = getBorderOffset();

		// Zeichne Hintergrund

		setBackground(Color.black);
		BufferedImage backgroundImg = current_skin.getImage("backg_typ1", field_size);
		TexturePaint slatetp = new TexturePaint(backgroundImg, new Rectangle(0, 0, backgroundImg.getWidth(), backgroundImg.getHeight()));
		Graphics2D g2d = (Graphics2D) g;
		g2d.setPaint(slatetp);
		int[] pg_size = aktuelles_level.getMap().getPGSize();
		g2d.fillRect(0, getTopBarHeight(), field_size*pg_size[0]+borderOffset[0]*2, field_size*pg_size[1]+borderOffset[1]*2);

		ArrayList<Tunnel> tunnel = aktuelles_level.getMap().getTunnel();

		for (int i = 0; i < tunnel.size(); i++) {

			Tunnel single_item = tunnel.get(i);

			BufferedImage unscaledImg = current_skin.scale(single_item.getImage(), field_size);

			int[] field = single_item.getField();
			int[] middle = getCenterOf(field);
			int x_pixel = middle[0] - (unscaledImg.getWidth() / 2);
			int y_pixel = middle[1] - (unscaledImg.getHeight() / 2);

			g.drawImage(unscaledImg, x_pixel, y_pixel, null);

			if(devFrames) {
				g.drawRect((field[0]-1) * field_size + borderOffset[0], (field[1]-1) * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		// Zeichne Diamanten

		ArrayList<Diamant> diamanten = aktuelles_level.getMap().getDiamonds();

		for (int i = 0; i < diamanten.size(); i++) {
			Diamant single_item = diamanten.get(i);
			BufferedImage diamImg = current_skin.scale(single_item.getImage(),field_size);

			int[] field = single_item.getField();
			int[] middle = getCenterOf(field);
			int x_pixel = middle[0] - (diamImg.getWidth() / 2);
			int y_pixel = middle[1] - (diamImg.getHeight() / 2);

			g.drawImage(diamImg, x_pixel, y_pixel, null);

			if(devFrames) {
				g.drawRect((field[0]-1) * field_size + borderOffset[0], (field[1]-1) * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}


		// Zeichne Geldsäcke

		ArrayList<Geldsack> geldsaecke = aktuelles_level.getMap().getGeldsaecke();

		for (int i = 0; i < geldsaecke.size(); i++) {

			Geldsack single_item = geldsaecke.get(i);
			BufferedImage moneyPodImg = current_skin.scale(single_item.getImage(),field_size);

			int[] field = single_item.getField();
			int[] middle = getCenterOf(field);
			int x_pixel = middle[0] - (moneyPodImg.getWidth() / 2);
			int y_pixel = middle[1] - (moneyPodImg.getHeight() / 2);

			g.drawImage(moneyPodImg, x_pixel, y_pixel, null);

			if(devFrames) {
				g.drawRect((field[0]-1) * field_size + borderOffset[0], (field[1]-1) * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		// Monster
		ArrayList<Hobbin> hobbins = aktuelles_level.getMap().getHobbins();
		Animation ani_hobbin_left = current_skin.getAnimation("hobbin_left");
		Animation ani_hobbin_right = current_skin.getAnimation("hobbin_right");

		BufferedImage hobbinImg = null;

		for (int i = 0; i < hobbins.size(); i++) {
			Hobbin single_item = hobbins.get(i);

			if (single_item.getMoveDir() == DIRECTION.RIGHT) {
				hobbinImg = ani_hobbin_right.nextFrame(field_size);
			}
			if (single_item.getMoveDir() == DIRECTION.LEFT) {
				hobbinImg = ani_hobbin_left.nextFrame(field_size);
			}

			int x_pixel = single_item.getPosition()[0] - (hobbinImg.getWidth() / 2);
			int y_pixel = single_item.getPosition()[1] - (hobbinImg.getHeight() / 2);

			g.drawImage(hobbinImg, x_pixel, y_pixel, null);

			if(devFrames) {
				int[] field = getFieldOf(single_item.getPosition());
				g.drawRect(field[0] * field_size + borderOffset[0], field[1] * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		Animation ani_nobbin = current_skin.getAnimation("nobbin");
		BufferedImage nobbinImg = ani_nobbin.nextFrame(field_size);

		ArrayList<Nobbin> nobbins = aktuelles_level.getMap().getNobbins();

		for (int i = 0; i < nobbins.size(); i++) {
			Nobbin single_item = nobbins.get(i);

			int x_pixel = single_item.getPosition()[0] - (nobbinImg.getWidth() / 2);
			int y_pixel = single_item.getPosition()[1] - (nobbinImg.getHeight() / 2);

			g.drawImage(nobbinImg, x_pixel, y_pixel, null);

			if(devFrames) {
				int[] field = getFieldOf(single_item.getPosition());
				g.drawRect(field[0] * field_size + borderOffset[0], field[1] * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		// Feuerball

		ArrayList<Feuerball> feuerball = aktuelles_level.getMap().getFeuerball();

		for (int i = 0; i < feuerball.size(); i++) {
			Feuerball single_item = feuerball.get(i);
			BufferedImage pic = current_skin.scale(single_item.getImage(), field_size);

			int[] pos = single_item.getPosition();
			int x_pixel = pos[0] - (pic.getWidth() / 2);
			int y_pixel = pos[1] - (pic.getHeight() / 2);

			g.drawImage(pic, x_pixel, y_pixel, null);

			if(devFrames) {
				int[] field = getFieldOf(single_item.getPosition());
				g.drawRect(field[0] * field_size + borderOffset[0], field[1] * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		// Geld

		ArrayList<Geld> geld = aktuelles_level.getMap().getGeld();

		for (int i = 0; i < geld.size(); i++) {
			Geld single_item = geld.get(i);
			Animation a = single_item.getAnimation();

			BufferedImage geldImg = a.nextFrame(field_size);

			int[] field = single_item.getField();
			int[] middle = getCenterOf(field);
			int x_pixel = middle[0] - (geldImg.getWidth() / 2);
			int y_pixel = middle[1] - (geldImg.getHeight() / 2);

			// scaling ...

			g.drawImage(geldImg, x_pixel, y_pixel, null);

			if(devFrames) {
				g.drawRect(field[0] * field_size + borderOffset[0], field[1] * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		// Kirsche
		if(aktuelles_level.getMap().getKirsche().getVisible()){
			BufferedImage kirscheImg = current_skin.getImage("cherry", field_size);

			int[] field = aktuelles_level.getMap().getKirsche().getField();

			int[] middle = getCenterOf(field);
			int x_pixel = middle[0] - (kirscheImg.getWidth() / 2);
			int y_pixel = middle[1] - (kirscheImg.getHeight() / 2);

			g.drawImage(kirscheImg, x_pixel, y_pixel, null);

			if (devFrames) {
				g.drawRect((field[0] - 1) * field_size + borderOffset[0], (field[1] - 1) * field_size + borderOffset[1], field_size, field_size);
				g.setColor(Color.RED);
			}
		}

		// Spieler

		if(sp1 != null) {
			if(sp1.isAlive()) {

				Animation ani_left = current_skin.getAnimation("digger_red_left");
				Animation ani_right = current_skin.getAnimation("digger_red_right");
				Animation ani_up = current_skin.getAnimation("digger_red_up");
				Animation ani_down = current_skin.getAnimation("digger_red_down");

				BufferedImage sp1Img = null;

				if (sp1.getMoveDir() == DIRECTION.RIGHT) {
					sp1Img = ani_right.nextFrame(field_size);
				}
				if (sp1.getMoveDir() == DIRECTION.LEFT) {
					sp1Img = ani_left.nextFrame(field_size);
				}
				if (sp1.getMoveDir() == DIRECTION.UP) {
					sp1Img = ani_up.nextFrame(field_size);
				}
				if (sp1.getMoveDir() == DIRECTION.DOWN) {
					sp1Img = ani_down.nextFrame(field_size);
				}


				int x_pixel = sp1.getPosition()[0] - (sp1Img.getWidth() / 2);
				int y_pixel = sp1.getPosition()[1] - (sp1Img.getHeight() / 2);
				g.drawImage(sp1Img, x_pixel, y_pixel, null);

			}
			else {
				// gegen Geist ersetzen
				Animation ani_grave = current_skin.getAnimation("Grave");
				BufferedImage sp1Img = ani_grave.nextFrame(field_size);
				int x_pixel = sp1.getPosition()[0] - (sp1Img.getWidth() / 2);
				int y_pixel = sp1.getPosition()[1] - (sp1Img.getHeight() / 2);
				g.drawImage(sp1Img, x_pixel, y_pixel, null);
			}
		}

		if(sp2 != null) {
			if(sp2.isAlive()) {
				Animation ani_left = current_skin.getAnimation("digger_gre_left");
				Animation ani_right = current_skin.getAnimation("digger_gre_right");
				Animation ani_up = current_skin.getAnimation("digger_gre_up");
				Animation ani_down = current_skin.getAnimation("digger_gre_down");

				BufferedImage spImg = null;

				if (sp2.getMoveDir() == DIRECTION.RIGHT) {
					spImg = ani_right.nextFrame(field_size);
				}
				if (sp2.getMoveDir() == DIRECTION.LEFT) {
					spImg = ani_left.nextFrame(field_size);
				}
				if (sp2.getMoveDir() == DIRECTION.UP) {
					spImg = ani_up.nextFrame(field_size);
				}
				if (sp2.getMoveDir() == DIRECTION.DOWN) {
					spImg = ani_down.nextFrame(field_size);
				}


				int x_pixel = sp2.getPosition()[0] - (spImg.getWidth() / 2);
				int y_pixel = sp2.getPosition()[1] - (spImg.getHeight() / 2);
				g.drawImage(spImg, x_pixel, y_pixel, null);
			}
		}

		// Zeichne Score
		int margin_y = field_size/4;
		int margin_x = field_size/2;

		int fontSize = field_size/2;
		g.setFont(current_skin.getFont().deriveFont(Font.PLAIN, fontSize));
		g.setColor(Color.white);
		g.drawString(String.format("%05d", spielstand), margin_x, margin_y+fontSize);

		// Zeichne Leben


		// Zeichne Leben von SP1
		BufferedImage sp1Img =current_skin.getImage("statusbar_digger_MP_red", field_size);
		margin_x = 3*field_size;
		for(int i = sp1.getLeben(); i > 0; i--) {
			g.drawImage(sp1Img, margin_x, margin_y, null);
			margin_x += sp1Img.getWidth();
		}

		// Zeichene auch leben von SP2
		if(sp2 != null) {
			margin_x = 9*field_size;
			BufferedImage sp2Img = current_skin.getImage("statusbar_digger_MP_gre", field_size);
			for(int i = sp2.getLeben(); i > 0; i--) {
				g.drawImage(sp2Img, margin_x, margin_y, null);
				margin_x -= sp1Img.getWidth();
			}

		}

	}

	private int[] getBorderOffset(){

		int[] borderOffset = new int[2];
		borderOffset[0] = (int)(field_size*border[0]);
		borderOffset[1] = (int)(field_size*border[1]);

		return borderOffset;
	}


	@Override
	public void run() {
		while(loop());
	}

	@Override
	public Dimension getPreferredSize() {

		int[] borderOffset = getBorderOffset();

		int[] playground_size = aktuelles_level.getMap().getPGSize();

		Dimension d = new Dimension(playground_size[0] * field_size + 2* borderOffset[0], playground_size[1] * field_size + 2* borderOffset[1] + getTopBarHeight());

		System.out.println(d);

		return d;
	}


	public int getFieldSize() {
		return field_size;
	}

	public void moveSP(int velx, int vely) {
		int[] spPos = sp1.getPosition().clone();

		spPos[0] += velx;
		spPos[1] += vely;

		int[] newField = getFieldOf(spPos);
		int[] pgSize = aktuelles_level.getMap().getPGSize();
		if(0 < newField[0] && newField[0] <= pgSize[0] && 0 < newField[1] && newField[1] <= pgSize[1])
			sp1.addPosOff(velx,vely);
	}
}
