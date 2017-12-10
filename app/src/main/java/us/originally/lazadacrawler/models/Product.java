package us.originally.lazadacrawler.models;

import java.util.ArrayList;

/**
 * Created by TuanAnh on 12/2/17.
 */

public class Product {
    public String product_url;
    public String name;
    public String brand;
    public String category;
    public String brandUrl;
    public ArrayList<String> detail;
    public ArrayList<String> imageUrls;

    public String current_price;
    public String old_price;
    public String currency;
    public String percen_sale;
    public String installment;

    public String warranty_time;
    public String warranty_type;
    public String warranty_detail;

    public String pay_method;
    public String payback_policy;
    public String payback_subtitle;
    public String payback_detail;

    public String product_included;
    public Saler saler;
    public ProductRating productRating;
    public ArrayList<Commnent> commnent;

    public String saling_location;

    public Product() {
    }

    public Product(String product_url,String name, String brand,String category, String brandUrl, ArrayList<String> detail, ArrayList<String> imageUrls,
                   String current_price, String old_price, String currency, String percen_sale, String installment,
                   String warranty_time, String warranty_type, String warranty_detail, String pay_method, String payback_policy,
                   String payback_subtitle, String payback_detail, String product_included, Saler saler, ProductRating productRating,
                   ArrayList<Commnent> commnent, String saling_location) {
        this.product_url = product_url;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.brandUrl = brandUrl;
        this.detail = detail;
        this.imageUrls = imageUrls;

        this.current_price = current_price;
        this.old_price = old_price;
        this.currency = currency;
        this.percen_sale = percen_sale;
        if(percen_sale.isEmpty())
            this.percen_sale = "0%";
        this.installment = installment;

        this.warranty_time = warranty_time;
        this.warranty_type = warranty_type;
        this.warranty_detail = warranty_detail;

        this.pay_method = pay_method;
        this.payback_policy = payback_policy;
        this.payback_detail = payback_detail;
        this.payback_subtitle = payback_subtitle;

        this.product_included = product_included;

        this.saler = saler;
        this.productRating = productRating;
        this.commnent = commnent;

        this.saling_location = saling_location;
    }

    public String getWarranty() {
        return warranty_time + " " + warranty_type;
    }
}
