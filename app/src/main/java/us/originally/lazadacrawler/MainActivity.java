package us.originally.lazadacrawler;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ebanx.swipebtn.OnActiveListener;
import com.ebanx.swipebtn.SwipeButton;
import com.google.gson.Gson;

import java.util.ArrayList;

import us.originally.lazadacrawler.activities.AlgorithmActivity;
import us.originally.lazadacrawler.activities.CrawlResultActivity;
import us.originally.lazadacrawler.activities.DecisionTreeActivity;
import us.originally.lazadacrawler.custom.RippleView;
import us.originally.lazadacrawler.custom.StereoView;
import us.originally.lazadacrawler.manager.CustomClipboardManager;
import us.originally.lazadacrawler.models.Product;
import us.originally.lazadacrawler.utils.LogUtil;
import us.originally.lazadacrawler.utils.ToastUtil;
import weka.core.Instances;

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
    private ImageView imgClearUrl;
    private LinearLayout btnPaste;
    private RippleView rvPaste;
    private SwipeButton btnRun;
    private SwipeButton btnRunDecision;
    private Button btnData;

    private int translateY;
    private ArrayList<String> detailUrls;

    private Instances instances;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<String> links = getCrawledLinkFromDb(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnRun.setText(getResources().getString(R.string.run_apriori, links.size()));
                        btnRunDecision.setText(getResources().getString(R.string.run_decision, links.size()));
                    }
                });
            }
        }).start();
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
                LogUtil.m("Cur Screen " + curScreen);
            }
        });

        crawler = new WebCrawler(this, mCallback);
        autoPasteIfValid();
    }

    private void initUI() {
        crawlingInfo = (LinearLayout) findViewById(R.id.crawlingInfo);
        urlInputView = (EditText) findViewById(R.id.webUrl);
        progressText = (TextView) findViewById(R.id.progressText);
        stereoView = (StereoView) findViewById(R.id.stereoView);

        startButton = (Button) findViewById(R.id.start);
        imgButtonIcon = findViewById(R.id.img_button_action);
        imgClearUrl = findViewById(R.id.img_url);
        btnPaste = findViewById(R.id.btnPaste);
        rvPaste = findViewById(R.id.rv_paste);

        rvUsername = (RippleView) findViewById(R.id.rv_username);
        rvReturn = (RippleView) findViewById(R.id.rv_return);
        tvInputtedUrl = findViewById(R.id.tv_inputted_url);
        tvUrlText = findViewById(R.id.tv_url_text);
        tvUrlText.setVisibility(View.INVISIBLE);

        btnData = findViewById(R.id.btn_view_data);
        btnData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CrawlResultActivity.class));
            }
        });

        btnRun = findViewById(R.id.btn_run);
        btnRun.setOnActiveListener(new OnActiveListener() {
            @Override
            public void onActive() {
                startActivity(new Intent(MainActivity.this, AlgorithmActivity.class));
            }
        });

        btnRunDecision = findViewById(R.id.btn_run_decision);
        btnRunDecision.setOnActiveListener(new OnActiveListener() {
            @Override
            public void onActive() {
                startActivity(new Intent(MainActivity.this, DecisionTreeActivity.class));
            }
        });

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
                    tvInputtedUrl.setVisibility(View.INVISIBLE);
                    return;
                }
                tvUrlText.setVisibility(View.VISIBLE);
                tvInputtedUrl.setVisibility(View.VISIBLE);
                String url = resolveUrl(editable.toString(), false);
                tvInputtedUrl.setText(url);
            }
        });
        startButton.setOnClickListener(this);
        rvUsername.setOnClickListener(this);
        rvReturn.setOnClickListener(this);
        imgClearUrl.setOnClickListener(this);
        rvPaste.setOnClickListener(this);
        btnPaste.setOnClickListener(this);
        imgButtonIcon.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        autoPasteIfValid();
//        btnRun.setHasActivationState(false);
    }

    /**
     * callback for crawling events
     */
    private WebCrawler.CrawlingCallback mCallback = new WebCrawler.CrawlingCallback() {

        @Override
        public void onPageCrawlingCompleted(String url) {
            crawledUrlCount++;
            progressText.post(new Runnable() {

                @Override
                public void run() {
                    progressText.setText(" Đã crawl được " + crawledUrlCount + " trang!!!");
                }
            });
            Log.e("Crawled URl", url);
        }

        @Override
        public void onPageCrawlingFailed(final String Url, final int errorCode) {
            // TODO Auto-generated method stub
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtil.showInfo(MainActivity.this, "Crawling is failed with this url " + Url + " with response code: " + errorCode);
                    Log.e("Crawling failed ", "Crawling is failed with this url " + Url + " with response code: " + errorCode);
                }
            });
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
            case R.id.img_button_action:
                startButton.callOnClick();
                break;
            case R.id.btnPaste:
                rvPaste.callOnClick();

                break;
            case R.id.rv_paste:
                rvPaste.setiRippleAnimListener(new RippleView.IRippleAnimListener() {
                    @Override
                    public void onComplete(View view) {
                        onBtnPasteClicked(true);
                    }
                });
                break;
            case R.id.img_url:
                urlInputView.setText("");
                break;
            case R.id.start:
                if (TextUtils.isEmpty(webUrl)) {
                    Toast.makeText(getApplicationContext(), R.string.txt_url_cannot_be_null,
                            Toast.LENGTH_SHORT).show();
                    stereoView.toNext();
                } else {
                    crawlingRunning = true;
                    String finalWebUrl = resolveUrl(webUrl, true);
                    if (finalWebUrl.isEmpty()) {
                        crawlingRunning = false;
                        stereoView.toNext();
                        return;
                    }

                    Log.e("Main", finalWebUrl);
                    crawler.startCrawlerTask(finalWebUrl, true, false, false);

                    //btn run logic
                    btnRunDisabled();

                    startButton.setEnabled(false);
                    imgButtonIcon.setImageResource(android.R.drawable.ic_media_pause);
                    startButton.setText(R.string.txt_is_running);
                    crawlingInfo.setVisibility(View.VISIBLE);
                    crawlingInfo.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
                    // Send delayed message to handler for stopping crawling
                    handler.sendEmptyMessageDelayed(MSG_STOP_CRAWLING,
                            CRAWLING_RUNNING_TIME);
                }
                break;
            case R.id.stop:
                new MaterialDialog.Builder(this)
                        .title(R.string.txt_stop)
                        .content(R.string.cancel_promt)
                        .positiveText(R.string.agree)
                        .negativeText(R.string.cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                // remove any scheduled messages if user stopped crawling by
                                // clicking stop button
                                handler.removeMessages(MSG_STOP_CRAWLING);
                                stopCrawling();
                                dialog.dismiss();
                            }
                        })
                        .positiveColor(getResources().getColor(R.color.colorPrimaryDark))
                        .negativeColor(getResources().getColor(R.color.colorPrimaryDark))
                        .show();

                break;
            case R.id.rv_username:
                rvUsername.setiRippleAnimListener(new RippleView.IRippleAnimListener() {
                    @Override
                    public void onComplete(View view) {
                        if (TextUtils.isEmpty(webUrl)) {
                            Toast.makeText(getApplicationContext(), R.string.txt_url_cannot_be_null,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        stereoView.toPre();
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

    public void btnRunDisabled() {
        btnRun.setText(getResources().getString(R.string.run_apriori, 0));
        btnRun.setEnabled(false);
        btnRun.setButtonBackground(getResources().getDrawable(R.drawable.shape_button_disable));
        btnRun.setDisabledDrawable(getResources().getDrawable(R.drawable.ic_disable));
        btnRun.setEnabledDrawable(getResources().getDrawable(R.drawable.ic_disable));


        btnRunDecision.setText(getResources().getString(R.string.run_apriori, 0));
        btnRunDecision.setEnabled(false);
        btnRunDecision.setButtonBackground(getResources().getDrawable(R.drawable.shape_button_disable));
        btnRunDecision.setDisabledDrawable(getResources().getDrawable(R.drawable.ic_disable));
        btnRunDecision.setEnabledDrawable(getResources().getDrawable(R.drawable.ic_disable));
    }

    public void btnRunEnabled() {
        btnRun.setEnabled(true);
        btnRun.setButtonBackground(getResources().getDrawable(R.drawable.shape_button));
        btnRun.setDisabledDrawable(getResources().getDrawable(R.drawable.ic_code));
        btnRun.setText(getResources().getString(R.string.run_apriori, detailUrls.size()));
        btnRun.setEnabledDrawable(getResources().getDrawable(R.drawable.ic_done));

        btnRunDecision.setEnabled(true);
        btnRunDecision.setButtonBackground(getResources().getDrawable(R.drawable.shape_button));
        btnRunDecision.setDisabledDrawable(getResources().getDrawable(R.drawable.ic_code));
        btnRunDecision.setText(getResources().getString(R.string.run_apriori, detailUrls.size()));
        btnRunDecision.setEnabledDrawable(getResources().getDrawable(R.drawable.ic_done));
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    startButton.setEnabled(true);
                    startButton.setVisibility(View.VISIBLE);
                    imgButtonIcon.setImageResource(android.R.drawable.ic_media_play);
                    startButton.setText(getResources().getString(R.string.txt_start));
                    startButton.setTextColor(getResources().getColor(android.R.color.black));
                    crawlingInfo.setVisibility(View.GONE);
                    crawlingInfo.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_out));
                }
            });

            crawlingRunning = false;


            if (crawledUrlCount > 0)
                printCrawledEntriesFromDb();

            detailUrls = getCrawledLinkFromDb(this);
            ArrayList<Product> products = new ArrayList<>();
            for (String model : detailUrls) {
                Product product = new Gson().fromJson(model, Product.class);
                products.add(product);
            }

            Log.e("x", "" + products.size());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnRunEnabled();
                }
            });

        }

    }

    /**
     * API to output crawled urls in logcat
     *
     * @return number of rows saved in crawling database
     */
    protected void printCrawledEntriesFromDb() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;

                CrawlerDB mCrawlerDB = new CrawlerDB(MainActivity.this);
                SQLiteDatabase db = mCrawlerDB.getReadableDatabase();

                Cursor mCursor = db.query(CrawlerDB.TABLE_NAME, null, null, null, null,
                        null, null);
                if (mCursor != null && mCursor.getCount() > 0) {
                    count = mCursor.getCount();
                    mCursor.moveToFirst();
                    int columnIndex = mCursor
                            .getColumnIndex(CrawlerDB.COLUMNS_NAME.CRAWLED_URL);
                    try {
                        for (int i = 0; i < count - 1; i++) {
                            Log.d("AndroidSRC_Crawler",
                                    "Crawled Url " + mCursor.getString(columnIndex));
                            mCursor.moveToNext();
                        }
                    } finally {
                        mCursor.close();
                        db.close();
                    }
                }
                Message message = new Message();
                message.what = count;

                mCountFromDbHandler.sendMessage(message);
            }
        }).start();


    }


    Handler mCountFromDbHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int count = msg.what;

            Toast.makeText(MainActivity.this,
                    "Đã lưu " + count + " trang",
                    Toast.LENGTH_SHORT).show();
            crawledUrlCount = 0;
            progressText.setText("");

        }
    };


    public static ArrayList<String> getCrawledLinkFromDb(Context context) {
        ArrayList<String> returnLinks = new ArrayList<>();
        int count = 0;
        CrawlerDB mCrawlerDB = new CrawlerDB(context);
        SQLiteDatabase db = mCrawlerDB.getReadableDatabase();

        Cursor mCursor = db.query(CrawlerDB.TABLE_NAME, null, null, null, null,
                null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            count = mCursor.getCount();
            mCursor.moveToFirst();
            int columnIndex = mCursor
                    .getColumnIndex(CrawlerDB.COLUMNS_NAME.CRAWLED_URL);
            int productIndex = mCursor.getColumnIndex(CrawlerDB.COLUMNS_NAME.CRAWLED_PAGE_CONTENT);
            try {
                for (int i = 0; i < count - 1; i++) {
                    Log.d("AndroidSRC_Crawler",
                            "Crawled Url " + mCursor.getString(columnIndex));
                    returnLinks.add(mCursor.getString(productIndex));

                    mCursor.moveToNext();
                }
            } finally {
                mCursor.close();
                db.close();
            }
        }

        return returnLinks;
    }

    //----------------------------------------------------------------------------------------------
    // Clipboard
    //----------------------------------------------------------------------------------------------

    private void autoPasteIfValid() {
        CustomClipboardManager customClipboardManager = new CustomClipboardManager();
        String value = customClipboardManager.readFromClipboard(this);
        if (value.startsWith("https://www.lazada.vn") || value.startsWith("http://www.lazada.vn") || value.contains("lazada")) {
            onBtnPasteClicked(false);
        }
    }

    private void onBtnPasteClicked(boolean userInitiated) {
        CustomClipboardManager customClipboardManager = new CustomClipboardManager();
        String value = customClipboardManager.readFromClipboard(this);
        setCodeInput(value, userInitiated);
    }

    private void setCodeInput(String value, boolean userInitiated) {
        if (urlInputView.getText().toString().isEmpty()) {
            stereoView.toNext();
        }
        urlInputView.setText(value);

    }

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
