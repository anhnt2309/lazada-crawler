package us.originally.lazadacrawler;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.DSAKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import us.originally.lazadacrawler.manager.CacheManager;
import us.originally.lazadacrawler.models.Commnent;
import us.originally.lazadacrawler.models.Product;
import us.originally.lazadacrawler.models.ProductRating;
import us.originally.lazadacrawler.models.Saler;

public class WebCrawler {

    /**
     * Interface for crawling callback
     */
    interface CrawlingCallback {
        void onPageCrawlingCompleted(String url);

        void onPageCrawlingFailed(String Url, int errorCode);

        void onCrawlingCompleted();
    }

    private final int SC_OK = 200;
    private Context mContext;
    // SQLiteOpenHelper object for handling crawling database
    private CrawlerDB mCrawlerDB;
    // Set containing already visited URls
    private HashSet<String> crawledURL;

    // Queue for unvisited URL
    BlockingQueue<String> uncrawledURL;

    // For parallel crawling execution using ThreadPoolExecuter
    RunnableManager mManager;
    // Callback interface object to notify UI
    CrawlingCallback callback;

    // For sync of crawled and yet to crawl url lists
    Object lock;

    public WebCrawler(Context ctx, CrawlingCallback callback) {
        this.mContext = ctx;
        this.callback = callback;
        mCrawlerDB = new CrawlerDB(mContext);
        crawledURL = new HashSet<>();
        uncrawledURL = new LinkedBlockingQueue<>();
        lock = new Object();
    }

    /**
     * API to add crawler runnable in ThreadPoolExecutor workQueue
     *
     * @param Url       - Url to crawl
     * @param isRootUrl
     */
    public void startCrawlerTask(String Url, boolean isRootUrl, boolean isDetailCrawl, boolean isCrawlingComplete) {
        // If it's root URl, we clear previous lists and DB table content
        if (isRootUrl) {
            crawledURL.clear();
            uncrawledURL.clear();
            clearDB();
            mManager = new RunnableManager();
        }
        // If ThreadPoolExecuter is not shutting down, add runnable to workQueue
        if (!mManager.isShuttingDown()) {
            CrawlerRunnable mTask = new CrawlerRunnable(callback, Url, isDetailCrawl, isCrawlingComplete);
            mManager.addToCrawlingQueue(mTask);
        }
    }


    /**
     * API to shutdown ThreadPoolExecuter
     */
    public void stopCrawlerTasks() {
//        mManager.cancelAllRunnable();
    }

    /**
     * Runnable task which performs task of crawling and adding encountered URls
     * to crawling list
     */
    private class CrawlerRunnable implements Runnable {

        CrawlingCallback mCallback;
        String mUrl;
        boolean isDetailCrawl;
        boolean isCrawlingComplete;

        public CrawlerRunnable(CrawlingCallback callback, String Url, boolean isDetailCrawl, boolean isCrawlingComplete) {
            this.mCallback = callback;
            this.mUrl = Url;
            this.isDetailCrawl = isDetailCrawl;
            this.isCrawlingComplete = isCrawlingComplete;
        }

        @Override
        public void run() {
            if (isCrawlingComplete) {
                mCallback.onCrawlingCompleted();
                return;
            }

            if (!isDetailCrawl) {
                getLinksSearchPage(mUrl);
            }

            if (isDetailCrawl) {
                getProductInfo(mUrl);
            }


            // Send msg to handler that crawling for this url is finished
            // start more crawling tasks if queue is not empty
            mHandler.sendEmptyMessage(0);

        }

        public void getLinksSearchPage(String url) {
            String pageContent = retreiveHtmlContent(url);

            if (!TextUtils.isEmpty(pageContent.toString())) {
//                insertIntoCrawlerDB(url, pageContent);
                synchronized (lock) {
                    crawledURL.add(url);
                }
                mCallback.onPageCrawlingCompleted(url);
            } else {
                mCallback.onPageCrawlingFailed(url, -1);
            }

            if (!TextUtils.isEmpty(pageContent.toString())) {
                // START
                // JSoup Library used to filter urls from html body
                //get link in 1 search page
                Document doc = Jsoup.parse(pageContent.toString());
                Elements locationDoc = doc.select(".merchandise-list__item");

                for (Element element : locationDoc) {
                    Elements locationUrlDoc = element.select(".merchandise__link");
                    String locationUrl = locationUrlDoc.select("a[href]").attr("href");
                    String location = element.select(".c-m-product-card-location__title").text();
                    CacheManager.saveStringCacheData(locationUrl, location);
                }


                Elements searchProductLinks = doc.select(".merchandise__link");
                Elements links = searchProductLinks.select("a[href]");
                for (Element link : links) {
                    String extractedLink = link.attr("href");
                    if (!TextUtils.isEmpty(extractedLink)) {
                        synchronized (lock) {
                            if (!crawledURL.contains(extractedLink))
                                uncrawledURL.add(extractedLink);
                        }

                    }
                }


                // End JSoup 1 page get more page
                // get link next page
                Elements searchNextPage = doc.select(".page-link--next");
                Elements nextPagelinks = searchNextPage.select("a[href]");
                String nextPagelink = ((nextPagelinks != null && nextPagelinks.size() > 0) ? nextPagelinks.get(0).attr("href") : "");
                if (!nextPagelink.isEmpty()) {
                    getLinksSearchPage(nextPagelink);
                }

            }


        }

        public void getProductInfo(String url) {
            String pageContent = retreiveHtmlContent(url);
            if (!TextUtils.isEmpty(pageContent.toString())) {

                synchronized (lock) {
                    crawledURL.add(url);
                }
                mCallback.onPageCrawlingCompleted(url);
            }
//            else {
//                mCallback.onPageCrawlingFailed(url, -2);
//                return;
//            }


            if (!TextUtils.isEmpty(pageContent.toString())) {
                //TODO: get model array from cache

                //Todo: after get model array start crawl and construc the model then add to array and save back to cache

                Document doc = Jsoup.parse(pageContent.toString());

                //get saler info
                Saler saler = getSaler(doc);
                //get product Rating
                ProductRating productRating = getProductRating(doc);

                String location = CacheManager.getStringCacheData(url);

                Elements categoryDoc = doc.select(".breadcrumb__item-text");
                Elements categoryTextDoc = categoryDoc.select("span[itemprop]");
                String category="";
                if (categoryDoc.size() > 0)
                    category = categoryTextDoc.get(1).text();
                //get ProductDetail
                ArrayList<Commnent> commnents = new ArrayList<>();
                Product product = getProductDetail(doc, saler, productRating, commnents, location, category, url);
                String productString = new Gson().toJson(product);

                insertIntoCrawlerDB(url, productString);

            }


        }

        private Product getProductDetail(Document doc, Saler saler, ProductRating productRating, ArrayList<Commnent> commnents, String location, String category, String url) {
            Elements productDoc = doc.select("#prd-detail-page");
            String productName = productDoc.select("#prod_title").text();

            Elements brandDoc = productDoc.select("#prod_brand");
            Elements brandDeatilDoc = brandDoc.select(".prod_header_brand_action");
            Elements brandDetail = brandDeatilDoc.size() > 0 ? brandDeatilDoc.get(0).select("a[href]") : new Elements();
            String brandName = brandDetail.text();
            String brandUrl = brandDetail.size() > 0 ? brandDetail.get(0).attr("href") : "";

            Elements proContentDoc = productDoc.select(".prod_content");
            Elements proContentArray = proContentDoc.select("li");
            ArrayList<String> producDetails = new ArrayList<>();
            for (Element element : proContentArray) {
                producDetails.add(element.text());
            }
            Elements productImageDoc = productDoc.select(".itm-imageWrapper");
            Elements productImage = productImageDoc.select("img");
            ArrayList<String> producImageUrls = new ArrayList<>();
            for (Element element : productImage) {
                String elementUrl = element.attr("src");
                producImageUrls.add(elementUrl);

            }

            Elements productPriceDoc = doc.select("#product-price-box");
            Elements currenPriceDoc = productPriceDoc.select("#special_price_box");
            String currenPrice = currenPriceDoc.text();
            String currency = productPriceDoc.select("#special_currency_box").text();
            Elements oldPriceDoc = productPriceDoc.select(".price_erase");
            String oldPrice = oldPriceDoc.select("#price_box").text();
            String percenOff = productPriceDoc.select(".price_highlight").text();
            Elements installmentDoc = productPriceDoc.select(".manual_installments_buy");
            String installMent = installmentDoc.select("a").text();

            Elements warrantyDoc = doc.select(".prod-warranty");
            String warrantyTime = warrantyDoc.select(".prod-warranty__term").text();
            String warrantyType = warrantyDoc.select(".prod-warranty__type").text();
            String warrantyDetail = warrantyDoc.select(".warranty-popup__copy").text();


            Elements deliveryDoc = doc.select(".delivery-info");
            String paymentMethod = deliveryDoc.select(".cash-on-delivery__message").text();
            String paybackPolicy = deliveryDoc.select(".popup-tooltips__main-title").size() > 0 ? deliveryDoc.select(".popup-tooltips__main-title").get(0).text() : "";
            String payback_subtitle = deliveryDoc.select(".popup-tooltips__subtitle").text();

            Elements paybackDetailDoc = doc.select(".sellerreturn7dayswithchangeofmind-tooltip-cms");
            String payback_detail = paybackDetailDoc.select(".sellerreturn7dayswithchangeofmind-tooltipcms__description").text();
            String product_included = doc.select(".inbox__item").text();


            Product product = new Product(url, productName, brandName, category, brandUrl, producDetails, producImageUrls,
                    currenPrice, oldPrice, currency, percenOff, installMent, warrantyTime, warrantyType,
                    warrantyDetail, paymentMethod, paybackPolicy, payback_detail, payback_subtitle, product_included,
                    saler, productRating, commnents, location);

            return product;
        }

        private Saler getSaler(Document doc) {
            Elements salerDoc = doc.select(".seller-details");
            Elements salerNameDoc = salerDoc.select(".basic-info__name");
            String salerName = salerNameDoc.text();
            String salerUrl = salerNameDoc.attr("href");

            String salerRate = salerDoc.select(".c-positive-seller-ratings").text();
            String salerTime = salerDoc.select(".c-time-on-lazada__value").text();
            String salerTimeUnit = salerDoc.select(".c-time-on-lazada__unit").text();
            Elements salerScaleDoc = salerDoc.select(".c-seller-size");
            Elements salerSaleNumber = salerScaleDoc.select(".seller-size-icon__bar_painted");
            int salerScale = salerSaleNumber.size();
            Saler saler = new Saler(salerName, salerRate, salerTime, salerTimeUnit, salerScale, salerUrl);
            return saler;
        }

        private ProductRating getProductRating(Document doc) {
            Elements ratingDoc = doc.select(".c-rating-total");
            Elements overallRatingDoc = ratingDoc.select(".c-rating-total__text-rating-average");
            String overallRating = overallRatingDoc.text();

            Elements ratingAndCommentDoc = ratingDoc.select(".c-rating-total__text-total-review");
            String numOfRateAndCommnent = ratingAndCommentDoc.text();
            String num5star = "";
            String num4star = "";
            String num3star = "";
            String num2star = "";
            String num1star = "";


            Elements ratingCount = ratingDoc.select(".c-rating-bar-list__count");
            for (int i = 0; i < ratingCount.size(); i++) {
                if (i == 0)
                    num5star = ratingCount.get(i).text();
                if (i == 1)
                    num4star = ratingCount.get(i).text();
                if (i == 2)
                    num3star = ratingCount.get(i).text();
                if (i == 3)
                    num2star = ratingCount.get(i).text();
                if (i == 4)
                    num1star = ratingCount.get(i).text();
            }
            ProductRating productRating = new ProductRating(overallRating, numOfRateAndCommnent,
                    num5star, num4star, num3star, num2star, num1star);
            return productRating;

        }


        private String retreiveHtmlContent(String Url) {
            URL httpUrl = null;
            HttpURLConnection conn = null;
            try {
                httpUrl = new URL(Url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            int responseCode = SC_OK;
            StringBuilder pageContent = new StringBuilder();
            try {
                if (httpUrl != null) {

                    conn = (HttpURLConnection) httpUrl
                            .openConnection();
                    conn.setConnectTimeout(Integer.MAX_VALUE);
                    conn.setReadTimeout(25000);
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);


                    responseCode = conn.getResponseCode();
                    if (responseCode != SC_OK) {
                        throw new IllegalAccessException(
                                " http connection failed");
                    }
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        pageContent.append(line);
                    }
                    br.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
                mCallback.onPageCrawlingFailed(Url, -3);
                return "";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                mCallback.onPageCrawlingFailed(Url, responseCode);
                return "";
            } finally {
                if (conn != null)
                    conn.disconnect();
            }

            return pageContent.toString();
        }

    }


    /**
     * API to clear previous content of crawler DB table
     */
    public void clearDB() {
        SQLiteDatabase db = mCrawlerDB.getWritableDatabase();
        try {
            db.delete(CrawlerDB.TABLE_NAME, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    /**
     * API to insert crawled url info in database
     *
     * @param mUrl   - crawled url
     * @param result - html body content of url
     */
    public void insertIntoCrawlerDB(String mUrl, String result) {

        if (TextUtils.isEmpty(result))
            return;

        SQLiteDatabase db = mCrawlerDB.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(CrawlerDB.COLUMNS_NAME.CRAWLED_URL, mUrl);
            values.put(CrawlerDB.COLUMNS_NAME.CRAWLED_PAGE_CONTENT, result);
            db.insert(CrawlerDB.TABLE_NAME, null, values);
        } finally {

        }
    }

    /**
     * To manage Messages in a Thread
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {

            synchronized (lock) {
                if (uncrawledURL != null && uncrawledURL.size() > 0) {
                    int availableTasks = mManager.getUnusedPoolSize();
                    while (availableTasks > 0 && !uncrawledURL.isEmpty()) {
                        startCrawlerTask(uncrawledURL.remove(), false, true, false);
                        availableTasks--;
                    }
                    if (uncrawledURL.size() == 0)
                        startCrawlerTask(null, false, false, true);

                }
            }

        }

        ;
    };


    /**
     * Helper class to interact with ThreadPoolExecutor for adding and removing
     * runnable in workQueue
     */
    private class RunnableManager {

        // Sets the amount of time an idle thread will wait for a task before
        // terminating
        private static final int KEEP_ALIVE_TIME = 1;

        // Sets the Time Unit to seconds
        private final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Sets the initial threadpool size to 5
        private static final int CORE_POOL_SIZE = 5;

        // Sets the maximum threadpool size to 8
        private static final int MAXIMUM_POOL_SIZE = 8;

        // A queue of Runnables for crawling url
        private final BlockingQueue<Runnable> mCrawlingQueue;

        // A managed pool of background crawling threads
        private final ThreadPoolExecutor mCrawlingThreadPool;

        public RunnableManager() {
            mCrawlingQueue = new LinkedBlockingQueue<>();
            mCrawlingThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE,
                    MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                    mCrawlingQueue);
        }

        private void addToCrawlingQueue(Runnable runnable) {
            mCrawlingThreadPool.execute(runnable);
        }

        private void cancelAllRunnable() {
            mCrawlingThreadPool.shutdownNow();
        }

        private int getUnusedPoolSize() {
            return MAXIMUM_POOL_SIZE - mCrawlingThreadPool.getActiveCount();
        }

        private boolean isShuttingDown() {
            return mCrawlingThreadPool.isShutdown()
                    || mCrawlingThreadPool.isTerminating();
        }

    }

}
