package com.slattz.MLKitExample;

import static androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.media.Image;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.slattz.MLKitExample.databinding.ActivityMainBinding;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executors;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import java.util.*;
import java.util.concurrent.ExecutorService;

import android.os.Build;

import com.google.common.util.concurrent.ListenableFuture;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "CameraXApp";
    private final int REQUEST_CODE_PERMISSIONS = 10; //arbitrary number, can be changed accordingly
    private String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO"
    };

    private ActivityMainBinding viewBinding = null;
    private ExecutorService cameraExecutor;
    private FaceCanvas faceCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        faceCanvas = new FaceCanvas(this);
        viewBinding.getRoot().addView(faceCanvas);

        setContentView(viewBinding.getRoot());

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            REQUIRED_PERMISSIONS = new String[] {
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            };
        }


        if (allPermissionsGranted()) {
            startCamera(); //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        //make sure there isn't another camera instance running before starting
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                    // Choose the camera by requiring a lens facing
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();

                    cameraProvider.unbindAll();

                    ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                            .setTargetResolution(new Size(faceCanvas.getWidth(), faceCanvas.getHeight()))
                            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    imageAnalyzer.setAnalyzer(cameraExecutor, new MLKitAnalyser());

                    cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalyzer);

                } catch(Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //start camera when permissions have been granted otherwise exit app
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        //check if req permissions have been granted
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private class MLKitAnalyser implements ImageAnalysis.Analyzer {
        private final FaceDetector detector;

        MLKitAnalyser() {
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

            detector = FaceDetection.getClient(options);
        }

        @Override
        @ExperimentalGetImage
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                return;
            }

            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            detector.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                        @Override
                        public void onSuccess(List<Face> faces) {
                            if (faces.size() == 0) {
                                faceCanvas.resetFace();
                            }

                            else {
                                for (Face face : faces) {
                                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                                        faceCanvas.setFace(face, imageProxy.getWidth(), imageProxy.getHeight(), true);
                                    } else {
                                        faceCanvas.setFace(face, imageProxy.getHeight(), imageProxy.getWidth(), true);
                                    }
                                }
                            }

                            imageProxy.close();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            faceCanvas.resetFace();
                            imageProxy.close();
                        }
                    });
        }
    }
}