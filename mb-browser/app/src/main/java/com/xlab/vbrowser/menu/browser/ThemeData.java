package com.xlab.vbrowser.menu.browser;

public class ThemeData {
    private String name;
    private int color;

    public ThemeData(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
