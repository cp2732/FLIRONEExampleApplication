package com.flir.flironeexampleapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by cp2732 on 6/10/17.
 */

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_IMAGES = 1, MY_PERMISSIONS_REQUEST_WRITE_IMAGES = 2,
            MY_PERMISSIONS_REQUEST_INTERNET_ACCESS = 3;

    //Acts as a pair; each image has a name/unique ID and a path/location it's stored at
    protected class ImageInfo
    {
        private final String name;
        private final String path;

        protected ImageInfo(String aName, String aPath)
        {
            name = aName;
            path = aPath;
        }

        protected String getName() { return name; }
        protected String getPath() { return path; }
    }

    //private final ArrayList<Integer> image_ids = new ArrayList<>();
    //private final ArrayList<ImageInfo> imageNamesAndPaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        refreshView();

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final Activity activity = this;
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

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
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                                        MY_PERMISSIONS_REQUEST_READ_IMAGES);
                            }
                        });
                        alert.show();
                    }
                });
            }
            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                        MY_PERMISSIONS_REQUEST_READ_IMAGES);
            }
        }
        else
            startActivity(new Intent(this, GalleryActivity.class));
    }

    private void refreshView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.imagegallery);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        ArrayList<ImageInfo> createLists = prepareData();
        GalleryAdapter adapter = new GalleryAdapter(this, createLists);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Callback method for handling permissions for reading, writing, and using Internet.
     * @param requestCode The type of permission requested, stored as an int
     * @param permissions
     * @param grantResults Stores whether or not permission has been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_IMAGES: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    Log.i(TAG, "Read permission granted");
                    refreshView();
                }
                else {
                    // Permission was denied. Disable the functionality that depends on this permission.
                    Log.i(TAG, "Read permission denied");
                    finish();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_IMAGES: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted

                }
                else {
                    // Permission was denied. Disable the functionality that depends on this permission.

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_INTERNET_ACCESS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted


                }
                else {
                    // Permission was denied. Disable the functionality that depends on this permission.

                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private ArrayList<ImageInfo> prepareData() {

        ArrayList<ImageInfo> imageInfoList = new ArrayList<>();

        String mainPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        Log.d(TAG, "main path is " + mainPath);

        File f = new File(mainPath);
        File files[] = f.listFiles();
        if (files == null) {
            Log.w(TAG, "Main directory does not exist or I/O error occurred while retrieving images!");
            return imageInfoList;
        }
        for (File nextFile: files)
        {
            //Log.d(TAG, "next image name is " + nextFile.getName());
            //Log.d(TAG, "next image path is " + nextFile.getPath());

            //CreateList imageInfo = new CreateList();
            ImageInfo imageInfo = new ImageInfo(nextFile.getName(), nextFile.getPath());
            //imageInfo.setImageID(image_ids.get(i));
            //imageInfo.setImageLocation(files[i].getName());
            //imageInfo.setImageLocation(nextImagePath);
            imageInfoList.add(imageInfo);

            /*Bitmap bmp = BitmapFactory.decodeFile(nextImagePath);
            try {
                ImageView img = getThumbnail(getContentResolver(), nextImagePath);
                img.setImageBitmap(bmp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }*/
        }


        /*for (int i = 0; i < image_titles.length; ++i) {
            CreateList createList = new CreateList();
            createList.setImage_title(image_titles[i]);
            createList.setImage_ID(image_ids[i]);
            theimage.add(createList);
        }*/
        return imageInfoList;
    }
}
