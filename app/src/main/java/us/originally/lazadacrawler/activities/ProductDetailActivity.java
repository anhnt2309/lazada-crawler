package us.originally.lazadacrawler.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.klinker.android.sliding.MultiShrinkScroller;
import com.klinker.android.sliding.SlidingActivity;
import com.veinhorn.scrollgalleryview.MediaInfo;
import com.veinhorn.scrollgalleryview.ScrollGalleryView;
import com.veinhorn.scrollgalleryview.loader.DefaultImageLoader;
import com.veinhorn.scrollgalleryview.loader.DefaultVideoLoader;
import com.veinhorn.scrollgalleryview.loader.MediaLoader;

import java.util.ArrayList;
import java.util.List;

import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.customeloader.PicassoImageLoader;
import us.originally.lazadacrawler.models.Product;

/**
 * Created by TuanAnh on 12/10/17.
 */

public class ProductDetailActivity extends SlidingActivity {
    public static final String PRODUCT_DETAIL_MODEL = "PRODUCT_DETAIL_MODEL";
    private ScrollGalleryView scrollGalleryView;


    @Override
    public void init(Bundle savedInstanceState) {
        enableFullscreen();
        Intent intent = getIntent();
        if (intent == null)
            return;

        String modelString = intent.getStringExtra(PRODUCT_DETAIL_MODEL);

        Product model = new Gson().fromJson(modelString, Product.class);
        ArrayList<String> images;
        if (model.imageUrls == null)
            return;

        setTitle(model.name);

        setPrimaryColors(
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark)
        );
        setContent(R.layout.activity_content);
        setHeaderContent(R.layout.activity_product_detail);


        images = model.imageUrls;

        List<MediaInfo> infos = new ArrayList<>(images.size());
        for (String url : images) infos.add(MediaInfo.mediaLoader(new PicassoImageLoader(url)));

        scrollGalleryView = (ScrollGalleryView) findViewById(R.id.scroll_gallery_view);
        scrollGalleryView
                .setThumbnailSize(100)
                .setZoom(true)
                .setFragmentManager(getSupportFragmentManager())
                .addMedia(infos);

    }

    @Override
    protected void configureScroller(MultiShrinkScroller scroller) {
        super.configureScroller(scroller);
        scroller.setIntermediateHeaderHeightRatio(1);
    }
}
