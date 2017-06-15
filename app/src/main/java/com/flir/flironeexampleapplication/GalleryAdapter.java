package com.flir.flironeexampleapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import com.flir.flironeexampleapplication.GalleryActivity.ImageInfo;

import static com.flir.flironeexampleapplication.R.id.imageView;

/**
 * Created by cp2732 on 6/10/17.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private ArrayList<ImageInfo> galleryList;
    private Context context;

    public GalleryAdapter(Context context, ArrayList<ImageInfo> galleryList) {
        this.galleryList = galleryList;
        this.context = context;
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GalleryAdapter.ViewHolder viewHolder, int i) {
        viewHolder.title.setText(galleryList.get(i).getName());
        //viewHolder.img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        //viewHolder.img.setImageResource((galleryList.get(i).getImageID()));
        try {
            //viewHolder.img.setImageBitmap(getBitmap(galleryList.get(i).getPath()));
            viewHolder.img.setImageBitmap(getThumbnail(context.getContentResolver(), galleryList.get(i).getPath()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        viewHolder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Image", Toast.LENGTH_SHORT).show();

                //display the newly selected image at larger size
                //viewHolder.setImageBitmap(pic);
                //scale options
                //viewHolder.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
        });
    }

    @Override
    public int getItemCount() {
        return galleryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private ImageView img;
        public ViewHolder(View view) {
            super(view);

            title = (TextView) view.findViewById(R.id.title);
            img = (ImageView) view.findViewById(R.id.img);
        }
    }

    public static Bitmap getThumbnail(ContentResolver cr, String path) throws Exception {

        Cursor ca = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{ MediaStore.MediaColumns._ID },
                MediaStore.MediaColumns.DATA + "=?", new String[]{ path }, null);
        if (ca != null) {
            if (ca.moveToFirst()) {
                int id = ca.getInt(ca.getColumnIndex(MediaStore.MediaColumns._ID));
                ca.close();
                return MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
            } else
                ca.close();
        }
        return null;
    }

    /*public static Bitmap getBitmap(String path) throws Exception {
        File image = new File(path);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
        bitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
        imageView.setImageBitmap(bitmap);
    }*/
}