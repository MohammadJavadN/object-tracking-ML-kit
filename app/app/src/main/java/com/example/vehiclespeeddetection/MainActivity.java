package com.example.vehiclespeeddetection;

import static com.google.mlkit.vision.BitmapUtils.matToBitmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.GraphicOverlay;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final String TAG = "ObjectDetector";
    private static final int PERMISSION_REQUEST_CODE = 100;
    public static MyVideoEncoder out;
    public static boolean isBusy = false;
    private static String inVideoPath = "/sdcard/Download/video.mp4";
    private static String outVideoPath = "/sdcard/Download/ou_.mp4";
    private static String outCSVPath = "/sdcard/Download/out.csv";
    private static final String outCSVFileName = "out.csv";
    private static final String outVideoFileName = "out.mp4";
    private static VideoCapture cap;
    private ScheduledExecutorService scheduledExecutorService;
    private ObjectTrackerProcessor trackerProcessor;
    private int frameNum;
    private GraphicOverlay graphicOverlay;
    private View circle1, circle2, circle3, circle4;
    private boolean isOutAvailable = false;
    public static final int FRAME_STEP = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        getPermission();
        init();

        graphicOverlay = findViewById(R.id.overlayView);

        circle1 = findViewById(R.id.circle1);
        circle2 = findViewById(R.id.circle2);
        circle3 = findViewById(R.id.circle3);
        circle4 = findViewById(R.id.circle4);

        circle1.setOnTouchListener(this);
        circle2.setOnTouchListener(this);
        circle3.setOnTouchListener(this);
        circle4.setOnTouchListener(this);

        // Multiple object detection in static images
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();


        trackerProcessor = new ObjectTrackerProcessor(this, options);

    }

    private void initializeSurface() {
        graphicOverlay.roadLine.initializeCircles(circle1, circle2, circle3, circle4);
        findViewById(R.id.changBtn).setVisibility(View.VISIBLE);
    }

    static int state = 1;
    static final int STATE = 3;
    public void changeCircles(android.view.View view){
        switch (state){
            case 0:
                RoadLine.setCirclesTop(circle1, circle2, circle3, circle4);
                break;
            case 1:
                RoadLine.setCirclesSide1(circle1, circle2, circle3, circle4);
                break;
            case 2:
                RoadLine.setCirclesSide2(circle1, circle2, circle3, circle4);
                break;
        }
        state = (state + 1) % STATE;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE)
            graphicOverlay.roadLine.movePoint(v, event);
//        if (graphicOverlay.show(true))
//            findViewById(R.id.floatingActionButton).setVisibility(View.VISIBLE);
        return true;
    }

    void getPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        PERMISSION_REQUEST_CODE);
            }
        } else if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            System.out.println("*** has not internet access..");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.INTERNET},
                    PERMISSION_REQUEST_CODE);

        }
    }

    public void init() {
        System.loadLibrary("opencv_java4");
    }

    int REQUEST_VIDEO_CODE = 1;
    public void browseVideo(android.view.View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*"); // Filter to show only videos
        startActivityForResult(intent, REQUEST_VIDEO_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIDEO_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri selectedVideoUri = data.getData();
                // Now you have the selected video URI to use in your app
                String path = FilePathHelper.getPathFromUri(this, selectedVideoUri);
                if (path == null)
                    path = inVideoPath;
                if (new File(path).exists())
                    inVideoPath = path;

                String[] paths = inVideoPath.split("/");
                paths[paths.length - 1] = "output.mp4";
                outVideoPath = String.join("/", paths);

                paths[paths.length - 1] = "output.csv";
                outCSVPath = String.join("/", paths);


                Toast.makeText(getApplicationContext(),
                        "out_path: " + outVideoPath,
                        Toast.LENGTH_LONG).show();
                System.out.println(MainActivity.inVideoPath);
                System.out.println(outCSVPath);

                // Start updating frames periodically
                findViewById(R.id.saveBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.browseBtn).setVisibility(View.GONE);
                initializeSurface();
                try {
                    startProcess();
                } catch (Exception e){
                    System.out.println(e.toString());
                }
            }
        }
    }

    private void startProcess() {
        releaseResources();

        initialInOutVideo();

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        Runnable updateFrameTask = () -> {
            if (frameNum % FRAME_STEP != 0){
                cap.read(new Mat());
                frameNum++;
            }
            if (!isBusy) {
//                if (isOutAvailable && graphicOverlay.isValidBitmap)
//                    out.encodeFrame(graphicOverlay.getBitmap());

                Mat frame = new Mat();
                boolean ret = cap.read(frame);
                if (ret) {
                    Bitmap bitmap = matToBitmap(frame);

                    if (frameNum == 0)
                        initialParameters(bitmap);

                    frameNum++;
                    try {
                        isBusy = true;
                        trackerProcessor.processBitmap(bitmap, graphicOverlay);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                } else
                    onDestroy();
            }
        };

        // Schedule the task to run every 33 milliseconds (30 frames per second)
        scheduledExecutorService.scheduleAtFixedRate(
                updateFrameTask,
                0, // Initial delay
                20, // Period (milliseconds)
                TimeUnit.MILLISECONDS);


    }

    private void releaseResources() {
        // Release resources
        if (cap != null && cap.isOpened())
            cap.release();
        if (out != null && out.isMuxerStarted())
            out.stopEncoder();
        if (scheduledExecutorService != null)
            scheduledExecutorService.shutdown();
    }

    private void initialInOutVideo() {
        cap = new VideoCapture();
        cap.open(inVideoPath);

        double fps = cap.get(Videoio.CAP_PROP_FPS);
        int width = (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);

        try {
            File file = new File(this.getExternalFilesDir(null), outVideoFileName);
            out = new MyVideoEncoder(width, height, (int) fps, file.getPath());
            out.startEncoder();
            isOutAvailable = true;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
        frameNum = 0;

        RoadLine.globalCoeff *= (float) (fps / 30);
    }

    private void initialParameters(Bitmap bitmap) {
        graphicOverlay.setImageSourceInfo(bitmap.getWidth(), bitmap.getHeight(), false);
        trackerProcessor.DISTANCE_TH = (double) bitmap.getWidth() / 10 * FRAME_STEP;
        MyDetectedObject.imgWidth = bitmap.getWidth();
        MyDetectedObject.imgHeight = bitmap.getHeight();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release resources
        if (MainActivity.cap != null && MainActivity.cap.isOpened())
            MainActivity.cap.release();
        if (out != null && out.isMuxerStarted())
            out.stopEncoder();

        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }


    public void saveCsv(View view) {
        HashMap<Integer, HashMap<Integer, Float>> ObjectsSpeed = MyDetectedObject.getObjectsSpeed();
        Set<Integer> unnecessaryKey = new HashSet<>();
        for (Integer key : ObjectsSpeed.keySet()) {
            if (Objects.requireNonNull(ObjectsSpeed.get(key)).isEmpty())
                unnecessaryKey.add(key);
        }

        for (Integer key : unnecessaryKey) {
            ObjectsSpeed.remove(key);
        }
        try {
            File file = new File(this.getExternalFilesDir(null), outCSVFileName);
            CsvWriter.saveHashMapToCsv(ObjectsSpeed, file.getPath());
            Toast.makeText(getApplicationContext(),
                    "out_path: " + file.getPath(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG)
                    .show();
        }
    }
}