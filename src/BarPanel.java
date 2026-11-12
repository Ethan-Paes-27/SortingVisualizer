import javax.swing.JPanel;
import java.awt.*;

/**
 * Draws the list as bars. Taller bar = larger value.
 *
 *   yellow  = being compared
 *   red     = being swapped
 *   purple  = being written (set)
 *   green   = finished and correctly sorted
 *
 * (Framework file - students do not need to edit this.)
 */
public class BarPanel extends JPanel {

    static final Color BACKGROUND = new Color(30, 32, 40);
    static final Color NORMAL     = new Color(90, 150, 220);
    static final Color COMPARE    = new Color(250, 205, 60);
    static final Color SWAP       = new Color(235, 80, 80);
    static final Color WRITE      = new Color(190, 110, 230);
    static final Color SORTED     = new Color(90, 200, 120);

    private Step step;
    private double min, max;
    private boolean showSorted;

    public BarPanel() {
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(800, 420));
    }

    public void show(Step step, double min, double max, boolean showSorted) {
        this.step = step;
        this.min = min;
        this.max = max;
        this.showSorted = showSorted;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (step == null) return;
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Item[] a = step.snapshot;
        int n = a.length;
        if (n == 0) return;

        int pad = 20;
        int labelSpace = 40;
        int w = getWidth() - 2 * pad;
        int h = getHeight() - pad - labelSpace;
        double slot = (double) w / n;
        int gap = slot > 6 ? Math.max(1, (int) (slot * 0.15)) : 0;
        boolean drawLabels = slot >= 12;

        Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(9, Math.min(16, slot * 0.45)));
        Font indexFont = new Font(Font.MONOSPACED, Font.PLAIN, (int) Math.max(8, Math.min(11, slot * 0.35)));

        for (int k = 0; k < n; k++) {
            double t = (max > min) ? (a[k].key() - min) / (max - min) : 0.5;
            int barH = (int) (h * (0.05 + 0.95 * t));
            int x = pad + (int) (k * slot);
            int bw = Math.max(1, (int) slot - gap);
            int y = pad + h - barH;

            g.setColor(colorFor(k));
            g.fillRoundRect(x, y, bw, barH, 4, 4);

            if (drawLabels) {
                g.setFont(labelFont);
                g.setColor(Color.WHITE);
                drawCentered(g, a[k].getLabel(), x + bw / 2, pad + h + 18);
                g.setFont(indexFont);
                g.setColor(new Color(150, 155, 170));
                drawCentered(g, String.valueOf(k), x + bw / 2, pad + h + 34);
            }
        }
    }

    private Color colorFor(int k) {
        if (showSorted) return SORTED;
        if (k == step.i || k == step.j) {
            switch (step.type) {
                case COMPARE: return COMPARE;
                case SWAP:    return SWAP;
                case WRITE:   return WRITE;
                default:      return NORMAL;
            }
        }
        return NORMAL;
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, baseline);
    }
}
