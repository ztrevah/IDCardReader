package com.example.id_card_reader.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.util.Log;

import androidx.camera.core.ImageProxy;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static byte[] bitmapToByteArray(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(format, quality, stream);
        return stream.toByteArray();
    }

    // Convert ImageProxy to Bitmap, including rotation
    public static Bitmap imageProxyToBitmap(ImageProxy imageProxy) throws Exception {
        Bitmap bitmap = null;
        switch (imageProxy.getFormat()) {
            case ImageFormat.YUV_420_888:
                bitmap = yuv420888ToBitmap(imageProxy);
                break;
            case ImageFormat.JPEG:
                bitmap = jpegToBitmap(imageProxy);
                break;
            // Add handling for other formats if needed
            default:
                throw new Exception("Unsupported format");
        }
        if(bitmap != null) {
            bitmap = rotateBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
        }
        return bitmap;
    }

    private static Bitmap yuv420888ToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U and V planes are often swapped in NV21, check the order if the image is colored incorrectly
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private static Bitmap jpegToBitmap(ImageProxy imageProxy) {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // Convert Uri to Bitmap, including rotation
    public static Bitmap uriToBitmap(Context context, Uri imageUri) throws IOException {
        if (context == null || imageUri == null) {
            return null;
        }

        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = null;
        Bitmap bitmap = null;
        // First, decode the bitmap
        inputStream = contentResolver.openInputStream(imageUri);
        if (inputStream != null) {
            bitmap = BitmapFactory.decodeStream(inputStream);
            // Close the stream after decoding, as we might open another for Exif
            inputStream.close();
            inputStream = null; // Set to null to avoid closing again in finally
        }

        // If bitmap was decoded successfully, read Exif orientation
        if (bitmap != null) {
            inputStream = contentResolver.openInputStream(imageUri); // Open a new stream for ExifInterface
            if (inputStream != null) {
                ExifInterface exifInterface = new ExifInterface(inputStream);
                int orientation = exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);

                int rotationDegrees = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotationDegrees = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotationDegrees = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotationDegrees = 270;
                        break;
                    default:
                        break;
                }
                bitmap = rotateBitmap(bitmap, rotationDegrees);
            }
        }
        if (inputStream != null) {
            inputStream.close();
        }

        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        // Create a new bitmap with the rotation applied
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true // Filter to smooth the rotation
        );
        // Recycle the original bitmap to free up memory
        bitmap.recycle();
        bitmap = rotatedBitmap; // Use the rotated bitmap
        return bitmap;
    }

    public static Bitmap flipBitmapHorizontal(Bitmap originalBitmap) {
        if (originalBitmap == null) {
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);

        return Bitmap.createBitmap(originalBitmap, 0, 0,
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                matrix, false);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        if(bitmap == null) return null;

        try {
            int x = Math.max(0, rect.left);
            int y = Math.max(0, rect.top);
            int width = Math.min(bitmap.getWidth() - x, rect.width());
            int height = Math.min(bitmap.getHeight() - y, rect.height());
            if (width > 0 && height > 0) {
                return Bitmap.createBitmap(bitmap, x, y, width, height);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error cropping bitmap: " + e.getMessage());
        }
        return null;
    }

    public static Bitmap cropAndScaleBitmap(Bitmap originalBitmap, Rect boundingBox, int targetWidth, int targetHeight) {
        Bitmap croppedBitmap = cropBitmap(originalBitmap, boundingBox);
        if (croppedBitmap != null) {
            return Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true);
        }
        return null;
    }

    public static ByteBuffer preprocessImage(Bitmap bitmap, int inputWidth, int inputHeight) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        ByteBuffer imgData = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputWidth * inputHeight];
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        // Normalize pixel values to [-1, 1]
        float mean = 0.0f;
        float std = 255.0f;
        int pixel = 0;
        for (int i = 0; i < inputHeight; ++i) {
            for (int j = 0; j < inputWidth; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF) - mean) / std); // Red
                imgData.putFloat((((val >> 8) & 0xFF) - mean) / std);  // Green
                imgData.putFloat((((val) & 0xFF) - mean) / std);     // Blue
            }
        }
        return imgData;
    }


}
