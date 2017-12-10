package us.originally.lazadacrawler.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;


import java.util.ArrayList;
import java.util.Date;

import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.models.Product;

/**
 * Created by TuanAnh on 11/13/17.
 */

public class SearchResultRecyclerviewAdapter extends RecyclerView.Adapter<SearchResultRecyclerviewAdapter.ViewHolder> {
    public static final String AIRLINE_LOGO_URL = "http://pics.avs.io/200/200/";
    private ArrayList<Product> items;
    private Context mContext;
    private int type;
    private String mCurrency;


    boolean isRoundTrip = true;
    boolean hasStopFlight = true;
    boolean ibhasStopFlight = true;

    public SearchResultRecyclerviewAdapter(Context context, ArrayList<Product> items, String currency) {
        mContext = context;
        this.items = items;
        mCurrency = currency;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Product model = items.get(position);
        if (model == null)
            return;

        holder.grpContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnItemClickListener.onItemClick(model);
            }
        });
        String productImage = model.imageUrls.get(0);
        String productName = model.name;
        String productOldPrice = model.old_price;
        String productNewPrice = model.current_price;
        String productsalePercent = model.percen_sale;
        String rate = model.productRating.overall_rate;
        String seller = model.saler.saler_name;
        String location = model.saling_location;

        String currency = model.currency;

        //format data
        String formattedPrice = productNewPrice + " " + currency;
        String oldPrice = productOldPrice.substring(0, productOldPrice.length() - 1);
        String salePercent = "-" + productsalePercent;
        if (rate.isEmpty())
            rate = "Chưa có đánh giá";

        holder.tvProductName.setText(productName);
        holder.tvRate.setText(rate);
        holder.tvOldPrice.setText(oldPrice);
        holder.tvOldPrice.setPaintFlags(holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.tvSalePercent.setText(salePercent);
        holder.tvNewPrice.setText(formattedPrice);
        holder.tvSeller.setText(seller);
        holder.tvLocation.setText(location);

        Glide.with(mContext).load(productImage).into(holder.imgProduct);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public LinearLayout grpContainer;
        public ImageView imgProduct;
        public TextView tvProductName;
        public TextView tvRate;
        public TextView tvOldPrice;
        public TextView tvSalePercent;
        public TextView tvNewPrice;
        public TextView tvSeller;
        public TextView tvLocation;


        public ViewHolder(View itemView) {
            super(itemView);
            grpContainer = itemView.findViewById(R.id.grpContainer);
            imgProduct = itemView.findViewById(R.id.img_product);
            tvProductName = itemView.findViewById(R.id.tv_product_title);
            tvRate = (TextView) itemView.findViewById(R.id.tv_rate);
            tvOldPrice = (TextView) itemView.findViewById(R.id.tv_old_price);
            tvSalePercent = itemView.findViewById(R.id.tv_sale_percent);
            tvNewPrice = itemView.findViewById(R.id.tv_new_price);
            tvSeller = itemView.findViewById(R.id.tv_seller);
            tvLocation = itemView.findViewById(R.id.tv_location);

        }

        @Override
        public void onClick(View view) {

        }
    }

    private static OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        public void onItemClick(Product model);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }
}
