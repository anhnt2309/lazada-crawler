package us.originally.lazadacrawler.models;

/**
 * Created by TuanAnh on 12/2/17.
 */

public class Commnent {
    public int start;
    public String by;
    public String title;
    public String detail;
    public String time;
    public String num_of_like;

    public Commnent(int start, String by, String title, String detail, String time, String num_of_like) {
        this.start = start;
        this.by = by;
        this.title = title;
        this.detail = detail;
        this.time = time;
        this.num_of_like = num_of_like;
    }
}
