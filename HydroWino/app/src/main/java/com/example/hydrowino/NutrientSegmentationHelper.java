package com.example.hydrowino;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NutrientSegmentationHelper {

    private static final int INPUT_SIZE = 640;
    private static final int NUM_CLASSES = 6;
    private static final int NUM_DETECTIONS = 8400;
    private static final int MASK_COEFFICIENTS = 32;

    // Start with 60% based on your Colab tests
    private static final float CONF_THRESHOLD = 0.50f;

    // Non-Maximum Suppression threshold
    private static final float IOU_THRESHOLD = 0.30f;

    private Interpreter interpreter;
    private List<String> labels;

    public NutrientSegmentationHelper(Context context) throws IOException {

        interpreter = new Interpreter(
                FileUtil.loadMappedFile(
                        context,
                        "hydrowino_nutrient_segmentation.tflite"
                )
        );

        labels = FileUtil.loadLabels(
                context,
                "nutrient_segmentation_labels.txt"
        );
    }

    public static class Detection {

        public float left;
        public float top;
        public float right;
        public float bottom;

        public float confidence;

        public int classId;
        public String className;

        public float[] maskCoefficients;

        public Detection(
                float left,
                float top,
                float right,
                float bottom,
                float confidence,
                int classId,
                String className,
                float[] maskCoefficients
        ) {

            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;

            this.confidence = confidence;

            this.classId = classId;
            this.className = className;

            this.maskCoefficients = maskCoefficients;
        }
    }


    public List<Detection> detect(Bitmap originalBitmap) {

        LetterboxResult letterboxResult =
                letterbox(originalBitmap, INPUT_SIZE);

        Bitmap inputBitmap = letterboxResult.bitmap;

        ByteBuffer inputBuffer =
                convertBitmapToNCHW(inputBitmap);

        // Output 0:
        // [1, 42, 8400]
        float[][][] detectionsOutput =
                new float[1][42][NUM_DETECTIONS];

        // Output 1:
        // [1, 32, 160, 160]
        float[][][][] maskOutput =
                new float[1][32][160][160];

        Map<Integer, Object> outputs = new HashMap<>();

        outputs.put(0, detectionsOutput);
        outputs.put(1, maskOutput);

        Object[] inputs = new Object[]{
                inputBuffer
        };

        interpreter.runForMultipleInputsOutputs(
                inputs,
                outputs
        );

        List<Detection> detections =
                parseDetections(
                        detectionsOutput[0],
                        letterboxResult,
                        originalBitmap.getWidth(),
                        originalBitmap.getHeight()
                );

        return nonMaximumSuppression(detections);
    }


    private List<Detection> parseDetections(
            float[][] output,
            LetterboxResult letterbox,
            int originalWidth,
            int originalHeight
    ) {

        List<Detection> detections =
                new ArrayList<>();

        for (int i = 0; i < NUM_DETECTIONS; i++) {

            // YOLO TFLite outputs normalized box coordinates.
            // Convert them back to the 640x640 model input coordinates.
            float centerX = output[0][i] * INPUT_SIZE;
            float centerY = output[1][i] * INPUT_SIZE;
            float width = output[2][i] * INPUT_SIZE;
            float height = output[3][i] * INPUT_SIZE;

            int bestClass = -1;
            float bestScore = 0f;

            // Classes start from channel 4
            for (int c = 0; c < NUM_CLASSES; c++) {

                float score = output[4 + c][i];

                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }

            if (bestScore < CONF_THRESHOLD) {
                continue;
            }

            float x1 = centerX - width / 2f;
            float y1 = centerY - height / 2f;

            float x2 = centerX + width / 2f;
            float y2 = centerY + height / 2f;

            // Remove letterbox padding
            x1 = (x1 - letterbox.padX) / letterbox.scale;
            y1 = (y1 - letterbox.padY) / letterbox.scale;

            x2 = (x2 - letterbox.padX) / letterbox.scale;
            y2 = (y2 - letterbox.padY) / letterbox.scale;

            // Clamp to original image size
            x1 = Math.max(0, Math.min(x1, originalWidth));
            y1 = Math.max(0, Math.min(y1, originalHeight));

            x2 = Math.max(0, Math.min(x2, originalWidth));
            y2 = Math.max(0, Math.min(y2, originalHeight));

            float[] maskCoefficients =
                    new float[MASK_COEFFICIENTS];

            // Mask coefficients start after:
            // 4 bounding box values + 6 classes = channel 10
            for (int m = 0; m < MASK_COEFFICIENTS; m++) {

                maskCoefficients[m] =
                        output[
                                4
                                        + NUM_CLASSES
                                        + m
                                ][i];
            }

            String className =
                    bestClass >= 0
                            && bestClass < labels.size()
                            ? labels.get(bestClass)
                            : "Unknown";

            detections.add(
                    new Detection(
                            x1,
                            y1,
                            x2,
                            y2,
                            bestScore,
                            bestClass,
                            className,
                            maskCoefficients
                    )
            );
        }

        return detections;
    }


    private List<Detection> nonMaximumSuppression(
            List<Detection> detections
    ) {

        // Highest confidence first
        Collections.sort(
                detections,
                (a, b) -> Float.compare(
                        b.confidence,
                        a.confidence
                )
        );

        List<Detection> selected = new ArrayList<>();
        boolean[] removed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {

            if (removed[i]) {
                continue;
            }

            Detection current = detections.get(i);

            // Keep strongest detection
            selected.add(current);

            for (int j = i + 1; j < detections.size(); j++) {

                if (removed[j]) {
                    continue;
                }

                Detection other = detections.get(j);

                float iou = calculateIoU(
                        current,
                        other
                );

                // Remove duplicate overlapping detections
                // even if their predicted classes are different
                if (iou > IOU_THRESHOLD) {
                    removed[j] = true;
                }
            }
        }

        return selected;
    }


    private float calculateIoU(
            Detection a,
            Detection b
    ) {

        float intersectionLeft =
                Math.max(a.left, b.left);

        float intersectionTop =
                Math.max(a.top, b.top);

        float intersectionRight =
                Math.min(a.right, b.right);

        float intersectionBottom =
                Math.min(a.bottom, b.bottom);

        float intersectionWidth =
                Math.max(
                        0,
                        intersectionRight
                                - intersectionLeft
                );

        float intersectionHeight =
                Math.max(
                        0,
                        intersectionBottom
                                - intersectionTop
                );

        float intersectionArea =
                intersectionWidth
                        * intersectionHeight;

        float areaA =
                (a.right - a.left)
                        * (a.bottom - a.top);

        float areaB =
                (b.right - b.left)
                        * (b.bottom - b.top);

        float union =
                areaA
                        + areaB
                        - intersectionArea;

        if (union <= 0) {
            return 0;
        }

        return intersectionArea / union;
    }


    // Converts Bitmap to:
    // [1, 3, 640, 640]
    // FLOAT32 NCHW
    private ByteBuffer convertBitmapToNCHW(
            Bitmap bitmap
    ) {

        ByteBuffer buffer =
                ByteBuffer.allocateDirect(
                        1
                                * 3
                                * INPUT_SIZE
                                * INPUT_SIZE
                                * 4
                );

        buffer.order(
                ByteOrder.nativeOrder()
        );

        int[] pixels =
                new int[
                        INPUT_SIZE
                                * INPUT_SIZE
                        ];

        bitmap.getPixels(
                pixels,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
        );

        // RED channel
        for (int pixel : pixels) {

            float red =
                    Color.red(pixel) / 255.0f;

            buffer.putFloat(red);
        }

        // GREEN channel
        for (int pixel : pixels) {

            float green =
                    Color.green(pixel) / 255.0f;

            buffer.putFloat(green);
        }

        // BLUE channel
        for (int pixel : pixels) {

            float blue =
                    Color.blue(pixel) / 255.0f;

            buffer.putFloat(blue);
        }

        buffer.rewind();

        return buffer;
    }


    private static class LetterboxResult {

        Bitmap bitmap;

        float scale;

        float padX;
        float padY;

        LetterboxResult(
                Bitmap bitmap,
                float scale,
                float padX,
                float padY
        ) {

            this.bitmap = bitmap;
            this.scale = scale;
            this.padX = padX;
            this.padY = padY;
        }
    }


    private LetterboxResult letterbox(
            Bitmap source,
            int targetSize
    ) {

        int originalWidth =
                source.getWidth();

        int originalHeight =
                source.getHeight();

        float scale =
                Math.min(
                        (float) targetSize
                                / originalWidth,

                        (float) targetSize
                                / originalHeight
                );

        int newWidth =
                Math.round(
                        originalWidth
                                * scale
                );

        int newHeight =
                Math.round(
                        originalHeight
                                * scale
                );

        Bitmap resized =
                Bitmap.createScaledBitmap(
                        source,
                        newWidth,
                        newHeight,
                        true
                );

        Bitmap output =
                Bitmap.createBitmap(
                        targetSize,
                        targetSize,
                        Bitmap.Config.ARGB_8888
                );

        Canvas canvas =
                new Canvas(output);

        // YOLO letterbox background
        canvas.drawColor(
                Color.rgb(
                        114,
                        114,
                        114
                )
        );

        float padX =
                (targetSize - newWidth)
                        / 2f;

        float padY =
                (targetSize - newHeight)
                        / 2f;

        canvas.drawBitmap(
                resized,
                padX,
                padY,
                null
        );

        return new LetterboxResult(
                output,
                scale,
                padX,
                padY
        );
    }


    public void close() {

        if (interpreter != null) {

            interpreter.close();
            interpreter = null;
        }
    }
}