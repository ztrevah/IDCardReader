package com.example.id_card_reader.services.facemodel;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.id_card_reader.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class FaceModelWrapper {
    private final static String TAG = "FaceModelWrapper";
    private final Context context;
    private Interpreter tfliteInterpreter;
    private int inputImageWidth;
    private int inputImageHeight;
    private int embeddingSize;

    public FaceModelWrapper(Context context, String modelPath) throws IOException {
        this.context = context;
        try {
            tfliteInterpreter = new Interpreter(loadModelFile(modelPath));
            getInputOutputDetails();
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model: " + e.getMessage());
            tfliteInterpreter = null;
            throw e;
        }
    }

    private void getInputOutputDetails() {
        int inputTensorIndex = 0; // Assuming only one input tensor
        int[] inputShape = tfliteInterpreter.getInputTensor(inputTensorIndex).shape();
        inputImageHeight = inputShape[1];
        inputImageWidth = inputShape[2];

        int outputTensorIndex = 0; // Assuming only one output tensor for embeddings
        int[] outputShape = tfliteInterpreter.getOutputTensor(outputTensorIndex).shape();
        embeddingSize = outputShape[1];
    }

    private ByteBuffer loadModelFile(String modelPath) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(modelPath);
        int fileLength = inputStream.available(); // Get the size of the input stream
        ByteBuffer modelBuffer = ByteBuffer.allocateDirect(fileLength);
        modelBuffer.order(ByteOrder.nativeOrder());
        byte[] buffer = new byte[fileLength];
        int num_bytes = inputStream.read(buffer); // Read all bytes from the input stream
        modelBuffer.put(buffer); // Put the bytes into the ByteBuffer
        modelBuffer.rewind();
        inputStream.close();
        return modelBuffer;
    }

    public float[] getFaceEmbedding(Bitmap faceBitmap) {
        if (tfliteInterpreter == null) {
            Log.e(TAG, "TFLite interpreter not initialized.");
            return null;
        }
        ByteBuffer inputBuffer = ImageUtils.preprocessImage(faceBitmap, inputImageWidth, inputImageHeight);
        float[][] outputEmbedding = new float[1][embeddingSize];
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputEmbedding);
        Object[] inputArray = {inputBuffer};
        try {
            tfliteInterpreter.runForMultipleInputsOutputs(inputArray, outputMap);
            return outputEmbedding[0];
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error running TFLite model: " + e.getMessage());
            return null;
        }
    }

    public Interpreter getTfliteInterpreter() {
        return tfliteInterpreter;
    }

    public int getInputImageWidth() {
        return inputImageWidth;
    }

    public int getInputImageHeight() {
        return inputImageHeight;
    }

    public int getEmbeddingSize() {
        return embeddingSize;
    }

    public void close() {
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }
}
