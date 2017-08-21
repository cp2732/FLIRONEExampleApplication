package com.flir.flironeexampleapplication.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

import com.flir.flironeexampleapplication.R;
import com.flir.flironeexampleapplication.model.Image;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Initially created by FLIR for an example app for the FLIR One.
 * Extended for FLIR One camera support by Chris Puda on 06/22/2017.
 */
public class SlideshowDialogFragment extends DialogFragment {
    public static String TAG = SlideshowDialogFragment.class.getSimpleName();
    private GalleryActivity galleryActivity;
    private ArrayList<Image> images;
    private ViewPager viewPager;
    private TextView lblCount;
    private int selectedPosition = 0;

    @BindView(R.id.slideshow_toolbar)
    Toolbar toolbar;

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
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppCompat);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.d(TAG, "Called onCreateView");
        View v = inflater.inflate(R.layout.fragment_image_slider, container, false);
        viewPager = v.findViewById(R.id.viewpager);
        lblCount = v.findViewById(R.id.lbl_count);

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

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated(): " + savedInstanceState);
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        setHasOptionsMenu(true);
        toolbar.setTitle("IR Slideshow Gallery");
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    }

    @Override
    public void onResume() {
        getActivity().invalidateOptionsMenu();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.e(TAG, "onCreateOptionsMenu()");
        menu.clear();
        if (getChildFragmentManager().getBackStackEntryCount() == 0) {
            inflater.inflate(R.menu.slideshow_menu, menu);
        }
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

        //Displays the number the image is in the collection (# of #)
        lblCount.setText((position + 1) + " of " + images.size());
    }

    //adapter
    public class MyViewPagerAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
            //Log.d(TAG, "Called MyViewPagerAdapter Constructor");
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //Log.d(TAG, "Called instantiateItem");
            layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.image_fullscreen_preview, container, false);

            ImageView imageViewPreview = view.findViewById(R.id.image_preview);

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
            return view == obj;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //Log.d(TAG, "Called destroyItem");
            container.removeView((View) object);
        }
    }
}