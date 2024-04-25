package com.example.vehiclespeeddetection;

import static java.lang.Math.log;
import static java.lang.Math.log10;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.GraphicOverlay;
import com.google.mlkit.vision.objects.DetectedObject;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MyDetectedObject extends DetectedObject{

    public static HashMap<Integer, HashMap<Integer, Float>> getObjectsSpeed() {
        return objectsSpeed;
    }

    private static HashMap<Integer, HashMap<Integer, Float>> objectsSpeed = new HashMap<>();
    private static int nextId = 1;
    int id = -1;
    int frameNum, frameNumUpdated;
    private Rect boundingBox;

    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

//    public void setBoundingBox(Rect boundingBox) {
//        this.boundingBox = new Rect(boundingBox);
//    }
    private final int SPEED_CNT = 15;
    private final float[] speeds = new float[SPEED_CNT];
    private final Point[] speedVectors = new Point[SPEED_CNT];
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

    public void updateBoxAndSpeed(Rect box, int frameNum){
//        System.out.println(box);
        RectF newLocation = new RectF(
                box.left/imgWidth,
                box.top/imgHeight,
                box.right/imgWidth,
                box.bottom/imgHeight
        );
//        System.out.println(newLocation + ", " + getCenter(newLocation));
//        System.out.println(location + ", " + getCenter(location));
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
                location,
                newLocation,
                frameNum - this.frameNumUpdated
        );
        speedVectors[speedCnt % SPEED_CNT] = tmpSpeed;
        speedCnt++;
        float speed1 = speed;
        float lastSpeed = speed;
        speed1 = 0;
        int cnt = min(speedCnt, SPEED_CNT);
        double speedX = 0, speedY = 0;

//        System.out.println("&&& id= " + id);
        for (int i = 0; i < cnt; i++) {
            Point v = speedVectors[i];
//            System.out.println(v);
//        for (Point v: speedVectors) {
            speedX += v.x;
            speedY += v.y;
        }
        speedX /= cnt;
        speedY /= cnt;
        speed1 = (float) sqrt(speedX * speedX + speedY * speedY) * 0.6f;

        double val1 = 90 - 40 * log10(900 / (speed1 - 10) - 12);
        double val2 = 50 * log10(speed1/2);
        speed = (float) min(max(0, val1), val2);
//        System.out.println("&&&& " + speed1 + ", " + speed + ", " + val1 + ", " + val2);
//        System.out.println(frameNum-this.frameNumUpdated);
        if (speed < 10 || ((int) speed) < 20) {
            speedCnt--;
            speed = lastSpeed;
            return;
        }

//        for (int i = 0; i < cnt; i++) {
//            if ((speeds[i] < 0.8 * lastSpeed || speeds[i] > 1.2 * lastSpeed) && speed > 0)
//                speed += lastSpeed/cnt;
//            else
//                this.speed += speeds[i]/cnt;
//        }

        setLocation(newLocation);

        Objects.requireNonNull(objectsSpeed.get(id)).put(frameNum, speed);
//        this.frameNum++;
        this.frameNumUpdated = frameNum;
    }

    float coef = 10000f;
    private float calculateSpeed(Point center, Point center1, int frameNum) {
        return distance(center, center1) * coef / frameNum;
    }
    private float distance(Point center, Point center1){
        return (float) sqrt(
                pow(center.x - center1.x, 2) +
                        pow(center.y - center1.y, 2)
        );
    }
    public MyDetectedObject(@NonNull Rect boundingBox, int frameNum) {
        super(boundingBox, nextId, new ArrayList<>());
//        this.boundingBox = boundingBox;
        setLocationInt(boundingBox);
        id = nextId;
        this.frameNum = frameNum;
        this.frameNumUpdated = frameNum;
        nextId++;
        objectsSpeed.put(id, new HashMap<>());
    }

    public void setLocationInt(Rect locationInt) {
        this.boundingBox = locationInt;
        this.location = new RectF(locationInt.left / imgWidth,
                locationInt.top / imgHeight,
                locationInt.right / imgWidth,
                locationInt.bottom / imgHeight);
    }
    public Integer getTrackingId() {
        return this.id; // TODO: 22.04.24 int to Integer
    }

    public void setId(int id) {
        this.id = id;
    }
}
