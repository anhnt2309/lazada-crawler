package us.originally.lazadacrawler.activities;

import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.furture.react.DuktapeEngine;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import us.originally.lazadacrawler.MainActivity;
import us.originally.lazadacrawler.R;
import us.originally.lazadacrawler.manager.CustomClipboardManager;
import us.originally.lazadacrawler.models.GraphViz;
import us.originally.lazadacrawler.models.Product;
import us.originally.lazadacrawler.models.js.AssetScript;
import us.originally.lazadacrawler.utils.ToastUtil;
import weka.associations.Apriori;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.gui.visualize.plugins.GraphVizPanel;
import weka.gui.visualize.plugins.GraphVizTreeVisualization;


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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Decision Tree Result");
        setContentView(R.layout.activity_algorithm);
        tvResult = findViewById(R.id.tv_algorithm_result);
        ltLoading = findViewById(R.id.animation_view);
        ltLoading.setVisibility(View.VISIBLE);
        imgGraph = findViewById(R.id.tv_algorithm_graph);

        new Thread(new Runnable() {
            @Override
            public void run() {
                runJ48();
            }
        }).start();
    }

    public void runJ48() {

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
                    instances.setClassIndex(3);
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

                                byte[] imageByte = createDotGraph(model.graph());
                                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                                imgGraph.setImageBitmap(imageBitmap);


                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
//                    for (AssociationRule associationRule : model.getAssociationRules().getRules()) {
//                        Log.e("Rules", associationRule.getPremise().toString());
//                    }
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
//        ArrayList<String> classVal = new ArrayList<String>();
//        classVal.add("A");
//        classVal.add("B");
        atts.add(new Attribute("installment", (ArrayList<String>) null));
        atts.add(new Attribute("current_price", (ArrayList<String>) null));
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

            instanceValue1[0] = instances.attribute(0).addStringValue(product.installment);
            instanceValue1[1] = instances.attribute(1).addStringValue(product.current_price);
            instanceValue1[2] = instances.attribute(2).addStringValue(product.percen_sale);
            instanceValue1[3] = instances.attribute(3).addStringValue(product.saler.saler_rate);
            instanceValue1[4] = instances.attribute(4).addStringValue(product.saler.getSalerSalingTime());
            instanceValue1[5] = instances.attribute(5).addStringValue(product.getWarranty());
            instanceValue1[6] = instances.attribute(6).addStringValue(product.productRating.overall_rate);
            instanceValue1[7] = instances.attribute(7).addStringValue(product.brand);
            instanceValue1[8] = instances.attribute(8).addStringValue(product.saler.saler_name);
            instanceValue1[9] = instances.attribute(9).addStringValue("" + product.saler.saler_scale);
            instanceValue1[10] = instances.attribute(10).addStringValue("" + product.category);

            instances.add(new DenseInstance(1.0, instanceValue1));
        }

        System.out.println("After adding any instance");
        System.out.println("--------------------------");
        System.out.println(instances);
        System.out.println("--------------------------");
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

