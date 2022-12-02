/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.upgenicsint.phonecheck.barcode;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.activities.CameraTestActivity;
import com.upgenicsint.phonecheck.barcode.Camera.CameraManager;

import java.io.ByteArrayOutputStream;
import java.util.Map;

final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private CaptureActivity activity = null;
    private CameraTestActivity activity2 = null;
    private final MultiFormatReader multiFormatReader;
    private boolean running = true;

    /*public DecodeHandler(CameraTestActivity activity2, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity2 = activity2;
    }*/

    DecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

//  DecodeHandler(CameraTestActivity activity, Map<DecodeHintType,Object> hints) {
//    multiFormatReader = new MultiFormatReader();
//    multiFormatReader.setHints(hints);
//    this.activity2 = activity;
//  }

    @Override
    public void handleMessage(Message message) {
        if (message == null || !running) {
            return;
        }

        //decode((byte[]) message.obj, message.arg1, message.arg2);

        switch (message.what) {
            case R.id.decode:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case R.id.quit:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    @SuppressLint("SetTextI18n")
    private void decode(byte[] data, int width, int height) {

        long start = System.currentTimeMillis();
        Result rawResult = null;

        /*if (activity2 != null) {
            CameraManager cameraManager = new CameraManager(activity2);
            PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(data, width, height);
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                    Log.d(TAG, "Barcode value is: " + rawResult);
                } catch (ReaderException re) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }

            Handler handler = activity2.getHandler();
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                long end = System.currentTimeMillis();
                Log.d(TAG, "Found barcode in " + (end - start) + " ms");
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
                    Bundle bundle = new Bundle();
                    bundleThumbnail(source, bundle);
                    message.setData(bundle);
                    message.sendToTarget();
                }
            } else {
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_failed);
                    message.sendToTarget();
                }
            }
        }*/

        if (activity != null) {
            PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                    final Result finalRawResult = rawResult;
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activity.getBarcodeText().setText("Barcode Result:  " + finalRawResult.toString());
//                        }
//                    });
                    Log.d(TAG, "Barcode value is: " + rawResult);
                } catch (ReaderException re) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }

            Handler handler = activity.getHandler();
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                long end = System.currentTimeMillis();
                Log.d(TAG, "Found barcode in " + (end - start) + " ms");
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
                    Bundle bundle = new Bundle();
                    bundleThumbnail(source, bundle);
                    message.setData(bundle);
                    message.sendToTarget();
                }
            } else {
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_failed);
                    message.sendToTarget();
                }
            }
        }


    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }

}
