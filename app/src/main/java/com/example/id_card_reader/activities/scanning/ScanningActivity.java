package com.example.id_card_reader.activities.scanning;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.id_card_reader.services.device.DeviceIdFactory;
import com.example.id_card_reader.services.facemodel.FaceModelWrapper;
import com.example.id_card_reader.R;
import com.example.id_card_reader.services.api.RetrofitApiClient;
import com.example.id_card_reader.dialog.AccessDeniedDialog;
import com.example.id_card_reader.dialog.AccessGrantedDialog;
import com.example.id_card_reader.models.dto.AddLogDto;
import com.example.id_card_reader.models.mrz.MRZInfo;
import com.example.id_card_reader.models.mrz.MRZParser;
import com.example.id_card_reader.services.api.ApiService;
import com.example.id_card_reader.utils.EmbeddingFeatureUtils;
import com.example.id_card_reader.utils.ImageUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.IsoDepCardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class ScanningActivity extends AppCompatActivity {
    public enum STEP {
        MRZ_STEP,
        NFC_STEP,
        FACE_STEP
    }
    private STEP current_step;
    private static final String TAG = "MainActivity";
    private PreviewView previewView;
    private StepItemView mrzStepView;
    private StepItemView nfcStepView;
    private StepItemView faceStepView;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.NFC};
    private static final int PERMISSION_REQUEST_CODE = 100;
    private AlertDialog permissionDenialDialog = null;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ExecutorService cameraExecutor;
    private Preview previewUseCase;
    private ImageAnalysis imageAnalysisUseCase;
    private final AtomicBoolean isCameraProcessing = new AtomicBoolean(false);

    private MRZInfo mrzInfo;
    private final AtomicBoolean isCheckingMember = new AtomicBoolean(false);

    private NfcAdapter nfcAdapter = null;
    private ExecutorService readNfcExecutor = null;
    private Handler readNfcResultHandler = new Handler(Looper.getMainLooper());
    private boolean isResumeFromNfcIntent = false;
    private Bitmap idImageBitmap = null;

    private FaceDetector faceDetector = null;
    private FaceModelWrapper faceModelWrapper = null;
    private float[] comparedFaceEmbedding = null;

    private final AtomicBoolean isSendingLog = new AtomicBoolean(false);

    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        previewView = findViewById(R.id.previewView);

        LinearLayout mrzLayout = findViewById(R.id.stepMrzLayout);
        mrzStepView = new StepItemView(mrzLayout);
        mrzStepView.setStepNumber("Step 1: ");
        mrzStepView.setStepDescription("Scanning MRZ of ID card");
        mrzStepView.setVisibility(View.GONE);

        LinearLayout nfcLayout = findViewById(R.id.stepNfcLayout);
        nfcStepView = new StepItemView(nfcLayout);
        nfcStepView.setStepNumber("Step 2: ");
        nfcStepView.setStepDescription("Scanning NFC of ID card");
        nfcStepView.setVisibility(View.GONE);

        LinearLayout faceLayout = findViewById(R.id.stepFaceLayout);
        faceStepView = new StepItemView(faceLayout);
        faceStepView.setStepNumber("Step 3: ");
        faceStepView.setStepDescription("Scanning Face");
        faceStepView.setVisibility(View.GONE);

        Button restartButton = findViewById(R.id.restartButton);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartScanning();
            }
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        timeoutRunnable = () -> {
            if(current_step != STEP.MRZ_STEP) {
                Toast.makeText(this, "Timed out.", Toast.LENGTH_SHORT).show();
                restartScanning();
            }
        };

        if(checkPermissions()) {
            initNfc();
            initCameraFunction();
            startMrzStep();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean cameraPermissionGranted = false;
            boolean nfcPermissionGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA)) {
                    cameraPermissionGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                } else if (permissions[i].equals(Manifest.permission.NFC)) {
                    nfcPermissionGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
            }

            if (cameraPermissionGranted && nfcPermissionGranted) {
                initNfc();
                initCameraFunction();
                startMrzStep();
            } else {
                handleDeniedPermissions();
            }
        }
    }

    private void handleDeniedPermissions() {
        if(permissionDenialDialog != null) {
            permissionDenialDialog.dismiss();
            permissionDenialDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Required");
        builder.setMessage("This app requires camera and NFC permissions to function correctly. Please grant these permissions in the app settings.");

        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        builder.setNegativeButton("Exit App", (dialog, which) -> {
            finish(); // Close the app
        });

        builder.setCancelable(false);
        permissionDenialDialog = builder.create();
        permissionDenialDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            if(permissionDenialDialog != null) {
                permissionDenialDialog.dismiss();
                permissionDenialDialog = null;
            }
            enableNfc();
            // If the activity is resumed not from NFC discovery, then return to first step
            if(!(current_step == STEP.NFC_STEP && isResumeFromNfcIntent)) {
                isResumeFromNfcIntent = false;
                initCameraFunction();
                restartScanning();
            }

        }
        else if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.NFC)) {
            handleDeniedPermissions();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        disableNfc();
    }

    private void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.e("NfcActivity", "NFC not supported on this device");
            finish();
        }
    }

    private void enableNfc() {
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(
                    this,
                    PendingIntent.getActivity(
                            this,
                            0,
                            new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            PendingIntent.FLAG_MUTABLE
                    ),
                    new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)},
                    new String[][] { new String[] { IsoDep.class.getName() } }
            );

            readNfcExecutor = Executors.newSingleThreadExecutor();
        }
    }

    private void disableNfc() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    private void initCameraFunction() {
        startCamera();
        configureFaceDetector();
        initFaceModel();
    }

    private void configureFaceDetector() {
        if(faceDetector != null) return;
        FaceDetectorOptions faceDetectorOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .build();
        faceDetector = FaceDetection.getClient(faceDetectorOptions);
    }

    private void initFaceModel() {
        if(faceModelWrapper != null) return;
        try {
            faceModelWrapper = new FaceModelWrapper(this, "mobile_facenet_model.tflite");
        } catch (IOException e) {
            Toast.makeText(this, "Error loading face model", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCamera() {
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }

    private void bindPreviewUseCase(@NonNull ProcessCameraProvider cameraProvider) {
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }
        previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
    }

    private void startMrzStep() {
        current_step = STEP.MRZ_STEP;
        runOnUiThread(() -> {
            resetEmphasis();
            mrzStepView.setVisibility(View.VISIBLE);
            mrzStepView.emphasize(true);
        });

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, 20000);
        
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindPreviewUseCase(cameraProvider);
                bindScanningMrzUseCase(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindScanningMrzUseCase(@NonNull ProcessCameraProvider cameraProvider) {
        if(imageAnalysisUseCase != null) {
            cameraProvider.unbind(imageAnalysisUseCase);
        }
        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                .setResolutionStrategy(
                        new ResolutionStrategy(
                                new Size(860,1280),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                )
                .build();

        imageAnalysisUseCase = new ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysisUseCase.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (isCameraProcessing.compareAndSet(false, true)) {
                    readMrzFromImageProxy(imageProxy);
                } else {
                    imageProxy.close();
                }
            }
        });
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisUseCase);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void readMrzFromImageProxy(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            Log.e(TAG, "ImageProxy media image is null.");
            imageProxy.close();
            isCameraProcessing.set(false);
            return;
        }

        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        TextRecognizer recognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String result = visionText.getText();
                    result = result.replaceAll("«", "<");
                    String mrzCode = MRZParser.extractVietnameseMrzCode(result);
                    if (mrzCode != null) {
                        MRZInfo info = MRZParser.parseVietnameseMRZ(mrzCode);
                        if(info != null) {
                            mrzInfo = info;
                            checkMember(mrzInfo.getIdNumber());
                            imageProxy.close();
                        }
                        else {
                            imageProxy.close();
                            isCameraProcessing.set(false);
                        }
                    }
                    else {
                        imageProxy.close();
                        isCameraProcessing.set(false);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("MRZ", "Text recognition failed", e);
                    imageProxy.close();
                    isCameraProcessing.set(false);
                });
    }

    private void checkMember(String idNumber) {
        ApiService apiService = RetrofitApiClient.getApiService();
        Call<ResponseBody> call = apiService.getMemberInfo(new DeviceIdFactory(this).getDeviceId(), idNumber);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()) {
//                    Toast.makeText(ScanningActivity.this, "Member found", Toast.LENGTH_SHORT).show();
                    startNfcStep();
                }
                isCameraProcessing.set(false);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isCameraProcessing.set(false);
            }
        });
    }

    private void startNfcStep() {
        current_step = STEP.NFC_STEP;
        runOnUiThread(() -> {
            mrzStepView.showTick(true);
            resetEmphasis();
            nfcStepView.setVisibility(View.VISIBLE);
            nfcStepView.emphasize(true);
        });

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, 20000);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
        comparedFaceEmbedding = null;
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null) {
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) && mrzInfo != null) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tag != null) {
                    isResumeFromNfcIntent = true;
                    Log.d(TAG, "NFC tag discovered: " + tag);
                    readNfcTag(tag);
                }
            }
        }
    }

    private void readNfcTag(Tag tag) {
        readNfcExecutor.execute(() -> {
            IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                Log.e(TAG, "IsoDep is null");
                readNfcResultHandler.post(() -> Toast.makeText(ScanningActivity.this, "Error reading NFC tag", Toast.LENGTH_SHORT).show());
                return;
            }

            CardFileInputStream is1 = null;
            CardFileInputStream is2 = null;

            try {
                isoDep.connect();
                CardService cardService = IsoDepCardService.getInstance(isoDep);
                PassportService passportService = new PassportService(
                        cardService,
                        PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                        PassportService.DEFAULT_MAX_BLOCKSIZE,
                        false,
                        false
                );
                passportService.sendSelectApplet(false);
                passportService.open();

                // APDU Logging
                passportService.addAPDUListener(apduEvent -> {
                    Log.d("APDU", "Command: " + bytesToHex(apduEvent.getCommandAPDU().getBytes()));
                    Log.d("APDU", "Response: " + bytesToHex(apduEvent.getResponseAPDU().getBytes()));
                });

                if(mrzInfo == null) {
                    Log.d(TAG, "MRZ null");
                }

                BACKeySpec bacKey = new BACKey(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getExpiryDate());
                passportService.doBAC(bacKey);

                Log.d(TAG, "Reading NFC tag");

                is1 = passportService.getInputStream(PassportService.EF_DG1);
                is2 = passportService.getInputStream(PassportService.EF_DG2);

                DG1File dg1File = new DG1File(is1);
                DG2File dg2File = new DG2File(is2);

                List<FaceInfo> faceInfos = dg2File.getFaceInfos();
                if (!faceInfos.isEmpty()) {
                    List<FaceImageInfo> faceImageInfos = faceInfos.get(0).getFaceImageInfos();

                    if (faceImageInfos != null && !faceImageInfos.isEmpty()) {
                        FaceImageInfo imageInfo = faceImageInfos.get(0);
                        DataInputStream imageInputStream = new DataInputStream(imageInfo.getImageInputStream());
                        int imageLength = imageInfo.getImageLength();
                        byte[] imageBytes = new byte[imageLength];
                        imageInputStream.readFully(imageBytes);

                        processFaceImageFromIdCard(imageBytes);
                    }
                    else {
                        Log.w(TAG, "faceImageInfos is null or empty");
                    }
                }
                else {
                    Log.w(TAG, "faceInfos is empty");
                }

                passportService.close();
                isoDep.close();
            } catch (Exception e) {
                Log.e(TAG, "Error reading NFC tag: " + e.getMessage());
                readNfcResultHandler.post(() -> Toast.makeText(ScanningActivity.this, "Error reading NFC tag", Toast.LENGTH_SHORT).show());
            } finally {
                isResumeFromNfcIntent = false;
                try {
                    if (is1 != null) is1.close();
                    if (is2 != null) is2.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams: " + e);
                }
            }
        });
    }

    // Attempt to retrieve face embedding of the image from id card
    private void processFaceImageFromIdCard(byte[] imageBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap != null) {
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            if (faces.size() > 1) {
                                Toast.makeText(this, "Multiple faces detected in the selected image. Please select an image with only one face.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            Face firstFace = faces.get(0);
                            Bitmap croppedFace = ImageUtils.cropAndScaleBitmap(
                                    bitmap,
                                    firstFace.getBoundingBox(),
                                    faceModelWrapper.getInputImageWidth(),
                                    faceModelWrapper.getInputImageHeight()
                            );
                            if (croppedFace != null) {
                                comparedFaceEmbedding = faceModelWrapper.getFaceEmbedding(croppedFace);
                                if (comparedFaceEmbedding != null) {
//                                    Toast.makeText(this, "Face for comparison loaded.", Toast.LENGTH_SHORT).show();
                                    idImageBitmap = bitmap;
                                    startFaceStep();
                                } else {
                                    Toast.makeText(this, "Error getting embedding from id image.", Toast.LENGTH_SHORT).show();
                                    comparedFaceEmbedding = null;
                                }
                            } else {
                                Toast.makeText(this, "Error cropping face from id image.", Toast.LENGTH_SHORT).show();
                                comparedFaceEmbedding = null;
                            }
                        } else {
                            Toast.makeText(this, "No face detected in the id image.", Toast.LENGTH_SHORT).show();
                            comparedFaceEmbedding = null;
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error detecting face in id image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        comparedFaceEmbedding = null;
                    });
        }
        else {
            Toast.makeText(this, "Error decoding id image.", Toast.LENGTH_SHORT).show();
            comparedFaceEmbedding = null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        if (bytes != null) {
            for (byte b : bytes) {
                hexString.append(String.format("%02X", b));
            }
        }
        return hexString.toString();
    }

    private void startFaceStep() {
        current_step = STEP.FACE_STEP;
        runOnUiThread(() -> {
            resetEmphasis();
            nfcStepView.showTick(true);
            faceStepView.setVisibility(View.VISIBLE);
            faceStepView.emphasize(true);
        });

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, 20000);

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                bindPreviewUseCase(cameraProvider);
                bindScanningFaceUseCase(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindScanningFaceUseCase(@NonNull ProcessCameraProvider cameraProvider) {
        if(faceModelWrapper == null) {
            return;
        }
        if(imageAnalysisUseCase != null) {
            cameraProvider.unbind(imageAnalysisUseCase);
        }
        
        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                .setResolutionStrategy(
                        new ResolutionStrategy(
                                new Size(860,1280),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                )
                .build();

        imageAnalysisUseCase = new ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysisUseCase.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (isCameraProcessing.compareAndSet(false, true)) {
                    detectAndRecognizeFace(imageProxy);
                } else {
                    imageProxy.close();
                }
            }
        });
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisUseCase);
    }
    
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void detectAndRecognizeFace(ImageProxy imageProxy) {
        InputImage inputImage;
        try {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                Log.e(TAG, "ImageProxy media image is null.");
                imageProxy.close();
                isCameraProcessing.set(false);
                return;
            }

            inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to create InputImage", e);
            imageProxy.close();
            isCameraProcessing.set(false);
            return;
        }


        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    try {
                        if (!faces.isEmpty()) {
                            if(faces.size() > 1) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ScanningActivity.this, "Too many faces!", Toast.LENGTH_SHORT).show();
                                });
                                imageProxy.close();
                                isCameraProcessing.set(false);
                                return;
                            }

                            Bitmap imageBitmap = ImageUtils.imageProxyToBitmap(imageProxy);
                            if(imageBitmap == null) {
                                imageProxy.close();
                                isCameraProcessing.set(false);
                                return;
                            }
                            Bitmap faceBitmap = ImageUtils.cropAndScaleBitmap(
                                    imageBitmap,
                                    faces.get(0).getBoundingBox(),
                                    faceModelWrapper.getInputImageWidth(),
                                    faceModelWrapper.getInputImageHeight()
                            );
                            if (faceBitmap != null) {
                                float[] currentEmbedding = faceModelWrapper.getFaceEmbedding(faceBitmap);
                                if (currentEmbedding != null) {
                                    if (comparedFaceEmbedding != null) {
                                        float distance = EmbeddingFeatureUtils.calculateDistance(currentEmbedding, comparedFaceEmbedding);
                                        Log.d(TAG, "Face distance: " + distance);
                                        if (distance <= 1.0f) {
                                            if(isSendingLog.compareAndSet(false, true)) {
//                                                    runOnUiThread(() -> {
//                                                        Toast.makeText(ScanningActivity.this, "Face Matched!", Toast.LENGTH_SHORT).show();
//                                                    });
                                                showAccessGrantedDialog();
                                                restartScanning();
                                                AddLogDto addLogDto = new AddLogDto(
                                                        new DeviceIdFactory(ScanningActivity.this).getDeviceId(),
                                                        mrzInfo.getIdNumber(),
                                                        imageBitmap
                                                );
                                                sendLog(addLogDto);
                                            }
                                        }
                                    }
                                }

                                if (!faceBitmap.isRecycled()) {
                                    faceBitmap.recycle();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during face recognition logic", e);
                    } finally {
                        imageProxy.close();
                        isCameraProcessing.set(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);
                    imageProxy.close();
                    isCameraProcessing.set(false);
                });
    }

    private void sendLog(AddLogDto addLogDto) {
        Log.d(TAG, "Sending log to server");
        try {
            byte[] imageBytes = ImageUtils.bitmapToByteArray(addLogDto.getImageBitmap(), Bitmap.CompressFormat.JPEG, 100);

            RequestBody imageRequestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg")); // Adjust MIME type if using PNG
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "device_log_image.jpg", imageRequestBody);

            UUID deviceId = addLogDto.getDeviceId();
            String civilianId = addLogDto.getCivilianId();
            Date createdAt = addLogDto.getCreatedAt();

            RequestBody deviceIdPart = RequestBody.create(deviceId.toString(), MediaType.parse("text/plain"));
            RequestBody civilianIdPart = RequestBody.create(civilianId, MediaType.parse("text/plain"));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            String createdAtString = sdf.format(createdAt);
            RequestBody createdAtPart = RequestBody.create(createdAtString, MediaType.parse("text/plain"));

            ApiService apiService = RetrofitApiClient.getApiService();

            Call<ResponseBody> call = apiService.addLog(
                    imagePart,
                    deviceIdPart,
                    civilianIdPart,
                    createdAtPart
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        comparedFaceEmbedding = null;
                        restartScanning();
                    } else {
                        // If the person is not the member of the room
                        if(response.code() == 403) {
                            comparedFaceEmbedding = null;
                        }
                        // If the device is not registered
                        else if(response.code() == 404) {
                            comparedFaceEmbedding = null;

                            AlertDialog.Builder builder = new AlertDialog.Builder(ScanningActivity.this);
                            builder.setMessage("This device is not registered.");


                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ScanningActivity.this.finish();
                                }
                            });

                            builder.setCancelable(false);

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        else {
                            comparedFaceEmbedding = null;

                            AlertDialog.Builder builder = new AlertDialog.Builder(ScanningActivity.this);
                            builder.setMessage("Error.");

                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ScanningActivity.this.finish();
                                }
                            });

                            builder.setCancelable(false);

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }

                    }
                    isSendingLog.set(false);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(ScanningActivity.this, "Device error", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Device Error", t);
                    comparedFaceEmbedding = null;
                    isSendingLog.set(false);
                }
            });

        } catch (Exception e) {
            Toast.makeText(ScanningActivity.this, "Device error", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Device Error", e);
            isSendingLog.set(false);
        }
    }

    private void showAccessGrantedDialog() {
        runOnUiThread(() -> {
            if(mrzInfo != null && idImageBitmap != null) {
                AccessGrantedDialog dialog = new AccessGrantedDialog(
                        this,
                        mrzInfo.getIdNumber(),
                        mrzInfo.getFullName(),
                        mrzInfo.getDateOfBirth(),
                        mrzInfo.getGender(),
                        idImageBitmap
                );

                dialog.show();
            }
        });
    }

    private void showAccessDeniedDialog() {
        runOnUiThread(() -> {
            AccessDeniedDialog dialog = new AccessDeniedDialog(this);
            dialog.show();
        });
    }

    private void resetEmphasis() {
        mrzStepView.emphasize(false);
        nfcStepView.emphasize(false);
        faceStepView.emphasize(false);
    }

    private void resetUI() {
        runOnUiThread(() -> {
            resetEmphasis();
            mrzStepView.setVisibility(View.GONE);
            nfcStepView.setVisibility(View.GONE);
            faceStepView.setVisibility(View.GONE);

            mrzStepView.showTick(false);
            nfcStepView.showTick(false);
            faceStepView.showTick(false);
        });
    }

    private void restartScanning() {
        resetUI();
        startMrzStep();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceModelWrapper != null) {
            faceModelWrapper.close();
        }

        cameraProvider.unbindAll();
        if(cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        if(readNfcExecutor != null) {
            readNfcExecutor.shutdown();
        }

        timeoutHandler.removeCallbacks(timeoutRunnable);
    }
}