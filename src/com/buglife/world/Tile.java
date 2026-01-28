package com.buglife.world;

import java.awt.image.BufferedImage;

public class Tile {
    public BufferedImage image;
    public boolean solid = false; // Is this tile a wall? We'll use this later.

    // A simple constructor
    public Tile(BufferedImage image, boolean isSolid) {
        this.image = image;
        this.solid = isSolid;
    }
}