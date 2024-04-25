package com.example.vehiclespeeddetection;

import static java.lang.Math.pow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import com.google.mlkit.vision.GraphicOverlay;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.Arrays;

public class RoadLine {
    private Point point1, point2, point3, point4;
    private Point P1, P2;
    private double W1, W2, LineLength;
    private View circle1, circle2, circle3, circle4;

    public static float globalCoeff = 1810f;

//    public  void setParameters(Point point1, Point point2, Point point3, Point point4, float globalCoeff) {
//        this.point1 = point1;
//        this.point2 = point2;
//        this.point3 = point3;
//        this.point4 = point4;
//        this.globalCoeff = globalCoeff;
//
//    }

    public RoadLine(GraphicOverlay overlay) {
        this.overlay = overlay;
    }

    public void initializeCircles(View circle1, View circle2, View circle3, View circle4){
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(5);

        this.circle1 = circle1;
        this.circle2 = circle2;
        this.circle3 = circle3;
        this.circle4 = circle4;
        setCirclesTop(circle1, circle2, circle3, circle4);
//        setCirclesSide1(circle1, circle2, circle3, circle4);
//        setCirclesSide2(circle1, circle2, circle3, circle4);

        updateParameters();

        setVisible(View.VISIBLE);
    }

    GraphicOverlay overlay;

    public void updateParameters() {
        setPoints();
//        setVisible(View.INVISIBLE);
//        drawLines(overlay.getFrameBitmap());
    }
    private Matrix normToViewTransform;

    void setVisible(int visible){
        circle1.setVisibility(visible);
        circle2.setVisibility(visible);
        circle3.setVisibility(visible);
        circle4.setVisibility(visible);
    }

    private void setPoints(){
        // TODO: 16.04.24
        float w = overlay.getWidth();
        float h = overlay.getHeight();
        normToViewTransform = ImageUtils.getTransformationMatrix(
                1,
                1,
                (int) w,
                (int) h,
                0,
                false
        );

        int offsetX = (int) overlay.getX() - circle1.getWidth() / 2;
        int offsetY = (int) overlay.getY() - circle1.getHeight() / 2;
        float x1 = circle1.getX() - offsetX;
        float x2 = circle2.getX() - offsetX;
        float x3 = circle3.getX() - offsetX;
        float x4 = circle4.getX() - offsetX;

        float y1 = circle1.getY() - offsetY;
        float y2 = circle2.getY() - offsetY;
        float y3 = circle3.getY() - offsetY;
        float y4 = circle4.getY() - offsetY;

        float dx1 = x1 - x3;
        float dx2 = x2 - x4;
        float dy1 = y3 - y1;
        float dy2 = y4 - y2;

        float Y1 = dy1 > 0 ? h - y1 : y1;
        float Y2 = dy2 > 0 ? h - y2 : y2;
        float Y3 = dy1 > 0 ? y3 : h - y3;
        float Y4 = dy2 > 0 ? y4 : h - y4;
        float X1 = dx1 > 0 ? x1 : w - x1;
        float X2 = dx2 > 0 ? x2 : w - x2;
        float X3 = dx1 > 0 ? w - x3 : x3;
        float X4 = dx2 > 0 ? w - x4 : x4;

        point1 = new Point(
                Math.max(Math.min(x3 + dx1 * Y3 / Math.abs(dy1), w - 1), 0),
                Math.min(Math.max(y3 - dy1 * (X3) / Math.abs(dx1), 1), h)
        );
        point2 = new Point(
                Math.max(Math.min(x4 + dx2 * Y4 / Math.abs(dy2), w - 1), 0),
                Math.min(Math.max(y4 - (dy2 * (X4) / Math.abs(dx2)), 1), h)
        );
        point3 = new Point(
                Math.min(Math.max(x1 - dx1 * (Y1) / Math.abs(dy1), 1), w),
                Math.max(Math.min(y1 + dy1 * X1 / Math.abs(dx1), h - 1), 0)
        );
        point4 = new Point(
                Math.min(Math.max(x2 - dx2 * (Y2) / Math.abs(dy2), 1), w),
                Math.max(Math.min(y2 + dy2 * X2 / Math.abs(dx2), h - 1), 0)
        );
        P1 = new Point((point1.x + point2.x)/2, (point1.y + point2.y)/2);
        P2 = new Point((point3.x + point4.x)/2, (point3.y + point4.y)/2);
        W1 = pow(d(point1, point2), 1);
        W2 = pow(d(point3, point4), 1);
        LineLength = d(P1, P2);
    }

    public void movePoint(View circle, MotionEvent event) {
//        View view = overlay.getRootView();
        if (event.getRawX() < overlay.getX()
                || event.getRawY() < overlay.getY()
                || event.getRawX() > overlay.getX() + overlay.getWidth()
                || event.getRawY() > overlay.getY() + overlay.getHeight()
        )
            return;
        // TODO: 12.04.24
        circle.setX((int) event.getRawX());
        circle.setY((int) event.getRawY());

//        drawLines(overlay.getFrameBitmap());

    }

    private double calculateLocalCoefficient(Point point) {
        // TODO: 12.04.24
        System.out.println(P1 + ", " + P2 + ", " + point);
        System.out.println("W1 = " + W1 + ", W2 = " + W2);
        double d1 = pow(d(point, P1), 4.5);
        double d2 = pow(d(point, P2), 4.5);
        double tmp = ((W1 * d2 + W2 * d1)/(d1 + d2)/ Math.max(W1, W2));
        tmp /= overlay.getHeight()/Math.abs(P1.y - P2.y);
        return 1/(tmp);
    }

    public float calculateSpeed(Point pN1, Point pN2, int frames) {
        float[] pts = {(float) pN1.x, (float) pN1.y, (float) pN2.x, (float) pN2.y};

        normToViewTransform.mapPoints(pts);

        Point p1 = new Point(pts[0], pts[1]);
        Point p2 = new Point(pts[2], pts[3]);

        double coef = calculateLocalCoefficient(p1);

        return (float) (coef * globalCoeff * d(p1, p2) /frames);
    }
    public Point calculateSignSpeed(Point pN1, Point pN2, int frames) {
        float[] pts = {(float) pN1.x, (float) pN1.y};

        double dx = (pN2.x - pN1.x);
        double dy = (pN2.y - pN1.y);

        normToViewTransform.mapPoints(pts);

        double coef = calculateLocalCoefficient(new Point(pts[0], pts[1]));

        System.out.println("### coef = " + coef + ", dx = " + dx + ", dy = " + dy);

        return new Point((coef * globalCoeff * dx / frames), (coef * globalCoeff * (dy) /frames));
    }
    private double d(Point p1, Point p2){
        return Math.sqrt(pow(p1.x - p2.x, 2) + pow(p1.y - p2.y, 2));
    }

    private final Paint linePaint = new Paint();

    public void drawLines(Canvas canvas) {
//        System.out.println("in drawLines, W1=" + W1 + ", W2=" + W2 + ", LineLength=" + LineLength);
//        System.out.println("vid.w=" + overlay.getWidth() + ", vid.h=" + overlay.getHeight());
//        System.out.println(" " + point1 + ", " + point2 + ", " + point3 + ", " + point4);
        if (circle1 == null)
            return;
        updateParameters();

        double sx = (double) overlay.getImageWidth() /overlay.getWidth();
        double sy = (double) overlay.getImageHeight() /overlay.getHeight();

        canvas.drawLine((int) (point3.x * sx), (int) (point3.y * sy), (int) (point1.x * sx), (int) (point1.y * sy) ,linePaint);
        canvas.drawLine((int) (point2.x * sx), (int) (point2.y * sy), (int) (point4.x * sx), (int) (point4.y * sy) ,linePaint);
        canvas.drawCircle((float) (P1.x * sx), (float) (P1.y * sy), 15, linePaint);
        canvas.drawCircle((float) (P2.x * sx), (float) (P2.y * sy), 15, linePaint);

//        canvas.drawLine((int) point3.x, (int) point3.y, (int) point1.x, (int) point1.y ,linePaint);
//        canvas.drawLine((int) point2.x, (int) point2.y, (int) point4.x, (int) point4.y ,linePaint);
//        canvas.drawCircle((float) P1.x, (float) P1.y, 15, linePaint);
//        canvas.drawCircle((float) P2.x, (float) P2.y, 15, linePaint);

//        System.out.println("c1: " + circle1.getX() + ", " + circle1.getY() + ", " + circle1.getWidth() + ", " + circle1.getHeight());
//        System.out.println("c2: " + circle2.getX() + ", " + circle2.getY() + ", " + circle2.getWidth() + ", " + circle2.getHeight());
//        System.out.println("c3: " + circle3.getX() + ", " + circle3.getY() + ", " + circle3.getWidth() + ", " + circle3.getHeight());
//        System.out.println("c4: " + circle4.getX() + ", " + circle4.getY() + ", " + circle4.getWidth() + ", " + circle4.getHeight());
    }

    private static void setCirclesTop(View circle1, View circle2, View circle3, View circle4){
        circle1.setX(652);
        circle1.setY(745);

        circle2.setX(1171);
        circle2.setY(712);

        circle3.setX(598);
        circle3.setY(1544);

        circle4.setX(1486);
        circle4.setY(1580);

    }

    private static void setCirclesSide1(View circle1, View circle2, View circle3, View circle4){
        circle1.setX(1653);
        circle1.setY(1218);

        circle2.setX(1764);
        circle2.setY(1331);

        circle3.setX(105);
        circle3.setY(1416);

        circle4.setX(380);
        circle4.setY(1616);

    }

    private static void setCirclesSide2(View circle1, View circle2, View circle3, View circle4){
        circle1.setX(1417);
        circle1.setY(1074);

        circle2.setX(1820);
        circle2.setY(1100);

        circle3.setX(203);
        circle3.setY(1282);

        circle4.setX(747);
        circle4.setY(1552);

    }
}