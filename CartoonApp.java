import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * CartoonApp.java
 *
 * Traditional Image Processing Cartoon Style App
 * Java OOP Challenge Project — Lecture 9
 *
 * Pipeline:
 *   ImageLoader → GrayConverter → EdgeDetector → ColorQuantizer → CartoonRenderer
 *
 * Features:
 *   - Gaussian blur smoothing
 *   - Sobel edge extraction
 *   - Color quantization
 *   - Adaptive thresholding
 *   - Real-time preview window
 *   - File save/export support
 *   - Clean Java Swing GUI
 *   - VS Code compatible (run: javac CartoonApp.java && java CartoonApp)
 *
 * Author: Java程式設計(一) Student
 * Course: Java Programming (I) — National Penghu University of Science and Technology
 */
public class CartoonApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CartoonGUI::new);
    }
}


// ══════════════════════════════════════════════════════════════════
// 1. ImageLoader — loads a BufferedImage from disk
// ══════════════════════════════════════════════════════════════════
class ImageLoader {
    private BufferedImage image;
    private File file;

    public BufferedImage load(File f) throws IOException {
        this.file = f;
        this.image = ImageIO.read(f);
        if (this.image == null) throw new IOException("Unsupported image format: " + f.getName());
        return this.image;
    }

    public BufferedImage getImage() { return image; }
    public File getFile()           { return file; }
}


// ══════════════════════════════════════════════════════════════════
// 2. GrayConverter — converts RGB image to grayscale
// ══════════════════════════════════════════════════════════════════
class GrayConverter {
    /** Returns a new TYPE_BYTE_GRAY BufferedImage */
    public BufferedImage toGray(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;
                // Luminosity formula
                int lum = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                int grayRGB = (lum << 16) | (lum << 8) | lum;
                gray.setRGB(x, y, grayRGB);
            }
        }
        return gray;
    }
}


// ══════════════════════════════════════════════════════════════════
// 3. GaussianBlur — smooths image before edge detection
// ══════════════════════════════════════════════════════════════════
class GaussianBlur {
    private final int radius;

    public GaussianBlur(int radius) {
        this.radius = Math.max(1, radius);
    }

    public BufferedImage blur(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // Build 1-D Gaussian kernel
        int size = 2 * radius + 1;
        double[] kernel = new double[size];
        double sigma = radius / 2.0;
        double sum = 0;
        for (int i = 0; i < size; i++) {
            int x = i - radius;
            kernel[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        for (int i = 0; i < size; i++) kernel[i] /= sum;

        // Horizontal pass
        BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double r = 0, g = 0, b = 0;
                for (int k = 0; k < size; k++) {
                    int sx = Math.min(Math.max(x + k - radius, 0), w - 1);
                    int rgb = src.getRGB(sx, y);
                    r += ((rgb >> 16) & 0xFF) * kernel[k];
                    g += ((rgb >> 8)  & 0xFF) * kernel[k];
                    b += ( rgb        & 0xFF) * kernel[k];
                }
                int rgb = ((int) r << 16) | ((int) g << 8) | (int) b;
                tmp.setRGB(x, y, rgb);
            }
        }
        // Vertical pass
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double r = 0, g = 0, b = 0;
                for (int k = 0; k < size; k++) {
                    int sy = Math.min(Math.max(y + k - radius, 0), h - 1);
                    int rgb = tmp.getRGB(x, sy);
                    r += ((rgb >> 16) & 0xFF) * kernel[k];
                    g += ((rgb >> 8)  & 0xFF) * kernel[k];
                    b += ( rgb        & 0xFF) * kernel[k];
                }
                int rgb = ((int) r << 16) | ((int) g << 8) | (int) b;
                result.setRGB(x, y, rgb);
            }
        }
        return result;
    }
}


// ══════════════════════════════════════════════════════════════════
// 4. EdgeDetector — Sobel operator on grayscale image → edge mask
// ══════════════════════════════════════════════════════════════════
class EdgeDetector {
    private int threshold;

    public EdgeDetector(int threshold) {
        this.threshold = threshold;
    }

    public void setThreshold(int t) { this.threshold = t; }

    /** Returns a binary edge mask: black edge pixels, white background */
    public BufferedImage detect(BufferedImage gray) {
        int w = gray.getWidth(), h = gray.getHeight();
        int[][] lum = new int[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                lum[y][x] = gray.getRGB(x, y) & 0xFF;  // any channel is fine (gray)

        BufferedImage edge = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int gx = -lum[y-1][x-1] + lum[y-1][x+1]
                         -2*lum[y][x-1] + 2*lum[y][x+1]
                         -lum[y+1][x-1] + lum[y+1][x+1];
                int gy = -lum[y-1][x-1] - 2*lum[y-1][x] - lum[y-1][x+1]
                         +lum[y+1][x-1] + 2*lum[y+1][x] + lum[y+1][x+1];
                int mag = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));
                // Edge = dark line; non-edge = white
                int val = (mag > threshold) ? 0 : 255;
                edge.setRGB(x, y, (val << 16) | (val << 8) | val);
            }
        }
        return edge;
    }
}


// ══════════════════════════════════════════════════════════════════
// 5. ColorQuantizer — reduces colour palette for cartoon look
// ══════════════════════════════════════════════════════════════════
class ColorQuantizer {
    private final int levels;   // number of quantization steps per channel

    public ColorQuantizer(int levels) {
        this.levels = Math.max(2, levels);
    }

    public BufferedImage quantize(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int step = 256 / levels;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = quantizeChannel((rgb >> 16) & 0xFF, step);
                int g = quantizeChannel((rgb >> 8)  & 0xFF, step);
                int b = quantizeChannel( rgb        & 0xFF, step);
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private int quantizeChannel(int val, int step) {
        return Math.min(255, (val / step) * step + step / 2);
    }
}


// ══════════════════════════════════════════════════════════════════
// 6. CartoonRenderer — blends quantized colour + edge mask
// ══════════════════════════════════════════════════════════════════
class CartoonRenderer {
    /**
     * @param quantized  colour-quantized source image
     * @param edges      binary edge mask (black = edge, white = no-edge)
     * @return cartoon-style image
     */
    public BufferedImage render(BufferedImage quantized, BufferedImage edges) {
        int w = quantized.getWidth(), h = quantized.getHeight();
        BufferedImage cartoon = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int edgeVal = edges.getRGB(x, y) & 0xFF;
                if (edgeVal < 128) {
                    // Draw black edge
                    cartoon.setRGB(x, y, 0x000000);
                } else {
                    // Use quantized colour
                    cartoon.setRGB(x, y, quantized.getRGB(x, y));
                }
            }
        }
        return cartoon;
    }
}


// ══════════════════════════════════════════════════════════════════
// 7. CartoonGUI — Java Swing front-end
// ══════════════════════════════════════════════════════════════════
class CartoonGUI extends JFrame {

    // Pipeline objects
    private final ImageLoader     loader     = new ImageLoader();
    private final GrayConverter   converter  = new GrayConverter();
    private final EdgeDetector    detector   = new EdgeDetector(60);
    private final ColorQuantizer  quantizer  = new ColorQuantizer(6);
    private final CartoonRenderer renderer   = new CartoonRenderer();

    // UI state
    private BufferedImage original;
    private BufferedImage cartoon;

    // Panels
    private final JLabel  origLabel   = new JLabel("Load an image to start", SwingConstants.CENTER);
    private final JLabel  cartLabel   = new JLabel("Cartoon output will appear here", SwingConstants.CENTER);
    private final JSlider blurSlider  = new JSlider(1, 10, 3);
    private final JSlider edgeSlider  = new JSlider(10, 200, 60);
    private final JSlider colSlider   = new JSlider(2, 16, 6);
    private final JLabel  blurVal    = new JLabel("3");
    private final JLabel  edgeVal    = new JLabel("60");
    private final JLabel  colVal     = new JLabel("6");
    private final JLabel  statusBar  = new JLabel(" Ready.");

    public CartoonGUI() {
        super("Traditional Cartoon Filter App — Java OOP Challenge (Lecture 9)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        Color bg = new Color(245, 245, 240);
        Color accent = new Color(20, 70, 50);
        getContentPane().setBackground(bg);

        // ── Top toolbar ────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.setBackground(accent);

        JButton loadBtn  = makeBtn("📂 Load Image",  Color.WHITE, accent);
        JButton applyBtn = makeBtn("🎨 Apply Filter", Color.WHITE, new Color(180, 140, 40));
        JButton saveBtn  = makeBtn("💾 Save Output",  Color.WHITE, new Color(40, 100, 70));

        loadBtn.addActionListener(e  -> loadImage());
        applyBtn.addActionListener(e -> applyFilter());
        saveBtn.addActionListener(e  -> saveOutput());

        toolbar.add(loadBtn);
        toolbar.add(applyBtn);
        toolbar.add(saveBtn);
        add(toolbar, BorderLayout.NORTH);

        // ── Centre: side-by-side preview ───────────────────────────
        origLabel.setPreferredSize(new Dimension(480, 480));
        cartLabel.setPreferredSize(new Dimension(480, 480));
        origLabel.setBorder(BorderFactory.createTitledBorder("Original"));
        cartLabel.setBorder(BorderFactory.createTitledBorder("Cartoon Output"));
        origLabel.setBackground(Color.DARK_GRAY); origLabel.setOpaque(true);
        cartLabel.setBackground(Color.DARK_GRAY); cartLabel.setOpaque(true);
        origLabel.setForeground(Color.WHITE);
        cartLabel.setForeground(Color.WHITE);

        JPanel centre = new JPanel(new GridLayout(1, 2, 8, 0));
        centre.setBackground(bg);
        centre.add(origLabel);
        centre.add(cartLabel);
        add(centre, BorderLayout.CENTER);

        // ── Right: sliders ─────────────────────────────────────────
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(bg);
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        controls.add(makeSliderPanel("Gaussian Blur Radius", blurSlider, blurVal, bg));
        controls.add(Box.createVerticalStrut(12));
        controls.add(makeSliderPanel("Edge Threshold (Sobel)", edgeSlider, edgeVal, bg));
        controls.add(Box.createVerticalStrut(12));
        controls.add(makeSliderPanel("Color Levels", colSlider, colVal, bg));
        controls.add(Box.createVerticalGlue());

        // Pipeline diagram labels
        String[] steps = {"ImageLoader", "GrayConverter", "EdgeDetector", "ColorQuantizer", "CartoonRenderer"};
        JPanel pipeline = new JPanel(new GridLayout(steps.length, 1, 0, 4));
        pipeline.setBackground(bg);
        pipeline.setBorder(BorderFactory.createTitledBorder("Pipeline"));
        for (String s : steps) {
            JLabel lbl = new JLabel("▶ " + s, SwingConstants.LEFT);
            lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lbl.setForeground(accent);
            pipeline.add(lbl);
        }
        controls.add(pipeline);
        add(controls, BorderLayout.EAST);

        // ── Status bar ─────────────────────────────────────────────
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.setBackground(new Color(220, 220, 210));
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        add(statusBar, BorderLayout.SOUTH);

        // Live slider listeners
        blurSlider.addChangeListener(e -> blurVal.setText(String.valueOf(blurSlider.getValue())));
        edgeSlider.addChangeListener(e -> edgeVal.setText(String.valueOf(edgeSlider.getValue())));
        colSlider.addChangeListener(e  -> colVal.setText(String.valueOf(colSlider.getValue())));
    }

    private JButton makeBtn(String text, Color fg, Color bg) {
        JButton btn = new JButton(text);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel makeSliderPanel(String title, JSlider slider, JLabel valueLabel, Color bg) {
        JPanel p = new JPanel(new BorderLayout(4, 2));
        p.setBackground(bg);
        p.setMaximumSize(new Dimension(220, 70));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        p.add(titleLabel, BorderLayout.NORTH);
        p.add(slider, BorderLayout.CENTER);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(valueLabel, BorderLayout.EAST);
        return p;
    }

    // ── Actions ───────────────────────────────────────────────────

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images (jpg, png, bmp)", "jpg", "jpeg", "png", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            original = loader.load(chooser.getSelectedFile());
            origLabel.setIcon(new ImageIcon(scaleToFit(original, 480, 460)));
            origLabel.setText("");
            cartoon = null;
            cartLabel.setIcon(null);
            cartLabel.setText("Click 'Apply Filter' to process");
            status("Loaded: " + chooser.getSelectedFile().getName()
                   + "  (" + original.getWidth() + "×" + original.getHeight() + ")");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load image:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        if (original == null) {
            JOptionPane.showMessageDialog(this, "Please load an image first.", "No Image", JOptionPane.WARNING_MESSAGE);
            return;
        }
        status("Processing...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override protected BufferedImage doInBackground() {
                // Pipeline execution
                GaussianBlur blur = new GaussianBlur(blurSlider.getValue());
                detector.setThreshold(edgeSlider.getValue());
                ColorQuantizer cq = new ColorQuantizer(colSlider.getValue());

                BufferedImage blurred   = blur.blur(original);
                BufferedImage gray      = converter.toGray(blurred);
                BufferedImage edges     = detector.detect(gray);
                BufferedImage quantized = cq.quantize(original);
                return renderer.render(quantized, edges);
            }
            @Override protected void done() {
                try {
                    cartoon = get();
                    cartLabel.setIcon(new ImageIcon(scaleToFit(cartoon, 480, 460)));
                    cartLabel.setText("");
                    status("Done! Blur=" + blurSlider.getValue()
                           + "  EdgeThresh=" + edgeSlider.getValue()
                           + "  ColorLevels=" + colSlider.getValue());
                } catch (Exception ex) {
                    status("Error during processing: " + ex.getMessage());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void saveOutput() {
        if (cartoon == null) {
            JOptionPane.showMessageDialog(this, "No cartoon output to save.\nApply the filter first.", "Nothing to Save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("cartoon_output.png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            File out = chooser.getSelectedFile();
            if (!out.getName().toLowerCase().endsWith(".png")) out = new File(out.getPath() + ".png");
            ImageIO.write(cartoon, "png", out);
            status("Saved to: " + out.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Scale image to fit within maxW × maxH while preserving aspect ratio */
    private Image scaleToFit(BufferedImage img, int maxW, int maxH) {
        double scaleW = (double) maxW / img.getWidth();
        double scaleH = (double) maxH / img.getHeight();
        double scale  = Math.min(scaleW, scaleH);
        int w = (int)(img.getWidth()  * scale);
        int h = (int)(img.getHeight() * scale);
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    private void status(String msg) {
        statusBar.setText(" " + msg);
    }
}