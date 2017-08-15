package com.flir.flironeexampleapplication.helper;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

/**
 * Monitors the position of the device relative to the earth's frame of reference (the magnetic north pole)
 * The sensor works with APK >= 19 (SDK >= Kitkat)
 *
 * Please see https://developer.android.com/guide/topics/sensors/sensors_position.html for more info
 *
 * @author Christopher Puda
 */

public class SensorHelper implements SensorEventListener {

    public String TAG = SensorHelper.class.getSimpleName();

    private SensorManager mSensorManager;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private OnShakeListener mListener;

    /**
     * Get the calling activity's sensor info.
     */
    public SensorHelper(Activity activity) {
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Sets the shake listener
     *
     * @param listener Shake listener
     */
    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    /**
     * Shake Listener Interface
     */
    public interface OnShakeListener {
        void onAngleUpdate();
    }

    /** Get updates from the accelerometer and magnetometer at a constant rate.
     *  To make batch operations more efficient and reduce power consumption,
     *  provide support for delaying updates to the application.
     *
     *  In this example, the sensor reporting delay is small enough such that
     *  the application receives an update before the system checks the sensor
     *  readings again.
     */
    public void requestUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

            //optional:
            //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    //SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        else {
            Log.w(TAG, "Cannot register sensors! API must be >= 19 (Kitkat) for orientation to work.");
        }
    }

    public void stopUpdates() {
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "Called onSensorChanged");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        updateOrientationAngles();
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    private void updateOrientationAngles() {
        Log.d(TAG, "Called updateOrientationAngles");
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        //SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);

        //Print out angles
        float mAzimuth = (float) Math.toDegrees(mOrientationAngles[0]);
        float mPitch = (float) Math.toDegrees(mOrientationAngles[1]);
        float mRoll = (float) Math.toDegrees(mOrientationAngles[2]);

        Log.d(TAG, "Azimuth is " + mAzimuth); //has a range of [-180, 180]: north = ±0º, east = 90º, south = ±180º, west = -90º
        //Log.d(TAG, "Pitch is " + mPitch); //has a range of [-90, 90]: up = -90º, flat = ±0º, upside down = 90º
        //Log.d(TAG, "Roll is " + mRoll); //has a range of [-180, 180]: left = -90º, up = ±0º, right = 90º, down = ±180º
    }
}