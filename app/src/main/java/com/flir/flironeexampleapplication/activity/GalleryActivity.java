package com.flir.flironeexampleapplication.activity;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import com.flir.flironeexampleapplication.R;
import com.flir.flironeexampleapplication.adapter.GalleryAdapter;
import com.flir.flironeexampleapplication.model.Image;

import static android.provider.MediaStore.Images.Media.getBitmap;
import static android.provider.MediaStore.Images.Media.insertImage;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Chris Puda on 06/22/2017.
 * Provides support for a basic gallery app, similar to the gallery found in the official
 * FLIR One application and the built-in Android Gallery app, for viewing infrared images
 * taken with the FLIR One camera.
 */
public class GalleryActivity extends AppCompatActivity {
    public String TAG = GalleryActivity.class.getSimpleName();
    private final String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
    private static final int MY_PERMISSIONS_REQUEST_READ_IMAGES = 1;
    private static final int VIEW_IN_APP = 1, SHARE_OR_SEND = 2;
    private static boolean requestingPermission = false;

    private ArrayList<Image> images;
    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;

    //This instance variable is saved from an Intent sent to this app from another app
    private Uri pendingImage;

    SlideshowDialogFragment slideshowDialogFragment;

    @BindView(R.id.gallery_toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
        Log.d(TAG, "Called onCreate");

        Toolbar fragment_tb = findViewById(R.id.gallery_toolbar);
        setSupportActionBar(fragment_tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // add a left arrow to back to parent activity,
        // no need to handle action selected event, this is handled by super
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        images = new ArrayList<>();
        mAdapter = new GalleryAdapter(this, images);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                //Log.d(TAG, "Called onClick");
                Bundle bundle = new Bundle();
                bundle.putSerializable("images", images);
                bundle.putInt("position", position);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                slideshowDialogFragment = SlideshowDialogFragment.newInstance();
                slideshowDialogFragment.setArguments(bundle);
                slideshowDialogFragment.show(ft, "slideshow");
            }

            /**
             * Long Click Pop-up Menu
             */
            @Override
            public void onLongClick(View view, final int position) {
                Log.d(TAG, "Called onLongClick");
                //Image selectedImage = images.get(position);
                //Log.d(TAG, "Selected image is " + selectedImage.getName());

                //we could add an option to add an image to a stitching process here
                final CharSequence[] items = {"Info", "Rotate Left", "Rotate Right",
                        "View In Another App", "Send To Another App", "Delete"};

                AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
                builder.setTitle("Image Options");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        final Image selectedImage = images.get(position);
                        Log.d(TAG, "Option clicked is " + items[item]);
                        File file;

                        Matrix matrix;

                        switch (item) {
                            case 0: //image info (date taken, resolution, max/min temperatures, etc.)
                                file = new File(selectedImage.getPath());
                                BitmapFactory.Options bitMapOption = new BitmapFactory.Options();
                                bitMapOption.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(file.getAbsolutePath(), bitMapOption);

                                /*Uri uri; // the URI you've received from the other app
                                InputStream in = null;
                                try {
                                    in = getContentResolver().openInputStream(Uri.parse(selectedImage.getPath()));
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        ExifInterface exifInterface = new ExifInterface(in);
                                        // Now you can extract any Exif tag you want
                                        // Assuming the image is a JPEG or supported raw format

                                        //geotag image
                                        float[] lat = new float[];
                                        exifInterface.getLatLong(lat);
                                        for (float nextElement: lat) {
                                            Log.d(TAG, "next element is " + nextElement);
                                        }
                                    }
                                }
                                catch (IOException e) {
                                    // Handle any errors
                                }
                                finally {
                                    if (in != null) {
                                        try {
                                            in.close();
                                        } catch (IOException ignored) { }
                                    }
                                }*/


                                Date lastModDate = new Date(file.lastModified());

                                //get location (latitude, longitude)
                                /*Location current = LocationProvider.getInstance().getLocation();
                                double lat = current.getLatitude();
                                double longitude = current.getLongitude();*/

                                try {
                                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                                    String latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                                    String longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

                                    AlertDialog.Builder info = new AlertDialog.Builder(GalleryActivity.this);
                                    info.setTitle("Image Options")
                                            .setMessage("Filename: " + selectedImage.getName() + "\nLast modified: " +
                                                    lastModDate.toString() + "\nResolution: " + bitMapOption.outWidth +
                                                    "x" + bitMapOption.outHeight + "\nLocation (latitude, longitude): "
                                                    + latitude + ", " + longitude)
                                            .create()
                                            .show();
                                }
                                catch (IOException e) {
                                    Log.d(TAG, "Error getting Location info: " + e.getMessage());
                                }
                                break;
                            case 1: //rotate left

                                rotateImage(-90, selectedImage);

                                //ImageProcessor processor = new ImageProcessor(selectedImage.getPath(), path, selectedImage.getName());
                                //processor.processAndSaveImage(1, ImageProcessor.Direction.LEFT);

                                /*file = new File(selectedImage.getPath());
                                ExifInterface exifInterface = null;
                                try {
                                    exifInterface = new ExifInterface(file.getPath());
                                }
                                catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                if ((orientation == ExifInterface.ORIENTATION_NORMAL) | (orientation == 0)) {
                                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
                                }
                                else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_180);
                                }
                                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_270);
                                }
                                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_NORMAL);
                                }
                                try {
                                    exifInterface.saveAttributes();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                //imageView.setImageBitmap(getBitmap(selectedImage.getPath()))  ;
                                refreshView();*/

                                /*try {
                                    //ExifInterface exif = new ExifInterface(selectedImage.getPath());
                                    Log.d(TAG, "The path is " + selectedImage.getPath());
                                    ExifInterface exif = new ExifInterface(path + "/" + selectedImage.getName());
                                    int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                    Log.d(TAG, "The current rotation is " + rotation);
                                    int rotationInDegrees = exifToDegrees(rotation);
                                    Log.d(TAG, "The degrees of the rotation is " + rotationInDegrees);
                                    Matrix matrix = new Matrix();
                                    if (rotation != 0f) {
                                        matrix.preRotate(rotationInDegrees);
                                    }
                                    Bitmap bmp = BitmapFactory.decodeFile(selectedImage.getPath());
                                    Bitmap adjustedBitmap = Bitmap.createBitmap(bmp, 0, 0,
                                            bmp.getWidth(), bmp.getHeight(), matrix, true);

                                    //Save the bitmap
                                    FileOutputStream out = null;
                                    try {
                                        //delete the previous file
                                        File f = new File(selectedImage.getPath());
                                        if (f.exists() && f.delete())
                                            Log.d(TAG, "Successfully deleted previous image!");
                                        else
                                            Log.e(TAG, "Failed to delete previous image. Will save modified image anyway.");
                                        out = new FileOutputStream(selectedImage.getPath());
                                        adjustedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                                        // PNG is a lossless format, the compression factor (100) is ignored
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        try {
                                            if (out != null) {
                                                out.close();
                                            }
                                        }
                                        catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                catch (IOException e) {
                                    Log.e(TAG, "Could not open file for rotation.");
                                }*/















                                //Open image using EXIF and print current rotation status
                                /*InputStream in = null;
                                ExifInterface exifInterface = null;
                                try {
                                    Log.d(TAG, "Content resolver is " + getContentResolver());
                                    in = getContentResolver().openInputStream(Uri.fromFile(new File(selectedImage.getPath())));
                                    Log.d(TAG, "in is initialized!");
                                    if (in != null) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            Log.d(TAG, "WILL TRY MAKING EXIF...");
                                            exifInterface = new ExifInterface(in);
                                            // Now you can extract any Exif tag you want
                                            // Assuming the image is a JPEG or supported raw format
                                        }
                                        else
                                            Log.i(TAG, "Build version is not >= API 24. Skipping rotation...");
                                    }
                                    else
                                        Log.w(TAG, "Cannot rotate image left since InputStream is null.");
                                }
                                catch (IOException e) {
                                    // Handle any errors
                                    Log.e(TAG, "Failed to create EXIF for rotating image: " + e.getMessage());
                                    e.printStackTrace();
                                }
                                finally {
                                    if (in != null) {
                                        try {
                                            in.close();
                                        }
                                        catch (IOException ignored) {
                                        }
                                    }
                                }

                                int rotation = 0;
                                if (exifInterface != null) {
                                    int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                            ExifInterface.ORIENTATION_NORMAL);
                                    switch (orientation) {
                                        case ExifInterface.ORIENTATION_ROTATE_90:
                                            rotation = 90;
                                            break;
                                        case ExifInterface.ORIENTATION_ROTATE_180:
                                            rotation = 180;
                                            break;
                                        case ExifInterface.ORIENTATION_ROTATE_270:
                                            rotation = 270;
                                            break;
                                    }
                                    Log.d(TAG, "rotation is " + rotation);

                                    //Pre-rotate
                                    int rotationInDegrees = exifToDegrees(rotation);
                                    Log.d(TAG, "The degrees of the rotation is " + rotationInDegrees);
                                    Matrix matrix = new Matrix();
                                    if (rotation != 0f) {
                                        matrix.preRotate(rotationInDegrees);
                                    }

                                    //Rotate left
                                    if (rotation == 0) {
                                        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_270);
                                    }
                                    else if (rotation == 90) {
                                        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_NORMAL);
                                    }
                                    else if (rotation == 180) {
                                        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
                                    }
                                    else if (rotation == 270) {
                                        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_180);
                                    }
                                    FileOutputStream out = null;
                                    try {
                                        //delete the previous file
                                        if (file.delete())
                                            Log.d(TAG, "Successfully deleted previous image!");
                                        else
                                            Log.e(TAG, "Failed to delete previous image. Will save modified image anyway.");
                                        out = new FileOutputStream(exifInterface);
                                        adjustedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                                        // PNG is a lossless format, the compression factor (100) is ignored
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        try {
                                            if (out != null) {
                                                out.close();
                                                refreshView();
                                            }
                                        }
                                        catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                        exifInterface.saveAttributes();
                                    }
                                    catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }*/















                                    //Overwrite old file
                                    //Bitmap bmp = BitmapFactory.decodeFile(selectedImage.getPath());
                                    /*file = new File(selectedImage.getPath());
                                    Bitmap bmp, adjustedBitmap;
                                    if (file.exists()) {
                                        Log.d(TAG, "Image exists; path of file is " + file.getPath());
                                        bmp = BitmapFactory.decodeFile(file.getPath());
                                        adjustedBitmap = Bitmap.createBitmap(bmp, 0, 0,
                                                bmp.getWidth(), bmp.getHeight(), matrix, true);
                                    }
                                    else {
                                        Log.e(TAG, "Image does not exist! Cannot continue rotation processing.");
                                        return;
                                    }

                                    //Save the bitmap
                                    FileOutputStream out = null;
                                    try {
                                        //delete the previous file
                                        if (file.delete())
                                            Log.d(TAG, "Successfully deleted previous image!");
                                        else
                                            Log.e(TAG, "Failed to delete previous image. Will save modified image anyway.");
                                        out = new FileOutputStream(file.getPath());
                                        adjustedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                                        // PNG is a lossless format, the compression factor (100) is ignored
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        try {
                                            if (out != null) {
                                                out.close();
                                                refreshView();
                                            }
                                        }
                                        catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }*/
                                //}
                                break;
                            case 2: //rotate right
                                rotateImage(90, selectedImage);
                                break;
                            case 3: //View in another app
                                Log.d(TAG, "Will view " + path);
                                openInAnotherApp(VIEW_IN_APP, images.get(position));
                                break;

                                /*file = new File(selectedImage.getPath());
                                if (file.isFile()) {
                                    //Intent intent = new Intent(Intent.ACTION_VIEW);
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    //intent.setDataAndType(Uri.parse(selectedImage.getPath()), "image/*"); //initially had: images.get(position).getPath()
                                    //intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(selectedImage.getPath()));
                                    //intent.putExtra(Intent.ACTION_VIEW, Uri.parse(selectedImage.getPath()));

                                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), new File(selectedImage.getPath()));
                                    Log.e(TAG, "Package name is " + getApplicationContext().getPackageName());
                                    intent.setDataAndType(photoURI, "image/*");
                                    intent.putExtra(Intent.EXTRA_STREAM, photoURI);
                                    intent.putExtra(Intent.ACTION_VIEW, photoURI);
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    GalleryActivity.this.startActivityForResult(intent, 0);*/


                                    /*MediaScannerConnection.scanFile(GalleryActivity.this, new String[]{ file.toString() },
                                            null, new MediaScannerConnection.OnScanCompletedListener() {
                                        @Override
                                        public void onScanCompleted(String path, Uri uri) {
                                            //Intent intent = new Intent(Intent.ACTION_VIEW);
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            //intent.setDataAndType(Uri.parse(selectedImage.getPath()), "image/*"); //initially had: images.get(position).getPath()
                                            //intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(selectedImage.getPath()));
                                            //intent.putExtra(Intent.ACTION_VIEW, Uri.parse(selectedImage.getPath()));
                                            intent.setDataAndType(Uri.fromFile(file), "image/*");
                                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path, selectedImage.getName())));
                                            intent.putExtra(Intent.ACTION_VIEW, Uri.fromFile(new File(path, selectedImage.getName())));
                                            GalleryActivity.this.startActivityForResult(intent, 0);
                                        }
                                    });*/
                                //}
                            case 4: //Share or send to another app
                                Log.d(TAG, "Will share " + path);
                                openInAnotherApp(SHARE_OR_SEND, images.get(position));
                                break;
                            case 5: //delete
                                deletePhoto(images.get(position));
                                break;
                            default: //do nothing
                        }
                    }
                });
                builder.show();

                //perhaps we can make editing options appear?
                /*btnDelete.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        File file = new File(imagePath);
                        if(file.exist())
                            file.delete();
                    }
                });*/
                // getExternalFilesDir() + "/Pictures" should match the declaration in file_paths.xml paths
                //File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), selectedImage.getName());

                // wrap File object into a content provider. NOTE: authority here should match authority in manifest declaration
                //Uri bmpUri = FileProvider.getUriForFile(GalleryActivity.this, "com.flir.flironeexampleapplication.file_paths", file);
            }
        }));
        registerForContextMenu(recyclerView);
    }

    /**
     * Do not verify permissions in onResume, as it will lead to an infinite loop if denied!
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Called onStart");
        images = new ArrayList<>(); //need to prevent IllegalStateException
        recyclerView.setAdapter(mAdapter);

        Intent intent = getIntent();
        Log.d(TAG, "Calling intent, type is " + intent.getType());

        if (intent.getType() == null)
            Log.i(TAG, "intent.getType is null");
        if (intent.getExtras() == null)
            Log.i(TAG, "intent.getExtras is null");

        if (intent.getType() != null) {
            Bundle imageBundle = intent.getExtras();
            if (imageBundle != null) {
                Log.d(TAG, "TYPE IS: " + intent.getType());
                // Figure out what to do based on the intent type
                if (intent.getType().contains("image/")) {
                    // Handle intents with image data...
                    pendingImage = (Uri) imageBundle.get(Intent.EXTRA_STREAM);
                    Log.d(TAG, "IMAGE FOUND; image is " + pendingImage);
                    setResult(RESULT_OK); //default intent for return to calling activity
                }
                //else if there is another intent type that started the activity, put it here
            }
            else {
                AlertDialog.Builder info = new AlertDialog.Builder(GalleryActivity.this);
                info.setTitle("Sharing Error")
                        .setMessage("Image content not found or is corrupt.")
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //continue to main screen
                                }
                        })
                        .create()
                        .show();
            }
        }
        verifyPermissions();
    }

    /**
     * Toolbar Menu
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        /*if (v.getId() == R.id.recycler_view) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.slideshow_menu, menu);
        }*/
    }

    /*@Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.add:
                // add stuff here
                return true;
            case R.id.edit:
                // edit stuff here
                return true;
            case R.id.delete:
                // remove stuff here
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.gallery_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "Called onOptionsItemSelected");
        // handle arrow click here
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                Log.i(TAG, "REFRESH SELECTED");
                refreshView();
                break;
            case R.id.action_settings:
                Log.i(TAG, "SETTINGS SELECTED");
                break;
            case android.R.id.home:
                Log.i(TAG, "BACK ARROW SELECTED");
                finish();
                break;
            case R.id.close_slideshow:
                Log.i(TAG, "CLOSE SELECTED");
                slideshowDialogFragment.dismiss();
                break;
            default: return super.onOptionsItemSelected(item);
        }
        return true;
    }



    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.slideshow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "Called onOptionsItemSelected");
        // handle arrow click here
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                Toast.makeText(this, "Refresh selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            case android.R.id.home:
                finish();
                break;
            default: return super.onOptionsItemSelected(item);
        }
        return true;
    }*/

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
        Log.d(TAG, "fragment is " + fragment);
        if (fragment instanceof SlideshowDialogFragment) {
            /*if (((SlideshowDialogFragment) fragment).onBackPressed()) {
                Log.d(TAG, "Back was pressed on a SlideshowDialogFragment! Hooray!");
                return;
            }*/
            Log.d(TAG, "Is SlideshowDialogFragment, but is false");
        }
        Log.d(TAG, "May not be instance of SlideshowDialogFragment");
        super.onBackPressed();
    }

    private void verifyPermissions() {
        //Log.d(TAG, "Called verifyPermissions");
        if (requestingPermission)
            return;
        if (ContextCompat.checkSelfPermission(GalleryActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestingPermission = true;
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
                                requestingPermission = false;
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
                                requestingPermission = false;
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
        //Log.d(TAG, "Called onRequestPermissionsResult");
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
                                    requestingPermission = false;
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
                                    requestingPermission = false;
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
        if (!somePermissionsForeverDenied)
            requestingPermission = false;
    }

    private Bitmap getBitmapFromURI(Uri imageUri) {
        //Log.d(TAG, "Called getBitmapFromURI");
        if (imageUri != null) {
            try {
                return getBitmap(getContentResolver(), imageUri);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void rotateImage(final int degrees, final Image image) {
        /*Bitmap oldBMP, rotatedBMP;
        Matrix matrix;

        oldBMP = BitmapFactory.decodeFile(image.getPath());

        matrix = new Matrix();
        matrix.postRotate(degrees);
        rotatedBMP = Bitmap.createBitmap(oldBMP, 0, 0, oldBMP.getWidth(), oldBMP.getHeight(), matrix, true);

        File file = new File(image.getPath());

        if (!file.exists()) {
            Log.e(TAG, "File does not exist!");
        }
        else if (!file.delete()) {
            Log.e(TAG, "Couldn't delete old file!");
            return;
        }
        saveBitmap(rotatedBMP, image.getName());*/



        try {
            File file = new File(image.getPath());
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            ContentValues values = new ContentValues();
            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            Log.d(TAG, "OLD ORIENTATION WAS " + orientation);

            if (degrees == 90) {
                if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_NORMAL))) {
                    Log.e(TAG, "Rotation is at 0º; Will rotate 90 degrees to the right");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                    values.put(MediaStore.Images.Media.ORIENTATION, 6);
                }
                else if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_ROTATE_90))) {
                    Log.e(TAG, "Rotation is at 90º; Will rotate 90 degrees to the right");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_180));
                    values.put(MediaStore.Images.Media.ORIENTATION, 3);
                }
                else if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_ROTATE_180))) {
                    Log.e(TAG, "Rotation is at 180º; Will rotate 90 degrees to the right");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
                    values.put(MediaStore.Images.Media.ORIENTATION, 8);
                }
                else {
                    Log.e(TAG, "Rotation is at 270º; Will rotate 90 degrees to the right");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                    values.put(MediaStore.Images.Media.ORIENTATION, 1);
                }
            }
            else if (degrees == -90) {
                if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_NORMAL))) {
                    Log.e(TAG, "Rotation is at 0º; Will rotate 90 degrees to the left");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
                    values.put(MediaStore.Images.Media.ORIENTATION, 8);
                }
                else if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_ROTATE_90))) {
                    Log.e(TAG, "Rotation is at 90º; Will rotate 90 degrees to the left");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                    values.put(MediaStore.Images.Media.ORIENTATION, 1);
                }
                else if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_ROTATE_180))) {
                    Log.e(TAG, "Rotation is at 180; Will rotate 90 degrees to the left");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                    values.put(MediaStore.Images.Media.ORIENTATION, 6);
                }
                else {
                    Log.e(TAG, "Rotation is at 270º; Will rotate 90 degrees to the left");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_180));
                    values.put(MediaStore.Images.Media.ORIENTATION, 3);
                }
            }
            exif.saveAttributes();

            Uri uri = getContentUriFromPath(image.getPath(), getApplicationContext());

            if (uri == null) {
                Log.e(TAG, "Image uri is null! Cannot update EXIF information for image.");
                return;
            }
            if (getApplicationContext().getContentResolver().update(uri, values, null, null) > 0) {
                Log.d(TAG, "Setting orientation was successful");
            }

            Log.d(TAG, "NEW ORIENTATION IS " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
        }
        catch (IOException e) {
            // do something
            Log.d(TAG, "IOException prevented saving location data to EXIF: " + e.getMessage());
        }



        new Thread(new Runnable() {
            public void run() {
                MediaScannerConnection.scanFile(GalleryActivity.this,
                        new String[]{ image.getPath() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                                refreshView();
                            }
                        });
            }
        });
        getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(new File(image.getPath()))));
        //deletePhoto(image);
    }

    /**
     * Get the content uri for the file path
     *
     * @param path The path of the file
     * @param context A context used for getting a Cursor to the Uri
     * @return The content Uri for the file
     */
    public Uri getContentUriFromPath(String path, Context context) {
        String[] projection = { MediaStore.Images.Media._ID };
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                MediaStore.Images.Media.DATA + " = ?", new String[] { path }, null);
        Uri result = null;
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    long mediaId = cursor.getLong(0);
                    result = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId);
                }
            }
            finally {
                cursor.close();
            }
        }
        return result;
    }

    private void openInAnotherApp(int action, Image image) {
        //Log.d(TAG, "Called openInAnotherApp");
        Bitmap bmp = image.getBitmap();
        String sendMessage;

        if (bmp == null)
            Log.e(TAG, "Bitmap is null! Cannot open in another app.");
        else {
            Intent intent;
            Uri uri = getContentUriFromPath(image.getPath(), getApplicationContext());
            switch (action) {
                case VIEW_IN_APP:
                    intent = new Intent(Intent.ACTION_VIEW);
                    sendMessage = "View IR Image With...";
                    intent.setDataAndType(uri, "image/*");
                    break;
                case SHARE_OR_SEND:
                    intent = new Intent(Intent.ACTION_SEND);
                    sendMessage = "Share IR Image With...";
                    intent.setType("image/*");
                    break;
                default:
                    return;
            }

            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, sendMessage));
        }
    }

    private void deletePhoto(Image image) {
        Log.d(TAG, "Will delete " + image.getName());
        File file = new File(path, image.getName());
        if (!file.exists())
            Log.w(TAG, "File could not be deleted because it does not exist!");
        if (file.delete())
            Log.d(TAG, "File " + image.getName() + " was deleted");
        getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(new File(image.getPath()))));
        refreshView();
    }

    /**
     * Saves the provided bitmap to internal storage
     * @param bitmapImage The bitmap of an image from another application
     */
    private void saveBitmap(Bitmap bitmapImage, final String fileName) {
        //Log.d(TAG, "Called saveBitmap");



        byte[] pictureBytes;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, bos);
        pictureBytes = bos.toByteArray();

        try {
            FileOutputStream fs = new FileOutputStream(new File(path, fileName));
            fs.write(pictureBytes);
            fs.close();
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File was not found!");
        }
        catch (IOException e) {
            Log.e(TAG, "File could not be saved!");
        }

        /*new Thread(new Runnable() {
            public void run() {
                MediaScannerConnection.scanFile(GalleryActivity.this,
                        new String[]{path + "/" + fileName}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);

                                if (path == null || uri == null)
                                    return;

                                try {
                                    File image = new File(path);
                                    Log.d(TAG, "PATH is " + path);
                                    ExifInterface exif = new ExifInterface(image.getAbsolutePath());
                                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, GPS.convert(latitude));
                                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(latitude));
                                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(longitude));
                                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(longitude));
                                    exif.saveAttributes();
                                }
                                catch (IOException e) {
                                    // do something
                                    Log.d(TAG, "IOException prevented saving location data to EXIF: " + e.getMessage());
                                }
                            }
                        });
            }
        });*/




        /*ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String imagePath = insertImage(getContentResolver(),
                bitmapImage, fileName, null);*/

        // Create imageDir
        /*FileOutputStream fos = null;
        try {
            Log.d(TAG, "FILENAME is " + fileName);
            fos = new FileOutputStream(new File(path, fileName));
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (FileNotFoundException | NullPointerException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        //broadcasting will allow file viewers, like the Gallery, to see the updated image immediately
        getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(new File(path, fileName))));
    }

    public void refreshView() {
        Log.d(TAG, "Called refreshView");
        if (pendingImage != null) {
            Log.d(TAG, "pending image path is " + pendingImage.getPath());
            //saveBitmap(getBitmapFromURI(pendingImage), new File(pendingImage.getPath()).getName());
            pendingImage = null;
            recyclerView.setAdapter(mAdapter);
        }
        Log.d(TAG, "Old size was " + images.size());
        prepareData(images);
        mAdapter.notifyDataSetChanged();
        mAdapter.updateImages(images);
        Log.d(TAG, "New size is " + images.size());
        //Log.d(TAG, "images.size is " + images.size());
    }

    private void prepareData(ArrayList<Image> imageList) {
        //Log.d(TAG, "Called prepareData");
        if (imageList == null)
            imageList = new ArrayList<>();
        else
            imageList.clear();

        File f = new File(path);
        File files[] = f.listFiles();
        if (files == null) {
            Log.w(TAG, "Main directory does not exist or an I/O error occurred while retrieving images!");
            return;
        }
        for (File nextFile: files)
        {
            Image image = new Image();
            image.setName(nextFile.getName());
            image.setPath(nextFile.getAbsolutePath());
            image.setTimestamp(Long.toString(SystemClock.currentThreadTimeMillis()));
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            image.setBitmap(BitmapFactory.decodeFile(nextFile.getAbsolutePath(), bmOptions));
            Log.e(TAG, "BITMAP IS " + image.getBitmap());
            imageList.add(image);
        }
    }
}