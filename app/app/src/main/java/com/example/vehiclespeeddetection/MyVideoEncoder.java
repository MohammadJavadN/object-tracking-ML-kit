package com.example.vehiclespeeddetection;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MyVideoEncoder {
//    private VideoWriter videoWriter;
//
//    public MyVideoEncoder( int frameWidth, int frameHeight, double fps, String outputFilePath) {
//        videoWriter = new VideoWriter();
//        int fourcc = VideoWriter.fourcc('m', 'p', '4', 'v');
////        int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
//        videoWriter.open(outputFilePath, fourcc, fps, new Size(frameWidth, frameHeight));
//    }
//
//    public void encodeFrame(Bitmap bitmap) {
////        System.out.println("^%& in encoderFrame");
//        Mat frame = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC3);
//        Utils.bitmapToMat(bitmap, frame);
////        System.out.println("^%& Utils.bitmapToMat(bitmap, frame); done");
//        videoWriter.write(frame);
////        System.out.println("^%& videoWriter.write(frame); done");
////        System.out.println("^%& frame size: " + frame.size());
////        System.out.println("^%& frame: " + Arrays.toString(frame.get(0, 0)));
//        frame.release();
//    }
//
//    public void stopEncoder() {
//        videoWriter.release();
//    }


    private MediaCodec encoder;
    private MediaMuxer muxer;
    private int trackIndex;
    private boolean muxerStarted;
    private File outputFile;

    public MyVideoEncoder(int width, int height, int frameRate, String outputPath) throws IOException {
        // Create MediaFormat for video
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        // Create encoder
        encoder = MediaCodec.createEncoderByType("video/avc");
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        if (outputPath.isEmpty())
            outputPath = "/output.mp4";
//        outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + outputPath);
        outputFile = new File(outputPath);
        muxerStarted = false;
    }

//    public void startEncoder() throws IOException {
//        encoder.start();
//        muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        muxerStarted = true;
//    }

    public void startEncoder() throws IOException {
        encoder.start();
        muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxerStarted = true;
        // Get the track index after starting the encoder
        trackIndex = muxer.addTrack(encoder.getOutputFormat());
        muxer.start();
    }

    public void stopEncoder() {
        encoder.stop();
        encoder.release();
        muxer.stop();
        muxer.release();
    }

    public boolean isMuxerStarted(){
        return muxerStarted;
    }

    public void encodeFrame(Bitmap bitmap) {
        System.out.println("%%% in encodeFrame");
        // Get input buffer index
        int inputBufferIndex = encoder.dequeueInputBuffer(0); // TODO: 23.04.24 0 -> -1 
        System.out.println("%%% " + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            System.out.println("%%% if (inputBufferIndex >= 0)");
            // Get input buffer
            ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
            // Clear input buffer
            assert inputBuffer != null;
            inputBuffer.clear();

            // Convert bitmap to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            // Put byte array into input buffer
            inputBuffer.put(byteArray);

            System.out.println("%%% inputBuffer.put(byteArray);");
            // Queue input buffer
            encoder.queueInputBuffer(inputBufferIndex, 0, byteArray.length, System.nanoTime() / 1000, 0);
            System.out.println("%%% encoder.queueInputBuffer(");
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        System.out.println("%%%  new MediaCodec.BufferInfo();");
        int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            System.out.println("%%% while (outputBufferIndex >= 0) ");
            ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferIndex);
            assert outputBuffer != null;
            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
            encoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            System.out.println("%%% outputBufferIndex");
        }
    }

}
