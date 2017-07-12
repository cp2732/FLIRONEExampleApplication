package com.flir.flironeexampleapplication.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flir.flironeexampleapplication.R;
import com.flir.flironeexampleapplication.util.SystemUiHider;
import com.flir.flironesdk.Device;
import com.flir.flironesdk.FlirUsbDevice;
import com.flir.flironesdk.Frame;
import com.flir.flironesdk.FrameProcessor;
import com.flir.flironesdk.RenderedImage;
import com.flir.flironesdk.SimulatedDevice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Created by Chris Puda on 05/30/2017.
 * A prototype Android street view app using infrared overlays. This expands from
 * an example activity provided by FLIR One.
 * Extended by Chris Puda on 05/30/2017.
 *
 * @see SystemUiHider
 * @see com.flir.flironesdk.Device.Delegate
 * @see com.flir.flironesdk.FrameProcessor.Delegate
 * @see com.flir.flironesdk.Device.StreamDelegate
 * @see com.flir.flironesdk.Device.PowerUpdateDelegate
 */
public class PreviewActivity extends Activity implements Device.Delegate, FrameProcessor.Delegate, Device.StreamDelegate, Device.PowerUpdateDelegate {

    private static final String TAG = PreviewActivity.class.getSimpleName();
    private static boolean requestingPermission = false;
    private static final int MY_PERMISSIONS_REQUEST_READ_IMAGES = 1, MY_PERMISSIONS_REQUEST_WRITE_IMAGES = 2,
        MY_PERMISSIONS_REQUEST_INTERNET_ACCESS = 3;

    ImageView thermalImageView;
    private volatile boolean imageCaptureRequested = false;
    private volatile Socket streamSocket = null;
    private boolean chargeCableIsConnected = true;

    private int deviceRotation = 0;
    private OrientationEventListener orientationEventListener;

    private volatile Device flirOneDevice;
    private FrameProcessor frameProcessor;
    private Frame lastFrame;

    private String lastSavedPath;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static boolean AUTO_HIDE = false;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 4000;

    private Device.TuningState currentTuningState = Device.TuningState.Unknown;

    // Device Delegate methods

    // Called during device discovery, when a device is connected
    // During this callback, you should save a reference to device
    // You should also set the power update delegate for the device if you have one
    // Go ahead and start frame stream as soon as connected, in this use case
    // Finally we create a frame processor for rendering frames

    /**
     * Indicate to the user that the device has been connected by showing camera footage.
     * @param device The FLIR One camera
     */
    public void onDeviceConnected(Device device) {
        //Log.i(TAG, "Called onDeviceConnected; Device connected!");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.pleaseConnect).setVisibility(View.GONE);
            }
        });
        
        flirOneDevice = device;
        AUTO_HIDE = true;
        quickHide();
        flirOneDevice.setPowerUpdateDelegate(this);
        flirOneDevice.startFrameStream(this);

        final ToggleButton chargeCableButton = (ToggleButton) findViewById(R.id.chargeCableToggle);
        if (flirOneDevice instanceof SimulatedDevice) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chargeCableButton.setChecked(chargeCableIsConnected);
                    chargeCableButton.setVisibility(View.VISIBLE);
                    isPermissionNeeded(MY_PERMISSIONS_REQUEST_WRITE_IMAGES);
                }
            });
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chargeCableButton.setChecked(chargeCableIsConnected);
                    chargeCableButton.setVisibility(View.INVISIBLE);
                    findViewById(R.id.connect_sim_button).setEnabled(false);
                }
            });
        }

        orientationEventListener.enable();
    }

    /**
     * Indicate to the user that the device has been disconnected.
     * @param device The FLIR One camera
     */
    public void onDeviceDisconnected(Device device) {
        Log.w(TAG, "Called onDeviceDisconnected; Device disconnected!");
        SystemClock.sleep(35); //prevents last image from being displayed after device is disconnected
        final ToggleButton chargeCableButton = (ToggleButton) findViewById(R.id.chargeCableToggle);
        final TextView levelTextView = (TextView) findViewById(R.id.batteryLevelTextView);
        final ImageView chargingIndicator = (ImageView) findViewById(R.id.batteryChargeIndicator);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.pleaseConnect).setVisibility(View.VISIBLE);
                thermalImageView.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8));
                levelTextView.setText("--");
                chargeCableButton.setChecked(chargeCableIsConnected);
                chargeCableButton.setVisibility(View.INVISIBLE);
                chargingIndicator.setVisibility(View.GONE);
                thermalImageView.clearColorFilter();
                findViewById(R.id.tuningProgressBar).setVisibility(View.GONE);
                findViewById(R.id.tuningTextView).setVisibility(View.GONE);
                findViewById(R.id.connect_sim_button).setEnabled(true);
                ((TextView) findViewById(R.id.spotMeterValue)).setText("");
            }
        });
        flirOneDevice = null;
        AUTO_HIDE = false;
        mHideHandler.removeCallbacks(mHideRunnable);
        orientationEventListener.disable();
    }

    /**
     * If using RenderedImage.ImageType.ThermalRadiometricKelvinImage, you should not rely on
     * the accuracy if tuningState is not Device.TuningState.Tuned
     * @param tuningState the current tuning state of the FLIR ONE device
     */
    public void onTuningStateChanged(Device.TuningState tuningState) {
        //Log.i(TAG, "Called onTuningStateChanged; Tuning state changed!");

        currentTuningState = tuningState;
        if (tuningState == Device.TuningState.InProgress) {
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    super.run();
                    thermalImageView.setColorFilter(Color.DKGRAY, PorterDuff.Mode.DARKEN);
                    findViewById(R.id.tuningProgressBar).setVisibility(View.VISIBLE);
                    findViewById(R.id.tuningTextView).setVisibility(View.VISIBLE);
                }
            });
        }
        else {
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    super.run();
                    thermalImageView.clearColorFilter();
                    findViewById(R.id.tuningProgressBar).setVisibility(View.GONE);
                    findViewById(R.id.tuningTextView).setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * A method to handle what happens when the device's automatic tuning state changes.
     * @param deviceWillTuneAutomatically True if FLIR One camera will now tune automatically
     */
    @Override
    public void onAutomaticTuningChanged(boolean deviceWillTuneAutomatically) {
        //Log.d(TAG, "Called onBatteryChargingStateReceived");
    }

    private ColorFilter originalChargingIndicatorColor = null;

    /**
     * Displays a battery charging icon indicator if the FLIR One camera is charging. The color
     * will vary based on what the current state of the charging is.
     * @param batteryChargingState The charging state
     */
    @Override
    public void onBatteryChargingStateReceived(final Device.BatteryChargingState batteryChargingState) {
        //Log.i(TAG, "Called onBatteryChargingStateReceived; Battery charging state received!");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView chargingIndicator = (ImageView) findViewById(R.id.batteryChargeIndicator);
                if (originalChargingIndicatorColor == null) {
                    originalChargingIndicatorColor = chargingIndicator.getColorFilter();
                }
                switch (batteryChargingState) {
                    case FAULT:
                    case FAULT_HEAT:
                        chargingIndicator.setColorFilter(Color.RED);
                        chargingIndicator.setVisibility(View.VISIBLE);
                        break;
                    case FAULT_BAD_CHARGER:
                        chargingIndicator.setColorFilter(Color.DKGRAY);
                        chargingIndicator.setVisibility(View.VISIBLE);
                    case MANAGED_CHARGING:
                        chargingIndicator.setColorFilter(originalChargingIndicatorColor);
                        chargingIndicator.setVisibility(View.VISIBLE);
                        break;
                    case NO_CHARGING:
                    default:
                        chargingIndicator.setVisibility(View.GONE);
                        break;
                }
            }
        });
    }

    /**
     * Displays the current battery percentage of the FLIR One camera.
     * @param percentage The battery percentage
     */
    @Override
    public void onBatteryPercentageReceived(final byte percentage) {
        //Log.i(TAG, "Called onBatteryPercentageReceived; Battery percentage received!");

        final TextView levelTextView = (TextView) findViewById(R.id.batteryLevelTextView);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                levelTextView.setText(String.format(Locale.getDefault(), "%d%%", percentage));
            }
        });
    }

    /**
     * Updates the thermal image with the current bitmap.
     * @param frame The current frame's bitmap
     */
    private void updateThermalImageView(final Bitmap frame) {
        //Log.v(TAG, "Called updateThermalImageView");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                thermalImageView.setImageBitmap(frame);
            }
        });
    }

    /**
     * StreamDelegate method; calls onFrameProcessed if camera is not tuning.
     * @param frame The current frame
     */
    public void onFrameReceived(Frame frame) {
        //Log.v(TAG, "Frame received!");

        if (currentTuningState != Device.TuningState.InProgress) {
            frameProcessor.processFrame(frame);
        }
    }

    private Bitmap thermalBitmap = null;

    /**
     * Frame Processor Delegate method, will be called each time a rendered frame is produced.
     * @param renderedImage The current rendered image
     */
    public void onFrameProcessed(final RenderedImage renderedImage) {
        //Log.d(TAG, "Called onFrameProcessed");
        if (renderedImage.imageType() == RenderedImage.ImageType.ThermalRadiometricKelvinImage) {
            // Note: this code is not optimized

            int[] thermalPixels = renderedImage.thermalPixelValues();
            // average the center 9 pixels for the spot meter

            int width = renderedImage.width();
            int height = renderedImage.height();
            int centerPixelIndex = width * (height / 2) + (width / 2);

            // Temperature Conversions
            //Log.d(TAG, "Kelvin temperature of center pixel is " + thermalPixels[centerPixelIndex] / 100.0);
            //Log.d(TAG, "Celsius temperature of center pixel is " + ((thermalPixels[centerPixelIndex] / 100.0) - 273.15));
            //Log.d(TAG, "Fahrenheit temperature of center pixel is " + ((((thermalPixels[centerPixelIndex] / 100.0) - 273.15) * (9 / 5.0)) + 32.0));

            // The ARGB values will always be 255, 0, 0, 0 for thermal images
            //int centerPixel = renderedImage.getBitmap().getPixel(width / 2, height / 2);
            //int alpha = Color.alpha(centerPixel), red = Color.red(centerPixel), blue = Color.blue(centerPixel), green = Color.green(centerPixel);

            // ARGB values
            //Log.d(TAG, "Alpha: " + alpha + ", Red: " + red + ", Green: " + green + ", Blue: " + blue);


            //Log.d(TAG, "Temperature (" + thermalPixels[centerPixelIndex] + ") to RGB is " + temperatureToRGB(thermalPixels[centerPixelIndex]));

            int[] centerPixelIndexes = new int[] {
                    centerPixelIndex,
                    centerPixelIndex - 1,
                    centerPixelIndex + 1,
                    centerPixelIndex - width,
                    centerPixelIndex - width - 1,
                    centerPixelIndex - width + 1,
                    centerPixelIndex + width,
                    centerPixelIndex + width - 1,
                    centerPixelIndex + width + 1
            };

            double averageTemp = 0;

            for (int i = 0; i < centerPixelIndexes.length; ++i) {
                // Remember: all primitives are signed, we want the unsigned value,
                // we've used renderedImage.thermalPixelValues() to get unsigned values
                int nextTemp = (thermalPixels[centerPixelIndexes[i]]);
                averageTemp += (((double) nextTemp) - averageTemp) / ((double) i + 1);
            }
            double averageC = (averageTemp / 100) - 273.15;
            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(2);
            numberFormat.setMinimumFractionDigits(2);
            final String spotMeterValue = numberFormat.format(averageC) + "ºC";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.spotMeterValue)).setText(spotMeterValue);
                }
            });
            // if radiometric is the only type, also show the image
            if (frameProcessor.getImageTypes().size() == 1) {
                // example of a custom colorization, maps temperatures 0-100C to 8-bit gray-scale
                byte[] argbPixels = new byte[width * height * 4];
                final byte aPixValue = (byte) 255;
                for (int p = 0; p < thermalPixels.length; ++p) {
                    int destP = p * 4;
                    byte pixValue = (byte) (Math.min(0xff, Math.max(0x00, (thermalPixels[p] - 27315) * (255.0 / 10000.0))));

                    argbPixels[destP + 3] = aPixValue;
                    // red pixel
                    argbPixels[destP] = argbPixels[destP + 1] = argbPixels[destP + 2] = pixValue;
                }
                final Bitmap demoBitmap = Bitmap.createBitmap(width, renderedImage.height(), Bitmap.Config.ARGB_8888);

                demoBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbPixels));

                updateThermalImageView(demoBitmap);
            }
        }
        //This else statement will always get called a second time even if thermal is selected!
        else {
            if (thermalBitmap == null) {
                thermalBitmap = renderedImage.getBitmap();
            }
            else {
                try {
                    renderedImage.copyToBitmap(thermalBitmap);
                }
                catch (IllegalArgumentException e) {
                    thermalBitmap = renderedImage.getBitmap();
                }
            }
            updateThermalImageView(thermalBitmap);

            // Get RGB values

            //int width = renderedImage.width();
            //int height = renderedImage.height();

            // The RGB values will vary
            //for (int i = 0; i < height / 100; ++i)
            //{
                //for (int j = 0; j < width / 100; ++j) {
                    //int centerPixel = renderedImage.getBitmap().getPixel(width / 2, height / 2);
                    //int centerPixel = renderedImage.getBitmap().getPixel(i, j);
                    //int alpha = Color.alpha(centerPixel), red = Color.red(centerPixel), blue = Color.blue(centerPixel), green = Color.green(centerPixel);

                    //Log.d(TAG, "Pixel value is " + centerPixel);

                    // ARGB values
                    //Log.d(TAG, "Alpha: " + alpha + ", Red: " + red + ", Green: " + green + ", Blue: " + blue);
                //}
            //}
        }
        /*
        Capture this image if requested.
        */
        if (this.imageCaptureRequested) {
            imageCaptureRequested = false;
            new Thread(new Runnable() {
                public void run() {
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                    Log.d(TAG, "PATH is " + path);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ssZ", Locale.getDefault());
                    String formattedDate = sdf.format(new Date());
                    String fileName = "FLIROne-" + formattedDate + ".jpg";
                    try {
                        lastSavedPath = path + "/" + fileName;

                        if (isPermissionNeeded(MY_PERMISSIONS_REQUEST_WRITE_IMAGES))
                            lastFrame = renderedImage.getFrame();

                        //image will not be saved if asked for permission; save it in an instance variable and save it upon acceptance
                        renderedImage.getFrame().save(new File(lastSavedPath), RenderedImage.Palette.Iron, RenderedImage.ImageType.BlendedMSXRGBA8888Image);

                        MediaScannerConnection.scanFile(PreviewActivity.this,
                                new String[]{ path + "/" + fileName }, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.i("ExternalStorage", "Scanned " + path + ":");
                                        Log.i("ExternalStorage", "-> uri=" + uri);
                                    }
                                });

                    }
                    catch (Exception e) {
                        Log.e(TAG, "Error saving image: " + e.getMessage());
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thermalImageView.animate().setDuration(50).scaleY(0).withEndAction((new Runnable() {
                                public void run() {
                                    thermalImageView.animate().setDuration(50).scaleY(1);
                                }
                            }));
                        }
                    });
                }
            }).start();
        }
        if (streamSocket != null && streamSocket.isConnected()) {
            try {
                // send PNG file over socket in another thread
                final OutputStream outputStream = streamSocket.getOutputStream();
                // make a output stream so we can get the size of the PNG
                final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();

                thermalBitmap.compress(Bitmap.CompressFormat.WEBP, 100, bufferStream);
                bufferStream.flush();
                (new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            /*
                             * Header is 6 bytes indicating the length of the image data and rotation
                             * of the device
                             * This could be expanded upon by adding bytes to have more metadata
                             * such as image format
                             */
                            byte[] headerBytes = ByteBuffer.allocate((Integer.SIZE + Short.SIZE) / 8).
                                    putInt(bufferStream.size()).putShort((short) deviceRotation).array();
                            synchronized (streamSocket) {
                                outputStream.write(headerBytes);
                                bufferStream.writeTo(outputStream);
                                outputStream.flush();
                            }
                            bufferStream.close();
                        }
                        catch (IOException ex) {
                            Log.e("STREAM", "Error sending frame: " + ex.toString());
                        }
                    }
                }).start();
            }
            catch (Exception ex) {
                Log.e("STREAM", "Error creating PNG: " + ex.getMessage());
            }
        }
    }

    /**
     * Requests permissions for reading from memory if viewing the Gallery or writing to memory
     * if trying to take a picture.
     * @param permissionType The type of permission (Read = 1, Write = 2)
     */
    public boolean isPermissionNeeded(int permissionType) {
        //if we are already requesting permission, we should indicate that permission
        //is still needed for whatever is calling on this method just to be safe
        if (requestingPermission)
            return false;

        switch (permissionType) {
            case MY_PERMISSIONS_REQUEST_READ_IMAGES: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
                    return false;
                requestingPermission = true;
                // Should we show an explanation for the permission we're requesting?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder alert = new AlertDialog.Builder(PreviewActivity.this);
                            alert.setTitle("Please Grant Storage Permission");
                            alert.setMessage("In order to view saved images, "
                                    + "please allow this app to access storage first.");
                            alert.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            });
                            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ActivityCompat.requestPermissions(PreviewActivity.this,
                                            new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                                            MY_PERMISSIONS_REQUEST_READ_IMAGES);
                                }
                            });
                            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    requestingPermission = false;
                                }
                            });
                            alert.show();
                        }
                    });
                }
                else {
                    // No explanation needed, we can request the permission immediately.
                    ActivityCompat.requestPermissions(PreviewActivity.this,
                            new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                            MY_PERMISSIONS_REQUEST_READ_IMAGES);
                }
                return true;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_IMAGES: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
                    return false;
                requestingPermission = true;
                if (ActivityCompat.shouldShowRequestPermissionRationale(PreviewActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder alert = new AlertDialog.Builder(PreviewActivity.this);
                            alert.setTitle("Please Grant Storage Permission");
                            alert.setMessage("In order to save images, "
                                    + "please allow this app to access storage first.");
                            alert.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    requestingPermission = false;
                                }
                            });
                            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ActivityCompat.requestPermissions(PreviewActivity.this,
                                            new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                            MY_PERMISSIONS_REQUEST_WRITE_IMAGES);
                                }
                            });
                            alert.show();
                        }
                    });
                }
                else {
                    ActivityCompat.requestPermissions(PreviewActivity.this,
                            new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                            MY_PERMISSIONS_REQUEST_WRITE_IMAGES);
                }
                return true;
            }
            default:
        }
        return false;
    }

    /**
     * Callback method for handling permissions for reading, writing, and using Internet.
     * @param requestCode The project defined type of permission requested (such as "MY_PERMISSIONS_REQUEST_READ_IMAGES")
     * @param permissions The android type of permission requested (such as "Manifest.permission.READ_EXTERNAL_STORAGE")
     * @param grantResults Stores whether or not permission has been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        boolean somePermissionsForeverDenied = false;
        if (permissions.length >= 1) {
            boolean allPermissionsGranted = true;
            for (int grantResult: grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                for (String permission: permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        //denied
                        Log.i("denied", permission);
                    }
                    else {
                        if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                            //allowed
                            Log.i("allowed", permission);
                        } else {
                            //set to never ask again
                            Log.i("set to never ask again", permission);
                            somePermissionsForeverDenied = true;
                        }
                    }
                }
                if (somePermissionsForeverDenied) {
                    //find out the permission that was denied and customize alert dialog
                    String action, category;
                    switch (requestCode) {
                        case MY_PERMISSIONS_REQUEST_READ_IMAGES:
                            action = "view captured images";
                            category = "Storage";
                            break;
                        case MY_PERMISSIONS_REQUEST_WRITE_IMAGES:
                            action = "save captured images";
                            category = "Storage";
                            break;
                        default:
                            action = "enable permissions again";
                            category = "the desired permission";
                    }

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Use Settings To Adjust Permissions")
                            .setMessage("You have opted out of receiving requests to enable " +
                                    "permissions in this app. To " + action + ", please " +
                                    "tap Settings, Permissions, and allow " + category + ".")
                            .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", getPackageName(), null));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    requestingPermission = false;
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    requestingPermission = false;
                                }
                            })
                            .setCancelable(true)
                            .create()
                            .show();
                }
            }
            else {
                //permissions were accepted; perform actions that depend on permissions
                if (requestCode == MY_PERMISSIONS_REQUEST_READ_IMAGES)
                    startActivity(new Intent(this, GalleryActivity.class));
                else if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_IMAGES) {
                    if (lastFrame != null && lastSavedPath != null) {
                        try {
                            lastFrame.save(new File(lastSavedPath), RenderedImage.Palette.Iron, RenderedImage.ImageType.BlendedMSXRGBA8888Image);
                        }
                        catch (IOException e) {
                            Log.e(TAG, "Could not save image after accepting permissions: " + e.getMessage());
                        }
                    }
                    else {
                        // Will occur if activity is paused, permissions are revoked, and are
                        // accepted again in the app after the FLIR One device is re-detected
                        Log.w(TAG, "Permissions were accepted, but path or saved frame is null (frame could not be saved)");
                    }
                }
                // other 'else if' lines to check for other
                // permissions this app might request
                else
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
        if (!somePermissionsForeverDenied)
            requestingPermission = false;
    }

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    /**
     * Manually recalibrates the FLIR One camera by tuning it.
     * @param v The view for the "Tune" button
     */
    public void onTuneClicked(View v) {
        //Log.d(TAG, "Called onTuneClicked");
        if (flirOneDevice != null) {
            flirOneDevice.performTuning();
        }
    }

    /**
     * Trigger the initial hide() shortly after the FLIR One device has been
     * connected, to briefly hint to the user that UI controls
     * are available. This is useful when a FLIR One camera is already
     * connected when the app has been started.
     */
    private void quickHide() {
        delayedHide(100);
    }

    /**
     * Takes a picture of the current frame and saves it to memory. Requests write permission if necessary.
     * @param v The view of the Camera button icon
     */
    public void onCaptureImageClicked(View v) {
        if (flirOneDevice == null) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder alert = new AlertDialog.Builder(PreviewActivity.this);

                    alert.setTitle("FLIR One Not Found");
                    alert.setMessage("In order to take a picture, please connect a FLIR One camera.");

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                    alert.show();
                }
            });
        }
        else
            this.imageCaptureRequested = true;
    }

    /**
     * Opens the gallery, requests permission if necessary, and displays pictures taken using this app.
     * @param v The view for the Gallery button icon
     */
    public void onOpenGalleryClicked(View v) {
        //Log.d(TAG, "Called onOpenGalleryClicked");
        if (!isPermissionNeeded(MY_PERMISSIONS_REQUEST_READ_IMAGES))
            startActivity(new Intent(this, GalleryActivity.class));
    }

    /**
     * Connects a virtual simulator to the app, which uses sample frames to simulate heat coming from a styrofoam cup.
     * @param v The view for the "Toggle Sim" button
     */
    public void onConnectSimClicked(View v) {
        Log.w(TAG, "Called onConnectSimClicked");
        if (flirOneDevice == null) {
            try {
                flirOneDevice = new SimulatedDevice(this, this, getResources().openRawResource(R.raw.sampleframes), 10);
                flirOneDevice.setPowerUpdateDelegate(this);
                chargeCableIsConnected = true;
            }
            catch (Exception ex) {
                flirOneDevice = null;
                //Log.w("FLIROneExampleApp", "IO EXCEPTION");
                ex.printStackTrace();
            }
        }
        else if (flirOneDevice instanceof SimulatedDevice) {
            flirOneDevice.close();
            flirOneDevice = null;
        }
    }

    /**
     * Simulates the power source to the FLIR One device when the simulator is running.
     * @param v The view for the "ON/OFF" button
     */
    public void onSimulatedChargeCableToggleClicked(View v) {
        //Log.d(TAG, "Called onSimulatedChargeCableToggleClicked");
        if (flirOneDevice instanceof SimulatedDevice) {
            chargeCableIsConnected = !chargeCableIsConnected;
            ((SimulatedDevice) flirOneDevice).setChargeCableState(chargeCableIsConnected);
        }
    }

    /**
     * Rotates the image 180º when the rotate button is pressed.
     * @param v The view for the rotate button
     */
    public void onRotateClicked(View v) {
        //Log.d(TAG, "Called onRotateClicked");
        ToggleButton theSwitch = (ToggleButton) v;
        if (theSwitch.isChecked()) {
            thermalImageView.setRotation(180);
        }
        else {
            thermalImageView.setRotation(0);
        }
    }

    /**
     * Shows/hides the color filter options.
     * @param v The view for the "Change View" button
     */
    public void onChangeViewClicked(View v) {
        //Log.d(TAG, "Called onChangeViewClicked");
        if (frameProcessor == null) {
            ((ToggleButton) v).setChecked(false);
            return;
        }
        ListView paletteListView = (ListView) findViewById(R.id.paletteListView);
        ListView imageTypeListView = (ListView) findViewById(R.id.imageTypeListView);
        if (((ToggleButton) v).isChecked()) {
            // only show palette list if selected image type is colorized
            paletteListView.setVisibility(View.INVISIBLE);
            for (RenderedImage.ImageType imageType : frameProcessor.getImageTypes()) {
                if (imageType.isColorized()) {
                    paletteListView.setVisibility(View.VISIBLE);
                    break;
                }
            }
            imageTypeListView.setVisibility(View.VISIBLE);
            findViewById(R.id.imageTypeListContainer).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.imageTypeListContainer).setVisibility(View.GONE);
        }
    }

    public void onImageTypeListViewClicked(View v) {
        //Log.d(TAG, "Called onImageTypeListViewClicked");
        int index = ((ListView) v).getSelectedItemPosition();
        RenderedImage.ImageType imageType = RenderedImage.ImageType.values()[index];
        frameProcessor.setImageTypes(EnumSet.of(imageType, RenderedImage.ImageType.ThermalRadiometricKelvinImage));
        int paletteVisibility = (imageType.isColorized()) ? View.VISIBLE : View.GONE;
        findViewById(R.id.paletteListView).setVisibility(paletteVisibility);
    }

    public void onPaletteListViewClicked(View v) {
        //Log.d(TAG, "Called onPaletteListViewClicked");
        RenderedImage.Palette pal = (RenderedImage.Palette )(((ListView) v).getSelectedItem());
        frameProcessor.setImagePalette(pal);
    }

    /**
     * Example method of starting/stopping a frame stream to a host
     * @param v The toggle button pushed
     */
    public void onNetStreamClicked(View v) {
        //Log.d(TAG, "Called onNetStreamClicked");
        final ToggleButton button = (ToggleButton) v;
        button.setChecked(false);

        // Prevents two simultaneous connections
        if (streamSocket == null || streamSocket.isClosed()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Start Network Stream");
            alert.setMessage("Provide hostname:port to connect");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);

            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    //hideSoftKeyboard(PreviewActivity.this);
                    //input.clearFocus();
                    //input.getText().clear();
                    /*if (input.length() > 0) {
                        TextKeyListener.clear(input.getText());
                    }*/
                    final String[] parts = value.split(":");
                    (new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                streamSocket = new Socket(parts[0], Integer.parseInt(parts[1], 10));
                                runOnUiThread(new Thread() {
                                    @Override
                                    public void run() {
                                        super.run();
                                        button.setChecked(streamSocket.isConnected());
                                    }
                                });
                            }
                            catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
                                Log.e(TAG, ex.getMessage());
                                connectionError("Usage: <hostname>:<port>");
                            }
                            catch (UnknownHostException ex) {
                                Log.e(TAG, ex.getMessage());
                                connectionError("Unknown Host. Please verify hostname is correct.");
                            }
                            catch (Exception ex) {
                                Log.e(TAG, ex.getMessage());
                                ex.printStackTrace();
                                connectionError("Could not connect to network. Please try again.");
                            }
                        }
                    }).start();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    /*if (input.length() > 0) {
                        TextKeyListener.clear(input.getText());
                    }*/
                }
            });

            alert.show();
        }
        // Closes an existing connection
        else {
            try {
                streamSocket.close();
            }
            catch (Exception ex) {
                //Log.e(TAG, "Could not close streamSocket: " + ex.getMessage());
            }
            button.setChecked(streamSocket != null && streamSocket.isConnected());
        }
    }

    @UiThread
    private void connectionError(final String message) {
        new Thread()
        {
            public void run()
            {
                PreviewActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        AlertDialog.Builder alert = new AlertDialog.Builder(PreviewActivity.this);
                        alert.setTitle("Connection Error");
                        alert.setMessage(message);
                        alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //continue to main screen
                            }
                        });
                        alert.show();
                    }
                });
            }
        }.start();
    }

    ScaleGestureDetector mScaleDetector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Called onCreate");

        setContentView(R.layout.activity_preview);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View controlsViewTop = findViewById(R.id.fullscreen_content_controls_top);
        final View contentView = findViewById(R.id.fullscreen_content);
        AUTO_HIDE = false;

        SparseArray<String> imageTypeNames = new SparseArray<>();
        // Massage the type names for display purposes and skip any deprecated
        for (Field field : RenderedImage.ImageType.class.getDeclaredFields()) {
            if (field.isEnumConstant() && !field.isAnnotationPresent(Deprecated.class)) {
                RenderedImage.ImageType t = RenderedImage.ImageType.valueOf(field.getName());
                String name = t.name().replaceAll("(RGBA)|(YCbCr)|(8)","").replaceAll("([a-z])([A-Z])", "$1 $2");
                imageTypeNames.put(t.ordinal(), name);
            }
        }
        String[] imageTypeNameValues = new String[imageTypeNames.size()];
        for (int i = 0; i < imageTypeNames.size(); ++i) {
            int key = imageTypeNames.keyAt(i);
            imageTypeNameValues[key] = imageTypeNames.get(key);
        }

        RenderedImage.ImageType defaultImageType = RenderedImage.ImageType.BlendedMSXRGBA8888Image;
        frameProcessor = new FrameProcessor(this, this, EnumSet.of(defaultImageType,
                RenderedImage.ImageType.ThermalRadiometricKelvinImage));

        ListView imageTypeListView = ((ListView) findViewById(R.id.imageTypeListView));
        imageTypeListView.setAdapter(new ArrayAdapter<>(this, R.layout.emptytextview, imageTypeNameValues));
        imageTypeListView.setSelection(defaultImageType.ordinal());
        imageTypeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (frameProcessor != null) {
                    RenderedImage.ImageType imageType = RenderedImage.ImageType.values()[position];
                    frameProcessor.setImageTypes(EnumSet.of(imageType, RenderedImage.ImageType.ThermalRadiometricKelvinImage));
                    if (imageType.isColorized()) {
                        findViewById(R.id.paletteListView).setVisibility(View.VISIBLE);
                    }
                    else {
                        findViewById(R.id.paletteListView).setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        imageTypeListView.setDivider(null);

        // Palette List View Setup
        ListView paletteListView = ((ListView) findViewById(R.id.paletteListView));
        paletteListView.setDivider(null);
        paletteListView.setAdapter(new ArrayAdapter<>(this, R.layout.emptytextview, RenderedImage.Palette.values()));
        paletteListView.setSelection(frameProcessor.getImagePalette().ordinal());
        paletteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (frameProcessor != null) {
                    frameProcessor.setImagePalette(RenderedImage.Palette.values()[position]);
                }
            }
        });

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();

        mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
            // Cached values.
            int mControlsHeight;
            int mShortAnimTime;

            @Override
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
            public void onVisibilityChange(boolean visible) {
                //Log.d(TAG, "Called onVisibilityChange");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                    // If the ViewPropertyAnimator API is available
                    // (Honeycomb MR2 and later), use it to animate the
                    // in-layout UI controls at the bottom of the
                    // screen.
                    if (mControlsHeight == 0) {
                        mControlsHeight = controlsView.getHeight();
                    }
                    if (mShortAnimTime == 0) {
                        mShortAnimTime = getResources().getInteger(
                                android.R.integer.config_shortAnimTime);
                    }
                    controlsView.animate()
                            .translationY(visible ? 0 : mControlsHeight)
                            .setDuration(mShortAnimTime);
                    controlsViewTop.animate().translationY(visible ? 0 : -1 * mControlsHeight).setDuration(mShortAnimTime);
                }
                else {
                    // If the ViewPropertyAnimator APIs aren't
                    // available, simply show or hide the in-layout UI
                    // controls.
                    controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                    controlsViewTop.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
                if (visible && !((ToggleButton) findViewById(R.id.change_view_button)).isChecked() && AUTO_HIDE) {
                    // Schedule a hide().
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "Called onClick for contentView");
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                }
                else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.change_view_button).setOnTouchListener(mDelayHideTouchListener);

        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                //Log.d(TAG, "Called onOrientationChanged");
                deviceRotation = orientation;
            }
        };
        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                //Log.d(TAG, "Called onScaleEnd");
            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                //Log.d(TAG, "Called onScaleBegin");
                return true;
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                //Log.d(TAG, "zoom ongoing, scale: " + detector.getScaleFactor());
                frameProcessor.setMSXDistance(detector.getScaleFactor());
                return false;
            }
        });

        findViewById(R.id.fullscreen_content).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Log.d(TAG, "Called onTouch");
                mScaleDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Called onStart");
        thermalImageView = (ImageView) findViewById(R.id.imageView);
        if (Device.getSupportedDeviceClasses(this).contains(FlirUsbDevice.class)){
            findViewById(R.id.pleaseConnect).setVisibility(View.VISIBLE);
        }
        try {
            Device.startDiscovery(this, this);
        }
        catch (IllegalStateException e) {
            //Log.d(TAG, "caught IllegalStateException in onStart");
            // it's okay if we've already started discovery
        }
        catch (SecurityException e) {
            // On some platforms, we need the user to select the app to give us permission to the USB device.
            Toast.makeText(this, "Please insert FLIR One and select " +
                    getString(R.string.app_name), Toast.LENGTH_LONG).show();
            // There is likely a cleaner way to recover, but for now, exit the activity and
            // wait for user to follow the instructions;
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Called onResume");
        if (flirOneDevice != null) {
            flirOneDevice.startFrameStream(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Called onPause");
        if (flirOneDevice != null) {
            flirOneDevice.stopFrameStream();
        }
    }

    @Override
    public void onStop() {
        // We must unregister our usb receiver, otherwise we will steal events from other apps
        Log.d(TAG, "onStop, stopping discovery!");
        Device.stopDiscovery();
        flirOneDevice = null;
        super.onStop();
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //Log.d(TAG, "Called onTouch");
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            //Log.d(TAG, "Called run for mSystemUiHider");
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        //Log.d(TAG, "Called run");
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}