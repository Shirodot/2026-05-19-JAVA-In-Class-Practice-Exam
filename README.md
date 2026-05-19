# Traditional Image Processing Cartoon Style App

> **Course**: Java程式設計(一) — Java Programming (I)  


---

## 📖 Overview

A **pure traditional image processing** cartoon-style filter application built entirely with Java OOP and Java Swing — **no AI or deep learning involved**.

The app transforms any photo into a cartoon-style image through a 5-stage pipeline:

```
ImageLoader → GrayConverter → EdgeDetector → ColorQuantizer → CartoonRenderer
```

All processing stages are encapsulated as independent Java classes, making the codebase clean, modular, and easy to extend.

---

## 🗂️ Project Structure

```
cartoon-java/
├── src/
│   └── CartoonApp.java      # All 7 classes + Swing GUI in one file
└── README.md
```

### Class Summary

| Class | Responsibility |
|-------|---------------|
| `ImageLoader` | Loads a `BufferedImage` from disk via `ImageIO` |
| `GrayConverter` | Converts RGB → grayscale using the luminosity formula (0.299R + 0.587G + 0.114B) |
| `GaussianBlur` | Applies separable 1-D Gaussian kernel to smooth the image |
| `EdgeDetector` | Sobel operator extracts edges; produces a binary edge mask |
| `ColorQuantizer` | Reduces the colour palette per channel to create flat cartoon regions |
| `CartoonRenderer` | Combines edge mask (black lines) with quantized colour image |
| `CartoonGUI` | Java Swing GUI with live sliders and real-time preview |

---

## ⚙️ Requirements

- **Java 8** or higher (Java 21 recommended)
- No external libraries — uses only `javax.swing`, `java.awt`, and `javax.imageio`
- Works in **VS Code** (with the Java Extension Pack), IntelliJ IDEA, or the command line

---

## 🚀 How to Compile and Run

### VS Code (recommended)

1. Open the `cartoon-java` folder in VS Code.
2. Install the **Extension Pack for Java** if not already installed.
3. Open `src/CartoonApp.java` and click **▶ Run**.

### Command Line

```bash
# Clone
git clone https://github.com/<your-username>/cartoon-java.git
cd cartoon-java/src

# Compile
javac CartoonApp.java

# Run
java CartoonApp
```

---

## 🎨 How to Use the App

1. Click **📂 Load Image** — select any JPG, PNG, or BMP file.
2. Adjust the sliders on the right panel:
   - **Gaussian Blur Radius** (1–10): higher = smoother edges, less noise
   - **Edge Threshold / Sobel** (10–200): lower = more edges detected
   - **Color Levels** (2–16): lower = more flat cartoon regions
3. Click **🎨 Apply Filter** — the cartoon output appears on the right.
4. Click **💾 Save Output** — saves the cartoon image as a PNG file.

---

## 🔬 Processing Pipeline Details

### Stage 1 — Gaussian Blur
Applies a separable 2-pass 1-D Gaussian kernel to reduce noise before edge detection. Sigma is derived from the radius parameter.

### Stage 2 — Grayscale Conversion
```
L = 0.299·R + 0.587·G + 0.114·B
```
Uses the standard luminosity formula (ITU-R BT.601).

### Stage 3 — Sobel Edge Detection
Computes the gradient magnitude at each pixel:
```
Gx = [-1 0 +1; -2 0 +2; -1 0 +1]
Gy = [-1 -2 -1;  0  0  0; +1 +2 +1]
magnitude = sqrt(Gx² + Gy²)
```
Pixels with magnitude > threshold are marked as edges (black).

### Stage 4 — Color Quantization
Each RGB channel is independently quantized into `levels` bins:
```
quantized = floor(channel / step) * step + step/2
```
This creates the flat colour regions characteristic of cartoon style.

### Stage 5 — Cartoon Rendering
The edge mask and quantized image are merged: edge pixels → black, non-edge → quantized colour.

---

## 📸 Example Parameters

| Style | Blur | Edge Threshold | Color Levels |
|-------|------|---------------|--------------|
| Soft cartoon | 5 | 40 | 6 |
| Comic book | 2 | 80 | 4 |
| Sketch-like | 3 | 30 | 12 |
| Bold cartoon | 4 | 60 | 5 |

---

## 📚 References

- Lecture 9: Traditional Image Processing Cartoon Style App — 黃祥睿 (Xiang-Rui Huang), NPUST
- Oracle, "Java SE Documentation." Available: https://docs.oracle.com/en/java/
- R. C. Gonzalez & R. E. Woods, *Digital Image Processing*, 4th ed., Pearson, 2018.