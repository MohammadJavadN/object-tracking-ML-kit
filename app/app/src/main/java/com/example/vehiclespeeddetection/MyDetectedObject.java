package com.example.vehiclespeeddetection;

import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.GraphicOverlay;
import com.google.mlkit.vision.objects.DetectedObject;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class MyDetectedObject extends DetectedObject{
    private static int nextId = 1;
    int id = -1;
    int frameNum;
    private Rect boundingBox;

    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

//    public void setBoundingBox(Rect boundingBox) {
//        this.boundingBox = new Rect(boundingBox);
//    }
    private final int SPEED_CNT = 15;
    private float[] speeds = new float[SPEED_CNT];
    private Point[] speedVectors = new Point[SPEED_CNT];
    private int speedCnt = 0;
    private float speed;
    protected RectF location;
    // size of the input image
    public static float imgHeight;
    public static float imgWidth;
    public void setLocation(RectF location) {
        this.location = location;
        this.boundingBox = new Rect(Math.round(location.left * imgWidth),
                Math.round(location.top * imgHeight),
                Math.round(location.right * imgWidth),
                Math.round(location.bottom * imgHeight));
    }
    private Point getCenter(RectF rect) {
        return new Point(rect.centerX(), rect.centerY());
    }

    public float getSpeed() {
        return ((int) (speed));
    }

    public void updateBoxAndSpeed(Rect box){
        System.out.println(box);
        RectF newLocation = new RectF(
                box.left/imgWidth,
                box.top/imgHeight,
                box.right/imgWidth,
                box.bottom/imgHeight
        );
        System.out.println(newLocation + ", " + getCenter(newLocation));
        System.out.println(location + ", " + getCenter(location));
//        float tmpSpeed = GraphicOverlay.getOverlayInstance().roadLine.calculateSpeed(
//                getCenter(location),
//                getCenter(newLocation),
//                1 //frameNum - this.frameNum
//        );
//        float tmpSpeed = calculateSpeed(
//                getCenter(location),
//                getCenter(newLocation),
//                1 //frameNum - this.frameNum
//        );

        Point tmpSpeed = GraphicOverlay.getOverlayInstance().roadLine.calculateSignSpeed(
                new Point(location.left, location.top), //getCenter(location),
                new Point(newLocation.left, newLocation.top), //getCenter(newLocation),
                1 //frameNum - this.frameNum
        );
        speedVectors[speedCnt % SPEED_CNT] = tmpSpeed;
        speedCnt++;
        float lastSpeed = speed;
        speed = 0;
        int cnt = Math.min(speedCnt, SPEED_CNT);
        double speedX = 0, speedY = 0;

        System.out.println("&&& id= " + id);
        for (int i = 0; i < cnt; i++) {
            Point v = speedVectors[i];
            System.out.println(v);
//        for (Point v: speedVectors) {
            speedX += v.x;
            speedY += v.y;
        }
        speedX /= cnt;
        speedY /= cnt;
        speed = (float) Math.sqrt(speedX * speedX + speedY * speedY) * 0.6f;

//        for (int i = 0; i < cnt; i++) {
//            if ((speeds[i] < 0.8 * lastSpeed || speeds[i] > 1.2 * lastSpeed) && speed > 0)
//                speed += lastSpeed/cnt;
//            else
//                this.speed += speeds[i]/cnt;
//        }

        setLocation(newLocation);
//        this.frameNum = frameNum;
    }

    float coef = 10000f;
    private float calculateSpeed(Point center, Point center1, int frameNum) {
        return distance(center, center1) * coef / frameNum;
    }
    private float distance(Point center, Point center1){
        return (float) Math.sqrt(
                Math.pow(center.x - center1.x, 2) +
                        Math.pow(center.y - center1.y, 2)
        );
    }
    public MyDetectedObject(@NonNull Rect boundingBox, int frameNum) {
        super(boundingBox, nextId, new ArrayList<>());
//        this.boundingBox = boundingBox;
        setLocationInt(boundingBox);
        id = nextId;
        this.frameNum = frameNum;
        nextId++;
    }

    public void setLocationInt(Rect locationInt) {
        this.boundingBox = locationInt;
        this.location = new RectF(locationInt.left / (float) imgWidth,
                locationInt.top / (float) imgHeight,
                locationInt.right / (float) imgWidth,
                locationInt.bottom / (float) imgHeight);
    }
    public Integer getTrackingId() {
        return this.id; // TODO: 22.04.24 int to Integer
    }

    public void setId(int id) {
        this.id = id;
    }
}
