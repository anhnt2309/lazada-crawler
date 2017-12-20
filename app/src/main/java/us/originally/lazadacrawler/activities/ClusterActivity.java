package us.originally.lazadacrawler.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.furture.react.DuktapeEngine;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import me.grantland.widget.AutofitTextView;
import us.originally.lazadacrawler.MainActivity;
import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.manager.CustomClipboardManager;
import us.originally.lazadacrawler.models.Product;
import us.originally.lazadacrawler.utils.ToastUtil;
import weka.classifiers.trees.J48;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;

/**
 * Created by TuanAnh on 12/18/17.
 */

public class ClusterActivity extends AppCompatActivity {
    private ArrayList<String> detailUrls;
    private Instances instances;
    private AutofitTextView tvResult;
    private LottieAnimationView ltLoading;
    private ImageView imgGraph;
    private DuktapeEngine duktapeEngine;
    private LinearLayout btnDrawTree;
    private Spinner spinnerAtrr;
    private List<Integer> attrs;
    private String decisiveAttr;
    private ScrollView svResult;
    private LinearLayout grpSpinner;
    private TextView tvDecisiveAttr;
    private TextView tvSelectAttrText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Chạy gom cụm");
        setContentView(R.layout.activity_algorithm);
        tvResult = findViewById(R.id.tv_algorithm_result);
        ltLoading = findViewById(R.id.animation_view);

        imgGraph = findViewById(R.id.tv_algorithm_graph);
        btnDrawTree = findViewById(R.id.btnDrawTree);
        spinnerAtrr = findViewById(R.id.spinner_decicion_attr);
        svResult = findViewById(R.id.sv_result);
        grpSpinner = findViewById(R.id.grpSpinner);
        tvDecisiveAttr = findViewById(R.id.tv_decisive_attr);
        tvSelectAttrText = findViewById(R.id.tv_select_attr);
        tvSelectAttrText.setText("Chọn số lượng cụm");

        btnDrawTree.setVisibility(View.GONE);

        attrs = new ArrayList<Integer>();
        attrs.add(0);
        attrs.add(1);
        attrs.add(2);
        attrs.add(3);
        attrs.add(4);
        attrs.add(5);
        attrs.add(6);
        attrs.add(7);
        attrs.add(8);
        attrs.add(9);
        attrs.add(10);


        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, attrs);
        spinnerAtrr.setAdapter(spinnerAdapter);
        spinnerAtrr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0)
                    return;
                final Integer attr = attrs.get(i);

                tvDecisiveAttr.setText(getResources().getString(R.string.txt_num_attr,""+attr));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runSimpleKMean(attr);
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }


        });


    }

    public void runSimpleKMean(final int attr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ltLoading.setVisibility(View.VISIBLE);
                grpSpinner.setVisibility(View.GONE);
            }
        });

        ArrayList<Product> products = getProductFromDB();

        formatDataToDRFF(products);

        //Apply filter to convert String to Nominal to run Apriori
        String[] options = new String[2];
        options[0] = "-R";                // "range"
//            options[1] = "4"; // first attribute

        weka.filters.unsupervised.attribute.StringToNominal ff = new weka.filters.unsupervised.attribute.StringToNominal(); // new instance of filter
        try {
//                ff.setOptions(options);
            ff.setAttributeRange("first-last");
            ff.setInputFormat(instances);
            instances = Filter.useFilter(instances, ff);// set options
        } catch (Exception e) {
            e.printStackTrace();
        }


        //run weka SimpleKMean api
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final SimpleKMeans model = new SimpleKMeans();
                    model.setNumClusters(attr);

                    model.buildClusterer(instances);


                    Log.d("xxx", model.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.e("result_xx", model.getRevision()
                                );

                                Log.e("result", model.toString());
                                tvResult.setText(model.toString());
                                ltLoading.setVisibility(View.GONE);
                                svResult.setVisibility(View.VISIBLE);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

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
        atts.add(new Attribute("category", (ArrayList<String>) null));
        atts.add(new Attribute("current_price", (ArrayList<String>) null));
        atts.add(new Attribute("percen_sale", (ArrayList<String>) null));
        atts.add(new Attribute("saler_rate", (ArrayList<String>) null));
        atts.add(new Attribute("saler_saleTime", (ArrayList<String>) null));
        atts.add(new Attribute("product_rate", (ArrayList<String>) null));
        atts.add(new Attribute("brand", (ArrayList<String>) null));
        atts.add(new Attribute("saler_name", (ArrayList<String>) null));
        atts.add(new Attribute("saler_scale", (ArrayList<String>) null));


        instances = new Instances("TestInstances", atts, products.size());
        System.out.println("Before adding any instance");
        System.out.println("--------------------------");
        System.out.println(instances);
        System.out.println("--------------------------");
        for (Product product : products) {
            double[] instanceValue1 = new double[instances.numAttributes()];

            instanceValue1[0] = instances.attribute(0).addStringValue("" + product.category);
            instanceValue1[1] = instances.attribute(1).addStringValue(product.current_price);
            instanceValue1[2] = instances.attribute(2).addStringValue(product.percen_sale);
            instanceValue1[3] = instances.attribute(3).addStringValue(product.saler.saler_rate);
            instanceValue1[4] = instances.attribute(4).addStringValue(product.saler.getSalerSalingTime());
            instanceValue1[5] = instances.attribute(5).addStringValue(product.productRating.overall_rate);
            instanceValue1[6] = instances.attribute(6).addStringValue(product.brand);
            instanceValue1[7] = instances.attribute(7).addStringValue(product.saler.saler_name);
            instanceValue1[8] = instances.attribute(8).addStringValue("" + product.saler.saler_scale);


            instances.add(new DenseInstance(1.0, instanceValue1));
        }

        System.out.println("After adding any instance");
        System.out.println("--------------------------");
        System.out.println(instances);
        System.out.println("--------------------------");
    }

}
