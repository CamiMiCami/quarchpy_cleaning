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
import java.awt.GradientPaint;
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
import java.util.concurrent.Semaphore;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;
import src.com.quarch.graphics.GraphPanelEvent;
import src.com.quarch.graphics.GraphPanelEventListener;

public class GraphPanelDQ
extends JPanel
implements MouseListener,
MouseMotionListener {
    protected EventListenerList listenerList = new EventListenerList();
    private static final long serialVersionUID = 1L;
    private final Semaphore semaphore = new Semaphore(1, true);
    private boolean updateInProgress = false;
    private int displayMode = 0;
    private int maxXPoints = 0;
    private double chartMaxY = Double.MIN_VALUE;
    private int scaleDelayCount = 0;
    private int scaleDelayMax = 120;
    private int padding = 25;
    private int labelPadding = 25;
    private int labelYPadding = 10;
    private int labelXPadding = 55;
    private Color gridColor = new Color(200, 200, 200, 200);
    private static final Stroke GRAPH_STROKE = new BasicStroke(1.0f);
    private int numberYDivisions = 10;
    private int numberXDivisions = 10;
    private double axisXMultiplier = 4.0E-6;
    private boolean normalize = false;
    private ArrayList<String> xLabels;
    private ArrayList<String> yLabels;
    private ArrayList<String> zLabels;
    private SeriesList seriesList = new SeriesList(6);
    private boolean YFromZero = false;
    private boolean DrawDots = true;
    private Color bgColor = Color.BLACK;
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
    private volatile boolean inPaint = false;
    private boolean mouseInArea = false;

    public void addEventListener(GraphPanelEventListener listener) {
        this.listenerList.add(GraphPanelEventListener.class, listener);
    }

    public void removeEventListener(GraphPanelEventListener listener) {
        this.listenerList.remove(GraphPanelEventListener.class, listener);
    }

    void fireGraphPanelEvent(GraphPanelEvent evt) {
        Object[] listeners = this.listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] != GraphPanelEventListener.class) continue;
            ((GraphPanelEventListener)listeners[i + 1]).GraphPanelEventOccurred(evt);
        }
    }

    public void beginUpdate() {
        try {
            this.semaphore.acquire();
            this.updateInProgress = true;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void endUpdate() {
        if (this.updateInProgress) {
            this.semaphore.release();
            this.updateInProgress = false;
        }
    }

    public int getDisplayMode() {
        return this.displayMode;
    }

    public void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
    }

    public SeriesList getSeriesList() {
        return this.seriesList;
    }

    public boolean isTrackMouseOver() {
        return this.trackMouseOver;
    }

    public void setTrackMouseOver(boolean trackMouseOver) {
        this.trackMouseOver = trackMouseOver;
    }

    public GraphPanelDQ() {
        this.initGraphPanel();
    }

    public GraphPanelDQ(int mode) {
        this.displayMode = mode;
        this.initGraphPanel();
    }

    private void initGraphPanel() {
        super.setDoubleBuffered(true);
        this.seriesList.series = new ArrayList();
        this.seriesList.bounds = new ArrayList();
        this.xLabels = new ArrayList();
        this.yLabels = new ArrayList();
        this.zLabels = new ArrayList();
        this.chartMaxY = Double.MIN_VALUE;
        this.scaleDelayCount = 0;
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
        for (int lineIndex = 0; lineIndex < this.seriesList.series.size(); ++lineIndex) {
            if (this.newMinMaxRequired(lineIndex, true)) {
                this.calcMinMaxBounds(lineIndex, this.seriesList.bounds);
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
                for (int i = 1; i < this.seriesList.series.size(); ++i) {
                    if (this.seriesList.series.get(lineIndex).size() <= 0) continue;
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
            if (this.seriesList.series == null) {
                return;
            }
            if (this.seriesList.series.get(lineIndex) != null) continue;
            return;
        }
        if (this.scaleDelayCount >= this.scaleDelayMax || Ymax >= this.chartMaxY || this.chartMaxY == Double.MIN_VALUE) {
            this.chartMaxY = Ymax;
            this.chartMaxY *= 1.1;
            this.scaleDelayCount = 0;
        }
        ++this.scaleDelayCount;
        g2.setColor(this.bgColor);
        g2.fillRect(this.padding + this.labelXPadding, this.padding, this.getWidth() - 2 * this.padding - this.labelXPadding, this.getHeight() - 2 * this.padding - this.labelYPadding);
        g2.setColor(Color.BLACK);
        if (this.seriesList.series.size() > 0) {
            for (int i = 0; i < this.numberYDivisions + 1; ++i) {
                int y0;
                int x0 = this.padding + this.labelXPadding;
                int x1 = this.seriesList.pointWidth + x0;
                int y1 = y0 = this.getHeight() - (i * (this.getHeight() - this.padding * 2 - this.labelYPadding) / this.numberYDivisions + this.padding + this.labelYPadding);
                if (xPoints > 0) {
                    g2.setColor(this.gridColor);
                    g2.drawLine(x1 + 1, y0, this.getWidth() - this.padding, y1);
                    g2.setColor(Color.BLACK);
                    String yLabel = this.yLabels.size() == this.numberYDivisions + 1 ? this.yLabels.get(i) : this.dfY.format((double)((int)((Ymin + (this.chartMaxY - Ymin) * ((double)i * 1.0 / (double)this.numberYDivisions)) * 1000.0)) / 1000.0);
                    int labelWidth = fontMetrics.stringWidth(yLabel);
                    if (!this.normalize || this.seriesList.series.size() <= 1) {
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
        if (this.seriesList.series.size() > 1) {
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

    private void drawLines1(Graphics2D g2, double Xmin, double Xmax, double Ymin) {
        int pointsSize = this.seriesList.series.size();
        for (int lineIndex = 0; lineIndex < pointsSize; ++lineIndex) {
            int x1;
            Color lineColor = this.seriesList.series.get(lineIndex).color;
            g2.setColor(lineColor);
            g2.setStroke(GRAPH_STROKE);
            double xScale = (double)this.getChartWidth() / (Xmax - Xmin);
            double yScale = ((double)this.getHeight() - (double)(2 * this.padding) - (double)this.labelYPadding) / (this.chartMaxY - Ymin);
            int prevX = 0;
            int prevY = 0;
            ArrayList points = this.seriesList.series.get(lineIndex);
            int startPoint = points.size() - 1;
            int pixelChartWidth = this.getWidth() - this.padding - (this.padding + this.labelXPadding);
            int pointChartWidth = (int)((double)pixelChartWidth * this.zoomFactor);
            int endPoint = 0;
            endPoint = startPoint > pointChartWidth ? startPoint - pointChartWidth : 0;
            int xPadding = this.padding + this.labelXPadding;
            if (this.zoomFactor <= 1.0) {
                int y1;
                prevX = x1 = this.getWidth() - this.padding;
                double dVal = ((double[])points.get(startPoint))[1];
                prevY = y1 = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
                int subPixelPixel = (int)(1.0 / this.zoomFactor);
                int subPixelCount = this.sequenceCount % subPixelPixel;
                for (int i = startPoint; i >= endPoint; --i) {
                    subPixelCount = subPixelPixel;
                    dVal = ((double[])points.get(i))[1];
                    y1 = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
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
            double dVal = ((double[])points.get(startPoint))[1];
            prevY = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
            for (int i = startPoint; i >= endPoint; --i) {
                if (seqCount % (int)this.zoomFactor == 0) {
                    --x1;
                    newXPrev = true;
                }
                if (--seqCount < 1) {
                    seqCount = this.maxZoomFactor;
                }
                dVal = ((double[])points.get(i))[1];
                int y1 = (int)((this.chartMaxY - dVal) * yScale) + this.padding;
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
        ArrayList point = this.seriesList.series.get(lineIndex);
        bound[0] = ((double[])point.get(0))[0];
        bound[1] = ((double[])point.get(0))[0];
        bound[2] = ((double[])point.get(0))[1];
        bound[3] = ((double[])point.get(0))[1];
        int arrayLen = point.size();
        if (arrayLen == 2) {
            bound[0] = Math.min(bound[0], ((double[])point.get(1))[0]);
            bound[1] = Math.max(bound[1], ((double[])point.get(1))[0]);
            bound[2] = Math.min(bound[2], ((double[])point.get(1))[1]);
            bound[3] = Math.max(bound[3], ((double[])point.get(1))[1]);
        } else {
            for (int pIdx = 2; pIdx < arrayLen - 2; pIdx += 2) {
                double x0 = ((double[])point.get(pIdx))[0];
                double x1 = ((double[])point.get(pIdx + 1))[0];
                double y0 = ((double[])point.get(pIdx))[1];
                double y1 = ((double[])point.get(pIdx + 1))[1];
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
                bound[0] = Math.min(bound[0], ((double[])point.get(arrayLen - 1))[0]);
                bound[1] = Math.max(bound[1], ((double[])point.get(arrayLen - 1))[0]);
                bound[2] = Math.min(bound[2], ((double[])point.get(arrayLen - 1))[1]);
                bound[3] = Math.max(bound[3], ((double[])point.get(arrayLen - 1))[1]);
            }
        }
    }

    public int getChartWidth() {
        return this.getWidth() - 2 * this.padding - this.labelXPadding;
    }

    private RenderPlane makeVerticalPlane(int leftX, int rightX, int xAdjust, int bottomY, int yAdjust, int planeHeight) {
        RenderPlane renderPlane = new RenderPlane();
        renderPlane.topLeft.x = leftX + xAdjust;
        renderPlane.topLeft.y = bottomY - yAdjust - planeHeight;
        renderPlane.bottomLeft.x = leftX + xAdjust;
        renderPlane.bottomLeft.y = bottomY - yAdjust;
        renderPlane.bottomRight.x = rightX;
        renderPlane.bottomRight.y = bottomY - yAdjust;
        renderPlane.topRight.x = rightX;
        renderPlane.topRight.y = bottomY - yAdjust - planeHeight;
        return renderPlane;
    }

    private RenderPlane makeWallPlane(int chartPlaneLeftX, int chartPlaneBottomY, int xAdjust, int zAdjust, int wallHeight) {
        RenderPlane renderPlane = new RenderPlane();
        renderPlane.topLeft.x = chartPlaneLeftX;
        renderPlane.topLeft.y = chartPlaneBottomY - wallHeight;
        renderPlane.bottomLeft.x = chartPlaneLeftX;
        renderPlane.bottomLeft.y = chartPlaneBottomY;
        renderPlane.bottomRight.x = chartPlaneLeftX + xAdjust;
        renderPlane.bottomRight.y = chartPlaneBottomY - zAdjust;
        renderPlane.topRight.x = chartPlaneLeftX + xAdjust;
        renderPlane.topRight.y = chartPlaneBottomY - zAdjust - wallHeight;
        return renderPlane;
    }

    private RenderPlane makeFloorPlane(int chartPlaneLeftX, int chartPlaneRightX, int chartPlaneBottomY, int xAdjust, int zAdjust) {
        RenderPlane renderPlane = new RenderPlane();
        renderPlane.topLeft.x = chartPlaneLeftX + xAdjust;
        renderPlane.topLeft.y = chartPlaneBottomY - zAdjust;
        renderPlane.bottomLeft.x = chartPlaneLeftX;
        renderPlane.bottomLeft.y = chartPlaneBottomY;
        renderPlane.bottomRight.x = chartPlaneRightX - xAdjust;
        renderPlane.bottomRight.y = chartPlaneBottomY;
        renderPlane.topRight.x = chartPlaneRightX;
        renderPlane.topRight.y = chartPlaneBottomY - zAdjust;
        return renderPlane;
    }

    private void paintXYZ(Graphics g) {
        int offset;
        int i;
        double seriesWidth;
        double zLength;
        int nSeriesGroups;
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
        if (this.seriesList.series != null) {
            nSeriesGroups = this.seriesList.getGroupCount();
            if (nSeriesGroups <= 0) {
                nSeriesGroups = 0;
            }
        } else {
            nSeriesGroups = 0;
        }
        if ((double)chartAreaHeight < (zLength = (seriesWidth = 30.0) * (double)nSeriesGroups) + 30.0) {
            zLength = chartAreaHeight - 30;
            seriesWidth = zLength / (double)nSeriesGroups;
        }
        double xStep = seriesWidth * isoAngleCos;
        double zStep = seriesWidth * isoAngleSin;
        int xAdjust = (int)(zLength * isoAngleCos + 0.5);
        int zAdjust = (int)(zLength * isoAngleSin + 0.5);
        int wallHeight = chartAreaHeight - zAdjust - 10;
        RenderPlane leftWallpoints = this.makeWallPlane(chartPlaneLeftX, chartPlaneBottomY, xAdjust, zAdjust, wallHeight);
        GradientPaint gp1 = new GradientPaint(0.0f, 0.0f, new Color(1.0f, 1.0f, 1.0f), leftWallpoints.getWidth() / 2, leftWallpoints.getHeight(), new Color(0.9f, 0.9f, 0.9f), true);
        g2.setPaint(gp1);
        leftWallpoints.fill(g2);
        RenderPlane backWallpoints = this.makeVerticalPlane(chartPlaneLeftX, chartPlaneRightX, xAdjust, chartPlaneBottomY, zAdjust, wallHeight);
        gp1 = new GradientPaint(0.0f, 0.0f, new Color(0.8f, 0.8f, 0.8f), backWallpoints.getWidth() / 2, backWallpoints.getHeight(), new Color(1.0f, 1.0f, 1.0f), true);
        g2.setPaint(gp1);
        backWallpoints.fill(g2);
        RenderPlane floorWallpoints = this.makeFloorPlane(chartPlaneLeftX, chartPlaneRightX, chartPlaneBottomY, xAdjust, zAdjust);
        gp1 = new GradientPaint(20.0f, 20.0f, new Color(0.9f, 0.9f, 0.9f), floorWallpoints.getWidth() / 2, floorWallpoints.getHeight() / 2, new Color(1.0f, 1.0f, 1.0f), true);
        g2.setPaint(gp1);
        floorWallpoints.fill(g2);
        for (int i2 = 0; i2 < nSeriesGroups; ++i2) {
            RenderPlane axesPoints = this.makeVerticalPlane(chartPlaneLeftX + (int)(xStep * (double)i2), chartPlaneRightX - (int)(xStep * (double)(nSeriesGroups - 1 - i2)), (int)xStep, chartPlaneBottomY - (int)(zStep * (double)i2), (int)zStep, wallHeight);
            if (i2 < nSeriesGroups - 1) {
                g2.setColor(Color.lightGray);
                axesPoints.drawSides(g2, true, true, false, false);
            }
            if (this.zLabels == null || this.zLabels.size() <= i2) continue;
            FontMetrics metrics = g2.getFontMetrics();
            int labelWidth = metrics.stringWidth(this.xLabels.get(i2));
            g2.setColor(Color.black);
            g2.drawString(this.zLabels.get(i2), axesPoints.topLeft.x - labelWidth - (int)(xStep / 2.0), axesPoints.topLeft.y - metrics.getHeight() / 2 + (int)(zStep / 2.0));
        }
        int ticksX = 0;
        if (this.xLabels != null) {
            ticksX = this.xLabels.size();
        }
        double floorWidth = floorWallpoints.getWidth();
        for (i = 0; i < ticksX; ++i) {
            offset = ticksX > 1 ? (int)(floorWidth / (double)(ticksX - 1) * (double)i) : 0;
            RenderPlane axesPanel = this.makeWallPlane(chartPlaneLeftX + offset, chartPlaneBottomY, xAdjust, zAdjust, wallHeight);
            g2.setColor(Color.lightGray);
            axesPanel.drawSides(g2, false, true, true, false);
            FontMetrics metrics = g2.getFontMetrics();
            if (i >= this.xLabels.size()) continue;
            int labelWidth = metrics.stringWidth(this.xLabels.get(i));
            g2.setColor(Color.black);
            g2.drawString(this.xLabels.get(i), axesPanel.bottomLeft.x - labelWidth / 2, axesPanel.bottomLeft.y + metrics.getHeight() + 3);
        }
        for (i = 1; i < this.numberYDivisions; ++i) {
            offset = (wallHeight * i + 1) / this.numberYDivisions;
            RenderPlane floorWallaxes = this.makeFloorPlane(chartPlaneLeftX, chartPlaneRightX, chartPlaneBottomY - offset, xAdjust, zAdjust);
            g2.setColor(Color.lightGray);
            floorWallaxes.drawSides(g2, true, false, false, true);
        }
        g2.setColor(Color.BLACK);
        leftWallpoints.draw(g2);
        backWallpoints.draw(g2);
        floorWallpoints.draw(g2);
        if (!this.normalize) {
            double[] minMaxArray = this.calcDataMinMAx(-1);
            dataXmin = minMaxArray[0];
            dataXmax = minMaxArray[1];
            dataYmin = minMaxArray[2];
            dataYmax = minMaxArray[3];
        }
        if (this.seriesList.series == null) {
            return;
        }
        this.seriesList.renderPlanesList.clear();
        for (int lineIndex = 0; lineIndex < nSeriesGroups; ++lineIndex) {
            int xOffset = (int)(xStep / 2.0);
            int zOffset = (int)(zStep / 2.0);
            RenderPlane renderPlane = this.makeVerticalPlane(chartPlaneLeftX + (int)(xStep * (double)lineIndex) - xOffset, chartPlaneRightX - (int)(xStep * (double)(nSeriesGroups - 1 - lineIndex)) - xOffset, (int)xStep, chartPlaneBottomY - (int)(zStep * (double)lineIndex) + zOffset, (int)zStep, wallHeight);
            this.seriesList.addRenderPlane(renderPlane);
        }
        if (this.seriesList.series.size() > 0) {
            for (int i3 = 0; i3 < this.numberYDivisions + 1; ++i3) {
                int y0;
                int x0 = chartPlaneLeftX;
                int x1 = chartPlaneLeftX - this.seriesList.pointWidth;
                int y1 = y0 = chartPlaneBottomY - i3 * wallHeight / this.numberYDivisions;
                if (xPoints > 0) {
                    g2.setColor(this.gridColor);
                    g2.setColor(Color.BLACK);
                    String yLabel = this.yLabels.size() == this.numberYDivisions + 1 ? this.yLabels.get(i3) : Double.toString((double)((int)((dataYmin + (dataYmax - dataYmin) * ((double)i3 * 1.0 / (double)this.numberYDivisions)) * 100.0)) / 100.0);
                    FontMetrics metrics = g2.getFontMetrics();
                    int labelWidth = metrics.stringWidth(yLabel);
                    if (!this.normalize || this.seriesList.series.size() <= 1) {
                        g2.drawString(yLabel, x0 - labelWidth - 5, y0 + metrics.getHeight() / 2 - 3);
                    }
                }
                g2.drawLine(x0, y0, x1, y1);
            }
        }
        boolean needPopOut = false;
        Series popOutSeries = null;
        for (int lineIndex = this.seriesList.series.size() - 1; lineIndex >= 0; --lineIndex) {
            int start;
            Stroke lineStroke;
            Color lineColor;
            Series thisSeries = this.seriesList.getGroupedSeries(lineIndex);
            if (thisSeries == null || thisSeries.size() == 0) continue;
            if (thisSeries.highlightPlane) {
                thisSeries.highlightPlane = false;
                thisSeries.highlightLine = true;
            }
            Stroke oldStroke = g2.getStroke();
            BasicStroke highlightLineStroke = null;
            Color higlightLineColor = null;
            if (thisSeries.highlightLine) {
                lineColor = thisSeries.getColor();
                lineStroke = GRAPH_STROKE;
                highlightLineStroke = new BasicStroke(4.0f);
                higlightLineColor = thisSeries.getCompColor();
            } else {
                lineColor = thisSeries.getColor();
                lineStroke = GRAPH_STROKE;
            }
            g2.setStroke(lineStroke);
            if (this.normalize) {
                dataXmin = this.getXMin(lineIndex);
                dataXmax = this.getXMax(lineIndex);
                dataYmin = this.getYMin(lineIndex);
                dataYmax = this.getYMax(lineIndex);
            }
            g2.setStroke(lineStroke);
            g2.setColor(lineColor);
            thisSeries.initScaleValues(dataXmin, dataXmax, dataYmin, dataYmax);
            boolean pointValid = false;
            PlanePoint prevPoint = null;
            for (start = 0; start < thisSeries.size() && !pointValid; ++start) {
                prevPoint = thisSeries.dataToPoint(start);
                pointValid = thisSeries.renderPlane.pointValid(prevPoint);
            }
            if (!pointValid) continue;
            if (this.DrawDots) {
                g2.setStroke(oldStroke);
                if (this.drawDotAtPoint(g2, prevPoint, thisSeries, !needPopOut)) {
                    this.mouseOverYValues[0] = ((double[])thisSeries.get(start - 1))[1];
                    needPopOut = true;
                    popOutSeries = thisSeries;
                }
            }
            for (int i4 = start; i4 < thisSeries.size(); ++i4) {
                PlanePoint thisPoint = thisSeries.dataToPoint(i4);
                if (!thisSeries.renderPlane.pointValid(thisPoint)) continue;
                if (thisSeries.highlightLine) {
                    g2.setColor(lineColor);
                    g2.setStroke(highlightLineStroke);
                    g2.drawLine(prevPoint.x, prevPoint.y, thisPoint.x, thisPoint.y);
                    g2.setStroke(lineStroke);
                    g2.setColor(higlightLineColor);
                    g2.drawLine(prevPoint.x, prevPoint.y, thisPoint.x, thisPoint.y);
                    g2.setColor(lineColor);
                } else {
                    g2.setStroke(lineStroke);
                    g2.setColor(lineColor);
                    g2.drawLine(prevPoint.x, prevPoint.y, thisPoint.x, thisPoint.y);
                }
                prevPoint = thisPoint;
                if (!this.DrawDots) continue;
                g2.setStroke(oldStroke);
                if (this.drawDotAtPoint(g2, prevPoint, thisSeries, !needPopOut)) {
                    this.mouseOverYValues[0] = ((double[])thisSeries.get(i4))[1];
                    needPopOut = true;
                    popOutSeries = thisSeries;
                }
                g2.setStroke(lineStroke);
                g2.setColor(lineColor);
            }
            thisSeries.highlightLine = false;
        }
        if (needPopOut) {
            this.highlightXYZ(g2, popOutSeries);
        }
        this.chartTopX = this.padding + labelXPadding;
        this.chartTopY = this.padding;
        this.chartBottomX = this.getWidth() - this.padding;
        this.chartBottomY = this.getHeight() - this.padding - labelYPadding;
    }

    private void outOfDateHighlightPlane(Graphics2D g2, Series series) {
        Stroke saveStroke = g2.getStroke();
        Color saveC = g2.getColor();
        Font saveFont = g2.getFont();
        Composite saveComp = g2.getComposite();
        g2.setFont(new Font("Helvetica", 1, 10));
        Color highlightColor = Color.LIGHT_GRAY;
        g2.setPaint(highlightColor);
        g2.setComposite(AlphaComposite.getInstance(3, 0.85f));
        GradientPaint gp = new GradientPaint(20.0f, 20.0f, Color.LIGHT_GRAY, series.renderPlane.getWidth(), series.renderPlane.getHeight(), Color.DARK_GRAY, true);
        g2.setPaint(gp);
        series.renderPlane.fill(g2);
        g2.setComposite(saveComp);
        g2.setFont(saveFont);
        g2.setPaint(saveC);
        g2.setStroke(saveStroke);
    }

    private boolean drawDotAtPoint(Graphics2D g2, PlanePoint point, Series thisSeries, boolean checkMouseOver) {
        Color saveColor;
        int x = point.x - this.seriesList.pointWidth / 2;
        int y = point.y - this.seriesList.pointWidth / 2;
        int ovalW = this.seriesList.pointWidth;
        int ovalH = this.seriesList.pointWidth;
        boolean dotHighlight = false;
        if (checkMouseOver && this.mouseInArea) {
            saveColor = g2.getColor();
            if (mouseOverX >= x - ovalW && mouseOverX <= x + ovalW && mouseOverY >= y - ovalH && mouseOverY <= y + ovalH) {
                g2.setColor(Color.RED);
                g2.fillOval(x - ovalW / 2, y - ovalH / 2, ovalW * 2, ovalH * 2);
                dotHighlight = true;
                thisSeries.highlightPlane = true;
                GraphPanelEvent evt = new GraphPanelEvent(thisSeries);
                this.fireGraphPanelEvent(evt);
            }
            if (dotHighlight) {
                g2.setColor(saveColor);
            }
        }
        if (!dotHighlight) {
            if (thisSeries.highlightLine) {
                g2.fillOval(x - ovalW / 2, y - ovalH / 2, ovalW * 2, ovalH * 2);
            } else {
                g2.fillOval(x, y, ovalW, ovalH);
                saveColor = g2.getColor();
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, ovalW, ovalH);
                g2.setColor(saveColor);
            }
        }
        return dotHighlight;
    }

    private void highlightXYZ(Graphics2D g2, Series thisSeries) {
        Stroke saveStroke = g2.getStroke();
        Color saveC = g2.getColor();
        Font saveFont = g2.getFont();
        Composite saveComp = g2.getComposite();
        g2.setFont(new Font("Helvetica", 1, 11));
        Color popoutColor = thisSeries.color;
        g2.setPaint(popoutColor);
        g2.setComposite(AlphaComposite.getInstance(3, 0.85f));
        g2.setPaint(popoutColor);
        int rectTopX = mouseOverX > this.chartTopX + (this.chartBottomX - this.chartTopX) / 2 ? mouseOverX - 10 - 100 : mouseOverX + 10;
        int rectTopY = mouseOverY;
        if (rectTopY > this.chartBottomY - 10 - 30) {
            rectTopY = this.chartBottomY - 10 - 30;
        }
        if (rectTopY < this.chartTopY + 10) {
            rectTopY = this.chartTopY + 10;
        }
        int rectWidth = 100;
        int labelXOffset = 5;
        String seriesName = thisSeries.name == null ? "" : thisSeries.name;
        FontMetrics metrics = g2.getFontMetrics();
        int labelWidth = metrics.stringWidth(seriesName);
        if (labelWidth > rectWidth - labelXOffset) {
            rectWidth = labelWidth + labelWidth / 8;
        }
        g2.fillRect(rectTopX, rectTopY, rectWidth, 30);
        g2.setPaint(thisSeries.compColor);
        g2.drawRect(rectTopX, rectTopY, rectWidth, 30);
        g2.drawString(seriesName, rectTopX + labelXOffset, rectTopY + 15);
        g2.drawString("   " + this.dfY.format(this.mouseOverYValues[0]), rectTopX + labelXOffset, rectTopY + 25);
        g2.setComposite(saveComp);
        g2.setFont(saveFont);
        g2.setPaint(saveC);
        g2.setStroke(saveStroke);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (g == null) {
            return;
        }
        this.beginUpdate();
        this.inPaint = true;
        try {
            super.paintComponent(g);
            if (this.displayMode == 1) {
                this.paintQuickY(g);
            } else if (this.displayMode == 2) {
                this.paintXYZ(g);
            } else if (this.displayMode == 0) {
                this.paintOldMethod(g);
            }
        }
        finally {
            this.inPaint = false;
            this.endUpdate();
        }
    }

    private void paintOldMethod(Graphics g) {
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
        double[] minMaxArray = this.calcDataMinMAx(-1);
        dataXmin = minMaxArray[0];
        dataXmax = minMaxArray[1];
        dataYmin = minMaxArray[2];
        dataYmax = minMaxArray[3];
        ArrayList<ArrayList<Point>> graphPoints = new ArrayList<ArrayList<Point>>();
        if (this.seriesList.series == null) {
            return;
        }
        for (int lineIndex = 0; lineIndex < this.seriesList.series.size(); ++lineIndex) {
            if (this.seriesList.series.get(lineIndex) == null) {
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
        if (this.seriesList.series.size() > 0) {
            int i;
            int ticksX = 0;
            if (this.xLabels != null) {
                ticksX = this.xLabels.size();
            }
            double floorWidth = chartPlaneRightX - chartPlaneLeftX;
            for (i = 0; i < ticksX; ++i) {
                int n = ticksX > 1 ? (int)(floorWidth / (double)(ticksX - 1) * (double)i) : 0;
                g2.setColor(Color.lightGray);
                FontMetrics metrics = g2.getFontMetrics();
                if (i >= this.xLabels.size()) continue;
                int labelWidth = metrics.stringWidth(this.xLabels.get(i));
                g2.setColor(Color.black);
                g2.drawString(this.xLabels.get(i), chartPlaneLeftX + n - labelWidth / 2, chartPlaneBottomY + metrics.getHeight() + 3);
            }
            for (i = 0; i < this.numberYDivisions + 1; ++i) {
                int y0;
                int n = chartPlaneLeftX;
                int x1 = this.seriesList.pointWidth + chartPlaneLeftX;
                int y1 = y0 = chartPlaneBottomY - i * chartAreaHeight / this.numberYDivisions;
                if (xPoints > 0) {
                    g2.setColor(this.gridColor);
                    g2.drawLine(chartPlaneLeftX + 1 + this.seriesList.pointWidth, y0, chartPlaneRightX, y1);
                    g2.setColor(Color.BLACK);
                    String yLabel = this.yLabels.size() == this.numberYDivisions + 1 ? this.yLabels.get(i) : Double.toString((double)((int)((dataYmin + (dataYmax - dataYmin) * ((double)i * 1.0 / (double)this.numberYDivisions)) * 100.0)) / 100.0);
                    FontMetrics metrics = g2.getFontMetrics();
                    int labelWidth = metrics.stringWidth(yLabel);
                    if (!this.normalize || this.seriesList.series.size() <= 1) {
                        g2.drawString(yLabel, n - labelWidth - 5, y0 + metrics.getHeight() / 2 - 3);
                    }
                }
                g2.drawLine(n, y0, x1, y1);
            }
        }
        g2.drawLine(chartPlaneLeftX, chartPlaneBottomY, chartPlaneLeftX, chartPlaneTopY);
        g2.drawLine(chartPlaneLeftX, chartPlaneBottomY, chartPlaneRightX, chartPlaneBottomY);
        boolean needPopOut = false;
        Series popOutSeries = null;
        for (ArrayList arrayList : graphPoints) {
            Stroke lineStroke;
            Color lineColor;
            int lineIdx = graphPoints.indexOf(arrayList);
            Series thisSeries = this.seriesList.series.get(lineIdx);
            if (thisSeries == null || thisSeries.size() == 0) continue;
            if (thisSeries.highlightPlane) {
                thisSeries.highlightPlane = false;
                thisSeries.highlightLine = true;
            }
            Stroke oldStroke = g2.getStroke();
            BasicStroke highlightLineStroke = null;
            Color higlightLineColor = null;
            PlanePoint prevPoint = new PlanePoint();
            if (thisSeries.highlightLine) {
                lineColor = thisSeries.getColor();
                lineStroke = GRAPH_STROKE;
                highlightLineStroke = new BasicStroke(4.0f);
                higlightLineColor = thisSeries.getCompColor();
            } else {
                lineColor = thisSeries.getColor();
                lineStroke = GRAPH_STROKE;
            }
            g2.setStroke(lineStroke);
            boolean pointValid = false;
            if (this.DrawDots) {
                g2.setStroke(oldStroke);
                prevPoint.x = ((Point)arrayList.get((int)0)).x;
                prevPoint.y = ((Point)arrayList.get((int)0)).y;
                if (this.drawDotAtPoint(g2, prevPoint, thisSeries, !needPopOut)) {
                    this.mouseOverYValues[0] = ((double[])thisSeries.get(0))[1];
                    needPopOut = true;
                    popOutSeries = thisSeries;
                }
                g2.setStroke(lineStroke);
                g2.setColor(lineColor);
            }
            for (int i = 1; i < arrayList.size(); ++i) {
                int x2 = ((Point)arrayList.get((int)i)).x;
                int y2 = ((Point)arrayList.get((int)i)).y;
                boolean bl = pointValid = prevPoint.x >= chartPlaneLeftX && prevPoint.y <= chartPlaneBottomY && x2 >= chartPlaneLeftX && y2 <= chartPlaneBottomY;
                if (!pointValid) continue;
                if (thisSeries.highlightLine) {
                    g2.setColor(lineColor);
                    g2.setStroke(highlightLineStroke);
                    g2.drawLine(prevPoint.x, prevPoint.y, x2, y2);
                    g2.setStroke(lineStroke);
                    g2.setColor(higlightLineColor);
                    g2.drawLine(prevPoint.x, prevPoint.y, x2, y2);
                    g2.setColor(lineColor);
                } else {
                    g2.setStroke(lineStroke);
                    g2.setColor(lineColor);
                    g2.drawLine(prevPoint.x, prevPoint.y, x2, y2);
                }
                prevPoint.x = x2;
                prevPoint.y = y2;
                if (!this.DrawDots) continue;
                g2.setStroke(oldStroke);
                if (this.drawDotAtPoint(g2, prevPoint, thisSeries, !needPopOut)) {
                    this.mouseOverYValues[0] = ((double[])thisSeries.get(i))[1];
                    needPopOut = true;
                    popOutSeries = thisSeries;
                }
                g2.setStroke(lineStroke);
                g2.setColor(lineColor);
            }
            thisSeries.highlightLine = false;
        }
        if (needPopOut) {
            this.highlightXYZ(g2, popOutSeries);
        }
        this.chartTopX = this.padding + labelXPadding;
        this.chartTopY = this.padding;
        this.chartBottomX = this.getWidth() - this.padding;
        this.chartBottomY = this.getHeight() - this.padding - labelYPadding;
    }

    private ArrayList<Point> scaleSeriesPointsToPlane(double Xmin, double Xmax, double Ymin, double Ymax, int chartPlaneLeftX, int chartPlaneTopY, int chartPlaneWidth, int chartPlaneHeight, int lineIndex) {
        double xScale = (double)chartPlaneWidth / (Xmax - Xmin);
        double yScale = (double)chartPlaneHeight / (Ymax - Ymin);
        ArrayList<Point> thisPointsSet = new ArrayList<Point>();
        for (int i = 0; i < this.seriesList.series.get(lineIndex).size(); ++i) {
            double dVal = ((double[])this.seriesList.series.get(lineIndex).get(i))[0];
            int x1 = (int)(dVal * xScale) + chartPlaneLeftX;
            dVal = ((double[])this.seriesList.series.get(lineIndex).get(i))[1];
            int y1 = (int)((Ymax - dVal) * yScale) + chartPlaneTopY;
            thisPointsSet.add(new Point(x1, y1));
        }
        return thisPointsSet;
    }

    private double[] calcDataMinMAx(int lineIdx) {
        double[] minMaxArray = new double[4];
        for (int lineIndex = 0; lineIndex < this.seriesList.series.size(); ++lineIndex) {
            if (this.normalize && lineIdx == lineIndex) {
                minMaxArray[0] = this.getXMin(lineIndex);
                minMaxArray[1] = this.getXMax(lineIndex);
                minMaxArray[2] = this.getYMin(lineIndex);
                minMaxArray[3] = this.getYMax(lineIndex);
                continue;
            }
            if (lineIndex == 0) {
                minMaxArray[0] = this.getXMin(lineIndex);
                minMaxArray[1] = this.getXMax(lineIndex);
                minMaxArray[2] = this.getYMin(lineIndex);
                minMaxArray[3] = this.getYMax(lineIndex);
            } else {
                for (int i = 0; i < this.seriesList.series.size(); ++i) {
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
            }
            if (!this.YFromZero) continue;
            minMaxArray[2] = 0.0;
        }
        if (!this.normalize) {
            this.roundScale(minMaxArray);
        }
        return minMaxArray;
    }

    public double logOfBase(int base, double num) {
        return Math.log(num) / Math.log(base);
    }

    private void roundScale(double[] minMaxArray) {
        double normFacor;
        int base = 5;
        double roundVal = 5.0;
        if (this.YFromZero) {
            minMaxArray[2] = 0.0;
        } else if (minMaxArray[2] != 0.0) {
            int zerosInMin = (int)Math.log10(minMaxArray[2]);
            if (zerosInMin < 0 && ++zerosInMin < -2) {
                zerosInMin = -2;
            }
            normFacor = Math.pow(10.0, zerosInMin - 1);
            minMaxArray[2] = minMaxArray[2] / normFacor;
            minMaxArray[2] = minMaxArray[2];
            minMaxArray[2] = (double)((int)(minMaxArray[2] / 5.0)) * 5.0;
            minMaxArray[2] = (double)Math.round(minMaxArray[2]) * normFacor;
        }
        int zerosInMax = (int)Math.log10(minMaxArray[3]);
        if (zerosInMax < 0 && ++zerosInMax < -2) {
            zerosInMax = -2;
        }
        normFacor = Math.pow(10.0, zerosInMax - 1);
        minMaxArray[3] = minMaxArray[3] / normFacor;
        minMaxArray[3] = minMaxArray[3] + 5.0;
        minMaxArray[3] = (double)((int)(minMaxArray[3] / 5.0)) * 5.0;
        minMaxArray[3] = (double)Math.round(minMaxArray[3]) * normFacor;
    }

    private void clearMinMaxRequiredFlag(int lineIndex) {
        this.seriesList.bounds.get((int)lineIndex)[4] = -1.0;
        this.seriesList.bounds.get((int)lineIndex)[5] = -1.0;
    }

    private boolean newMinMaxRequired(int lineIndex, boolean ignoreX) {
        if (ignoreX) {
            return this.seriesList.bounds.get(lineIndex)[5] >= 0.0;
        }
        return this.seriesList.bounds.get(lineIndex)[4] >= 0.0 || this.seriesList.bounds.get(lineIndex)[5] >= 0.0;
    }

    private double getXMin(int lineIndex) {
        if (this.seriesList.bounds.size() == 0 || this.seriesList.bounds.size() <= lineIndex) {
            return 0.0;
        }
        return this.seriesList.bounds.get(lineIndex)[0];
    }

    private double getXMax(int lineIndex) {
        if (this.seriesList.bounds.size() == 0 || this.seriesList.bounds.size() <= lineIndex) {
            return 0.0;
        }
        return this.seriesList.bounds.get(lineIndex)[1];
    }

    private double getYMin(int lineIndex) {
        if (this.seriesList.bounds.size() == 0 || this.seriesList.bounds.size() <= lineIndex) {
            return 0.0;
        }
        return this.seriesList.bounds.get(lineIndex)[2];
    }

    private double getYMax(int lineIndex) {
        if (this.seriesList.bounds.size() == 0 || this.seriesList.bounds.size() <= lineIndex) {
            return 0.0;
        }
        return this.seriesList.bounds.get(lineIndex)[3];
    }

    public void setColor(int line, Color color) {
        if (line >= this.seriesList.series.size()) {
            this.seriesList.series.add(new Series(line));
        }
        this.seriesList.series.get(line).setColor(color);
    }

    public void setSeriesName(int line, String name) {
        while (line >= this.seriesList.series.size()) {
            this.seriesList.series.add(new Series(line));
        }
        this.seriesList.series.get((int)line).name = name;
    }

    public void addPoint(int line, double[] thisPoint) {
        if (line >= this.seriesList.series.size()) {
            this.seriesList.series.add(new Series(line));
            this.seriesList.bounds.add(new double[]{thisPoint[0], thisPoint[0], thisPoint[1], thisPoint[1], -1.0, -1.0});
            this.seriesList.series.get(line).color = Color.BLACK;
        }
        this.seriesList.series.get(line).add(thisPoint);
        this.seriesList.bounds.get((int)line)[0] = Math.min(this.seriesList.bounds.get(line)[0], thisPoint[0]);
        this.seriesList.bounds.get((int)line)[1] = Math.max(this.seriesList.bounds.get(line)[1], thisPoint[0]);
        this.seriesList.bounds.get((int)line)[2] = Math.min(this.seriesList.bounds.get(line)[2], thisPoint[1]);
        this.seriesList.bounds.get((int)line)[3] = Math.max(this.seriesList.bounds.get(line)[3], thisPoint[1]);
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
        for (int line = 0; line < this.seriesList.series.size(); ++line) {
            this.seriesList.series.get(line).add(idx, thisPoint);
        }
    }

    public void deleteOldestPoint() {
        for (int line = 0; line < this.seriesList.series.size(); ++line) {
            double[] bound;
            ArrayList points = this.seriesList.series.get(line);
            double[] oldestPoint = (double[])points.get(0);
            if (oldestPoint[0] <= (bound = this.seriesList.bounds.get(line))[0]) {
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
        for (int line = 0; line < this.seriesList.series.size(); ++line) {
            double[] bound;
            ArrayList points = this.seriesList.series.get(line);
            double[] thisPoint = (double[])points.get(idx);
            if (thisPoint[0] <= (bound = this.seriesList.bounds.get(line))[0]) {
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
        for (ArrayList arrayList : this.seriesList.series) {
            if (arrayList.size() <= max) continue;
            max = arrayList.size();
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
        this.seriesList.series.clear();
    }

    public int PointCount() {
        if (this.seriesList.series.size() > 0) {
            return this.seriesList.series.get(0).size();
        }
        return 0;
    }

    public void clear() {
        this.seriesList.renderPlanesList.clear();
        this.seriesList.series.clear();
        this.seriesList.bounds.clear();
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
        int sv = value * 40;
        if (sv != this.scaleDelayMax) {
            this.scaleDelayCount = this.scaleDelayMax = sv;
        }
    }

    public void SetAxisXMultiplier(double value) {
        this.axisXMultiplier = value;
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

    public static class SeriesList {
        public int pointWidth;
        public int groupSize = 1;
        public ArrayList<Series> series;
        public ArrayList<double[]> bounds;
        private int groupCount = 1;
        private ArrayList<RenderPlane> renderPlanesList = new ArrayList();

        public SeriesList(int pointWidth) {
            this.pointWidth = pointWidth;
        }

        public int getGroupCount() {
            if (this.series.size() < this.groupCount) {
                return this.series.size();
            }
            return this.groupCount;
        }

        public void setGroupCount(int nGroups) {
            this.groupCount = nGroups;
        }

        public void addRenderPlane(RenderPlane renderPlane) {
            this.renderPlanesList.add(renderPlane);
        }

        public Series getGroupedSeries(int lineIndex) {
            Series retSeries = null;
            if (lineIndex < this.series.size()) {
                retSeries = this.series.get(lineIndex);
                int planeIdx = lineIndex % this.groupCount;
                retSeries.renderPlane = planeIdx < this.renderPlanesList.size() ? this.renderPlanesList.get(planeIdx) : null;
            }
            return retSeries;
        }
    }

    public static class Series
    extends ArrayList<double[]> {
        private static final long serialVersionUID = 1L;
        public final int id;
        public RenderPlane renderPlane;
        public boolean highlightPlane = false;
        public boolean highlightLine = false;
        public String name = null;
        private Color color = null;
        private Color compColor = null;
        private double xmin;
        private double xmax;
        private double ymin;
        private double ymax;
        private double xScale;
        private double yScale;

        public Series(int id) {
            this.id = id;
        }

        public void initScaleValues(double Xmin, double Xmax, double Ymin, double Ymax) {
            this.xmin = Xmin;
            this.xmax = Xmax;
            this.ymin = Ymin;
            this.ymax = Ymax;
            if (this.renderPlane != null) {
                this.xScale = (double)this.renderPlane.getWidth() / (Xmax - Xmin);
                this.yScale = (double)this.renderPlane.getHeight() / (Ymax - Ymin);
            } else {
                this.xScale = 0.0;
                this.yScale = 0.0;
            }
        }

        public PlanePoint dataToPoint(int pointIdx) {
            PlanePoint thisPoint = new PlanePoint();
            double dVal = ((double[])this.get(pointIdx))[0];
            int x1 = (int)(dVal * this.xScale) + this.renderPlane.bottomLeft.x;
            dVal = ((double[])this.get(pointIdx))[1];
            int y1 = (int)((this.ymax - dVal) * this.yScale) + this.renderPlane.topLeft.y;
            thisPoint.x = x1;
            thisPoint.y = y1;
            return thisPoint;
        }

        private Color calcComplimentaryColor(Color c) {
            float[] rgb = new float[3];
            c.getRGBColorComponents(rgb);
            for (int i = 0; i < 3; ++i) {
                rgb[i] = (float)(1.0 - (double)rgb[i]);
                if (!(rgb[i] < 0.0f)) continue;
                rgb[i] = 0.0f;
            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        }

        public Color getColor() {
            return this.color;
        }

        public void setColor(Color color) {
            this.color = color;
            this.compColor = this.calcComplimentaryColor(this.color);
        }

        public Color getCompColor() {
            return this.compColor;
        }

        public void setCompColor(Color compColor) {
            this.compColor = compColor;
        }
    }

    public static class RenderPlane {
        PlanePoint topLeft = new PlanePoint();
        PlanePoint bottomLeft = new PlanePoint();
        PlanePoint topRight = new PlanePoint();
        PlanePoint bottomRight = new PlanePoint();

        public boolean pointValid(PlanePoint p) {
            return p.x >= this.topLeft.x && p.x <= this.topRight.x && p.y >= this.topLeft.y && p.y <= this.bottomLeft.y;
        }

        public int getWidth() {
            return this.bottomRight.x - this.bottomLeft.x;
        }

        public int getHeight() {
            return this.bottomLeft.y - this.topLeft.y;
        }

        public void fill(Graphics2D g2) {
            g2.fillPolygon(new int[]{this.topLeft.x, this.bottomLeft.x, this.bottomRight.x, this.topRight.x, this.topLeft.x}, new int[]{this.topLeft.y, this.bottomLeft.y, this.bottomRight.y, this.topRight.y, this.topLeft.y}, 5);
        }

        public void draw(Graphics2D g2) {
            g2.drawPolygon(new int[]{this.topLeft.x, this.bottomLeft.x, this.bottomRight.x, this.topRight.x, this.topLeft.x}, new int[]{this.topLeft.y, this.bottomLeft.y, this.bottomRight.y, this.topRight.y, this.topLeft.y}, 5);
        }

        public void drawSides(Graphics2D g2, boolean left, boolean bottom, boolean right, boolean top) {
            if (left) {
                g2.drawLine(this.topLeft.x, this.topLeft.y, this.bottomLeft.x, this.bottomLeft.y);
            }
            if (bottom) {
                g2.drawLine(this.bottomLeft.x, this.bottomLeft.y, this.bottomRight.x, this.bottomRight.y);
            }
            if (right) {
                g2.drawLine(this.bottomRight.x, this.bottomRight.y, this.topRight.x, this.topRight.y);
            }
            if (top) {
                g2.drawLine(this.topLeft.x, this.topLeft.y, this.topRight.x, this.topRight.y);
            }
        }
    }

    public static class PlanePoint {
        int x;
        int y;
    }
}

