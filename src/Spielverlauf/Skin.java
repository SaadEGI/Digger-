package Spielverlauf;

import Spielverlauf.Animation;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;

public class Skin {

	private HashMap<String, BufferedImage> images;
	private HashMap<String, Animation> animations;
	private Font font;

	private String name;
	private int reference;

	private final int animation_measure = 10;

	// Load Skin
	public Skin(File skinDir, String skinname){

		File skin_graphic_file = new File(skinDir, skinname+".png");
		File skin_catalog_file = new File(skinDir, skinname+".json");

		JSONObject obj = null;

		try {
			obj = new JSONObject(new String(Files.readAllBytes(Paths.get(skin_catalog_file.getPath()))));
		} catch (Exception e) {
			e.printStackTrace();
		}

		images = new HashMap<>();
		animations = new HashMap<>();

		name = obj.getString("name");
		reference = obj.getInt("reference");

		JSONObject imageData = obj.getJSONObject("data");

		Iterator<String> keys = imageData.keys();

		while(keys.hasNext()) {
			String key = keys.next();

			JSONArray pic_val = imageData.getJSONArray(key);

			// read catalog infos

			int x_off = pic_val.getInt(0);
			int y_off = pic_val.getInt(1);
			int width = pic_val.getInt(2);
			int hight = pic_val.getInt(3);

			// crop requested image from skin

			BufferedImage dest = null;
			try {
				dest = ImageIO.read(skin_graphic_file).getSubimage(x_off,y_off,width,hight);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			images.put(key, dest);


		}

		createAnimations();

		try{
			font = Font.createFont(Font.TRUETYPE_FONT, new File(skinDir, skinname+".ttf"));
		}
		catch (IOException|FontFormatException e){

		}

	}

	private void createAnimations() {
//Spieler 1
		BufferedImage[] rbilder = new BufferedImage[6];

		rbilder[0] = getImage("dig_red_rgt_f1");
		rbilder[1] = getImage("dig_red_rgt_f2");
		rbilder[2] = getImage("dig_red_rgt_f3");
		rbilder[3] = getImage("dig_red_rgt_f4");
		rbilder[4] = getImage("dig_red_rgt_f5");
		rbilder[5] = getImage("dig_red_rgt_f6");

		Animation a = new Animation(animation_measure, rbilder, this);
		animations.put("digger_red_right", a);

		BufferedImage[] lbilder = new BufferedImage[6];
		lbilder[0] = getImage("dig_red_lft_f1");
		lbilder[1] = getImage("dig_red_lft_f2");
		lbilder[2] = getImage("dig_red_lft_f3");
		lbilder[3] = getImage("dig_red_lft_f4");
		lbilder[4] = getImage("dig_red_lft_f5");
		lbilder[5] = getImage("dig_red_lft_f6");
		Animation b = new Animation(animation_measure, lbilder, this);
		animations.put("digger_red_left", b);

		BufferedImage[] ubilder = new BufferedImage[6];
		ubilder[0] = getImage("dig_red_up_f1");
		ubilder[1] = getImage("dig_red_up_f2");
		ubilder[2] = getImage("dig_red_up_f3");
		ubilder[3] = getImage("dig_red_up_f4");
		ubilder[4] = getImage("dig_red_up_f5");
		ubilder[5] = getImage("dig_red_up_f6");
		Animation c = new Animation(animation_measure, ubilder, this);
		animations.put("digger_red_up", c);

		BufferedImage[] dbilder = new BufferedImage[6];
		dbilder[0] = getImage("dig_red_dow_f1");
		dbilder[1] = getImage("dig_red_dow_f2");
		dbilder[2] = getImage("dig_red_dow_f3");
		dbilder[3] = getImage("dig_red_dow_f4");
		dbilder[4] = getImage("dig_red_dow_f5");
		dbilder[5] = getImage("dig_red_dow_f6");
		Animation d = new Animation(animation_measure, dbilder, this);
		animations.put("digger_red_down", d);

		//Spieler 2
		BufferedImage[] gre_rbilder = new BufferedImage[6];

		gre_rbilder[0] = getImage("dig_gre_rgt_f1");
		gre_rbilder[1] = getImage("dig_gre_rgt_f2");
		gre_rbilder[2] = getImage("dig_gre_rgt_f3");
		gre_rbilder[3] = getImage("dig_gre_rgt_f4");
		gre_rbilder[4] = getImage("dig_gre_rgt_f5");
		gre_rbilder[5] = getImage("dig_gre_rgt_f6");
		animations.put("digger_gre_right", new Animation(animation_measure, gre_rbilder, this));

		BufferedImage[] gre_lbilder = new BufferedImage[6];
		gre_lbilder[0] = getImage("dig_gre_lft_f1");
		gre_lbilder[1] = getImage("dig_gre_lft_f2");
		gre_lbilder[2] = getImage("dig_gre_lft_f3");
		gre_lbilder[3] = getImage("dig_gre_lft_f4");
		gre_lbilder[4] = getImage("dig_gre_lft_f5");
		gre_lbilder[5] = getImage("dig_gre_lft_f6");
		animations.put("digger_gre_left", new Animation(animation_measure, gre_lbilder, this));

		BufferedImage[] gre_ubilder = new BufferedImage[6];
		gre_ubilder[0] = getImage("dig_gre_up_f1");
		gre_ubilder[1] = getImage("dig_gre_up_f2");
		gre_ubilder[2] = getImage("dig_gre_up_f3");
		gre_ubilder[3] = getImage("dig_gre_up_f4");
		gre_ubilder[4] = getImage("dig_gre_up_f5");
		gre_ubilder[5] = getImage("dig_gre_up_f6");
		animations.put("digger_gre_up", new Animation(animation_measure, gre_ubilder, this));

		BufferedImage[] hob_left_bilder = new BufferedImage[4];
		hob_left_bilder[0] = getImage("hobbin_left_f1");
		hob_left_bilder[1] = getImage("hobbin_left_f2");
		hob_left_bilder[2] = getImage("hobbin_left_f3");
		hob_left_bilder[3] = getImage("hobbin_left_f4");
		animations.put("hobbin_left", new Animation(animation_measure, hob_left_bilder, this));

		BufferedImage[] hob_right_bilder = new BufferedImage[4];
		hob_right_bilder[0] = getImage("hobbin_right_f1");
		hob_right_bilder[1] = getImage("hobbin_right_f2");
		hob_right_bilder[2] = getImage("hobbin_right_f3");
		hob_right_bilder[3] = getImage("hobbin_right_f4");
		animations.put("hobbin_right", new Animation(animation_measure, hob_right_bilder, this));

		BufferedImage[] nob_bilder = new BufferedImage[4];
		nob_bilder[0] = getImage("nobbin_f1");
		nob_bilder[1] = getImage("nobbin_f2");
		nob_bilder[2] = getImage("nobbin_f3");
		nob_bilder[3] = getImage("nobbin_f4");
		animations.put("nobbin", new Animation(animation_measure, nob_bilder, this));
	}

	public BufferedImage getImage(String name, int fs) {

		BufferedImage dest = images.get(name);
		return scale(dest,fs);

	}

	public BufferedImage getImage(String name) {

		BufferedImage dest = images.get(name);
		return images.get(name);

	}

	public BufferedImage scale(BufferedImage bi, int fs){

		BufferedImage dest = bi;

		// scale cropped image

		double scale_factor = fs/reference; // calculate teh scaling factor by referece of skin

		int new_width = (int)Math.round(scale_factor * dest.getWidth());
		int new_height = (int)Math.round(scale_factor * dest.getHeight());

		BufferedImage resizedImage = new BufferedImage(new_width, new_height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(dest, 0, 0, new_width, new_height, null);
		g.dispose();

		return resizedImage;
	}

	public Font getFont() {
		return font;
	}

    public Animation getAnimation(String bez) {
        return animations.get(bez);
    }
}