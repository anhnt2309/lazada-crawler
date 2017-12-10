package us.originally.lazadacrawler.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.klinker.android.sliding.MultiShrinkScroller;
import com.klinker.android.sliding.SlidingActivity;
import com.veinhorn.scrollgalleryview.MediaInfo;
import com.veinhorn.scrollgalleryview.ScrollGalleryView;

import java.util.ArrayList;
import java.util.List;

import me.grantland.widget.AutofitTextView;
import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.customeloader.PicassoImageLoader;
import us.originally.lazadacrawler.models.Product;

/**
 * Created by TuanAnh on 12/10/17.
 */

public class ProductDetailActivity extends SlidingActivity {
    public static final String PRODUCT_DETAIL_MODEL = "PRODUCT_DETAIL_MODEL";
    private ScrollGalleryView scrollGalleryView;
    private ArrayList<String> images;

    public TextView tvRate;
    public TextView tvOldPrice;
    public TextView tvSalePercent;
    public TextView tvNewPrice;
    public TextView tvInstallment;
    public TextView tvIncluded;
    public TextView tvNum1StarRate;
    public TextView tvNum2StarRate;
    public TextView tvNum3StarRate;
    public TextView tvNum4StarRate;
    public TextView tvNum5StarRate;
    public AutofitTextView tvWarrantyDetail;
    public AutofitTextView tvWarrantyTime;
    public AutofitTextView tvCategory;
    public AutofitTextView tvLocation;
    public TextView tvProductDetail;
    public TextView tvSellerName;
    public TextView tvSellerTime;
    public TextView tvSellerRate;
    public TextView tvSellerScale;

    public LinearLayout btnProduct;
    public Button btnSeller;

    @Override
    public void init(Bundle savedInstanceState) {
        enableFullscreen();
        Intent intent = getIntent();
        if (intent == null)
            return;

        final String modelString = intent.getStringExtra(PRODUCT_DETAIL_MODEL);

        final Product model = new Gson().fromJson(modelString, Product.class);

        if (model.imageUrls == null)
            return;

        setTitle(model.name);

        setPrimaryColors(
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark)
        );
        setContent(R.layout.activity_content);
        setHeaderContent(R.layout.activity_product_detail);

        initUI();
        gallerySetUp(model);

        String productName = model.name;
        String productOldPrice = model.old_price;
        String productNewPrice = model.current_price;
        String productsalePercent = model.percen_sale;
        String rate = model.productRating.overall_rate;
        String seller = model.saler.saler_name;
        String location = getResources().getString(R.string.txt_sell_location, model.saling_location);
        String installment = model.installment;
        String included = model.product_included;
        String currency = model.currency;
        String warrantyDetail = model.warranty_detail;
        String warrantyTime = model.warranty_time + " " + model.warranty_type;
        String category = getResources().getString(R.string.txt_category, model.category);
        String detail = "";
        for (String detailString : model.detail)
            detail += detailString + "\n \n";

        String sellerName = model.saler.saler_name;
        String sellerRate = getResources().getString(R.string.txt_sell_rate, model.saler.saler_rate.isEmpty()?"0":model.saler.saler_rate + "%");
        String sellerTime = getResources().getString(R.string.txt_sell_time, !model.saler.saler_saleTime.isEmpty()? model.saler.getSalerSalingTime():"Không xác định");
        String sellerScale = getResources().getString(R.string.txt_sell_scale, "" + model.saler.saler_scale);


        String num1Star = getResources().getString(R.string.num_rate, model.productRating.num1Star);
        String num2Star = getResources().getString(R.string.num_rate, model.productRating.num2Star);
        String num3Star = getResources().getString(R.string.num_rate, model.productRating.num3Star);
        String num4Star = getResources().getString(R.string.num_rate, model.productRating.num4Star);
        String num5Star = getResources().getString(R.string.num_rate, model.productRating.num5Star);

        //format data
        String formattedPrice = productNewPrice + " " + currency;
        String oldPrice = productOldPrice.substring(0, productOldPrice.length() - 1);
        String salePercent = "-" + productsalePercent;
        if (rate.isEmpty())
            rate = "Chưa có đánh giá";

        tvOldPrice.setText(oldPrice);
        tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tvSalePercent.setText(salePercent);
        tvNewPrice.setText(formattedPrice);
        tvInstallment.setText(installment);
        tvIncluded.setText(included);
        tvRate.setText(rate);

        tvNum1StarRate.setText(num1Star);
        tvNum2StarRate.setText(num2Star);
        tvNum3StarRate.setText(num3Star);
        tvNum4StarRate.setText(num4Star);
        tvNum5StarRate.setText(num5Star);

        tvWarrantyDetail.setText(warrantyDetail);
        tvWarrantyTime.setText(warrantyTime);

        tvCategory.setText(category);
        tvLocation.setText(location);
        tvProductDetail.setText(detail);

        tvSellerName.setText(sellerName);
        tvSellerRate.setText(sellerRate);
        tvSellerTime.setText(sellerTime);
        tvSellerScale.setText(sellerScale);

        btnSeller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.saler.saler_url));
                startActivity(browserIntent);
            }
        });

        btnProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.product_url));
                startActivity(browserIntent);
            }
        });

    }

    public void initUI() {

        tvRate = (TextView) findViewById(R.id.tv_rate);
        tvOldPrice = (TextView) findViewById(R.id.tv_old_price);
        tvSalePercent = findViewById(R.id.tv_sale_percent);
        tvNewPrice = findViewById(R.id.tv_new_price);
        tvInstallment = findViewById(R.id.tv_installment);
        tvIncluded = findViewById(R.id.tv_included);

        tvNum1StarRate = findViewById(R.id.tv_1star_amount);
        tvNum2StarRate = findViewById(R.id.tv_2star_amount);
        tvNum3StarRate = findViewById(R.id.tv_3star_amount);
        tvNum4StarRate = findViewById(R.id.tv_4star_amount);
        tvNum5StarRate = findViewById(R.id.tv_5star_amount);

        tvWarrantyDetail = findViewById(R.id.tv_warranty_detail);
        tvWarrantyTime = findViewById(R.id.tv_warranty_time);

        tvCategory = findViewById(R.id.tv_category);
        tvLocation = findViewById(R.id.tv_sell_location);
        tvProductDetail = findViewById(R.id.tv_product_detail);

        tvSellerName = findViewById(R.id.tv_seller_name);
        tvSellerRate = findViewById(R.id.tv_seller_rate);
        tvSellerTime = findViewById(R.id.tv_seller_time);
        tvSellerScale = findViewById(R.id.tv_seller_scale);

        btnProduct = findViewById(R.id.btnProduct);
        btnSeller = findViewById(R.id.btn_seller_url);
    }

    public void gallerySetUp(Product model) {
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
