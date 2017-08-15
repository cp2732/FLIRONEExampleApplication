package com.flir.flironeexampleapplication.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;

import com.flir.flironeexampleapplication.R;
import com.flir.flironeexampleapplication.model.Image;

/**
 * Created by Lincoln on 31/03/16.
 * Extended for FLIR One camera support by Chris Puda on 06/22/2017.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.MyViewHolder> {

    private String TAG = GalleryAdapter.class.getSimpleName();
    private ArrayList<Image> images;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;

        public MyViewHolder(View view) {
            super(view);
            //Log.d(TAG, "Called MyViewHolder");
            thumbnail = view.findViewById(R.id.thumbnail);
        }
    }

    public GalleryAdapter(Context context, ArrayList<Image> images) {
        //Log.d(TAG, "Called GalleryAdapter Constructor");
        mContext = context;
        this.images = images;
    }

    public void updateImages(ArrayList<Image> images) {
        //Log.d(TAG, "Called updateImages");
        this.images = images;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Log.d(TAG, "Called onCreateViewHolder");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_thumbnail, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        //Log.d(TAG, "Called onBindViewHolder");
        Image image = images.get(position);

        Glide.with(mContext).load(image.getPath())
                .thumbnail(0.5f)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.thumbnail);
    }

    @Override
    public int getItemCount() {
        //Log.d(TAG, "Called getItemCount; count is " + images.size());
        if (images == null) {
            Log.e(TAG, "getItemCount says images is null!");
            return 0;
        }
        return images.size();
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private String TAG = RecyclerTouchListener.class.getSimpleName();
        private GestureDetector gestureDetector;
        private GalleryAdapter.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final GalleryAdapter.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    //Log.d(TAG, "Called onSingleTapUp");
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    //Log.d(TAG, "Called onLongPress");
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            //Log.d(TAG, "Called onInterceptTouchEvent");
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            //Log.d(TAG, "Called onTouchEvent");
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            //Log.d(TAG, "Called onRequestDisallowInterceptTouchEvent");
        }
    }
}