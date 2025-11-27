package com.example.cselik_edmond_s2431695;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // UI References
    private Button manualRefresh;
    private Button mainCurrBtn;
    private ListView listView;
    private FrameLayout fragmentLayout;
    private SearchView currencySearch;
    private TextView txtPubDate;

    // Data Variables
    private ArrayList<Currencies> currencies;
    private ArrayList<Currencies> mainCurrencies;
    private HashMap<String, Integer> flags;
    private ListAdapter customAdapter;
    private ListAdapter mainCurrAdapter;

    // Logic & Threading
    private String urlSource = "https://www.fx-exchange.com/gbp/rss.xml";
    private Handler mHandler;
    private Runnable refreshAtInterval;
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final int hour = 3600000; // 1 hour in milliseconds
    private boolean showingMainCurr = false;
    ExecutorService executorService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise Objects
        mHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();
        currencies = new ArrayList<>();
        mainCurrencies = new ArrayList<>();

        // Initialise Views
        listView = findViewById(R.id.currencyList);
        fragmentLayout = findViewById(R.id.fragment_layout);
        txtPubDate = findViewById(R.id.txtPubDate);
        manualRefresh = findViewById(R.id.manualRefresh);
        mainCurrBtn = findViewById(R.id.mainCurrBtn);
        currencySearch = findViewById(R.id.currencySearch);

        // Handle Visibility (Rotation Logic)
        if (savedInstanceState != null) {
            boolean wasVisible = savedInstanceState.getBoolean("IS_FRAGMENT_VISIBLE");
            if (wasVisible) {

                listView.setVisibility(View.GONE);
                manualRefresh.setVisibility(View.GONE);
                mainCurrBtn.setVisibility(View.GONE);
                fragmentLayout.setVisibility(View.VISIBLE);
            } else {
                listView.setVisibility(View.VISIBLE);
                manualRefresh.setVisibility(View.VISIBLE);
                mainCurrBtn.setVisibility(View.VISIBLE);
                fragmentLayout.setVisibility(View.GONE);
            }
        } else {
            // Default start state
            listView.setVisibility(View.VISIBLE);
            fragmentLayout.setVisibility(View.GONE);
        }

        // Setup Data
        setImages();
        startProgress();

        // Setup Listeners
        setupListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save whether the fragment is currently visible
        boolean isFragmentVisible = fragmentLayout.getVisibility() == View.VISIBLE;
        outState.putBoolean("IS_FRAGMENT_VISIBLE", isFragmentVisible);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks by stopping handlers and executors
        refreshHandler.removeCallbacksAndMessages(null);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    // Swaps the List View for the Converter Fragment
    public void replaceFragment(Fragment fragment, Bundle bundle) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, fragment);
        fragmentTransaction.commit();
    }

    // Removes the fragment and restores the List View
    public void closeFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_layout);
        if (fragment != null) {
            fragmentManager.beginTransaction().remove(fragment).commit();
        }
        fragmentLayout.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        manualRefresh.setVisibility(View.VISIBLE);
        mainCurrBtn.setVisibility(View.VISIBLE);
    }


    private void setupListeners() {

        // Manual Refresh Button
        manualRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showingMainCurr = false;
                startProgress();
            }
        });

        // Auto-Refresh Timer
        refreshAtInterval = new Runnable() {
            @Override
            public void run() {
                showingMainCurr = false;
                startProgress();
                refreshHandler.postDelayed(this, hour);
            }
        };
        refreshHandler.postDelayed(refreshAtInterval, hour);

        // Filter
        mainCurrBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mainCurrAdapter = new ListAdapter(getApplicationContext(), mainCurrencies);
                listView.setAdapter(mainCurrAdapter);
                showingMainCurr = true;
            }
        });

        // List Item Click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Currencies selectedCurr = (Currencies) parent.getItemAtPosition(position);
                if (selectedCurr == null) return;

                // Prepare data to pass to fragment
                String code = selectedCurr.getCurrencyCode();
                float rate = selectedCurr.getExchangeRate();

                Bundle bundle = new Bundle();
                bundle.putString("CURRENCY_CODE", code);
                bundle.putFloat("EXCHANGE_RATE", rate);
                bundle.putInt("IMAGE_ID", selectedCurr.getFlagId());

                // Hide list
                listView.setVisibility(View.GONE);
                manualRefresh.setVisibility(View.GONE);
                mainCurrBtn.setVisibility(View.GONE);
                fragmentLayout.setVisibility(View.VISIBLE);

                replaceFragment(new CurrenciesFrag(), bundle);
            }
        });

        // Search View Logic
        currencySearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                ArrayList<Currencies> filteredCurrency = new ArrayList<>();

                // If search is empty, restore full list
                if (newText == null || newText.isEmpty()) {
                    ListAdapter fullAdapter = new ListAdapter(getApplicationContext(), currencies);
                    listView.setAdapter(fullAdapter);
                    return false;
                }

                String query = newText.toLowerCase();

                // Filter loop
                for (Currencies currency : currencies) {
                    String name = currency.getCurrencyName();
                    if (name == null) name = "";

                    String code = currency.getCurrencyCode();
                    if (code == null) code = "";

                    if (name.toLowerCase().contains(query) || code.toLowerCase().contains(query)) {
                        filteredCurrency.add(currency);
                    }
                }
                ListAdapter searchAdapter = new ListAdapter(getApplicationContext(), filteredCurrency);
                listView.setAdapter(searchAdapter);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });
    }

    public void startProgress() {
        executorService.execute(new Task(urlSource));
    }

    private class Task implements Runnable {
        private String url;

        public Task(String aurl) {
            url = aurl;
        }

        @Override
        public void run() {
            String taskResult = "";
            ArrayList<Currencies> tempCurrencies = new ArrayList<>();
            ArrayList<Currencies> tempMainCurrencies = new ArrayList<>();

            URL aurl;
            URLConnection yc;
            BufferedReader in = null;
            String inputLine = "";

            try {
                aurl = new URL(url);
                yc = aurl.openConnection();
                yc.setRequestProperty("User-Agent", "Mozilla/5.0"); // Mimic a browser to avoid 403 errors
                in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    taskResult = taskResult + inputLine;
                }
                in.close();
            } catch (IOException ae) {
                Log.e("MyTask", "Network Error: " + ae.toString());
            }

            // Clean up XML string to prevent parsing errors
            int i = taskResult.indexOf("<?");
            if (i >= 0) taskResult = taskResult.substring(i);
            i = taskResult.indexOf("</rss>");
            if (i >= 0) taskResult = taskResult.substring(0, i + 6);

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(taskResult));

                int eventType = xpp.getEventType();
                Currencies item = null;

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            item = new Currencies();
                        } else if (item != null) {
                            try {
                                // Extract Name and Code from Title Tag
                                if (xpp.getName().equalsIgnoreCase("title")) {
                                    String temp = xpp.nextText();
                                    if (temp.contains("/")) {
                                        String[] parts = temp.split("/");
                                        if (parts.length > 1) {
                                            String targetPart = parts[1]; // e.g., "United States (USD)"
                                            int start = targetPart.lastIndexOf("(") + 1;
                                            int end = targetPart.lastIndexOf(")");

                                            if (start > 0 && end > start) {
                                                String code = targetPart.substring(start, end).trim().toUpperCase();
                                                item.setCurrentCurrencyCode(code);
                                                String name = targetPart.substring(0, start - 1).trim();
                                                item.setCurrencyName(name);

                                                // Assign Flag
                                                if (flags.containsKey(code)) {
                                                    item.setFlagId(flags.get(code));
                                                } else {
                                                    item.setFlagId(0);
                                                }
                                            } else {
                                                // Fallback if format differs
                                                item.setCurrencyName(targetPart);
                                                item.setCurrentCurrencyCode("N/A");
                                                item.setFlagId(0);
                                            }
                                        }
                                    } else {
                                        item.setCurrencyName(temp);
                                        item.setCurrentCurrencyCode("N/A");
                                        item.setFlagId(0);
                                    }
                                }
                                // Extract Rate from Description Tag
                                else if (xpp.getName().equalsIgnoreCase("description")) {
                                    String temp = xpp.nextText();
                                    String[] parts = temp.split(" = ");
                                    if (parts.length > 1) {
                                        String rightSide = parts[1].trim();
                                        String[] numberParts = rightSide.split(" ");
                                        item.setExchangeRate(numberParts[0]);
                                    } else {
                                        item.setExchangeRate(temp);
                                    }
                                }
                                // Extract Date
                                else if (xpp.getName().equalsIgnoreCase("pubDate")) {
                                    item.setPubDate(xpp.nextText());
                                }
                            } catch (Exception e) {
                                Log.e("Parsing", "Skipping bad data field: " + e.getMessage());
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        // End of item
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            if (item != null && item.getExchangeRate() != 0) {
                                String code = item.getCurrencyCode();
                                // Filter specific currencies for the "Main" list
                                if ("USD".equalsIgnoreCase(code) || "EUR".equalsIgnoreCase(code) || "JPY".equalsIgnoreCase(code)) {
                                    tempMainCurrencies.add(item);
                                }
                                tempCurrencies.add(item);
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (Exception e) {
                Log.e("Parsing", "General Parsing Error: " + e.toString());
            }

            mHandler.post(new Runnable() {
                public void run() {
                    currencies.clear();
                    mainCurrencies.clear();

                    currencies.addAll(tempCurrencies);
                    mainCurrencies.addAll(tempMainCurrencies);

                    customAdapter = new ListAdapter(getApplicationContext(), currencies);
                    listView.setAdapter(customAdapter);

                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, d MMM HH:mm:ss", java.util.Locale.getDefault());
                    String currentTime = sdf.format(new java.util.Date());
                    txtPubDate.setText("Last refreshed: " + currentTime);
                }
            });
        }
    }


    // Flags
    public void setImages() {
        flags = new HashMap<>();
        // Maps currency codes to drawable resources
        flags.put("AED", R.drawable.ae); flags.put("AFN", R.drawable.af); flags.put("ALL", R.drawable.al);
        flags.put("AMD", R.drawable.am); flags.put("ANG", R.drawable.cw); flags.put("AOA", R.drawable.ao);
        flags.put("ARS", R.drawable.ar); flags.put("AUD", R.drawable.au); flags.put("AWG", R.drawable.aw);
        flags.put("AZN", R.drawable.az); flags.put("BAM", R.drawable.ba); flags.put("BBD", R.drawable.bb);
        flags.put("BDT", R.drawable.bd); flags.put("BGN", R.drawable.bg); flags.put("BHD", R.drawable.bh);
        flags.put("BIF", R.drawable.bi); flags.put("BMD", R.drawable.bm); flags.put("BND", R.drawable.bn);
        flags.put("BOB", R.drawable.bo); flags.put("BRL", R.drawable.br); flags.put("BSD", R.drawable.bs);
        flags.put("BTN", R.drawable.bt); flags.put("BWP", R.drawable.bw); flags.put("BYN", R.drawable.by);
        flags.put("BYR", R.drawable.by); flags.put("BZD", R.drawable.bz); flags.put("CAD", R.drawable.ca);
        flags.put("CDF", R.drawable.cd); flags.put("CHF", R.drawable.ch); flags.put("CLP", R.drawable.cl);
        flags.put("CNY", R.drawable.cn); flags.put("COP", R.drawable.co); flags.put("CRC", R.drawable.cr);
        flags.put("CUP", R.drawable.cu); flags.put("CVE", R.drawable.cv); flags.put("CZK", R.drawable.cz);
        flags.put("DJF", R.drawable.dj); flags.put("DKK", R.drawable.dk); flags.put("DOP", R.drawable.resource_do);
        flags.put("DZD", R.drawable.dz); flags.put("EGP", R.drawable.eg); flags.put("ERN", R.drawable.er);
        flags.put("EEK", R.drawable.ee); flags.put("ETB", R.drawable.et); flags.put("EUR", R.drawable.eu);
        flags.put("FJD", R.drawable.fj); flags.put("FKP", R.drawable.fk); flags.put("GBP", R.drawable.gb);
        flags.put("GEL", R.drawable.ge); flags.put("GHS", R.drawable.gh); flags.put("GIP", R.drawable.gi);
        flags.put("GMD", R.drawable.gm); flags.put("GNF", R.drawable.gn); flags.put("GTQ", R.drawable.gt);
        flags.put("GYD", R.drawable.gy); flags.put("HKD", R.drawable.hk); flags.put("HNL", R.drawable.hn);
        flags.put("HRK", R.drawable.hr); flags.put("HTG", R.drawable.ht); flags.put("HUF", R.drawable.hu);
        flags.put("IDR", R.drawable.id); flags.put("ILS", R.drawable.il); flags.put("INR", R.drawable.in);
        flags.put("IQD", R.drawable.iq); flags.put("IRR", R.drawable.ir); flags.put("ISK", R.drawable.is);
        flags.put("JMD", R.drawable.jm); flags.put("JOD", R.drawable.jo); flags.put("JPY", R.drawable.jp);
        flags.put("KES", R.drawable.ke); flags.put("KGS", R.drawable.kg); flags.put("KHR", R.drawable.kh);
        flags.put("KMF", R.drawable.km); flags.put("KPW", R.drawable.kp); flags.put("KRW", R.drawable.kr);
        flags.put("KWD", R.drawable.kw); flags.put("KYD", R.drawable.ky); flags.put("KZT", R.drawable.kz);
        flags.put("LAK", R.drawable.la); flags.put("LBP", R.drawable.lb); flags.put("LKR", R.drawable.lk);
        flags.put("LRD", R.drawable.lr); flags.put("LSL", R.drawable.ls); flags.put("LTL", R.drawable.lt);
        flags.put("LVL", R.drawable.lv); flags.put("LYD", R.drawable.ly); flags.put("MAD", R.drawable.ma);
        flags.put("MDL", R.drawable.md); flags.put("MGA", R.drawable.mg); flags.put("MKD", R.drawable.mk);
        flags.put("MMK", R.drawable.mm); flags.put("MNT", R.drawable.mn); flags.put("MOP", R.drawable.mo);
        flags.put("MRO", R.drawable.mr); flags.put("MUR", R.drawable.mu); flags.put("MVR", R.drawable.mv);
        flags.put("MWK", R.drawable.mw); flags.put("MXN", R.drawable.mx); flags.put("MYR", R.drawable.my);
        flags.put("MZN", R.drawable.mz); flags.put("NAD", R.drawable.na); flags.put("NGN", R.drawable.ng);
        flags.put("NIO", R.drawable.ni); flags.put("NOK", R.drawable.no); flags.put("NPR", R.drawable.np);
        flags.put("NZD", R.drawable.nz); flags.put("OMR", R.drawable.om); flags.put("PAB", R.drawable.pa);
        flags.put("PEN", R.drawable.pe); flags.put("PGK", R.drawable.pg); flags.put("PHP", R.drawable.ph);
        flags.put("PKR", R.drawable.pk); flags.put("PLN", R.drawable.pl); flags.put("PYG", R.drawable.py);
        flags.put("QAR", R.drawable.qa); flags.put("RON", R.drawable.ro); flags.put("RSD", R.drawable.rs);
        flags.put("RUB", R.drawable.ru); flags.put("RWF", R.drawable.rw); flags.put("SAR", R.drawable.sa);
        flags.put("SBD", R.drawable.sb); flags.put("SCR", R.drawable.sc); flags.put("SDG", R.drawable.sd);
        flags.put("SEK", R.drawable.se); flags.put("SGD", R.drawable.sg); flags.put("SHP", R.drawable.sh);
        flags.put("SKK", R.drawable.sk); flags.put("SLL", R.drawable.sl); flags.put("SOS", R.drawable.so);
        flags.put("SRD", R.drawable.sr); flags.put("SSP", R.drawable.ss); flags.put("STD", R.drawable.st);
        flags.put("SVC", R.drawable.sv); flags.put("SYP", R.drawable.sy); flags.put("SZL", R.drawable.sz);
        flags.put("THB", R.drawable.th); flags.put("TJS", R.drawable.tj); flags.put("TMT", R.drawable.tm);
        flags.put("TND", R.drawable.tn); flags.put("TOP", R.drawable.to); flags.put("TRY", R.drawable.tr);
        flags.put("TTD", R.drawable.tt); flags.put("TWD", R.drawable.tw); flags.put("TZS", R.drawable.tz);
        flags.put("UAH", R.drawable.ua); flags.put("UGX", R.drawable.ug); flags.put("USD", R.drawable.us);
        flags.put("UYU", R.drawable.uy); flags.put("UZS", R.drawable.uz); flags.put("VEF", R.drawable.ve);
        flags.put("VND", R.drawable.vn); flags.put("VUV", R.drawable.vu); flags.put("WST", R.drawable.ws);
        flags.put("XPF", R.drawable.pf); flags.put("YER", R.drawable.ye); flags.put("ZAR", R.drawable.za);
        flags.put("ZMK", R.drawable.zm); flags.put("ZMW", R.drawable.zm); flags.put("ZWD", R.drawable.zw);
    }
}