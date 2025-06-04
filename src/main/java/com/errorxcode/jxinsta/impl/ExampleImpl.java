package com.errorxcode.jxinsta.impl;

import com.errorxcode.jxinsta.JxInsta;
import com.errorxcode.jxinsta.auth.LoginType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

public class ExampleImpl {
    public static void main(String[] args) {
        JxInsta jxInsta = new JxInsta("username", "password", LoginType.APP_AUTHENTICATION);
        System.out.println("jxInsta.getAuthInfo() = " + jxInsta.getAuthInfo());
        File newStory = new File("story.jpeg");

        try {
            BufferedImage image = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Arial", Font.BOLD, 48));
            graphics.drawString(UUID.randomUUID().toString(), 100, 100);
            graphics.dispose();

            ImageIO.write(image, "jpeg", newStory);

            jxInsta.uploadStory(newStory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}