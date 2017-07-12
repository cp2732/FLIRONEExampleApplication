package com.flir.flironeexampleapplication.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import com.flir.flironeexampleapplication.R;
import com.flir.flironeexampleapplication.model.Image;

/**
 * Initially created by FLIR for an example app for the FLIR One.
 * Extended for FLIR One camera support by Chris Puda on 06/22/2017.
 */
public class SlideshowDialogFragment extends DialogFragment {
    private static String TAG = SlideshowDialogFragment.class.getSimpleName();
    private GalleryActivity galleryActivity;
    private ArrayList<Image> images;
    private ViewPager viewPager;
    private TextView lblCount, lblTitle, lblDate;
    private int selectedPosition = 0;

    static SlideshowDialogFragment newInstance() {
        //Log.d(TAG, "Called newInstance");
        return new SlideshowDialogFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Log.d(TAG, "Called onAttach");
        if (context instanceof GalleryActivity)
            galleryActivity = (GalleryActivity) context; //useful for call back to GalleryActivity
        else
            throw new ClassCastException(context.toString() + " must be called by GalleryActivity.");
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Called onCreate");

        /*ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            Log.d(TAG, "getActivity is " + getActivity().toString());
            actionBar.setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) getActivity()).setSupportActionBar((Toolbar) getActivity().findViewById(R.id.toolbar));
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
        else
            Log.d(TAG, "getActivity is null");*/


        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        /*((AppCompatActivity) getActivity()).setSupportActionBar((Toolbar) getActivity().findViewById(R.id.toolbar));
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.d(TAG, "Called onCreateView");
        View v = inflater.inflate(R.layout.fragment_image_slider, container, false);
        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        lblCount = (TextView) v.findViewById(R.id.lbl_count);
        lblTitle = (TextView) v.findViewById(R.id.title);
        lblDate = (TextView) v.findViewById(R.id.date);

        // workaround to unchecked cast
        images = new ArrayList<>();
        ArrayList<?> result = (ArrayList<?>) getArguments().getSerializable("images");
        if (result != null) {
            for (Object object: result) {
                if (object instanceof Image) {
                    images.add((Image) object);
                }
            }
        }

        selectedPosition = getArguments().getInt("position");

        Log.e(TAG, "position: " + selectedPosition);
        Log.e(TAG, "images size: " + images.size());

        MyViewPagerAdapter myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        setCurrentItem(selectedPosition);

        setHasOptionsMenu(true);

        return v;
    }

    @Override
    public void onStop() {
        //Log.d(TAG, "Called onStop");

        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Called onDestroy");

        //refresh the view in the gallery
        galleryActivity.refreshView();

        super.onDestroy();
    }


    private void setCurrentItem(int position) {
        //Log.d(TAG, "Called setCurrentItem");
        viewPager.setCurrentItem(position, false);
        displayMetaInfo(selectedPosition);
    }

    //page change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            //Log.d(TAG, "Called onPageSelected");
            displayMetaInfo(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            //Log.d(TAG, "Called onPageScrolled");
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
            //Log.d(TAG, "Called onPageScrollStateChanged");
        }
    };

    private void displayMetaInfo(int position) {
        //Log.d(TAG, "Called displayMetaInfo");
        Image image = images.get(position);
        File file = new File(image.getPath());
        Date lastModDate = new Date(file.lastModified());

        //Displays the number the image is in the collection (ex: 1 of 10)
        lblCount.setText((position + 1) + " of " + images.size());

        //Displays the filename of the image
        lblTitle.setText(image.getName());

        //Displays the last modified date of the image
        lblDate.setText(lastModDate.toString());
    }

    //adapter
    public class MyViewPagerAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
            Log.d(TAG, "Called MyViewPagerAdapter Constructor");
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //Log.d(TAG, "Called instantiateItem");
            layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.image_fullscreen_preview, container, false);

            ImageView imageViewPreview = (ImageView) view.findViewById(R.id.image_preview);

            Image image = images.get(position);

            Glide.with(getActivity()).load(image.getPath())
                    .thumbnail(0.5f)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageViewPreview);

            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            //Log.d(TAG, "Called getCount; the size is " + images.size());
            return images.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            //Log.d(TAG, "Called isViewFromObject");
            return view == ((View) obj);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //Log.d(TAG, "Called destroyItem");
            container.removeView((View) object);
        }
    }
}