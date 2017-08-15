package com.flir.flironeexampleapplication.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by cp2732 on 7/27/17.
 *
 *
 */

public class ImageProcessor
{
    private String TAG = ImageProcessor.class.getSimpleName();
    private String mFilePath;
    private String mDestinationDirectory;
    private String mFinishedImageName;
    public enum Direction { LEFT, RIGHT, VERTICAL, HORIZONTAL }

    public ImageProcessor(String sourcePath, String destDir, String destName)
    {
        mFilePath = sourcePath;
        mDestinationDirectory = destDir;
        mFinishedImageName = destName;
    }

    public void processAndSaveImage(float scaleFactor, Direction rotateType)
    {
        Bitmap bitmap = BitmapFactory.decodeFile(mFilePath);
        Log.d(TAG, "PATH is " + mFilePath);
        if (bitmap != null)
            saveBitmapToDisk(resizeAndRotate(scaleFactor, bitmap, rotateType));
        else
            Log.e(TAG, "The path or filename provided for rotation is incorrect!");
    }

    private void saveBitmapToDisk(Bitmap bitmap)
    {
        File outFile = new File(mDestinationDirectory, mFinishedImageName);

        FileOutputStream fos;

        try
        {
            fos = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (FileNotFoundException e)
        {
            e.getMessage();
        }
    }

    private Bitmap resizeAndRotate(float scaleFactor, Bitmap src, Direction rotateType)
    {
        Matrix matrix = rotateLogic(scaleFactor, rotateType);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    private Matrix rotateLogic(float scaleFactor, Direction rotateType)
    {
        Matrix matrix = new Matrix();

        // perform error checking here for scale factor

        if (rotateType.equals(Direction.VERTICAL))
        {
            matrix.preScale(scaleFactor, -1f * scaleFactor);
        }
        else if (rotateType.equals(Direction.HORIZONTAL))
        {
            matrix.preScale(-1f * scaleFactor, scaleFactor);
        }
        else if (rotateType.equals(Direction.LEFT))
        {
            matrix.postRotate(270);
        }
        else if (rotateType.equals(Direction.RIGHT))
        {
            matrix.postRotate(90);
        }
        else
        {
            imageProcessorError();
        }

        return matrix;
    }

    private void imageProcessorError()
    {
        Log.d("ERROR", "There was an error in your request");
    }
}