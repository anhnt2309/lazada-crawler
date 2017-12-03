package us.originally.lazadacrawler.models;

/**
 * Created by TuanAnh on 12/2/17.
 */

public class ProductRating {
    public String overall_rate;
    public String num_of_rateAndComment;
    public String num5Star;
    public String num4Star;
    public String num3Star;
    public String num2Star;
    public String num1Star;

    public ProductRating(String overall_rate, String num_of_rateAndComment, String num5Star, String num4Star, String num3Star, String num2Star, String num1Star) {
        this.overall_rate = overall_rate;
        this.num_of_rateAndComment = num_of_rateAndComment;
        this.num5Star = num5Star;
        this.num4Star = num4Star;
        this.num3Star = num3Star;
        this.num2Star = num2Star;
        this.num1Star = num1Star;
    }
}
