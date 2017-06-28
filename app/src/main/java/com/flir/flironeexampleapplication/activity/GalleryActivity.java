package com.flir.flironeexampleapplication.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.flir.flironeexampleapplication.R;
import com.flir.flironeexampleapplication.adapter.GalleryAdapter;
import com.flir.flironeexampleapplication.model.Image;

/**
 * Created by Chris Puda on 06/22/2017.
 * Provides support for a basic gallery app, similar to the gallery found in the official
 * FLIR One application and the built-in Android Gallery app, for viewing infrared images
 * taken with the FLIR One camera.
 */
public class GalleryActivity extends AppCompatActivity {

    private String TAG = GalleryActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_READ_IMAGES = 1;

    private ArrayList<Image> images;
    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
        Log.d(TAG, "Called onCreate");

        images = new ArrayList<>();
        mAdapter = new GalleryAdapter(this, images);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("images", images);
                bundle.putInt("position", position);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                SlideshowDialogFragment newFragment = SlideshowDialogFragment.newInstance();
                newFragment.setArguments(bundle);
                newFragment.show(ft, "slideshow");
            }

            @Override
            public void onLongClick(View view, int position) {
                //perhaps we can make editing options appear?
            }
        }));

        // Get the intent that started this activity
        Intent intent = getIntent();

        if (intent.getType() != null) {
            Bundle imageBundle = intent.getExtras();
            Uri imageUri = (Uri) imageBundle.get(Intent.EXTRA_STREAM);
            File f = new File("" + imageUri);
            // Figure out what to do based on the intent type
            if (intent.getType().contains("image/")) {
                // Handle intents with image data...
                verifyPermissions();
                Bitmap image = getBitmapFromURI(imageUri);

                if (image != null)
                    saveBitmap(image, f.getName());
                else
                    Log.w(TAG, "Bitmap is corrupt; cannot save image");
            }
            //else if there is another intent type that started the activity, put it here
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    /**
     * Do not verify permissions in onResume, as it will lead to an infinite loop if denied!
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Called onStart");
        verifyPermissions();
    }

    private void verifyPermissions() {
        Log.d(TAG, "Called verifyPermissions");
        if (ContextCompat.checkSelfPermission(GalleryActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder alert = new AlertDialog.Builder(GalleryActivity.this);
                        alert.setTitle("Please Grant Storage Permission");
                        alert.setMessage("In order to view saved images, "
                                + "please allow this app to access storage first.");
                        alert.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                            }
                        });
                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ActivityCompat.requestPermissions(GalleryActivity.this,
                                        new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                                        MY_PERMISSIONS_REQUEST_READ_IMAGES);
                            }
                        });
                        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
                        alert.show();
                    }
                });
            }
            else {
                ActivityCompat.requestPermissions(GalleryActivity.this,
                        new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                        MY_PERMISSIONS_REQUEST_READ_IMAGES);
            }
        }
        else
            refreshView();
    }

    /**
     * Callback method for handling permissions for reading, writing, and using Internet.
     * @param requestCode The project defined type of permission requested (such as "MY_PERMISSIONS_REQUEST_READ_IMAGES")
     * @param permissions The android type of permission requested (such as "Manifest.permission.READ_EXTERNAL_STORAGE")
     * @param grantResults Stores whether or not permission has been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "Called onRequestPermissionsResult");
        if (permissions.length < 1)
            return;
        boolean allPermissionsGranted = true;
        for (int grantResult: grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (!allPermissionsGranted) {
            boolean somePermissionsForeverDenied = false;
            for (String permission: permissions) {
                // if we are still allowed to show the permissions dialog, it means the user denied the request
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    // denied
                    Log.i("denied", permission);
                    finish();
                }
                else {
                    // if we find that the permissions were granted, then we're good to go with what depends on them
                    if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                        //allowed
                        Log.i("allowed", permission);
                    }
                    // if permissions were not granted and permission dialogs are set to be
                    // permanently off, the "Never show again" option was selected
                    else {
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
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                finish();
                            }
                        })
                        .create()
                        .show();
            }
        }
        else {
            //permissions were accepted; perform actions that depend on permissions
            if (requestCode == MY_PERMISSIONS_REQUEST_READ_IMAGES)
                refreshView();
            // other 'else if' lines to check for other
            // permissions this app might request
            else
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private Bitmap getBitmapFromURI(Uri imageUri) {
        if (imageUri != null) {
            try {
                return MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Saves the provided bitmap to internal storage
     * @param bitmapImage The bitmap of an image from another application
     */
    private void saveBitmap(Bitmap bitmapImage, String fileName) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        // Create imageDir
        File imagePath = new File(path + "/" + fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagePath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null)
                    fos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshView() {
        Log.d(TAG, "Called refreshView");
        images.clear();
        images = prepareData();
        mAdapter.updateImages(images);
        mAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(mAdapter);
    }

    private ArrayList<Image> prepareData() {
        ArrayList<Image> imageList = new ArrayList<>();

        String mainPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File f = new File(mainPath);
        File files[] = f.listFiles();
        if (files == null) {
            Log.w(TAG, "Main directory does not exist or an I/O error occurred while retrieving images!");
            return imageList;
        }
        for (File nextFile: files)
        {
            Image image = new Image();
            image.setName(nextFile.getName());
            image.setPath(nextFile.getAbsolutePath());
            image.setTimestamp(Long.toString(SystemClock.currentThreadTimeMillis()));
            imageList.add(image);
        }
        return imageList;
    }
}