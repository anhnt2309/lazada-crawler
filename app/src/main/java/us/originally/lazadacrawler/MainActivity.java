package us.originally.lazadacrawler;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import us.originally.lazadacrawler.custom.RippleView;
import us.originally.lazadacrawler.custom.StereoView;
import us.originally.lazadacrawler.utils.LogUtil;
import us.originally.lazadacrawler.utils.ToastUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout crawlingInfo;
    private Button startButton;
    private EditText urlInputView;
    private TextView progressText;

    // WebCrawler object will be used to start crawling on root Url
    private WebCrawler crawler;
    // count variable for url crawled so far
    int crawledUrlCount;
    // state variable to check crawling status
    boolean crawlingRunning;
    // For sending message to Handler in order to stop crawling after 60000 ms
    private static final int MSG_STOP_CRAWLING = 111;
    private static final int CRAWLING_RUNNING_TIME = Integer.MAX_VALUE;

    private EditText etUsername;
    private Button etEmail;
    private RippleView rvUsername;
    private RippleView rvReturn;
    private TextView tvInputtedUrl;
    private TextView tvUrlText;
    private StereoView stereoView;
    private ImageView imgButtonIcon;
    private int translateY;
    private ArrayList<String> detailUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        detailUrls = new ArrayList<>();
        stereoView.setStartScreen(0);
        stereoView.post(new Runnable() {
            @Override
            public void run() {
                int[] location = new int[2];
                stereoView.getLocationOnScreen(location);
                translateY = location[1];
            }
        });
        stereoView.setiStereoListener(new StereoView.IStereoListener() {
            @Override
            public void toPre(int curScreen) {
                LogUtil.m("Pre Screen " + curScreen);
            }

            @Override
            public void toNext(int curScreen) {
                LogUtil.m("cur Screen " + curScreen);
            }
        });

        crawler = new WebCrawler(this, mCallback);
    }

    private void initUI() {
        crawlingInfo = (LinearLayout) findViewById(R.id.crawlingInfo);
        urlInputView = (EditText) findViewById(R.id.webUrl);
        progressText = (TextView) findViewById(R.id.progressText);
        stereoView = (StereoView) findViewById(R.id.stereoView);

        startButton = (Button) findViewById(R.id.start);
        imgButtonIcon = findViewById(R.id.img_button_action);

        rvUsername = (RippleView) findViewById(R.id.rv_username);
        rvReturn = (RippleView) findViewById(R.id.rv_return);
        tvInputtedUrl = findViewById(R.id.tv_inputted_url);
        tvUrlText = findViewById(R.id.tv_url_text);
        tvUrlText.setVisibility(View.INVISIBLE);

        urlInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().isEmpty()) {
                    tvUrlText.setVisibility(View.INVISIBLE);
                }
                tvUrlText.setVisibility(View.VISIBLE);
                String url = resolveUrl(editable.toString(), false);
                tvInputtedUrl.setText(url);
            }
        });
        startButton.setOnClickListener(this);
        rvUsername.setOnClickListener(this);
        rvReturn.setOnClickListener(this);

    }


    /**
     * callback for crawling events
     */
    private WebCrawler.CrawlingCallback mCallback = new WebCrawler.CrawlingCallback() {

        @Override
        public void onPageCrawlingCompleted() {
            crawledUrlCount++;
            progressText.post(new Runnable() {

                @Override
                public void run() {
                    progressText.setText(" Đã crawl được " + crawledUrlCount + " trang!!!");

                }
            });
        }

        @Override
        public void onPageCrawlingFailed(String Url, int errorCode) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onCrawlingCompleted() {
            stopCrawling();
        }

    };


    /**
     * Callback for handling button onclick events
     */
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        final String webUrl = urlInputView.getText().toString();
        switch (viewId) {
            case R.id.start:
                if (TextUtils.isEmpty(webUrl)) {
                    Toast.makeText(getApplicationContext(), "Please input web Url",
                            Toast.LENGTH_SHORT).show();
                    stereoView.toPre();
                } else {
                    crawlingRunning = true;
                    String finalWebUrl = resolveUrl(webUrl, true);
                    if (finalWebUrl.isEmpty()) {
                        crawlingRunning = false;
                        stereoView.toPre();
                        return;
                    }

                    Log.e("Main", finalWebUrl);
                    crawler.startCrawlerTask(finalWebUrl, true, false);
                    startButton.setEnabled(false);
                    imgButtonIcon.setImageResource(android.R.drawable.ic_media_pause);
                    startButton.setText(R.string.txt_is_running);
                    crawlingInfo.setVisibility(View.VISIBLE);
                    // Send delayed message to handler for stopping crawling
                    handler.sendEmptyMessageDelayed(MSG_STOP_CRAWLING,
                            CRAWLING_RUNNING_TIME);
                }
                break;
            case R.id.stop:
                // remove any scheduled messages if user stopped crawling by
                // clicking stop button
                handler.removeMessages(MSG_STOP_CRAWLING);
                stopCrawling();
                break;
            case R.id.rv_username:
                rvUsername.setiRippleAnimListener(new RippleView.IRippleAnimListener() {
                    @Override
                    public void onComplete(View view) {
                        if (TextUtils.isEmpty(webUrl)) {
                            Toast.makeText(getApplicationContext(), "Please input web Url",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        stereoView.toNext();
                    }
                });
                break;
            case R.id.rv_return:
                rvReturn.setiRippleAnimListener(new RippleView.IRippleAnimListener() {
                    @Override
                    public void onComplete(View view) {
                        stereoView.toPre();
                    }
                });
                break;

        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            stopCrawling();
        }
    };

    /**
     * API to handle post crawling events
     */
    private void stopCrawling() {
        if (crawlingRunning) {
            crawler.stopCrawlerTasks();
            crawlingInfo.setVisibility(View.INVISIBLE);
            startButton.setEnabled(true);
            startButton.setVisibility(View.VISIBLE);
            imgButtonIcon.setImageResource(android.R.drawable.ic_media_play);
            startButton.setText(getResources().getString(R.string.txt_start));
            crawlingRunning = false;
            if (crawledUrlCount > 0)
                Toast.makeText(getApplicationContext(),
                        printCrawledEntriesFromDb() + "pages crawled",
                        Toast.LENGTH_SHORT).show();
            detailUrls = getCrawledLinkFromDb();

            crawledUrlCount = 0;
            progressText.setText("");

        }

    }

    /**
     * API to output crawled urls in logcat
     *
     * @return number of rows saved in crawling database
     */
    protected int printCrawledEntriesFromDb() {

        int count = 0;
        CrawlerDB mCrawlerDB = new CrawlerDB(this);
        SQLiteDatabase db = mCrawlerDB.getReadableDatabase();

        Cursor mCursor = db.query(CrawlerDB.TABLE_NAME, null, null, null, null,
                null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            count = mCursor.getCount();
            mCursor.moveToFirst();
            int columnIndex = mCursor
                    .getColumnIndex(CrawlerDB.COLUMNS_NAME.CRAWLED_URL);
            for (int i = 0; i < count - 1; i++) {
                Log.d("AndroidSRC_Crawler",
                        "Crawled Url " + mCursor.getString(columnIndex));
                mCursor.moveToNext();
            }
        }

        return count;
    }

    protected ArrayList<String> getCrawledLinkFromDb() {
        ArrayList<String> returnLinks = new ArrayList<>();
        int count = 0;
        CrawlerDB mCrawlerDB = new CrawlerDB(this);
        SQLiteDatabase db = mCrawlerDB.getReadableDatabase();

        Cursor mCursor = db.query(CrawlerDB.TABLE_NAME, null, null, null, null,
                null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            count = mCursor.getCount();
            mCursor.moveToFirst();
            int columnIndex = mCursor
                    .getColumnIndex(CrawlerDB.COLUMNS_NAME.CRAWLED_URL);
            for (int i = 0; i < count - 1; i++) {
                Log.d("AndroidSRC_Crawler",
                        "Crawled Url " + mCursor.getString(columnIndex));
                returnLinks.add(mCursor.getString(columnIndex));
                mCursor.moveToNext();
            }
        }

        return returnLinks;
    }

    //----------------------------------------------------------------------------------------------
    // Clipboard
    //----------------------------------------------------------------------------------------------

//    private void autoPasteIfValid() {
//        if (this.tvPasteInstructions.getVisibility() == View.VISIBLE)
//            onBtnPasteClicked(false);
//    }
//
//    private void onBtnPasteClicked(boolean userInitiated) {
//        CustomClipboardManager customClipboardManager = new CustomClipboardManager();
//        String value = customClipboardManager.readFromClipboard(this);
//        setCodeInput(value, userInitiated);
//    }
//
//    private void setCodeInput(String value, boolean userInitiated) {
//        if (value.startsWith(CodeInfo.CODE_IR_PREFIX)) {
//            this.tvPasteInstructions.setVisibility(View.GONE);
//            this.grpPronto.setVisibility(View.GONE);
//            this.grpBroadlink.setVisibility(View.VISIBLE);
//            this.tvCodeValueBroadlink.setText(value);
//            return;
//        }
//
//        if (value.startsWith(CodeInfo.CODE_PRONTO_PREFIX)) {
//            //Convert Pronto to Broadlink format
//            String broadlinkCode = CodeFormatUtils.convertCodePronto2Broadlink(value);
//            if (broadlinkCode == null || broadlinkCode.length() < 10) {
//                String errMsg = getString(R.string.invalid_input_code);
//                if (userInitiated)
//                    ToastUtil.showErrorMessageWithSuperToast(this, errMsg, TAG);
//                return;
//            }
//
//            this.tvPasteInstructions.setVisibility(View.GONE);
//            this.grpBroadlink.setVisibility(View.VISIBLE);
//            this.grpPronto.setVisibility(View.VISIBLE);
//            this.tvCodeValuePronto.setText(value);
//            this.tvCodeValueBroadlink.setText(broadlinkCode);
//        }
//    }

    public String resolveUrl(String url, boolean showToast) {
        if (url.startsWith("https://www.lazada.vn") || url.startsWith("http://www.lazada.vn")) {
            return url;
        } else {
            if (url.startsWith("https://www") || url.startsWith("http://www") || url.startsWith("www")
                    || url.contains(".com") || url.contains(".org") || url.contains(".vn")) {
                if (showToast)
                    ToastUtil.showInfo(this, "Chúng tôi chỉ hỗ trợ crawl trang lazada thôi !!!");
                return "";
            }
        }

        url = url.replace(" ", "+");
        String finalUrl = "https://www.lazada.vn/catalog/?q=" + url;
        return finalUrl;

    }


}
