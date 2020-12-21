package Spielverlauf;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import Spielbereitstellug.Lokalsteuerung;
import org.json.*;

import Spielverlauf.*;

public class Test {


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Test::new);
    }

    public Test() {

        JFrame frame = new JFrame("Testing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        File skin_graphic_file = new File("bin/skins");
        Skin sk = new Skin(skin_graphic_file, "original_skin");
        String[] levels;
        File f = new File("bin/level/");
        levels = f.list();

        JSONObject obj = null;
        try {
             obj =  new JSONObject(new String(Files.readAllBytes(Paths.get("bin/level/"+levels[1]))));
        } catch (Exception e) {
            e.printStackTrace();
        }

        int[] size = {1000,1000};

        Map map = new Map(obj, size ,sk);

        // Beispielcontent

        map.spawnMonster();

        JSONArray geld_pos = new JSONArray("[3,10]");
        map.addGeld(new Geld(geld_pos));

        map.spawnSpieler(new Spieler(map.getSpawnSP1()));

        int[] fb_pos = map.getSP1().getPosition();
        DIRECTION dir = DIRECTION.RIGHT;
        map.addFeuerball(new Feuerball(fb_pos, dir));



        // Naiv-Testing Area:

        Lokalsteuerung lok = new Lokalsteuerung(map.getSP1().getPosition()[0], map.getSP1().getPosition()[1], map);


        frame.add(map);
        frame.pack();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }
}