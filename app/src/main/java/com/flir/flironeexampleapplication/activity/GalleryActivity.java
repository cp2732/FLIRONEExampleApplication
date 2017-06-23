package com.flir.flironeexampleapplication.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new GalleryAdapter(this, images);
        images = new ArrayList<>();
        verifyPermissions();

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

        refreshView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        verifyPermissions();
    }

    private void verifyPermissions() {
        if (ContextCompat.checkSelfPermission(GalleryActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(GalleryActivity.this,
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
                        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
                        alert.show();
                    }
                });
            }
            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(GalleryActivity.this,
                        new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                        MY_PERMISSIONS_REQUEST_READ_IMAGES);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_IMAGES: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Read permission granted");
                    refreshView();
                }
                else {
                    Log.i(TAG, "Read permission denied");
                }
                return;
            }
        }
    }

    private void refreshView() {
        images.clear();
        images = prepareData();
        mAdapter.notifyDataSetChanged();
        mAdapter = new GalleryAdapter(this, images);
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
            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            image.setName(nextFile.getName());
            image.setPath(nextFile.getAbsolutePath());
            /*image.setLarge(getBitmap(nextFile.getPath(), bmOptions, 1));
            image.setMedium(getBitmap(nextFile.getPath(), bmOptions, 2));
            image.setSmall(getBitmap(nextFile.getPath(), bmOptions, 4));*/
            image.setTimestamp(Long.toString(SystemClock.currentThreadTimeMillis()));
            //Log.i(TAG, "Next file is " + image.getName() + ", path is " + image.getPath() + ", and timestamp is " + image.getTimestamp());
            imageList.add(image);
        }
        return imageList;
    }

    /*public static Bitmap getBitmap(String path, BitmapFactory.Options options, int scale) {
        options.inSampleSize = scale;
        return BitmapFactory.decodeFile(path, options);
    }*/
}