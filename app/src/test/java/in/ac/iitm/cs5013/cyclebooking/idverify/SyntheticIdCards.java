package in.ac.iitm.cs5013.cyclebooking.idverify;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Draws fake ID cards in memory for the OCR prototype, then degrades them the way a
 * phone photo does. Fake names and roll numbers only, and nothing is written to disk:
 * we must not keep real resident ID photos (CLAUDE.md, ADR 0003). The layout is our
 * own approximation, not a copy of the real institute card.
 */
final class SyntheticIdCards {

    enum Condition { GOOD, GLARE, TILT, BLUR, LOW_LIGHT, SEVERE_BLUR }

    record Card(String name, String rollNumber, String hostel) {
    }

    private SyntheticIdCards() {
    }

    static byte[] photo(Card card, Condition condition) {
        BufferedImage img = draw(card);
        img = switch (condition) {
            case GOOD -> img;
            case GLARE -> glare(img);
            case TILT -> tilt(img, Math.toRadians(7));
            case BLUR -> blur(img, 5);
            case LOW_LIGHT -> lowLight(img);
            case SEVERE_BLUR -> blur(img, 17);
        };
        return jpeg(img);
    }

    private static BufferedImage draw(Card card) {
        BufferedImage img = new BufferedImage(856, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xF4F1E8));
        g.fillRect(0, 0, 856, 540);
        g.setColor(new Color(0x1F3A6E));
        g.fillRect(0, 0, 856, 90);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        g.drawString("SYNTHETIC TEST CARD", 40, 58);
        g.setColor(new Color(0xB8B8B8));
        g.fillRect(40, 130, 200, 250); // photo placeholder
        g.setColor(new Color(0x1B1F24));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
        g.drawString("Name: " + card.name(), 280, 180);
        g.drawString("Roll No: " + card.rollNumber(), 280, 250);
        g.drawString("Hostel: " + card.hostel(), 280, 320);
        g.dispose();
        return img;
    }

    /** A bright, soft-edged hotspot over the roll number, like a ceiling light reflection. */
    private static BufferedImage glare(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(470, 240, 170, new float[] {0f, 1f},
                new Color[] {new Color(255, 255, 255, 190), new Color(255, 255, 255, 0)}));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        return img;
    }

    /** The card rotated on a darker table surface, as when the phone is not held level. */
    private static BufferedImage tilt(BufferedImage card, double radians) {
        BufferedImage photo = new BufferedImage(1060, 760, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = photo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(new Color(0x6B5B4B));
        g.fillRect(0, 0, photo.getWidth(), photo.getHeight());
        g.rotate(radians, 530, 380);
        g.drawImage(card, 102, 110, null);
        g.dispose();
        return photo;
    }

    /** Box blur of the given width: hand shake or missed focus. */
    private static BufferedImage blur(BufferedImage img, int size) {
        float[] k = new float[size * size];
        java.util.Arrays.fill(k, 1f / k.length);
        return new ConvolveOp(new Kernel(size, size, k), ConvolveOp.EDGE_NO_OP, null).filter(img, null);
    }

    /** Dim corridor light: everything darker, plus sensor noise. Seeded so runs repeat. */
    private static BufferedImage lowLight(BufferedImage img) {
        Random noise = new Random(42);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int n = (int) (noise.nextGaussian() * 12);
                int r = clamp((int) (((rgb >> 16) & 0xFF) * 0.35) + n);
                int gr = clamp((int) (((rgb >> 8) & 0xFF) * 0.35) + n);
                int b = clamp((int) ((rgb & 0xFF) * 0.35) + n);
                img.setRGB(x, y, (r << 16) | (gr << 8) | b);
            }
        }
        return img;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static byte[] jpeg(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
