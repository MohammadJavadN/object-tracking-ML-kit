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
import java.util.List;

public class ObjectTrackerProcessor extends VisionProcessorBase<List<DetectedObject>> {

    private static final String TAG = "ObjectTrackerProcessor";

    private final ObjectDetector detector;
    private int frameNum;
    private final List<MyDetectedObject> prevObjects;

    public ObjectTrackerProcessor(Context context, ObjectDetectorOptionsBase options) {
        super(context);
        System.out.println("*** 34 of ObjectTrackerProcessor class");
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
        System.out.println("^^^ Task<List<DetectedObject>> detectInImage(InputImage image)");
//        Task<List<DetectedObject>>
        return detector.process(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay) {
        frameNum++;
        setIDAndSpeed(results);
        removeOutObj();
        for (MyDetectedObject object : prevObjects) {
//            if (object.frameNum == frameNum || true)
                graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
        }
        // TODO: 22.04.24 remove out objects
//        for (DetectedObject object : results) {
//            graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
//        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }

    private void removeOutObj(){
        prevObjects.removeIf(prevObj -> prevObj.frameNum < frameNum);

    }

    double DISTANCE_TH = 50;

    private void setIDAndSpeed(List<DetectedObject> currObjects){
        for (DetectedObject currObj: currObjects) {

            Rect currRect = currObj.getBoundingBox();
            int id = -1;
            double minD = Double.MAX_VALUE;

            for (MyDetectedObject prevObj: prevObjects) {
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

    private double distance(Rect rect1, Rect rect2){
        double d = Math.sqrt(
                Math.pow(rect1.centerX() - rect2.centerX(), 2) +
                        Math.pow(rect1.centerY() - rect2.centerY(), 2)
        );// * Math.max(rect1.height()/rect2.height(), rect2.height()/rect1.height());

        return d;
    }

    private void updateObj(int id, DetectedObject object){
        for (MyDetectedObject prevObj: prevObjects) {
            if (prevObj.id == id) {
                prevObj.updateBoxAndSpeed(object.getBoundingBox());
//                prevObj.setBoundingBox(object.getBoundingBox());
                prevObj.frameNum = frameNum;
                break;
            }
        }
    }
    private void addNewObj(DetectedObject object){
        prevObjects.add(
                new MyDetectedObject(
                        object.getBoundingBox(),
                        frameNum
                )
        );
    }

//    private Tracker tracker;
//    private boolean isObjectDetected = false;
//    private Rect detectedObject;
//
//    // Initialize the tracker with the first frame and the detected object(s)
//    public void initializeTracker(Mat frame, Rect objects) {
//
//
//    }
//
//    // Perform object tracking on subsequent frames
//    public void trackObject(Mat frame) {
//        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);
//        objectDetector.process(image)
//                .addOnSuccessListener(
//                        new OnSuccessListener<List<DetectedObject>>() {
//                            @Override
//                            public void onSuccess(List<DetectedObject> detectedObjects) {
//                                // Task completed successfully
//                                // ...
//                            }
//                        })
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                // Task failed with an exception
//                                // ...
//                            }
//                        });
////        if (isObjectDetected && tracker != null) {
////            // Update the tracker with the new frame
////            isObjectDetected = tracker.update(frame, detectedObject);
////            // Visualize the tracked object(s)
////                Imgproc.rectangle(frame, detectedObject.tl(), detectedObject.br(), new Scalar(0, 255, 0), 2); // Green bounding box
////        }
//    }
//
//    public ObjectTrackerProcessor() {
//        // Live detection and tracking
//        ObjectDetectorOptions options =
//                new ObjectDetectorOptions.Builder()
//                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//                        .enableClassification()  // Optional
//                        .build();
//
//        ObjectDetector objectDetector = ObjectDetection.getClient(options);
////        // Multiple object detection in static images
////        ObjectDetectorOptions options =
////                new ObjectDetectorOptions.Builder()
////                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
////                        .enableMultipleObjects()
////                        .enableClassification()  // Optional
////                        .build();
//
//    }
//
//    // Method to handle loss of tracking or occlusions
//    public void handleLossOfTracking() {
//        // Implement your logic here to handle loss of tracking or occlusions
//        // For example, you can try to re-detect the object or reset the tracking process
//    }
}
