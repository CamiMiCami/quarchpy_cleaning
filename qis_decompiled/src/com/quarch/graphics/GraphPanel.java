/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.JPanel;

public class GraphPanel
extends JPanel
implements MouseListener,
MouseMotionListener {
    private static final long serialVersionUID = 1L;
    public int displayMode = 0;
    private int maxXPoints = 0;
    private double chartMaxY = Double.MIN_VALUE;
    private long scaleDelayTimer = 0L;
    private int scaleDelayMax = 3000;
    private int padding = 25;
    private int labelPadding = 25;
    private int labelYPadding = 10;
    private int labelXPadding = 55;
    private Color chartGridColor = Color.BLACK;
    private Color chartBgColor = Color.WHITE;
    private static final Stroke GRAPH_STROKE = new BasicStroke(2.0f);
    private int pointWidth = 4;
    private int numberYDivisions = 10;
    private int numberXDivisions = 10;
    private double axisXMultiplier = 4.0E-6;
    private ArrayList<ArrayList<double[]>> pointArrays;
    private ArrayList<Color> lineColors;
    private boolean normalize = false;
    private ArrayList<String> xLabels;
    private ArrayList<String> yLabels;
    private ArrayList<String> zLabels;
    private ArrayList<double[]> bounds;
    private boolean YFromZero = false;
    private boolean DrawDots = true;
    private int preferedPowerYMax = 0;
    private int sequenceCount;
    public double zoomFactor;
    public int maxZoomFactor;
    private boolean trackMouseOver = false;
    private static boolean mouseOver = false;
    private static int mouseOverX = 0;
    private static int mouseOverY = 0;
    private double[] mouseOverYValues = new double[2];
    private int[] mouseOverYPoint = new int[2];
    private DecimalFormat dfX = new DecimalFormat("#0.##");
    private DecimalFormat dfY = new DecimalFormat("#0.000");
    private String[] TimeUnits = new String[]{"s", "ms", "us"};
    private int mouseOverXPos;
    static final float[] dash1 = new float[]{4.0f, 4.0f};
    static final BasicStroke dashed = new BasicStroke(1.0f, 0, 0, 10.0f, dash1, 0.0f);
    private int chartTopX;
    private int chartTopY;
    private int chartBottomX;
    private int chartBottomY;
    private boolean mouseInArea = false;

    public boolean isTrackMouseOver() {
        return this.trackMouseOver;
    }

    public void setTrackMouseOver(boolean trackMouseOver) {
        this.trackMouseOver = trackMouseOver;
    }

    public GraphPanel() {
        this.initGraphPanel();
    }

    public GraphPanel(int mode) {
        this.displayMode = mode;
        this.initGraphPanel();
    }

    private void initGraphPanel() {
        super.setDoubleBuffered(true);
        this.pointArrays = new ArrayList();
        this.lineColors = new ArrayList();
        this.xLabels = new ArrayList();
        this.yLabels = new ArrayList();
        this.zLabels = new ArrayList();
        this.bounds = new ArrayList();
        this.chartMaxY = Double.MIN_VALUE;
        this.sequenceCount = 1;
        this.maxZoomFactor = 8;
        this.zoomFactor = 1.0;
        this.trackMouseOver = true;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    private void paintQuickY(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        int fontSize = 11;
        g2.setFont(new Font("Helvetica", 0, fontSize));
        FontMetrics fontMetrics = g2.getFontMetrics();
        double Xmin = 0.0;
        double Xmax = 0.0;
        double Ymin = 0.0;
        double Ymax = 0.0;
        int xPoints = this.getXSize();
        int pixelChartWidth = this.getWidth() - this.padding - (this.padding + this.labelXPadding);
        int pointChartWidth = (int)((double)pixelChartWidth * this.zoomFactor);
        if (this.getHeight() - 2 * this.padding - this.labelYPadding < 2) {
            return;
        }
        for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
            if (this.newMinMaxRequired(lineIndex, true)) {
                this.calcMinMaxBounds(lineIndex, this.bounds);
            }
            if (this.normalize) {
                Xmin = this.getXMin(lineIndex);
                Xmax = this.getXMax(lineIndex);
                Ymin = this.getYMin(lineIndex);
                Ymax = this.getYMax(lineIndex);
            } else {
                if (lineIndex == 0) {
                    Xmin = 0.0;
                    Xmax = this.maxXPoints;
                    Ymin = this.getYMin(lineIndex);
                    Ymax = this.getYMax(lineIndex);
                }
                for (int i = 1; i < this.pointArrays.size(); ++i) {
                    if (this.pointArrays.get(lineIndex).size() <= 0) continue;
                    if (this.getYMin(i) < Ymin) {
                        Ymin = this.getYMin(i);
                    }
                    if (!(this.getYMax(i) > Ymax)) continue;
                    Ymax = this.getYMax(i);
                }
                if (this.YFromZero) {
                    Ymin = 0.0;
                }
            }
            if (this.pointArrays == null) {
                return;
            }
            if (this.pointArrays.get(lineIndex) != null) continue;
            return;
        }
        if (System.currentTimeMillis() - this.scaleDelayTimer >= (long)this.scaleDelayMax || Ymax >= this.chartMaxY || this.chartMaxY == Double.MIN_VALUE) {
            this.chartMaxY = Ymax;
            this.chartMaxY *= 1.1;
            this.scaleDelayTimer = System.currentTimeMillis();
        }
        if (this.preferedPowerYMax != 0 && (double)this.preferedPowerYMax > this.chartMaxY) {
            this.chartMaxY = this.preferedPowerYMax;
        }
        g2.setColor(this.getChartBgColor());
        g2.fillRect(this.padding + this.labelXPadding, this.padding, this.getWidth() - 2 * this.padding - this.labelXPadding, this.getHeight() - 2 * this.padding - this.labelYPadding);
        g2.setColor(this.getChartBgColor());
        if (this.pointArrays.size() > 0) {
            for (int i = 0; i < this.numberYDivisions + 1; ++i) {
                int y0;
                int x0 = this.padding + this.labelXPadding;
                int x1 = this.pointWidth + x0;
                int y1 = y0 = this.getHeight() - (i * (this.getHeight() - this.padding * 2 - this.labelYPadding) / this.numberYDivisions + this.padding + this.labelYPadding);
                if (xPoints > 0) {
                    g2.setColor(this.getChartGridColor());
                    g2.drawLine(x1 + 1, y0, this.getWidth() - this.padding, y1);
                    g2.setColor(Color.BLACK);
                    String yLabel = this.yLabels.size() == this.numberYDivisions + 1 ? this.yLabels.get(i) : this.dfY.format((double)((int)((Ymin + (this.chartMaxY - Ymin) * ((double)i * 1.0 / (double)this.numberYDivisions)) * 1000.0)) / 1000.0);
                    int labelWidth = fontMetrics.stringWidth(yLabel);
                    if (!this.normalize || this.pointArrays.size() <= 1) {
                        g2.drawString(yLabel, x0 - labelWidth - 5, y0 + fontMetrics.getHeight() / 2 - 3);
                    }
                }
                g2.drawLine(x0, y0, x1, y1);
            }
            if (xPoints > 1) {
                double xValue = (double)pointChartWidth * this.axisXMultiplier;
                int xUnitsIdx = 0;
                if (xValue < 0.0) {
                    xValue *= 1000.0;
                    xUnitsIdx = 1;
                    if (xValue < 0.0) {
                        xValue *= 1000.0;
                        xUnitsIdx = 2;
                    }
                }
                String xLabel = this.dfX.format(xValue) + this.TimeUnits[xUnitsIdx];
                int labelWidth = fontMetrics.stringWidth(xLabel);
                g2.drawString(xLabel, this.getWidth() - (labelWidth + 30), this.getHeight() - fontMetrics.getHeight());
            }
        }
        this.chartTopX = this.padding + this.labelXPadding;
        this.chartTopY = this.padding;
        this.chartBottomX = this.getWidth() - this.padding;
        this.chartBottomY = this.getHeight() - this.padding - this.labelYPadding;
        if (this.pointArrays.size() > 1) {
            this.drawLines1(g2, Xmin, Xmax, Ymin);
            if (this.trackMouseOver) {
                this.processMouseOver(g2, this.chartTopX, this.chartTopY, this.chartBottomX, this.chartBottomY);
            }
        }
    }

    private void processMouseOver(Graphics2D g2, int chartTopX, int chartTopY, int chartBottomX, int chartBottomY) {
        if (mouseOver) {
            Stroke saveStroke = g2.getStroke();
            Color saveC = g2.getColor();
            Font saveFont = g2.getFont();
            Composite saveComp = g2.getComposite();
            g2.setFont(new Font("Helvetica", 1, 10));
            Color popoutColor = new Color(0.8f, 0.8f, 0.2f);
            g2.setPaint(popoutColor);
            g2.setComposite(AlphaComposite.getInstance(3, 0.65f));
            int rectTopX = mouseOverX > chartTopX + (chartBottomX - chartTopX) / 2 ? mouseOverX - 10 - 100 : mouseOverX + 10;
            int rectTopY = mouseOverY;
            if (rectTopY > chartBottomY - 10 - 30) {
                rectTopY = chartBottomY - 10 - 30;
            }
            if (rectTopY < chartTopY + 10) {
                rectTopY = chartTopY + 10;
            }
            g2.fillRect(rectTopX, rectTopY, 100, 30);
            g2.setPaint(Color.BLACK);
            g2.drawString("5V:  " + this.dfY.format(this.mouseOverYValues[0]), rectTopX + 5, rectTopY + 15);
            g2.drawString("12V: " + this.dfY.format(this.mouseOverYValues[1]), rectTopX + 5, rectTopY + 25);
            g2.setPaint(popoutColor);
            g2.setStroke(dashed);
            g2.drawLine(mouseOverX, chartTopY, mouseOverX, chartBottomY);
            g2.drawLine(chartTopX, this.mouseOverYPoint[0], chartBottomX, this.mouseOverYPoint[0]);
            g2.drawLine(chartTopX, this.mouseOverYPoint[1], chartBottomX, this.mouseOverYPoint[1]);
            int circleR = 3;
            g2.fillOval(mouseOverX - circleR, this.mouseOverYPoint[0] - circleR, circleR * 2, circleR * 2);
            g2.fillOval(mouseOverX - circleR, this.mouseOverYPoint[1] - circleR, circleR * 2, circleR * 2);
            g2.setComposite(saveComp);
            g2.setPaint(Color.BLACK);
            g2.fillPolygon(new int[]{mouseOverX, mouseOverX - 5, mouseOverX + 5, mouseOverX}, new int[]{chartTopY, chartTopY - 8, chartTopY - 8, chartTopY}, 4);
            g2.setFont(saveFont);
            g2.setPaint(saveC);
            g2.setStroke(saveStroke);
        }
    }

    private void drawLines2(Graphics2D g2, double Xmin, double Xmax, double Ymin) {
        double xScale = (double)this.getChartWidth() / (Xmax - Xmin);
        double yScale = ((double)this.getHeight() - (double)(2 * this.padding) - (double)this.labelYPadding) / (this.chartMaxY - Ymin);
        int startPoint = this.pointArrays.get(0).size() - 1;
        int pixelChartWidth = this.getWidth() - this.padding - (this.padding + this.labelXPadding);
        int pointChartWidth = (int)((double)pixelChartWidth * this.zoomFactor);
        int endPoint = 0;
        endPoint = startPoint > pointChartWidth ? startPoint - pointChartWidth : 0;
        Color[] lineColor = new Color[this.pointArrays.size()];
        int prevX = 0;
        int[] prevY = new int[this.pointArrays.size()];
        for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
            lineColor[lineIndex] = this.lineColors.get(lineIndex);
        }
        int xPadding = this.padding + this.labelXPadding;
        if (this.zoomFactor <= 1.0) {
            int y1;
            double dVal;
            int x1;
            prevX = x1 = this.getWidth() - this.padding;
            for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
                dVal = this.pointArrays.get(lineIndex).get(startPoint)[1];
                prevY[lineIndex] = y1 = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
            }
            int subPixelPixel = (int)(1.0 / this.zoomFactor);
            int subPixelCount = this.sequenceCount % subPixelPixel;
            for (int i = startPoint; i >= endPoint; --i) {
                x1 -= subPixelCount;
                subPixelCount = subPixelPixel;
                for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
                    dVal = this.pointArrays.get(lineIndex).get(i)[1];
                    y1 = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
                    g2.setColor(lineColor[lineIndex]);
                    g2.drawLine(prevX, prevY[lineIndex], x1, y1);
                    prevY[lineIndex] = y1;
                }
                prevX = x1;
            }
        } else {
            double dVal;
            int x1;
            prevX = x1 = this.getWidth() - this.padding;
            int seqCount = this.sequenceCount;
            boolean newXPrev = false;
            for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
                dVal = this.pointArrays.get(lineIndex).get(startPoint)[1];
                prevY[lineIndex] = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
            }
            for (int i = startPoint; i >= endPoint; --i) {
                if (seqCount % (int)this.zoomFactor == 0) {
                    --x1;
                    newXPrev = true;
                }
                if (--seqCount < 1) {
                    seqCount = this.maxZoomFactor;
                }
                for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
                    dVal = this.pointArrays.get(lineIndex).get(i)[1];
                    dVal = (this.chartMaxY - dVal) * yScale;
                    int y1 = (int)dVal + this.padding;
                    g2.setColor(lineColor[lineIndex]);
                    g2.drawLine(prevX, prevY[lineIndex], x1, y1);
                    prevY[lineIndex] = y1;
                }
                if (!newXPrev) continue;
                prevX = x1;
                newXPrev = false;
            }
        }
    }

    private void drawLines1(Graphics2D g2, double Xmin, double Xmax, double Ymin) {
        int pointsSize = this.pointArrays.size();
        for (int lineIndex = 0; lineIndex < pointsSize; ++lineIndex) {
            int x1;
            Color lineColor = this.lineColors.get(lineIndex);
            g2.setColor(lineColor);
            g2.setStroke(GRAPH_STROKE);
            double xScale = (double)this.getChartWidth() / (Xmax - Xmin);
            double yScale = ((double)this.getHeight() - (double)(2 * this.padding) - (double)this.labelYPadding) / (this.chartMaxY - Ymin);
            int prevX = 0;
            int prevY = 0;
            ArrayList<double[]> points = this.pointArrays.get(lineIndex);
            int startPoint = points.size() - 1;
            int pixelChartWidth = this.getWidth() - this.padding - (this.padding + this.labelXPadding);
            int pointChartWidth = (int)((double)pixelChartWidth * this.zoomFactor);
            int endPoint = 0;
            endPoint = startPoint > pointChartWidth ? startPoint - pointChartWidth : 0;
            int xPadding = this.padding + this.labelXPadding;
            if (this.zoomFactor <= 1.0) {
                int y1;
                prevX = x1 = this.getWidth() - this.padding;
                double dVal = points.get(startPoint)[1];
                prevY = y1 = (int)Math.round((this.chartMaxY - dVal) * yScale) + this.padding;
                int subPixelPixel = (int)Math.round(1.0 / this.zoomFactor);
                int subPixelCount = this.sequenceCount % subPixelPixel;
                for (int i = startPoint; i >= endPoint; --i) {
                    subPixelCount = subPixelPixel;
                    dVal = points.get(i)[1];
                    y1 = (int)Math.round((this.chartMaxY - dVal) * yScale) + this.padding;
                    if (mouseOver && mouseOverX > (x1 -= subPixelCount) - subPixelCount && mouseOverX <= x1) {
                        this.mouseOverYValues[lineIndex] = dVal;
                        this.mouseOverXPos = x1;
                        this.mouseOverYPoint[lineIndex] = y1;
                    }
                    g2.drawLine(prevX, prevY, x1, y1);
                    prevX = x1;
                    prevY = y1;
                }
                continue;
            }
            prevX = x1 = this.getWidth() - this.padding;
            int seqCount = this.sequenceCount;
            boolean newXPrev = false;
            double dVal = points.get(startPoint)[1];
            prevY = (int)Math.round((this.chartMaxY - dVal) * yScale) + this.padding;
            for (int i = startPoint; i >= endPoint; --i) {
                if (seqCount % (int)Math.round(this.zoomFactor) == 0) {
                    --x1;
                    newXPrev = true;
                }
                if (--seqCount < 1) {
                    seqCount = this.maxZoomFactor;
                }
                dVal = points.get(i)[1];
                int y1 = (int)Math.round((this.chartMaxY - dVal) * yScale) + this.padding;
                if (mouseOver && mouseOverX >= x1 && mouseOverX <= x1) {
                    this.mouseOverYValues[lineIndex] = dVal;
                    this.mouseOverXPos = x1;
                    this.mouseOverYPoint[lineIndex] = y1;
                }
                g2.drawLine(prevX, prevY, x1, y1);
                if (newXPrev) {
                    prevX = x1;
                    newXPrev = false;
                }
                prevY = y1;
            }
        }
    }

    private void calcMinMaxBounds(int lineIndex, ArrayList<double[]> bounds) {
        this.clearMinMaxRequiredFlag(lineIndex);
        double[] bound = bounds.get(lineIndex);
        ArrayList<double[]> point = this.pointArrays.get(lineIndex);
        bound[0] = point.get(0)[0];
        bound[1] = point.get(0)[0];
        bound[2] = point.get(0)[1];
        bound[3] = point.get(0)[1];
        int arrayLen = point.size();
        if (arrayLen == 2) {
            bound[0] = Math.min(bound[0], point.get(1)[0]);
            bound[1] = Math.max(bound[1], point.get(1)[0]);
            bound[2] = Math.min(bound[2], point.get(1)[1]);
            bound[3] = Math.max(bound[3], point.get(1)[1]);
        } else {
            for (int pIdx = 2; pIdx < arrayLen - 2; pIdx += 2) {
                double x0 = point.get(pIdx)[0];
                double x1 = point.get(pIdx + 1)[0];
                double y0 = point.get(pIdx)[1];
                double y1 = point.get(pIdx + 1)[1];
                if (x0 > x1) {
                    bound[0] = Math.min(bound[0], x1);
                    bound[1] = Math.max(bound[1], x0);
                } else {
                    bound[0] = Math.min(bound[0], x0);
                    bound[1] = Math.max(bound[1], x1);
                }
                if (y0 > y1) {
                    bound[2] = Math.min(bound[2], y1);
                    bound[3] = Math.max(bound[3], y0);
                    continue;
                }
                bound[2] = Math.min(bound[2], y0);
                bound[3] = Math.max(bound[3], y1);
            }
            if ((arrayLen & 1) == 1) {
                bound[0] = Math.min(bound[0], point.get(arrayLen - 1)[0]);
                bound[1] = Math.max(bound[1], point.get(arrayLen - 1)[0]);
                bound[2] = Math.min(bound[2], point.get(arrayLen - 1)[1]);
                bound[3] = Math.max(bound[3], point.get(arrayLen - 1)[1]);
            }
        }
    }

    public int getChartWidth() {
        return this.getWidth() - 2 * this.padding - this.labelXPadding;
    }

    private int[][] makeVerticalPlane(int leftX, int rightX, int xAdjust, int bottomY, int yAdjust, int planeHeight) {
        int[][] planePoints = new int[2][4];
        planePoints[0][0] = leftX + xAdjust;
        planePoints[0][1] = leftX + xAdjust;
        planePoints[0][2] = rightX;
        planePoints[0][3] = rightX;
        planePoints[1][0] = bottomY - yAdjust - planeHeight;
        planePoints[1][1] = bottomY - yAdjust;
        planePoints[1][2] = bottomY - yAdjust;
        planePoints[1][3] = bottomY - yAdjust - planeHeight;
        return planePoints;
    }

    private void makeWallPlane(int chartPlaneLeftX, int chartPlaneBottomY, int xAdjust, int zAdjust, int wallHeight, int[][] leftWallpoints) {
        leftWallpoints[0] = new int[]{chartPlaneLeftX, chartPlaneLeftX + xAdjust, chartPlaneLeftX + xAdjust, chartPlaneLeftX, chartPlaneLeftX};
        leftWallpoints[1] = new int[]{chartPlaneBottomY, chartPlaneBottomY - zAdjust, chartPlaneBottomY - zAdjust - wallHeight, chartPlaneBottomY - wallHeight, chartPlaneBottomY};
    }

    private void makeFlootPlane(int chartPlaneLeftX, int chartPlaneRightX, int chartPlaneBottomY, int xAdjust, int zAdjust, int[][] floorWallpoints) {
        floorWallpoints[0] = new int[]{chartPlaneLeftX, chartPlaneLeftX + xAdjust, chartPlaneRightX, chartPlaneRightX - xAdjust};
        floorWallpoints[1] = new int[]{chartPlaneBottomY, chartPlaneBottomY - zAdjust, chartPlaneBottomY - zAdjust, chartPlaneBottomY};
    }

    private void paintXYZ(Graphics g) {
        int i;
        double seriesWidth;
        double zLength;
        int nSeries;
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int fontSize = 11;
        g2.setFont(new Font("Helvetica", 0, fontSize));
        double dataXmin = 0.0;
        double dataXmax = 0.0;
        double dataYmin = 0.0;
        double dataYmax = 0.0;
        int xPoints = this.getXSize();
        int labelXPadding = this.labelPadding;
        int labelYPadding = this.labelPadding;
        int topBorder = this.padding;
        int rightBorder = this.padding;
        int leftBorder = this.padding + labelXPadding;
        int bottomBorder = this.padding + labelYPadding;
        int chartAreaWidth = this.getWidth() - (topBorder + bottomBorder);
        int chartAreaHeight = this.getHeight() - (this.padding + leftBorder);
        int chartPlaneLeftX = leftBorder;
        int chartPlaneRightX = chartPlaneLeftX + chartAreaWidth;
        int chartPlaneTopY = topBorder;
        int chartPlaneBottomY = chartPlaneTopY + chartAreaHeight;
        int chartPlaneWidth = chartPlaneRightX - chartPlaneLeftX;
        int chartPlaneHeight = chartPlaneBottomY - chartPlaneTopY;
        g2.setColor(new Color(0.9f, 0.9f, 0.9f));
        g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        g2.setColor(Color.WHITE);
        double isoAngle = 45.0;
        double isoAngleSin = Math.sin(Math.toRadians(isoAngle));
        double isoAngleCos = Math.cos(Math.toRadians(isoAngle));
        if (this.pointArrays != null) {
            nSeries = this.pointArrays.size();
            if (nSeries <= 0) {
                nSeries = 1;
            }
        } else {
            nSeries = 1;
        }
        if ((double)chartAreaHeight < (zLength = (seriesWidth = 30.0) * (double)nSeries) + 30.0) {
            zLength = chartAreaHeight - 30;
            seriesWidth = zLength / (double)nSeries;
        }
        double xStep = seriesWidth * isoAngleCos;
        double zStep = seriesWidth * isoAngleSin;
        int xAdjust = (int)(zLength * isoAngleCos + 0.5);
        int zAdjust = (int)(zLength * isoAngleSin + 0.5);
        int wallHeight = chartAreaHeight - zAdjust - 10;
        int[][] leftWallpoints = new int[2][];
        this.makeWallPlane(chartPlaneLeftX, chartPlaneBottomY, xAdjust, zAdjust, wallHeight, leftWallpoints);
        g2.fillPolygon(leftWallpoints[0], leftWallpoints[1], leftWallpoints[1].length);
        int[][] backWallpoints = this.makeVerticalPlane(chartPlaneLeftX, chartPlaneRightX, xAdjust, chartPlaneBottomY, zAdjust, wallHeight);
        g2.fillPolygon(backWallpoints[0], backWallpoints[1], backWallpoints[1].length);
        int[][] floorWallpoints = new int[2][];
        this.makeFlootPlane(chartPlaneLeftX, chartPlaneRightX, chartPlaneBottomY, xAdjust, zAdjust, floorWallpoints);
        g2.fillPolygon(floorWallpoints[0], floorWallpoints[1], floorWallpoints[1].length);
        for (int i2 = 0; i2 < nSeries; ++i2) {
            int[][] axesPoints = this.makeVerticalPlane(chartPlaneLeftX + (int)(xStep * (double)i2), chartPlaneRightX - (int)(xStep * (double)(nSeries - 1 - i2)), (int)xStep, chartPlaneBottomY - (int)(zStep * (double)i2), (int)zStep, wallHeight);
            if (i2 < nSeries - 1) {
                g2.setColor(Color.lightGray);
                g2.drawPolyline(axesPoints[0], axesPoints[1], 3);
            }
            if (this.zLabels == null || this.zLabels.size() <= i2) continue;
            FontMetrics metrics = g2.getFontMetrics();
            int labelWidth = metrics.stringWidth(this.xLabels.get(i2));
            g2.setColor(Color.black);
            g2.drawString(this.zLabels.get(i2), axesPoints[0][0] - labelWidth - (int)(xStep / 2.0), axesPoints[1][0] - metrics.getHeight() / 2 + (int)(zStep / 2.0));
        }
        int ticksX = 0;
        if (this.xLabels != null) {
            ticksX = this.xLabels.size();
        }
        double floorWidth = floorWallpoints[0][3] - floorWallpoints[0][0];
        for (i = 0; i < ticksX; ++i) {
            int[][] axesPanel = new int[2][];
            int n = ticksX > 1 ? (int)(floorWidth / (double)(ticksX - 1) * (double)i) : 0;
            this.makeWallPlane(chartPlaneLeftX + n, chartPlaneBottomY, xAdjust, zAdjust, wallHeight, axesPanel);
            g2.setColor(Color.lightGray);
            g2.drawPolyline(axesPanel[0], axesPanel[1], 3);
            FontMetrics metrics = g2.getFontMetrics();
            if (i >= this.xLabels.size()) continue;
            int labelWidth = metrics.stringWidth(this.xLabels.get(i));
            g2.setColor(Color.black);
            g2.drawString(this.xLabels.get(i), axesPanel[0][0] - labelWidth / 2, axesPanel[1][0] + metrics.getHeight() + 3);
        }
        for (i = 1; i < this.numberYDivisions; ++i) {
            int[][] floorWallaxes = new int[2][];
            int n = (wallHeight * i + 1) / this.numberYDivisions;
            this.makeFlootPlane(chartPlaneLeftX, chartPlaneRightX, chartPlaneBottomY - n, xAdjust, zAdjust, floorWallaxes);
            g2.setColor(Color.lightGray);
            g2.drawPolyline(floorWallaxes[0], floorWallaxes[1], 3);
        }
        g2.setColor(Color.BLACK);
        g2.drawPolygon(leftWallpoints[0], leftWallpoints[1], leftWallpoints[1].length);
        g2.drawPolygon(backWallpoints[0], backWallpoints[1], backWallpoints[1].length);
        g2.drawPolygon(floorWallpoints[0], floorWallpoints[1], floorWallpoints[1].length);
        if (!this.normalize) {
            double[] minMaxArray = this.calcDataMinMAx();
            dataXmin = minMaxArray[0];
            dataXmax = minMaxArray[1];
            dataYmin = minMaxArray[2];
            dataYmax = minMaxArray[3];
        }
        if (this.pointArrays == null) {
            return;
        }
        ArrayList<ArrayList<Point>> graphPoints = new ArrayList<ArrayList<Point>>();
        for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
            if (this.pointArrays.get(lineIndex) == null) {
                return;
            }
            int n = (int)(xStep / 2.0);
            int zOffset = (int)(zStep / 2.0);
            int[][] planePoints = this.makeVerticalPlane(chartPlaneLeftX + (int)(xStep * (double)lineIndex) - n, chartPlaneRightX - (int)(xStep * (double)(nSeries - 1 - lineIndex)) - n, (int)xStep, chartPlaneBottomY - (int)(zStep * (double)lineIndex) + zOffset, (int)zStep, wallHeight);
            if (this.normalize) {
                dataXmin = this.getXMin(lineIndex);
                dataXmax = this.getXMax(lineIndex);
                dataYmin = this.getYMin(lineIndex);
                dataYmax = this.getYMax(lineIndex);
            }
            graphPoints.add(this.scaleSeriesPointsToPlane(dataXmin, dataXmax, dataYmin, dataYmax, planePoints[0][0], planePoints[1][0], planePoints[0][2] - planePoints[0][0], planePoints[1][1] - planePoints[1][0], lineIndex));
        }
        this.numberYDivisions = 9;
        if (this.pointArrays.size() > 0) {
            for (int i3 = 0; i3 < this.numberYDivisions + 1; ++i3) {
                int y0;
                int n = chartPlaneLeftX;
                int x1 = this.pointWidth + chartPlaneLeftX;
                int y1 = y0 = chartPlaneBottomY - i3 * wallHeight / this.numberYDivisions;
                if (xPoints > 0) {
                    g2.setColor(this.getChartGridColor());
                    g2.setColor(Color.BLACK);
                    String yLabel = this.yLabels.size() == this.numberYDivisions + 1 ? this.yLabels.get(i3) : Double.toString((double)((int)((dataYmin + (dataYmax - dataYmin) * ((double)i3 * 1.0 / (double)this.numberYDivisions)) * 100.0)) / 100.0);
                    FontMetrics metrics = g2.getFontMetrics();
                    int labelWidth = metrics.stringWidth(yLabel);
                    if (!this.normalize || this.pointArrays.size() <= 1) {
                        g2.drawString(yLabel, n - labelWidth - 5, y0 + metrics.getHeight() / 2 - 3);
                    }
                }
                g2.drawLine(n, y0, x1, y1);
            }
            if (xPoints > 1) {
                FontMetrics metrics = g2.getFontMetrics();
                String string = Integer.toString(xPoints);
                int labelWidth = metrics.stringWidth(string);
                g2.drawString(string, this.getWidth() - (labelWidth + 30), this.getHeight() - metrics.getHeight());
            }
        }
        for (ArrayList arrayList : graphPoints) {
            Stroke oldStroke = g2.getStroke();
            Color lineColor = this.lineColors.get(graphPoints.indexOf(arrayList));
            g2.setStroke(GRAPH_STROKE);
            for (int i4 = 0; i4 < arrayList.size() - 1; ++i4) {
                int x1 = ((Point)arrayList.get((int)i4)).x;
                int y1 = ((Point)arrayList.get((int)i4)).y;
                int x2 = ((Point)arrayList.get((int)(i4 + 1))).x;
                int y2 = ((Point)arrayList.get((int)(i4 + 1))).y;
                g2.setColor(lineColor);
                g2.drawLine(x1, y1, x2, y2);
            }
            if (!this.DrawDots) continue;
            g2.setStroke(oldStroke);
            g2.setColor(this.lineColors.get(graphPoints.indexOf(arrayList)));
            for (int i2 = 0; i2 < arrayList.size(); ++i2) {
                int x = ((Point)arrayList.get((int)i2)).x - this.pointWidth / 2;
                int y = ((Point)arrayList.get((int)i2)).y - this.pointWidth / 2;
                int ovalW = this.pointWidth;
                int ovalH = this.pointWidth;
                boolean dotHighlight = false;
                if (this.mouseInArea && mouseOverX >= x - ovalW && mouseOverX <= x + ovalW && mouseOverY >= y - ovalH && mouseOverY <= y + ovalH) {
                    g2.setColor(Color.RED);
                    g2.fillOval(x - ovalW / 2, y - ovalH / 2, ovalW * 2, ovalH * 2);
                    dotHighlight = true;
                }
                if (dotHighlight) {
                    g2.setColor(this.lineColors.get(graphPoints.indexOf(arrayList)));
                    continue;
                }
                g2.fillOval(x, y, ovalW, ovalH);
            }
        }
        this.chartTopX = this.padding + labelXPadding;
        this.chartTopY = this.padding;
        this.chartBottomX = this.getWidth() - this.padding;
        this.chartBottomY = this.getHeight() - this.padding - labelYPadding;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (g == null) {
            return;
        }
        super.paintComponent(g);
        if (this.displayMode == 1) {
            this.paintQuickY(g);
            return;
        }
        if (this.displayMode == 0) {
            this.paintXYZ(g);
            return;
        }
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int fontSize = 11;
        g2.setFont(new Font("Helvetica", 0, fontSize));
        double dataXmin = 0.0;
        double dataXmax = 0.0;
        double dataYmin = 0.0;
        double dataYmax = 0.0;
        int xPoints = this.getXSize();
        int labelXPadding = this.labelPadding;
        int labelYPadding = this.labelPadding;
        int topBorder = this.padding;
        int rightBorder = this.padding;
        int leftBorder = this.padding + labelXPadding;
        int bottomBorder = this.padding + labelYPadding;
        int chartAreaWidth = this.getWidth() - (topBorder + bottomBorder);
        int chartAreaHeight = this.getHeight() - (this.padding + leftBorder);
        int chartPlaneLeftX = leftBorder;
        int chartPlaneRightX = chartPlaneLeftX + chartAreaWidth;
        int chartPlaneTopY = topBorder;
        int chartPlaneBottomY = chartPlaneTopY + chartAreaHeight;
        int chartPlaneWidth = chartPlaneRightX - chartPlaneLeftX;
        int chartPlaneHeight = chartPlaneBottomY - chartPlaneTopY;
        g2.setColor(Color.WHITE);
        g2.fillRect(leftBorder, topBorder, chartAreaWidth, chartAreaHeight);
        g2.setColor(Color.BLACK);
        double[] minMaxArray = this.calcDataMinMAx();
        dataXmin = minMaxArray[0];
        dataXmax = minMaxArray[1];
        dataYmin = minMaxArray[2];
        dataYmax = minMaxArray[3];
        ArrayList<ArrayList<Point>> graphPoints = new ArrayList<ArrayList<Point>>();
        if (this.pointArrays == null) {
            return;
        }
        for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
            if (this.pointArrays.get(lineIndex) == null) {
                return;
            }
            if (this.normalize) {
                dataXmin = this.getXMin(lineIndex);
                dataXmax = this.getXMax(lineIndex);
                dataYmin = this.getYMin(lineIndex);
                dataYmax = this.getYMax(lineIndex);
            }
            graphPoints.add(this.scaleSeriesPointsToPlane(dataXmin, dataXmax, dataYmin, dataYmax, chartPlaneLeftX, chartPlaneTopY, chartPlaneWidth, chartPlaneHeight, lineIndex));
        }
        if (this.pointArrays.size() > 0) {
            for (int i = 0; i < this.numberYDivisions + 1; ++i) {
                int y0;
                int n = chartPlaneLeftX;
                int x1 = this.pointWidth + chartPlaneLeftX;
                int y1 = y0 = chartPlaneBottomY - i * chartAreaHeight / this.numberYDivisions;
                if (xPoints > 0) {
                    g2.setColor(this.getChartGridColor());
                    g2.drawLine(chartPlaneLeftX + 1 + this.pointWidth, y0, chartPlaneRightX, y1);
                    g2.setColor(Color.BLACK);
                    String yLabel = this.yLabels.size() == this.numberYDivisions + 1 ? this.yLabels.get(i) : Double.toString((double)((int)((dataYmin + (dataYmax - dataYmin) * ((double)i * 1.0 / (double)this.numberYDivisions)) * 100.0)) / 100.0);
                    FontMetrics metrics = g2.getFontMetrics();
                    int labelWidth = metrics.stringWidth(yLabel);
                    if (!this.normalize || this.pointArrays.size() <= 1) {
                        g2.drawString(yLabel, n - labelWidth - 5, y0 + metrics.getHeight() / 2 - 3);
                    }
                }
                g2.drawLine(n, y0, x1, y1);
            }
            if (xPoints > 1) {
                FontMetrics metrics = g2.getFontMetrics();
                String string = Integer.toString(xPoints);
                int labelWidth = metrics.stringWidth(string);
                g2.drawString(string, this.getWidth() - (labelWidth + 30), this.getHeight() - metrics.getHeight());
            }
        }
        g2.drawLine(chartPlaneLeftX, chartPlaneBottomY, chartPlaneLeftX, chartPlaneTopY);
        g2.drawLine(chartPlaneLeftX, chartPlaneBottomY, chartPlaneRightX, chartPlaneBottomY);
        for (ArrayList arrayList : graphPoints) {
            int i;
            Stroke oldStroke = g2.getStroke();
            Color lineColor = this.lineColors.get(graphPoints.indexOf(arrayList));
            g2.setStroke(GRAPH_STROKE);
            for (i = 0; i < arrayList.size() - 1; ++i) {
                int x1 = ((Point)arrayList.get((int)i)).x;
                int y1 = ((Point)arrayList.get((int)i)).y;
                int x2 = ((Point)arrayList.get((int)(i + 1))).x;
                int y2 = ((Point)arrayList.get((int)(i + 1))).y;
                g2.setColor(lineColor);
                g2.drawLine(x1, y1, x2, y2);
            }
            if (!this.DrawDots) continue;
            g2.setStroke(oldStroke);
            g2.setColor(this.lineColors.get(graphPoints.indexOf(arrayList)));
            for (i = 0; i < arrayList.size(); ++i) {
                int x = ((Point)arrayList.get((int)i)).x - this.pointWidth / 2;
                int y = ((Point)arrayList.get((int)i)).y - this.pointWidth / 2;
                int ovalW = this.pointWidth;
                int ovalH = this.pointWidth;
                g2.fillOval(x, y, ovalW, ovalH);
            }
        }
    }

    private ArrayList<Point> scaleSeriesPointsToPlane(double Xmin, double Xmax, double Ymin, double Ymax, int chartPlaneLeftX, int chartPlaneTopY, int chartPlaneWidth, int chartPlaneHeight, int lineIndex) {
        double xScale = (double)chartPlaneWidth / (Xmax - Xmin);
        double yScale = (double)chartPlaneHeight / (Ymax - Ymin);
        ArrayList<Point> thisPointsSet = new ArrayList<Point>();
        for (int i = 0; i < this.pointArrays.get(lineIndex).size(); ++i) {
            double dVal = this.pointArrays.get(lineIndex).get(i)[0];
            int x1 = (int)(dVal * xScale) + chartPlaneLeftX;
            dVal = this.pointArrays.get(lineIndex).get(i)[1];
            int y1 = (int)((Ymax - dVal) * yScale) + chartPlaneTopY;
            thisPointsSet.add(new Point(x1, y1));
        }
        return thisPointsSet;
    }

    private double[] calcDataMinMAx() {
        double[] minMaxArray = new double[4];
        for (int lineIndex = 0; lineIndex < this.pointArrays.size(); ++lineIndex) {
            for (int i = 0; i < this.pointArrays.size(); ++i) {
                if (i == 0) {
                    minMaxArray[0] = this.getXMin(lineIndex);
                    minMaxArray[1] = this.getXMax(lineIndex);
                    minMaxArray[2] = this.getYMin(lineIndex);
                    minMaxArray[3] = this.getYMax(lineIndex);
                }
                if (this.getXMin(i) < minMaxArray[0]) {
                    minMaxArray[0] = this.getXMin(i);
                }
                if (this.getXMax(i) > minMaxArray[1]) {
                    minMaxArray[1] = this.getXMax(i);
                }
                if (this.getYMin(i) < minMaxArray[2]) {
                    minMaxArray[2] = this.getYMin(i);
                }
                if (!(this.getYMax(i) > minMaxArray[3])) continue;
                minMaxArray[3] = this.getYMax(i);
            }
            if (!this.YFromZero) continue;
            minMaxArray[2] = 0.0;
        }
        return minMaxArray;
    }

    private void clearMinMaxRequiredFlag(int lineIndex) {
        this.bounds.get((int)lineIndex)[4] = -1.0;
        this.bounds.get((int)lineIndex)[5] = -1.0;
    }

    private boolean newMinMaxRequired(int lineIndex, boolean ignoreX) {
        if (ignoreX) {
            return this.bounds.get(lineIndex)[5] >= 0.0;
        }
        return this.bounds.get(lineIndex)[4] >= 0.0 || this.bounds.get(lineIndex)[5] >= 0.0;
    }

    private double getXMin(int lineIndex) {
        return this.bounds.get(lineIndex)[0];
    }

    private double getXMax(int lineIndex) {
        return this.bounds.get(lineIndex)[1];
    }

    private double getYMin(int lineIndex) {
        return this.bounds.get(lineIndex)[2];
    }

    private double getYMax(int lineIndex) {
        return this.bounds.get(lineIndex)[3];
    }

    public void addPoint(int line, double[] thisPoint) {
        if (line >= this.pointArrays.size()) {
            this.pointArrays.add(new ArrayList());
            this.bounds.add(new double[]{thisPoint[0], thisPoint[0], thisPoint[1], thisPoint[1], -1.0, -1.0});
            this.lineColors.add(Color.BLACK);
        }
        this.pointArrays.get(line).add(thisPoint);
        this.bounds.get((int)line)[0] = Math.min(this.bounds.get(line)[0], thisPoint[0]);
        this.bounds.get((int)line)[1] = Math.max(this.bounds.get(line)[1], thisPoint[0]);
        this.bounds.get((int)line)[2] = Math.min(this.bounds.get(line)[2], thisPoint[1]);
        this.bounds.get((int)line)[3] = Math.max(this.bounds.get(line)[3], thisPoint[1]);
        if (line == 0 && ++this.sequenceCount > this.maxZoomFactor) {
            this.sequenceCount = 1;
        }
    }

    public void addPoint(int line, Double[] ThisPoint) {
        Double x = 0.0;
        Double y = 0.0;
        try {
            x = (double)ThisPoint[0];
            y = (double)ThisPoint[1];
        }
        catch (NumberFormatException e) {
            System.err.println("graph: invalid point (" + ThisPoint[0] + "," + ThisPoint[1] + ") not plotted");
            return;
        }
        this.addPoint(line, new double[]{x, y});
    }

    public void addPoint(int line, String[] ThisPoint) {
        try {
            this.addPoint(line, new Double[]{Double.parseDouble(ThisPoint[0]), Double.parseDouble(ThisPoint[1])});
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void insertPoint(int idx) {
        double[] thisPoint = new double[]{0.0, 0.0};
        for (int line = 0; line < this.pointArrays.size(); ++line) {
            this.pointArrays.get(line).add(idx, thisPoint);
        }
    }

    public void deleteOldestPointS(int nPoints) {
        if (this.pointArrays.size() == 0) {
            return;
        }
        int currentPoints = this.pointArrays.get(0).size();
        if (nPoints > currentPoints) {
            nPoints = currentPoints;
        }
        for (int line = 0; line < this.pointArrays.size(); ++line) {
            ArrayList<double[]> points = this.pointArrays.get(line);
            points.subList(0, nPoints).clear();
            this.bounds.get((int)line)[4] = 1.0;
            this.bounds.get((int)line)[5] = 1.0;
        }
    }

    public void deleteOldestPoint() {
        for (int line = 0; line < this.pointArrays.size(); ++line) {
            double[] bound;
            ArrayList<double[]> points = this.pointArrays.get(line);
            double[] oldestPoint = points.get(0);
            if (oldestPoint[0] <= (bound = this.bounds.get(line))[0]) {
                bound[4] = 0.0;
            } else if (oldestPoint[0] >= bound[1]) {
                bound[4] = 1.0;
            }
            if (oldestPoint[1] <= bound[2]) {
                bound[5] = 0.0;
            } else if (oldestPoint[1] >= bound[3]) {
                bound[5] = 1.0;
            }
            points.remove(0);
        }
    }

    public void deletePoint(int idx) {
        for (int line = 0; line < this.pointArrays.size(); ++line) {
            double[] bound;
            ArrayList<double[]> points = this.pointArrays.get(line);
            double[] thisPoint = points.get(idx);
            if (thisPoint[0] <= (bound = this.bounds.get(line))[0]) {
                bound[4] = 0.0;
            } else if (thisPoint[0] >= bound[1]) {
                bound[4] = 1.0;
            }
            if (thisPoint[1] <= bound[2]) {
                bound[5] = 0.0;
            } else if (thisPoint[1] >= bound[3]) {
                bound[5] = 1.0;
            }
            points.remove(idx);
        }
    }

    private int getXSize() {
        int max = 0;
        for (ArrayList<double[]> thisLine : this.pointArrays) {
            if (thisLine.size() <= max) continue;
            max = thisLine.size();
        }
        return max;
    }

    public void addXLabel(String thisLabel) {
        this.xLabels.add(thisLabel);
    }

    public void addZLabel(String thisLabel) {
        this.zLabels.add(thisLabel);
    }

    public void setYMinZero(boolean state) {
        this.YFromZero = state;
    }

    public void drawDots(boolean state) {
        this.DrawDots = state;
    }

    public void clearPoints() {
        this.pointArrays.clear();
    }

    public int PointCount() {
        if (this.pointArrays.size() > 0) {
            return this.pointArrays.get(0).size();
        }
        return 0;
    }

    public void clear() {
        this.pointArrays.clear();
        this.bounds.clear();
        this.lineColors.clear();
        this.xLabels.clear();
        this.yLabels.clear();
        this.zLabels.clear();
        this.invalidate();
    }

    public void normalize(boolean state) {
        this.normalize = state;
    }

    public int GetMaxXPoints() {
        return this.maxXPoints;
    }

    public void SetMaxXPoints(int maxPoints) {
        this.maxXPoints = maxPoints;
    }

    public void SetScaleDelayMax(int value) {
        if (value != this.scaleDelayMax) {
            this.scaleDelayMax = value;
            this.scaleDelayTimer = System.currentTimeMillis();
        }
    }

    public void SetAxisXMultiplier(double value) {
        this.axisXMultiplier = value;
    }

    public void setLineColor(int line, Color color) {
        if (line < this.lineColors.size()) {
            this.lineColors.set(line, color);
        } else {
            this.lineColors.add(line, color);
        }
    }

    public Color getChartGridColor() {
        return this.chartGridColor;
    }

    public void setChartGridColor(Color chartGridColor) {
        this.chartGridColor = chartGridColor;
    }

    public Color getChartBgColor() {
        return this.chartBgColor;
    }

    public void setChartBgColor(Color chartBgColor) {
        this.chartBgColor = chartBgColor;
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        if (this.mouseInArea) {
            int mouseX = arg0.getX();
            int mouseY = arg0.getY();
            boolean bl = mouseOver = mouseX >= this.chartTopX && mouseX <= this.chartBottomX && mouseY >= this.chartTopY && mouseY <= this.chartBottomY;
            if (mouseOver) {
                mouseOverX = arg0.getX();
                mouseOverY = arg0.getY();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        if (this.trackMouseOver) {
            this.mouseInArea = true;
        }
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        if (this.trackMouseOver) {
            this.mouseInArea = false;
            mouseOver = false;
        }
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }

    public int getPreferedPowerYMax() {
        return this.preferedPowerYMax;
    }

    public void setPreferedPowerYMax(int preferedPowerYMax) {
        this.preferedPowerYMax = preferedPowerYMax;
    }
}

