package com.sirseni.simpleandroidwebviewexample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.ApplicationController;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import org.json.JSONObject;

public class MainActivity extends Activity implements BeaconConsumer {

    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;
    HashMap beaconSums = new HashMap<String, Integer>();
    int beaconSent=0;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    public static final String SERVICE_TYPE = "_workstation._tcp.";
    public String mServiceName = "SmarterHome";

    NsdServiceInfo mService;
    private Handler mUpdateHandler;


    String SmarterHomeIP;

    String phone;

    private boolean isPageLoadedComplete = false; //declare at class level
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public MainActivity() throws JSONException {
    }


    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();
        initializeResolveListener();
        discoverServices();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.bind(this);


        final WebView myWebView = (WebView) findViewById(R.id.myWebView);

        new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long l) {
                System.out.println("tick");
            }

            public void onFinish() {
                if (isPageLoadedComplete) {
                } else {
                    myWebView.loadUrl("file:///android_asset/errorpage.html");
                    setContentView(myWebView);
                }
            }
        }.start();

        Timer myTimer = new Timer();
        //Start this timer when you create you task
        myWebView.loadUrl(SmarterHomeIP);
        myWebView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                view.loadUrl("file:///android_asset/errorpage.html");
                setContentView(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                isPageLoadedComplete = true;
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                phone = tm.getLine1Number();
                myWebView.loadUrl("javascript: localStorage.setItem(\"phonenumber\", " + phone + ");");
            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    boolean calibrationMode=false;
    int calibratingBeacon;
    String rssiList = "'[";
    int calibrationCounter=0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    @JavascriptInterface
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
                    Log.i(TAG, "RSSI: "+beacons.iterator().next().getRssi());
                        final String URL = SmarterHomeIP+"/php/notify.php";
                        // Post params to be sent to the server
                    try {
                        JSONArray jsonArr = new JSONArray();
                        String beaconIdArray = "'[";

                        for (Beacon beacon : beacons) {
                            JSONObject JSONbeacons = new JSONObject();
                            beaconIdArray+=Integer.parseInt(beacon.getId2().toString().substring(2), 16)+",";
                            JSONbeacons.put("beaconID", Integer.parseInt(beacon.getId2().toString().substring(2), 16));
                            JSONbeacons.put("rssi", beacon.getRssi());
                            jsonArr.put(JSONbeacons);
                            if(calibrationMode && calibratingBeacon==Integer.parseInt(beacon.getId2().toString().substring(2), 16)){
                                rssiList+=beacon.getRssi()+",";
                                calibrationCounter++;
                            }
                        }
                        final String beaconArrayToSend = beaconIdArray.substring(0,beaconIdArray.length()-1)+"]'";
                        Log.i("SENDINGBEACONIDTOPHONE",beaconArrayToSend);
                        final WebView myWebView = (WebView) findViewById(R.id.myWebView);
                        myWebView.post( new Runnable(){
                            @SuppressLint("JavascriptInterface")
                            @Override
                            public void run(){
                                myWebView.loadUrl("javascript: localStorage.setItem(\"beaconList\", " + beaconArrayToSend + ");");
                                myWebView.evaluateJavascript(
                                        "(function() { return localStorage.getItem(\"calibration\"); })();",
                                        new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String html) {
                                                Log.d("HTML", html);
                                                if(html.contains("true")){
                                                    calibrationMode=true;
                                                    Log.d("HTML",html.split("-")[1].substring(0,html.split("-")[1].length()-1));
                                                    calibratingBeacon=Integer.parseInt(html.split("-")[1].substring(0,html.split("-")[1].length()-1));
                                                    if(calibrationCounter>10){
                                                        final String rssiVals = rssiList.substring(0,rssiList.length()-1)+"]'";
                                                        calibrationCounter=0;
                                                        myWebView.loadUrl("javascript: localStorage.setItem(\"calibration\", \"false\");");
                                                        calibrationMode=false;
                                                        calibratingBeacon=-1;
                                                        myWebView.loadUrl("javascript: localStorage.setItem(\"rssiVals\","+rssiVals+");");
                                                    }
                                                }
                                            }
                                        });

                            }
                        });
                        JSONObject rangesJSON = new JSONObject();

                        rangesJSON.put("beacons", jsonArr);
                        String ranges = rangesJSON.toString();
                        Log.i(TAG, ranges);
                        JSONObject jsonSend = new JSONObject();
                        jsonSend.put("phoneNumber",phone);
                        jsonSend.put("ranges", rangesJSON);
                        JsonObjectRequest req = new JsonObjectRequest(URL, jsonSend,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            VolleyLog.v("TAG", response.toString(4));
                                            Log.i(TAG, "RESPONSE\n");
                                            Log.i(TAG,response.toString(4));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                VolleyLog.e("Error: ", error.getMessage());
                            }
                        });

                        ApplicationController.getInstance().addToRequestQueue(req);
                    } catch(JSONException ex) {
                        ex.printStackTrace();
                    }
                } else{
                    Log.i(TAG, "No beacons visible!!!!!!!!!!!!!!!!!!!!");
                    Log.i(TAG, beacons.toString());
                    Log.i(TAG, region.toString());
                }
            }
        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d("NSDMANAGER", "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d("NSDMANAGER", "Service discovery success" + service);
                if (service.getServiceName().contains("SmarterHome")){
                    mNsdManager.resolveService(service, mResolveListener);
                    Log.d("NSDMANAGER", "Smarter Home Found!: " + service);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e("NSDMANAGER", "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i("NSDMANAGER", "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("NSDMANAGER", "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("NSDMANAGER", "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e("NSDMANAGER", "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e("NSDMANAGER", "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().contains("SmarterHome")) {
                    Log.d("NSDMANAGER", "Same IP.");

                    InetAddress rPi = serviceInfo.getHost();
                    SmarterHomeIP = "http://"+rPi.getHostName();
                    Log.d("NSDMANAGER",SmarterHomeIP);
                    return;
                }
                mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();
                Log.d("NSDMANAGER",host.toString());
            }
        };
    }

    // Use When the user clicks a link from a web page in your WebView
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().equals(SmarterHomeIP)) {
                return false;

            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }
}
