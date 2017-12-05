package us.originally.lazadacrawler.models;

/**
 * Created by TuanAnh on 12/2/17.
 */

public class Saler {
    public String saler_name;
    public String saler_rate;
    public String saler_saleTime;
    public String saler_saleTime_unit;
    public int saler_scale;
    public String saler_url;

    public Saler(String saler_name, String saler_rate, String saler_saleTime, String saler_saleTime_unit, int saler_scale, String saler_url) {
        this.saler_name = saler_name;
        this.saler_rate = saler_rate;
        this.saler_saleTime = saler_saleTime;
        this.saler_saleTime_unit = saler_saleTime_unit;
        this.saler_scale = saler_scale;
        this.saler_url = saler_url;
    }

    public String getSalerSalingTime() {
        return saler_saleTime + " " + saler_saleTime_unit;
    }
}
