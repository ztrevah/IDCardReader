package com.example.id_card_reader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.id_card_reader.mrz.MRZInfo;
import com.example.id_card_reader.mrz.MRZParser;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private PreviewView cameraPreview;
    private TextView resultTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageAnalysis imageAnalysis;
    private long lastProcessedTime = 0;
    private static final long PROCESSING_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        resultTextView = findViewById(R.id.resultTextView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                resultTextView.setText("Camera permission denied.");
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720)) // Adjust resolution as needed
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            processImageProxy(imageProxy);
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessedTime < PROCESSING_DELAY) {
            imageProxy.close();
            return; // Skip processing if within the delay
        }

        lastProcessedTime = currentTime; // Update the last processed time

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String result = visionText.getText();
                    String mrz = extractMRZ(result);
                    if (mrz != null) {
                        MRZInfo info = MRZParser.parseVietnameseMRZ(mrz);
                        if(info != null) {
                            resultTextView.setText(info.toString());
                        }
                    }
//                    else
//                    {
//                        resultTextView.setText("Place ID Card near bottom of camera");
//                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("MRZ", "Text recognition failed", e);
                    imageProxy.close();
                });
    }

    private String extractMRZ(String text) {
//        Pattern mrzPattern = Pattern.compile("[A-Z<0-9]{30,}"); // Adjust regex as needed
//        Matcher matcher = mrzPattern.matcher(text);
//        if (matcher.find()) {
//            return matcher.group();
//        }
//        return null;
        if (text != null && !text.isEmpty()) {
            text = text.replaceAll("\\r?\\n", ""); // Remove all newline characters
            text = text.replaceAll(" ", "");
            text = text.replaceAll("«", "<"); // Dealing with wrong reading < to «

            return text;
        }
        return null;
    }
}