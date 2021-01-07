package Spielverlauf;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Animation {

    private Skin s;

    private int count = 0;
    final int DELAY_PERIOD;
    int remaining;



    private BufferedImage[] images;

    public Animation(int loss, BufferedImage[] bi, Skin sk) {
        DELAY_PERIOD = loss;
        remaining = loss;

        images = bi;
        s = sk;
    }

    public BufferedImage nextFrame(int fs) {

        if(remaining>0) {
            remaining--;
        }
        else{
            count = (count + 1) % images.length;
            remaining = DELAY_PERIOD;
        }

        return s.scale(images[count], fs);
    }
}
