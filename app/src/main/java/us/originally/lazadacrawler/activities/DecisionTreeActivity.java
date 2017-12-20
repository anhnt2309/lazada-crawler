package us.originally.lazadacrawler.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Scroller;
import android.widget.Spinner;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.furture.react.DuktapeEngine;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import us.originally.lazadacrawler.MainActivity;
import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.manager.CustomClipboardManager;
import us.originally.lazadacrawler.models.GraphViz;
import us.originally.lazadacrawler.models.Product;
import us.originally.lazadacrawler.utils.ToastUtil;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;


/**
 * Created by TuanAnh on 12/7/17.
 */

public class DecisionTreeActivity extends AppCompatActivity {
    private ArrayList<String> detailUrls;
    private Instances instances;
    private TextView tvResult;
    private LottieAnimationView ltLoading;
    private ImageView imgGraph;
    private DuktapeEngine duktapeEngine;
    private LinearLayout btnDrawTree;
    private Spinner spinnerAtrr;
    private List<String> attrs;
    private String decisiveAttr;
    private ScrollView svResult;
    private LinearLayout grpSpinner;
    private TextView tvDecisiveAttr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Chạy cây quyết định");
        setContentView(R.layout.activity_algorithm);
        tvResult = findViewById(R.id.tv_algorithm_result);
        ltLoading = findViewById(R.id.animation_view);

        imgGraph = findViewById(R.id.tv_algorithm_graph);
        btnDrawTree = findViewById(R.id.btnDrawTree);
        spinnerAtrr = findViewById(R.id.spinner_decicion_attr);
        svResult = findViewById(R.id.sv_result);
        grpSpinner = findViewById(R.id.grpSpinner);
        tvDecisiveAttr = findViewById(R.id.tv_decisive_attr);


        btnDrawTree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DecisionTreeActivity.this, BrowserActivity.class);
                intent.putExtra("url", "http://www.webgraphviz.com");
                startActivity(intent);
            }
        });

        attrs = new ArrayList<String>();
        attrs.add("Thuộc tính ");
        attrs.add("Installment");
        attrs.add("Current Price");
        attrs.add("Percen Sale");
        attrs.add("Saler Rate");
        attrs.add("Sale Time On Laza");
        attrs.add("Product Rate");
        attrs.add("Saler Name");
        attrs.add("Saler Scale");
        attrs.add("Category");
        attrs.add("Brand");


        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, attrs);
        spinnerAtrr.setAdapter(spinnerAdapter);
        spinnerAtrr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0)
                    return;
                String attr = attrs.get(i);
                decisiveAttr = "";
                if (attr.equals("Current Price"))
                    decisiveAttr = "current_price";
                if (attr.equals("Installment"))
                    decisiveAttr = "installment";
                if (attr.equals("Percen Sale"))
                    decisiveAttr = "percen_sale";
                if (attr.equals("Saler Rate"))
                    decisiveAttr = "saler_rate";
                if (attr.equals("Sale Time On Laza"))
                    decisiveAttr = "saler_saleTime";
                if (attr.equals("Product Rate"))
                    decisiveAttr = "product_rate";
                if (attr.equals("Saler Name"))
                    decisiveAttr = "saler_name";
                if (attr.equals("Saler Scale"))
                    decisiveAttr = "saler_scale";
                if (attr.equals("Category"))
                    decisiveAttr = "category";
                if (attr.equals("Brand"))
                    decisiveAttr = "brand";

                tvDecisiveAttr.setText(getResources().getString(R.string.txt_decisive_attr,attr));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runJ48(decisiveAttr);
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }


        });


    }

    public void runJ48(final String attr) {
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


        //run weka J48 api
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Chi dinh thuoc tinh quyet dinh
                    instances.setClassIndex(instances.attribute(attr).index());
                    final J48 model = new J48();
                    model.buildClassifier(instances);

                    Log.d("xxx", model.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                CustomClipboardManager clipboardManager = new CustomClipboardManager();
                                clipboardManager.copyToClipboard(DecisionTreeActivity.this, model.graph());
                                ToastUtil.showInfo(DecisionTreeActivity.this, "Đã copy cây quyết định vào clipboard");
                                Log.e("graph", model.graph());
                                tvResult.setText(model.toString());
                                ltLoading.setVisibility(View.GONE);
                                svResult.setVisibility(View.VISIBLE);
                                btnDrawTree.setVisibility(View.VISIBLE);
                                byte[] imageByte = createDotGraph(model.graph());
                                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                                imgGraph.setImageBitmap(imageBitmap);


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
        atts.add(new Attribute("percen_sale", (ArrayList<String>) null));
        atts.add(new Attribute("saler_rate", (ArrayList<String>) null));
        atts.add(new Attribute("saler_saleTime", (ArrayList<String>) null));
        atts.add(new Attribute("warranty_time", (ArrayList<String>) null));
        atts.add(new Attribute("product_rate", (ArrayList<String>) null));
        atts.add(new Attribute("brand", (ArrayList<String>) null));
        atts.add(new Attribute("saler_name", (ArrayList<String>) null));
        atts.add(new Attribute("saler_scale", (ArrayList<String>) null));
        atts.add(new Attribute("category", (ArrayList<String>) null));



        instances = new Instances("TestInstances", atts, products.size());
        System.out.println("Before adding any instance");
        System.out.println("--------------------------");
        System.out.println(instances);
        System.out.println("--------------------------");
        for (Product product : products) {
            double[] instanceValue1 = new double[instances.numAttributes()];

            instanceValue1[0] = instances.attribute(0).addStringValue(product.percen_sale);
            instanceValue1[1] = instances.attribute(1).addStringValue(product.saler.saler_rate);
            instanceValue1[2] = instances.attribute(2).addStringValue(product.saler.getSalerSalingTime());
            instanceValue1[3] = instances.attribute(3).addStringValue(product.getWarranty());
            instanceValue1[4] = instances.attribute(4).addStringValue(product.productRating.overall_rate);
            instanceValue1[5] = instances.attribute(5).addStringValue(product.brand);
            instanceValue1[6] = instances.attribute(6).addStringValue(product.saler.saler_name);
            instanceValue1[7] = instances.attribute(7).addStringValue("" + product.saler.saler_scale);
            instanceValue1[8] = instances.attribute(8).addStringValue("" + product.category);


            instances.add(new DenseInstance(1.0, instanceValue1));
        }
    }

    public byte[] createDotGraph(String dotFormat) {
        GraphViz gv = new GraphViz(DecisionTreeActivity.this);
        gv.add(dotFormat);
        // String type = "gif";
        String type = "png";
        // gv.increaseDpi();
        gv.decreaseDpi();
        gv.decreaseDpi();

//        File out = new File(fileName+"."+ type);
//        gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );

        return gv.getGraph(gv.getDotSource(), type);
    }


}

