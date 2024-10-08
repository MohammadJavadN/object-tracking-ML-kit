package com.example.vehiclespeeddetection;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.GraphicOverlay;
import com.google.mlkit.vision.VisionProcessorBase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

//import org.opencv.tracking.Tracker;
//import org.opencv.tracking.TrackerKCF; // Import the desired tracking algorithm

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectTrackerProcessor extends VisionProcessorBase<List<DetectedObject>> {

    private static final String TAG = "ObjectTrackerProcessor";

    private final ObjectDetector detector;
    private final List<MyDetectedObject> prevObjects;
    double DISTANCE_TH = 50;
    private int frameNum;

    public ObjectTrackerProcessor(Context context, ObjectDetectorOptionsBase options) {
        super(context);
        detector = ObjectDetection.getClient(options);
        frameNum = 0;
        prevObjects = new ArrayList<>();
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<DetectedObject>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay) {
        frameNum += MainActivity.FRAME_STEP;
        setIDAndSpeed(results);
        removeOutObj();
        for (MyDetectedObject object : prevObjects) {
            graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }

    private void removeOutObj() {
        prevObjects.removeIf(prevObj -> prevObj.frameNum < frameNum || prevObj.location.width() > 0.6 || prevObj.location.height() > 0.6);
//        constantObjects.removeIf(prevObj -> prevObj.frameNum < frameNum || prevObj.location.width() > 0.6 || prevObj.location.height() > 0.6);
    }

    private void setIDAndSpeed(List<DetectedObject> currObjects) {
        for (DetectedObject currObj : currObjects) {

            Rect currRect = currObj.getBoundingBox();
            int id = -1;
            double minD = Double.MAX_VALUE;

            for (MyDetectedObject prevObj : prevObjects) {
                if (prevObj.frameNum < frameNum) {
                    double d = distance(currRect, prevObj.getBoundingBox());
                    if (d < minD) {
                        minD = d;
                        id = prevObj.id;
                    }
                }
            }
            if (minD > DISTANCE_TH)
                addNewObj(currObj);
            else
                updateObj(id, currObj);
        }
    }

    private double distance(Rect rect1, Rect rect2) {
        double d = Math.sqrt(
                Math.pow(rect1.centerX() - rect2.centerX(), 2) +
                        Math.pow(rect1.centerY() - rect2.centerY(), 2)
        ); // * Math.min(rect1.height()/rect2.height(), rect2.height()/rect1.height()); // todo

        return d;
    }

    private void updateObj(int id, DetectedObject object) {
        for (MyDetectedObject prevObj : prevObjects) {
            if (prevObj.id == id) {
                prevObj.updateBoxAndSpeed(object.getBoundingBox(), frameNum);
                prevObj.frameNum = frameNum;
                break;
            }
        }
    }

    private void addNewObj(DetectedObject object) {
        MyDetectedObject myDetectedObject =
                new MyDetectedObject(
                        object.getBoundingBox(),
                        frameNum
                );
        if (myDetectedObject.id != -1)
            prevObjects.add(myDetectedObject);
    }
}
