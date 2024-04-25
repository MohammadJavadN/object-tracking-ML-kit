package com.example.vehiclespeeddetection;

import static com.google.mlkit.vision.BitmapUtils.matToBitmap;

import android.annotation.SuppressLint;
import android.content.Context;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.GraphicOverlay;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import android.Manifest;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.IOException;
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
    private static String inVideoPath = "/sdcard/Download/video(1).mp4";
    private static String outVideoPath = "/sdcard/Download/ou_.mp4";
    private static final String outCSVPath = "/sdcard/Download/out.csv";
    private static VideoCapture cap;
    ActivityResultLauncher<String> filechoser;
    private ScheduledExecutorService scheduledExecutorService;
    private ObjectTrackerProcessor trackerProcessor;
    private int frameNum;
    private GraphicOverlay graphicOverlay;
    private View circle1, circle2, circle3, circle4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        getPermission();
        init();

        graphicOverlay = findViewById(R.id.overlayView);

        filechoser = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                o -> {
                    String path = getRealPathFromURI(MainActivity.this, o);
                    if (path == null)
                        path = MainActivity.inVideoPath;
                    if (new File(path).exists())
                        MainActivity.inVideoPath = path;
                    String[] paths = MainActivity.inVideoPath.split("/");
                    paths[paths.length - 1] = "output.mp4";
                    MainActivity.outVideoPath = String.join("/", paths);
                    Toast.makeText(getApplicationContext(),
                            "out_path: " + outVideoPath,
                            Toast.LENGTH_LONG).show();
                    System.out.println(MainActivity.inVideoPath);

                    // Start updating frames periodically
                    findViewById(R.id.saveBtn).setVisibility(View.VISIBLE);
                    findViewById(R.id.browseBtn).setVisibility(View.GONE);
                    initializeSurface();
                    startProcess();
                }
        );

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

    public void browseVideo(android.view.View view) {
        filechoser.launch("video/*");
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        String filePath = null;
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Video.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                filePath = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return filePath;
    }

    private void startProcess() {
        releaseResources();

        initialInOutVideo();

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        Runnable updateFrameTask = () -> {
            if (!isBusy) {
                if (graphicOverlay.isValidBitmap)
                    out.encodeFrame(graphicOverlay.getBitmap());

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
            out = new MyVideoEncoder(width, height, (int) fps, outVideoPath);
            out.startEncoder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        frameNum = 0;

        RoadLine.globalCoeff *= (float) (fps / 30);
    }

    private void initialParameters(Bitmap bitmap) {
        graphicOverlay.setImageSourceInfo(bitmap.getWidth(), bitmap.getHeight(), false);
        trackerProcessor.DISTANCE_TH = (double) bitmap.getWidth() / 10;
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
//        System.out.println("in public void saveCsv(View view)");
        HashMap<Integer, HashMap<Integer, Float>> ObjectsSpeed = MyDetectedObject.getObjectsSpeed();
        Set<Integer> unnecessaryKey = new HashSet<>();
        for (Integer key : ObjectsSpeed.keySet()) {
            if (Objects.requireNonNull(ObjectsSpeed.get(key)).isEmpty())
                unnecessaryKey.add(key);
        }

        for (Integer key : unnecessaryKey) {
            ObjectsSpeed.remove(key);
        }
        CsvWriter.saveHashMapToCsv(ObjectsSpeed, outCSVPath);
    }
}