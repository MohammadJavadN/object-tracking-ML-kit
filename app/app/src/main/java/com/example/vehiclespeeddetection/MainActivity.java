package com.example.vehiclespeeddetection;

import static com.google.mlkit.vision.BitmapUtils.matToBitmap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.moduleinstall.InstallStatusListener;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tflite.java.TfLite;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.GraphicOverlay;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;

import android.Manifest;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    ActivityResultLauncher<String> filechoser;

    public static final String TAG = "ObjectDetector";
    private static String inVideoPath = "/sdcard/Download/video(1).mp4";
    private static String outVideoPath = "/sdcard/Download/ou_.mp4";
    private static int maxFrames = 500;

    private static VideoCapture cap;
    public static VideoWriter out;
    private static final Scalar speedColor = new Scalar(255,0,0);
    private ScheduledExecutorService scheduledExecutorService;
    private ObjectTrackerProcessor trackerProcessor;
    ModuleInstallClient moduleInstallClient;
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

//        surfaceView = findViewById(R.id.surfaceView);
        graphicOverlay = findViewById(R.id.overlayView);

        filechoser = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                o -> {
                    String path = getRealPathFromURI(MainActivity.this, o); // TODO: uncomment
                    if (path == null)
                        path = MainActivity.inVideoPath;
                    if(new File(path).exists())
                        MainActivity.inVideoPath = path;
                    String[] paths = MainActivity.inVideoPath.split("/");
                    paths[paths.length-1] = "output.mp4";
                    MainActivity.outVideoPath = String.join("/", paths);
                    Toast.makeText(getApplicationContext(),
                            "out_path: " + outVideoPath,
                            Toast.LENGTH_LONG).show();
//                    MainActivity.inVideoPath = getRealPathFromURI(MainActivity.this, o); // TODO: uncomment
                    System.out.println(MainActivity.inVideoPath);

                    // Start updating frames periodically
//                    startUpdatingFrames();
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

//        // Live detection and tracking
//        ObjectDetectorOptions options =
//                new ObjectDetectorOptions.Builder()
//                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
////                        .enableClassification()  // Optional
//                        .build();
//        ObjectDetectorOptions options =
//                new ObjectDetectorOptions.Builder()
//                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//                        .enableMultipleObjects()
//                        .enableClassification()  // Optional
//                        .build();
//
        // Multiple object detection in static images
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();


        trackerProcessor = new ObjectTrackerProcessor(this, options);

/* ************************************************************************** */

//        ProgressBar progressBar = findViewById(R.id.progressBar);
//
//        // Get an instance of ModuleInstallClient
//        moduleInstallClient = ModuleInstall.getClient(this);
//
//        System.out.println("*** ModuleInstall.getClient(this); called");
//
//        // Check the availability of an optional module using its OptionalModuleApi
//        OptionalModuleApi optionalModuleApi = TfLite.getClient(this);


        // Live detection and tracking
//        ObjectDetectorOptions options =
//                new ObjectDetectorOptions.Builder()
//                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//                        .enableClassification()  // Optional
//                        .build();
//        ObjectDetector objectDetector = ObjectDetection.getClient(options);
//
//        InputImage image = InputImage.fromBitmap(Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888), 0);
//
//        objectDetector.process(image)
//                .addOnSuccessListener(
//                        new OnSuccessListener<List<DetectedObject>>() {
//                            @Override
//                            public void onSuccess(List<DetectedObject> detectedObjects) {
//                                // Task completed successfully
//                                // ...
//                                System.out.println("### on Success");
//                                for (DetectedObject detectedObject : detectedObjects) {
//                                    Rect boundingBox = detectedObject.getBoundingBox();
//                                    Integer trackingId = detectedObject.getTrackingId();
//                                    for (DetectedObject.Label label : detectedObject.getLabels()) {
//                                        System.out.println("### label: " + label);
//                                        String text = label.getText();
//                                        if (PredefinedCategory.FOOD.equals(text)) {
//                                            System.out.println("### PredefinedCategory.FOOD.equals(text)");
//                                        }
//                                        int index = label.getIndex();
//                                        if (PredefinedCategory.FOOD_INDEX == index) {
//                                            System.out.println("### PredefinedCategory.FOOD_INDEX == index");
//                                        }
//                                        float confidence = label.getConfidence();
//                                    }
//                                }
//                            }
//                        })
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                // Task failed with an exception
//                                // ...
//                                System.out.println("### on Failure");
//                            }
//                        });

        //
//        System.out.println("*** TfLite.getClient(this); called");
//        moduleInstallClient
//                .areModulesAvailable(optionalModuleApi)
//                .addOnSuccessListener(
//                        response -> {
//                            System.out.println("*** in response -> {}");
//                            if (response.areModulesAvailable()) {
//                                // Modules are present on the device...
//                                System.out.println("** in if (response.areModulesAvailable())");
//                            } else {
//                                // Modules are not present on the device...
//                                System.out.println("*** in else (response.areModulesAvailable())");
//                            }
//                        })
//                .addOnFailureListener(
//                        e -> {
//                            // Handle failure…
//                            System.out.println("*** .addOnFailureListener(e -> {");
//                        });
//
//        moduleInstallClient.deferredInstall(optionalModuleApi);
//
//        // Create an InstallStatusListener to handle the install status updates.
//        InstallStatusListener listener = new ModuleInstallProgressListener(progressBar, moduleInstallClient);
//
//
//        // Configure the ModuleInstallRequest and add the OptionalModuleApi to the request
//        ModuleInstallRequest moduleInstallRequest =
//                ModuleInstallRequest.newBuilder()
//                        .addApi(optionalModuleApi)
//                        // Add more API if you would like to request multiple optional modules
////                        .addApi(...)
//                        // Set the listener if you need to monitor the download progress
//                        //.setListener(listener)
//                        .build();
//
//        // Send the install request
//        moduleInstallClient.installModules(moduleInstallRequest)
//                .addOnSuccessListener(
//                        response -> {
//                            if (response.areModulesAlreadyInstalled()) {
//                                // Modules are already installed when the request is sent.
//                                System.out.println("*** areModulesAlreadyInstalled()");
//                            }
//                        })
//                .addOnFailureListener(
//                        e -> {
//                            // Handle failure...
//                            System.out.println("installModules -> OnFailureListener");
//                        });

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
    private static final int PERMISSION_REQUEST_CODE = 100;

    void getPermission(){
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
        }

        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        PERMISSION_REQUEST_CODE);
            }
        }

        else if (ContextCompat.checkSelfPermission(MainActivity.this,
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

    public static boolean isBusy = false;
    private void startProcess() {
        // Release resources
        if (cap != null && cap.isOpened())
            cap.release();
        if (out != null && out.isOpened())
            out.release();
        if (scheduledExecutorService != null)
            scheduledExecutorService.shutdown();

        cap = new VideoCapture();
        cap.open(inVideoPath);
        int fourcc = VideoWriter.fourcc('m', 'p', '4', 'v');
        double fps = cap.get(Videoio.CAP_PROP_FPS);
        RoadLine.globalCoeff *= (float) (fps/30);
        int width = (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        out = new VideoWriter(outVideoPath, fourcc, fps, new Size(width, height));

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        frameNum = 0;
        // Render the frame onto the canvas
//        Runnable updateFrameTask = this::updateFrameTaskFunc;
        Runnable updateFrameTask = () -> {
            if (!isBusy) {
                Mat frame = new Mat();
                boolean ret = cap.read(frame);
                if (ret) {
                    frameNum++;
                    Bitmap bitmap = matToBitmap(frame);

                    graphicOverlay.setImageSourceInfo(bitmap.getWidth(), bitmap.getHeight(), false);
                    trackerProcessor.DISTANCE_TH = (double) bitmap.getWidth() / 10;
                    MyDetectedObject.imgWidth = bitmap.getWidth();
                    MyDetectedObject.imgHeight = bitmap.getHeight();
                    System.out.println("^^^ calling trackerProcessor.detectInImage(image), frameNum=" + frameNum);
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


    public void updateFrameTaskFunc(){
        Mat frame = new Mat();
        boolean ret = cap.read(frame);
        if (ret) {
            frameNum++;
            Bitmap bitmap = matToBitmap(frame);
            graphicOverlay.setImageSourceInfo(frame.width(), frame.height(), false);

            System.out.println("^^^ calling trackerProcessor.detectInImage(image), frameNum=" + frameNum);
            try {
                trackerProcessor.processBitmap(bitmap, graphicOverlay);
            } catch (Exception e) {
                Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release resources
//        if (MainActivity.cap != null && MainActivity.cap.isOpened())
//            MainActivity.cap.release();
//        if (MainActivity.out != null && MainActivity.out.isOpened())
//            MainActivity.out.release();

        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }



}