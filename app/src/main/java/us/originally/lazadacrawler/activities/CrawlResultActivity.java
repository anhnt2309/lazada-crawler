package us.originally.lazadacrawler.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;

import us.originally.lazadacrawler.MainActivity;
import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.adapters.SearchResultRecyclerviewAdapter;
import us.originally.lazadacrawler.models.Product;
import us.originally.lazadacrawler.utils.ToastUtil;

/**
 * Created by TuanAnh on 12/10/17.
 */

public class CrawlResultActivity extends AppCompatActivity implements SearchResultRecyclerviewAdapter.OnItemClickListener {
    private RecyclerView rvResult;
    private TextView tvTotal;

    private SearchResultRecyclerviewAdapter mAdapter;
    private ArrayList<String> detailUrls;
    private ArrayList<Product> models;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crawl_result);
        rvResult = findViewById(R.id.rv_result);
        tvTotal = findViewById(R.id.tv_total_product);
        models = getProductFromDB();

        if (models == null || models.size() == 0)
            return;

        tvTotal.setText("" + models.size());
        mAdapter = new SearchResultRecyclerviewAdapter(this, models, "");
        mAdapter.setOnItemClickListener(this);
        rvResult.setAdapter(mAdapter);
        rvResult.setLayoutManager(new LinearLayoutManager(this));


    }

    public ArrayList<Product> getProductFromDB() {
        detailUrls = MainActivity.getCrawledLinkFromDb(this);
        ArrayList<Product> products = new ArrayList<>();
        for (String model : detailUrls) {
            Product product = new Gson().fromJson(model, Product.class);
            products.add(product);
        }

        Log.e("x", "" + products.size());
        return products;
    }

    @Override
    public void onItemClick(Product model) {
        ToastUtil.showInfo(this,model.name);
        Intent intent = new Intent(this,ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.PRODUCT_DETAIL_MODEL,new Gson().toJson(model));
        startActivity(intent);
    }
}
