package com.example.hydrowino;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private PreviewView viewFinder;
    private ImageView btnShutter;
    private ImageButton btnClose;
    private ImageView btnGallery;
    private ImageView btnFlip;
    private android.view.View loadingLayout;
    private ImageView ivCapturedBackground;
    private android.view.View instructionBadge;
    private android.view.View scanningContainer;
    private android.view.View bottomBar;

    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private ExecutorService cameraExecutor;

    private ActivityResultLauncher<String> galleryLauncher;

    private boolean isProcessing = false;

    // PRIMARY: YOLO Nutrient Segmentation
    private NutrientSegmentationHelper nutrientSegmentationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        btnShutter = findViewById(R.id.btnShutter);
        btnClose = findViewById(R.id.btnClose);
        btnGallery = findViewById(R.id.btnGallery);
        btnFlip = findViewById(R.id.btnFlip);
        loadingLayout = findViewById(R.id.loadingLayout);
        ivCapturedBackground = findViewById(R.id.ivCapturedBackground);
        instructionBadge = findViewById(R.id.instruction_badge);
        scanningContainer = findViewById(R.id.scanning_container);
        bottomBar = findViewById(R.id.bottom_bar);

        android.view.View btnCancelLoading = findViewById(R.id.btnCancelLoading);
        if (btnCancelLoading != null) {
            btnCancelLoading.setOnClickListener(v -> showLoading(false));
        }

        btnClose.setOnClickListener(v -> finish());
        btnShutter.setOnClickListener(v -> takePhoto());
        btnFlip.setOnClickListener(v -> flipCamera());

        // Initialize gallery launcher
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        showLoading(true);
                        handleGalleryImage(uri);
                    } else {
                        showLoading(false);
                    }
                });

        btnGallery.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });

        // Initialize YOLO Nutrient Segmentation Helper
        try {
            nutrientSegmentationHelper = new NutrientSegmentationHelper(this);
            Log.d(TAG, "Nutrient segmentation helper loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load nutrient segmentation helper", e);
        }

        startCamera();

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isProcessing) {
            showLoading(false);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void flipCamera() {
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        }
        startCamera();
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void showLoading(boolean show) {
        isProcessing = show;
        if (loadingLayout != null) {
            loadingLayout.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        if (instructionBadge != null) {
            instructionBadge.setVisibility(show ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        if (scanningContainer != null) {
            scanningContainer.setVisibility(show ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        if (bottomBar != null) {
            bottomBar.setVisibility(show ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        if (!show && ivCapturedBackground != null) {
            ivCapturedBackground.setVisibility(android.view.View.GONE);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        showLoading(true);

        File photoFile = new File(getCacheDir(), UUID.randomUUID().toString() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Change Executor to cameraExecutor for background processing
        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                processAndShowResult(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(CameraActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleGalleryImage(Uri uri) {
        cameraExecutor.execute(() -> {
            try {
                File photoFile = new File(getCacheDir(), UUID.randomUUID().toString() + ".jpg");
                InputStream inputStream = getContentResolver().openInputStream(uri);
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(photoFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                processAndShowResult(photoFile);
            } catch (IOException e) {
                Log.e(TAG, "Error handling gallery image", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(CameraActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void processAndShowResult(File photoFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap == null) {
                runOnUiThread(() -> showLoading(false));
                return;
            }

            // Update UI to freeze background immediately
            runOnUiThread(() -> {
                if (ivCapturedBackground != null) {
                    ivCapturedBackground.setImageBitmap(bitmap);
                    ivCapturedBackground.setVisibility(android.view.View.VISIBLE);
                }
            });

            // PRIMARY: YOLO Nutrient Segmentation Inference
            if (nutrientSegmentationHelper == null) {
                Log.e(TAG, "YOLO helper is null");
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Internal Error: Inference failed", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // DEBUG: YOLO Detection Box Inspection
            Log.d(TAG, "SEG IMAGE SIZE: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            List<NutrientSegmentationHelper.Detection> detections =
                    nutrientSegmentationHelper.detect(bitmap);

            Log.d(TAG, "SEG Detected Plants: " + detections.size());

            // DEBUG: YOLO Detection Box Inspection
            for (int i = 0; i < detections.size(); i++) {
                NutrientSegmentationHelper.Detection detection = detections.get(i);
                float width = detection.right - detection.left;
                float height = detection.bottom - detection.top;
                float centerX = (detection.left + detection.right) / 2f;
                float centerY = (detection.top + detection.bottom) / 2f;

                Log.d(TAG, "SEG Plant " + (i + 1) + ": " + detection.className + " - " +
                        String.format(Locale.US, "%.2f%%", detection.confidence * 100));

                Log.d(TAG, "SEG Box " + (i + 1) + ": left=" + detection.left + ", top=" + detection.top +
                        ", right=" + detection.right + ", bottom=" + detection.bottom +
                        ", width=" + width + ", height=" + height +
                        ", centerX=" + centerX + ", centerY=" + centerY);
            }

            if (detections.isEmpty()) {
                // TASK 3: No Lettuce Found
                runOnUiThread(() -> {
                    showLoading(false);
                    Intent intent = new Intent(CameraActivity.this, ResultActivity.class);
                    intent.putExtra("image_url", photoFile.getAbsolutePath());
                    intent.putExtra("prediction", "Not Lettuce");
                    intent.putExtra("confidence", 0.0f);
                    startActivity(intent);
                });
            } else {
                // TASK 4 & 5: Sort Detections and Assign Plant Numbers
                // Sort visually: TOP TO BOTTOM, then LEFT TO RIGHT
                detections.sort((a, b) -> {
                    float threshold = bitmap.getHeight() * 0.05f;
                    if (Math.abs(a.top - b.top) > threshold) {
                        return Float.compare(a.top, b.top);
                    } else {
                        return Float.compare(a.left, b.left);
                    }
                });

                ArrayList<PlantDetectionResult> resultsList = new ArrayList<>();
                HashMap<String, Integer> classCounts = new HashMap<>();
                boolean allHealthy = true;
                ArrayList<String> uniqueDeficiencies = new ArrayList<>();

                for (int i = 0; i < detections.size(); i++) {
                    NutrientSegmentationHelper.Detection d = detections.get(i);
                    int plantNum = i + 1;
                    
                    resultsList.add(new PlantDetectionResult(
                            plantNum,
                            d.className,
                            d.confidence * 100,
                            d.left,
                            d.top,
                            d.right,
                            d.bottom
                    ));

                    classCounts.put(d.className, classCounts.getOrDefault(d.className, 0) + 1);
                    
                    if (!"Healthy".equals(d.className)) {
                        allHealthy = false;
                        if (!uniqueDeficiencies.contains(d.className)) {
                            uniqueDeficiencies.add(d.className);
                        }
                    }

                    Log.d(TAG, "SEG Plant " + plantNum + ": " + d.className + " - " + String.format(Locale.US, "%.2f%%", d.confidence * 100));
                }

                String overallResult = allHealthy ? "Healthy plants detected." : "Nutrient deficiency detected.";
                Log.d(TAG, "SEG Overall Result: " + overallResult);

                // NEW: Plant Bounding Box Overlay
                Bitmap annotatedBitmap = drawPlantBoxes(bitmap, resultsList);
                String annotatedImagePath = null;
                if (annotatedBitmap != null) {
                    try {
                        File annotatedFile = new File(getCacheDir(), "annotated_" + UUID.randomUUID().toString() + ".jpg");
                        java.io.FileOutputStream out = new java.io.FileOutputStream(annotatedFile);
                        annotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                        annotatedImagePath = annotatedFile.getAbsolutePath();
                        Log.d(TAG, "ANNOTATION: Annotated image saved to " + annotatedImagePath);
                    } catch (IOException e) {
                        Log.e(TAG, "ANNOTATION: Failed to save annotated image", e);
                    }
                }

                // TASK 10: Upload to Firebase
                uploadToFirebase(photoFile, resultsList, classCounts, overallResult, uniqueDeficiencies, annotatedImagePath);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            runOnUiThread(() -> showLoading(false));
        }
    }

    private void uploadToFirebase(File photoFile,
                                  ArrayList<PlantDetectionResult> detections,
                                  HashMap<String, Integer> classCounts,
                                  String overallResult,
                                  ArrayList<String> uniqueDeficiencies,
                                  String annotatedImagePath) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(CameraActivity.this, "Authentication required.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        String fileName = "processedimages/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(Uri.fromFile(photoFile))
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveToFirestore(uri.toString(), detections, classCounts, overallResult, uniqueDeficiencies, annotatedImagePath);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(CameraActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void saveToFirestore(String imageUrl,
                                 ArrayList<PlantDetectionResult> detections,
                                 HashMap<String, Integer> classCounts,
                                 String overallResult,
                                 ArrayList<String> uniqueDeficiencies,
                                 String annotatedImagePath) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            runOnUiThread(() -> showLoading(false));
            return;
        }

        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String greenhouseId = userDoc.getString("greenhouseID");
            if (greenhouseId == null || greenhouseId.isEmpty()) {
                greenhouseId = userDoc.getString("greenhouseId");
            }
            if (greenhouseId == null || greenhouseId.isEmpty()) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "No greenhouse assigned.", Toast.LENGTH_LONG).show();
                });
                return;
            }

            String formattedDate = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String randomUUIDSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            String scanId = "SCN_" + greenhouseId + "_" + formattedDate + "_" + randomUUIDSuffix;

            Map<String, Object> scanData = new HashMap<>();
            scanData.put("scanId", scanId);
            scanData.put("greenhouseId", greenhouseId);
            scanData.put("imageUrl", imageUrl);
            scanData.put("timestamp", com.google.firebase.Timestamp.now());
            scanData.put("userId", userId);
            scanData.put("overallResult", overallResult);
            scanData.put("detectedPlantCount", detections.size());

            List<Map<String, Object>> detectionList = new ArrayList<>();
            for (PlantDetectionResult r : detections) {
                Map<String, Object> detMap = new HashMap<>();
                detMap.put("plantNumber", r.getPlantNumber());
                detMap.put("prediction", r.getClassName());
                detMap.put("confidence", r.getConfidence());
                detectionList.add(detMap);
            }
            scanData.put("detections", detectionList);
            scanData.put("summary", classCounts);

            db.collection("scans")
                .document(scanId)
                .set(scanData)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Intent intent = new Intent(CameraActivity.this, ResultActivity.class);
                        intent.putExtra("image_url", imageUrl);
                        intent.putExtra("annotated_image_path", annotatedImagePath);
                        intent.putParcelableArrayListExtra("detections", detections);
                        intent.putExtra("overall_result", overallResult);
                        intent.putExtra("summary", classCounts);
                        intent.putStringArrayListExtra("unique_deficiencies", uniqueDeficiencies);
                        startActivity(intent);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    runOnUiThread(() -> showLoading(false));
                });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch user profile", e);
            runOnUiThread(() -> showLoading(false));
        });
    }

    // NEW: Plant Bounding Box Overlay
    private Bitmap drawPlantBoxes(Bitmap originalBitmap, ArrayList<PlantDetectionResult> detections) {
        if (originalBitmap == null || detections == null || detections.isEmpty()) return null;

        Log.d(TAG, "ANNOTATION: Drawing " + detections.size() + " plant boxes");

        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        float imageWidth = originalBitmap.getWidth();
        float strokeWidth = Math.max(6f, imageWidth * 0.008f); // Thicker outline
        float textSize = Math.max(24f, imageWidth * 0.03f);

        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(strokeWidth);

        Paint textBgPaint = new Paint();
        textBgPaint.setColor(Color.GREEN);
        textBgPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setAntiAlias(true);

        for (PlantDetectionResult res : detections) {
            float left = res.getLeft();
            float top = res.getTop();
            float right = res.getRight();
            float bottom = res.getBottom();

            boolean isHealthy = "Healthy".equalsIgnoreCase(res.getClassName());
            int boxColor = isHealthy ? Color.parseColor("#00AA5B") : Color.parseColor("#FF5722");
            boxPaint.setColor(boxColor);
            textBgPaint.setColor(boxColor);

            Log.d(TAG, "ANNOTATION P" + res.getPlantNumber() + " (" + res.getClassName() + "): left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom);

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Draw label background and text
            String label = "P" + res.getPlantNumber();
            float textWidth = textPaint.measureText(label);
            float textHeight = textPaint.getTextSize();

            canvas.drawRect(left, top - textHeight - (strokeWidth * 2) < 0 ? top : top - textHeight - (strokeWidth * 2), left + textWidth + 10, (top - textHeight - (strokeWidth * 2) < 0 ? top : top - textHeight - (strokeWidth * 2)) + textHeight + 5, textBgPaint);
            canvas.drawText(label, left + 5, (top - textHeight - (strokeWidth * 2) < 0 ? top : top - textHeight - (strokeWidth * 2)) + textHeight, textPaint);
        }

        return mutableBitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (nutrientSegmentationHelper != null) {
            nutrientSegmentationHelper.close();
        }
    }
}
