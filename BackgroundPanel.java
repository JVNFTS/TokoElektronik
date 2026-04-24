import javax.swing.*;
import java.awt.*;

public class BackgroundPanel extends JPanel {

    private Image backgroundImage;
    private float opacity = 1.0f;

    public enum ScaleMode {
        STRETCH,
        FIT,
        COVER,
        CENTER
    }

    private ScaleMode scaleMode = ScaleMode.COVER;

    public BackgroundPanel(Image image) {
        this.backgroundImage = image;
        setOpaque(false);
    }

    public void setBackgroundImage(Image image) {
        this.backgroundImage = image;
        repaint();
    }

    public void setOpacity(float opacity) {
        if (opacity < 0.0f) opacity = 0.0f;
        if (opacity > 1.0f) opacity = 1.0f;
        this.opacity = opacity;
        repaint();
    }

    public void setScaleMode(ScaleMode mode) {
        if (mode != null) {
            this.scaleMode = mode;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage == null) return;

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imgWidth = backgroundImage.getWidth(this);
        int imgHeight = backgroundImage.getHeight(this);

        if (panelWidth <= 0 || panelHeight <= 0 || imgWidth <= 0 || imgHeight <= 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            switch (scaleMode) {
                case STRETCH:
                    g2d.drawImage(backgroundImage, 0, 0, panelWidth, panelHeight, this);
                    break;

                case FIT: {
                    double scale = Math.min((double) panelWidth / imgWidth, (double) panelHeight / imgHeight);
                    int newW = (int) Math.round(imgWidth * scale);
                    int newH = (int) Math.round(imgHeight * scale);
                    int x = (panelWidth - newW) / 2;
                    int y = (panelHeight - newH) / 2;
                    g2d.drawImage(backgroundImage, x, y, newW, newH, this);
                    break;
                }

                case COVER: {
                    double scale = Math.max((double) panelWidth / imgWidth, (double) panelHeight / imgHeight);
                    int newW = (int) Math.round(imgWidth * scale);
                    int newH = (int) Math.round(imgHeight * scale);
                    int x = (panelWidth - newW) / 2;
                    int y = (panelHeight - newH) / 2;
                    g2d.drawImage(backgroundImage, x, y, newW, newH, this);
                    break;
                }

                case CENTER:
                    g2d.drawImage(backgroundImage, (panelWidth - imgWidth) / 2, (panelHeight - imgHeight) / 2, this);
                    break;
            }
        } finally {
            g2d.dispose();
        }
    }
}