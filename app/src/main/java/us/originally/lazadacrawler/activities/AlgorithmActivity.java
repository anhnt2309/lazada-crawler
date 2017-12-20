package us.originally.lazadacrawler.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;

import java.util.ArrayList;

import us.originally.lazadacrawler.CrawlerDB;
import us.originally.lazadacrawler.MainActivity;
import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.models.Product;
import weka.associations.Apriori;
import weka.associations.AssociationRule;
import weka.associations.AssociatorEvaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.filters.Filter;

/**
 * Created by TuanAnh on 12/4/17.
 */

public class AlgorithmActivity extends AppCompatActivity {
    private ArrayList<String> detailUrls;
    private Instances instances;
    private TextView tvResult;
    private LottieAnimationView ltLoading;
    private LinearLayout grpSpinner;
    private ScrollView svResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Chạy Apriori");
        setContentView(R.layout.activity_algorithm);
        tvResult = findViewById(R.id.tv_algorithm_result);
        ltLoading = findViewById(R.id.animation_view);
        ltLoading.setVisibility(View.VISIBLE);
        grpSpinner = findViewById(R.id.grpSpinner);
        grpSpinner.setVisibility(View.GONE);
        svResult = findViewById(R.id.sv_result);
        svResult.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                runApriori();
            }
        }).start();
    }

    public void runApriori() {

        ArrayList<Product> products = getProductFromDB();

        formatDataToDRFF(products);

        //Apply filter to convert String to Nominal to run Apriori
        weka.filters.unsupervised.attribute.StringToNominal ff = new weka.filters.unsupervised.attribute.StringToNominal(); // new instance of filter
        try {
//                ff.setOptions(options);
            ff.setAttributeRange("first-last");
            ff.setInputFormat(instances);
            instances = Filter.useFilter(instances, ff);// set options
        } catch (Exception e) {
            e.printStackTrace();
        }


        //run weka apriori api
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //get data from url
//                        String dataSet = "http://storm.cis.fordham.edu/~gweiss/data-mining/weka-data/weather.nominal.arff";
//                        ConverterUtils.DataSource dataSource = new ConverterUtils.DataSource(dataSet);
//                        Instances instances = dataSource.getDataSet();
                    //local data
                    final Apriori model = new Apriori();
                    model.setNumRules(100);
                    model.setLowerBoundMinSupport(0.1);
                    model.buildAssociations(instances);
                    Log.d("xxx", model.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvResult.setText(model.toString());
                            ltLoading.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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

    public void formatDataToDRFF(ArrayList<Product> products) {
        //construct weka Instances object with crawled data
        ArrayList<Attribute> atts = new ArrayList<Attribute>(8);
        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("A");
        classVal.add("B");
        atts.add(new Attribute("installment", (ArrayList<String>) null));
        atts.add(new Attribute("current_price", (ArrayList<String>) null));
        atts.add(new Attribute("percen_sale", (ArrayList<String>) null));
        atts.add(new Attribute("saler_rate", (ArrayList<String>) null));
        atts.add(new Attribute("saler_saleTime", (ArrayList<String>) null));
        atts.add(new Attribute("warranty_time", (ArrayList<String>) null));
        atts.add(new Attribute("product_rate", (ArrayList<String>) null));
//        atts.add(new Attribute("brand", (ArrayList<String>) null));


        instances = new Instances("TestInstances", atts, products.size());
        System.out.println("Before adding any instance");
        System.out.println("--------------------------");
        System.out.println(instances);
        System.out.println("--------------------------");
        for (Product product : products) {
            double[] instanceValue1 = new double[instances.numAttributes()];

            instanceValue1[0] = instances.attribute(0).addStringValue(product.installment);
            instanceValue1[1] = instances.attribute(1).addStringValue(product.current_price);
            instanceValue1[2] = instances.attribute(2).addStringValue(product.percen_sale);
            instanceValue1[3] = instances.attribute(3).addStringValue(product.saler.saler_rate);
            instanceValue1[4] = instances.attribute(4).addStringValue(product.saler.getSalerSalingTime());
            instanceValue1[5] = instances.attribute(5).addStringValue(product.getWarranty());
            instanceValue1[6] = instances.attribute(6).addStringValue(product.productRating.overall_rate);
//            instanceValue1[7] = instances.attribute(7).addStringValue(product.brand);

            instances.add(new DenseInstance(1.0, instanceValue1));
        }
    }
}
