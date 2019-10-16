package com.e.drinkstation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.WriterException;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import static com.e.drinkstation.R.id.textView;
import static com.e.drinkstation.R.id.todayimgv1;

public class MainActivity extends AppCompatActivity {

    public MqttAndroidClient client;

    public IMqttMessageListener iMqttMessageListener;

    public IMqttToken iMqttToken;

    public  String pustr;

    public  JSONObject jsoninfo;

    public TextToSpeech textToSpeech;

    private BluetoothDevice esp32_ble;

    private BluetoothLeScanner scanner;

    private BluetoothAdapter bluetoothAdapter;

    private boolean mScanning;

    private final static String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";

    private MapView mapView;

    private RequestQueue requestQueue;

    public  String MID = "DC07AH001";

    public  String watercustom = "0";

    public  String watertype = "";

    public  String watercup = "2";

    public  String watersy ="000000";

    public  String price = "1.25";



    private ScanCallback scanCallback = new ScanCallback() {

        private ScanCallback scanCallbackend = new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (esp32_ble == null) {
                esp32_ble = result.getDevice();
                Log.d("aha", "esp32_ble is " + esp32_ble.getName());

/*
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speakhello("Hi hunry finded " + esp32_ble.getName() + ". Can i help you?");
                    }
                });
*/
                MID = esp32_ble.getName();
                String sayname = "henry";
                try {
                    if(jsoninfo.getString("name") != null) {
                        sayname = jsoninfo.getString("name");
                        speakhello("Hi " + sayname  + " finded " + MID + ". Can i help you?");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }



            } else {
                if (mScanning)
                    scanner.stopScan(scanCallbackend);
                mScanning = false;
            }
        }
    };


    private void scanLeDevice() {

        Log.d("aha", "to scanLeDevice: ");


        scanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).build();
        filters.add(filter);
        mScanning = true;
        scanner.startScan(filters, settings, scanCallback);
    }


    public void mqttpub(String s) throws MqttException {
        MqttMessage message = new MqttMessage(s.getBytes());
        if (client.isConnected()) {
            client.publish("DrankStation_setwater", message);
        }else {
            client.connect();
            client.publish("DrankStation_setwater", message);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveinfo("{\"name\":\"henry\"}");
        Log.d("aha", "save on ");
        String theinfo1 = loadinfo();

        try {
            JSONObject jobj = new JSONObject(theinfo1);
            Log.d("aha", "name is " + jobj.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            jsoninfo = new JSONObject(loadinfo());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        requestQueue = Volley.newRequestQueue(this);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("aha", "ble admin not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 101);
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("aha", "access coarse location not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
        }else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {

            Log.d("aha", "modify audio setting not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS}, 103);
        }

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS)
                {
                   int result = textToSpeech.setLanguage(Locale.US);
                   if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                   {
                       Log.d("aha","lang not supported");
                   }
                }else {
                    Log.d("aha","texttospeech init failed");
                }
            }
        });

        showpage1();



        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(getApplication(), "tcp://94.191.14.111:2000", clientId);

        try {
            client.connect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


        IMqttActionListener actionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("aha", "onSuccess: connected mqtt server");
                try {
                    //client.subscribe("dw/demo", 0, iMqttMessageListener);
                    iMqttToken = client.subscribe("DrankStation_setwater", 0, iMqttMessageListener);

                } catch (MqttException e) {
                    //e.printStackTrace();
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("aha", "onFailure: " + exception.toString());
            }
        };

        iMqttMessageListener = new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("aha", "messageArrived: " + message);

                final String s1 = message.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView aha = (TextView)findViewById(R.id.aha);
                        aha.setText(s1);
                    }
                });
            }
        };

        try {
            client.connect().setActionCallback(actionListener);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        scanLeDevice();
    }

    public void topay() throws MqttException {

        SeekBar seek1 = (SeekBar) findViewById(R.id.seekBar1);
        SeekBar seek2 = (SeekBar) findViewById(R.id.seekBar2);
        SeekBar seek3 = (SeekBar) findViewById(R.id.seekBar3);
        SeekBar seek4 = (SeekBar) findViewById(R.id.seekBar4);
        SeekBar seek5 = (SeekBar) findViewById(R.id.seekBar5);
        SeekBar seek6 = (SeekBar) findViewById(R.id.seekBar6);

        int syrup1 =  seek1.getProgress();
        int syrup2 =  seek2.getProgress();
        int syrup3 =  seek3.getProgress();
        int syrup4 =  seek4.getProgress();
        int syrup5 =  seek5.getProgress();
        int syrup6 =  seek6.getProgress();

        watertype = "SP";
        watersy = Integer.toString(syrup1) + Integer.toString(syrup2) + Integer.toString(syrup3) +Integer.toString(syrup4) + Integer.toString(syrup5) + Integer.toString(syrup6);

        //pustr = "QDRIN_SUCC:" + Integer.toString(syrup1) + "_" + Integer.toString(syrup2) + "_"  + Integer.toString(syrup3) + "_" + Integer.toString(syrup4) + "_" + Integer.toString(syrup5) + "_" + Integer.toString(syrup6);
        //mqttpub(Integer.toString(syrup1) + "_" + Integer.toString(syrup2) + "_"  + Integer.toString(syrup3) + "_" + Integer.toString(syrup4) );

        showcupchoose();

        //makeQR("ok");
        //QREEncoder qreEncoder = new

    }



    public void topay2() {

        SeekBar seek1 = (SeekBar) findViewById(R.id.seekBar21);
        SeekBar seek2 = (SeekBar) findViewById(R.id.seekBar22);
        SeekBar seek3 = (SeekBar) findViewById(R.id.seekBar23);
        SeekBar seek4 = (SeekBar) findViewById(R.id.seekBar24);
        SeekBar seek5 = (SeekBar) findViewById(R.id.seekBar25);
        SeekBar seek6 = (SeekBar) findViewById(R.id.seekBar26);

        int syrup1 =  seek1.getProgress();
        int syrup2 =  seek2.getProgress();
        int syrup3 =  seek3.getProgress();
        int syrup4 =  seek4.getProgress();
        int syrup5 =  seek5.getProgress();
        int syrup6 =  seek6.getProgress();

        watertype = "CD";
        watersy = Integer.toString(syrup1) + Integer.toString(syrup2) + Integer.toString(syrup3) +Integer.toString(syrup4) + Integer.toString(syrup5) + Integer.toString(syrup6);
        showcupchoose();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Scene scene;
        Transition transition = new AutoTransition();

        final ViewGroup mainview = findViewById(R.id.mainview);



        int id = item.getItemId();
        if (id == R.id.menu_today)
        {
            scene = Scene.getSceneForLayout(mainview, R.layout.today, getApplicationContext());
            TransitionManager.go(scene, transition);

            avinfoView avview = (avinfoView) findViewById(R.id.avinfoView);
            TextView textView_daydrink = (TextView) findViewById(R.id.textView_daydrink);
            TextView textView_daysave = (TextView) findViewById(R.id.textView_daysave);
            Log.d("aha", jsoninfo.toString());
            try {
                int weight = Integer.parseInt(jsoninfo.getString("weight"));
                Log.d("aha", "weight: " + weight);

                int water = jsoninfo.getInt("water");

                float angle = water / weight * 18;
                Log.d("aha", "angle " + angle);
                avview.setAngle((int)angle);

                textView_daydrink.setText(water +"/" + weight*20);
                textView_daysave.setText("Saved " + (int)(water/450));
                avview.invalidate();

            } catch (JSONException e) {
                e.printStackTrace();
            }




            ImageView imgv = (ImageView) findViewById(R.id.imagev_cup);
            imgv.setImageDrawable(getDrawable(R.drawable.cup));

            ImageView imgvbk = (ImageView) findViewById(todayimgv1);
            imgvbk.setImageDrawable(getDrawable(R.drawable.leaves));

           //LinearLayout linearLayout = findViewById(R.id.showused);
            //DayDrinkView myView = new DayDrinkView(this);
            //linearLayout.addView(myView);

        }else if (id == R.id.menu_info)
        {
            scene = Scene.getSceneForLayout(mainview, R.layout.myinfo, getApplicationContext());
            TransitionManager.go(scene, transition);

            final EditText t1 = (EditText) findViewById(R.id.username);
            final EditText t2 = (EditText) findViewById(R.id.userweight);


            try {
                if (jsoninfo.getString("name") != null)
                {
                    t1.setText(jsoninfo.getString("name"));
                }

                if (jsoninfo.getString("weight") != null)
                {
                    t2.setText(jsoninfo.getString("weight"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }



            t1.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        jsoninfo.put("name",t1.getText().toString());
                        saveinfo(jsoninfo.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                });


            t2.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        jsoninfo.put("weight",t2.getText().toString());
                        saveinfo(jsoninfo.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }else if (id == R.id.menu_search)
        {
            scene = Scene.getSceneForLayout(mainview, R.layout.map, getApplicationContext());
            TransitionManager.go(scene, transition);

            mapView = (MapView) findViewById(R.id.map);
            mapView.onCreate(null);
            loadDS();

        }else if (id == R.id.menu_DK)
        {
            resetdata();
            showpage1();
        }

        return super.onOptionsItemSelected(item);
    }


    public void  makeQR(String str) {

        QRGEncoder qrgEncoder = new QRGEncoder(str, null, QRGContents.Type.TEXT, 400);

        try {
            // Getting QR-Code as Bitmap
            Bitmap bitmap = qrgEncoder.encodeAsBitmap();
            // Setting Bitmap to ImageView
            ImageView imgv = findViewById(R.id.QRimg);
            imgv.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.v("aha", e.toString());
        }
    }

    public void toQR() {
        Scene scene;
        Transition transition = new AutoTransition();

        final ViewGroup mainview = findViewById(R.id.mainview);
        scene = Scene.getSceneForLayout(mainview, R.layout.qrcode, getApplicationContext());
        TransitionManager.go(scene, transition);

        ImageView imgv = findViewById(R.id.page4imgv);
        imgv.setImageDrawable(getDrawable(R.drawable.screen4));
        makeQR(pustr);

        try {
            String your_name = "Henry";

            if (jsoninfo.getString("name") != null) {
                    your_name = jsoninfo.getString("name");
            }
            final JSONObject jsonBody = new JSONObject("{\"name\":\"" + your_name + "\",\"waterinfo\":\"" + pustr + "\",\"state\":\"payed\",\"price\":\"1.25\"}");
            Log.d("aha",jsonBody.toString());
            saveorder(jsonBody);



            if (jsoninfo.has("water"))
            {
                jsoninfo.put("water", jsoninfo.getLong("water") + 450);
            }else {
                jsoninfo.put("water", 450);
            }
            Log.d("aha", jsoninfo.toString());
            saveinfo(jsoninfo.toString());

        }catch (Exception e)
        {
            Log.d("aha", e.toString());
        }

        resetdata();

        //makeQR("http://www.windcoffee.club");
    }


    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("aha", "onRequestPermissionsResult: " + "get ble adnim permission");
                } else {
                    Log.d("aha", "onRequestPermissionsResult: " + " not get ble adnim permission");
                }
                return;
            }

            case 102: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("aha", "onRequestPermissionsResult: " + "get ACCESS_COARSE_LOCATION permission");
                } else {
                    Log.d("aha", "onRequestPermissionsResult: " + " not get ACCESS_COARSE_LOCATION permission");
                }
                return;
            }


            case 103: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("aha", "onRequestPermissionsResult: " + "get MODIFY_AUDIO_SETTINGS  permission");
                } else {
                    Log.d("aha", "onRequestPermissionsResult: " + " not get  MODIFY_AUDIO_SETTINGS permission");
                }
                return;
            }

            default:
        }
    }


    public void  resetdata()
    {
        watercustom = "0";

        watertype = "";

        watercup = "2";

        watersy ="000000";

        price = "1.25";
    }

    public void speakhello(String str) {
        Bundle bundle = new Bundle();
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, bundle, null);
    }


    public void showpage1()
    {
        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.page1, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);
        ImageView base1 = (ImageView)findViewById(R.id.page1imgv);
        base1.setImageDrawable(getDrawable(R.drawable.screen));

        Button bt = (Button)findViewById(R.id.page1BT);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    showpage2();
                    return false;
                }
                return  false;
            }
        });

        Button bt2 = (Button)findViewById(R.id.page1BT2);
        bt2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    showpage3();
                    return false;
                }
                return  false;
            }
        });

        Button bt3 = (Button)findViewById(R.id.page1BT3);
        bt3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watertype = "AK";
                    showcupchoose();

                    return false;
                }
                return  false;
            }
        });

        Button bt4 = (Button)findViewById(R.id.page1BT4);
        bt4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watertype = "HT";
                    showcupchoose();
                    return false;
                }
                return  false;
            }
        });
    }

    public void showpage2()
    {

        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.page2, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);

        watercustom = "2";
        ImageView base1 = (ImageView)findViewById(R.id.page2imgv);
        base1.setImageDrawable(getDrawable(R.drawable.screen2));

        final ImageView imageView1 = (ImageView)findViewById(R.id.page2imageV1);
        imageView1.setImageDrawable(getDrawable(R.drawable.bticon2));

        final ImageView imageView2 = (ImageView)findViewById(R.id.page2imageV2);
        imageView2.setImageDrawable(getDrawable(R.drawable.bticon2));

        final ImageView imageView3 = (ImageView)findViewById(R.id.page2imageV3);
        imageView3.setImageDrawable(getDrawable(R.drawable.bticon2));

        Button bt = (Button)findViewById(R.id.page2BT);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    view.setAlpha((float) 0.7);

                    watertype = "SP";
                    showcupchoose();
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
            {
                view.setAlpha(0);
                return false;
            }

                return  false;
            }
        });


        Button bt2 = (Button)findViewById(R.id.page2btcustom);
        bt2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    view.setAlpha((float) 0.7);
                    showpagecustom();
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }
                return  false;
            }
        });


        Button bts1 = (Button)findViewById(R.id.page2BT_s1);
        bts1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercustom = "1";
                    view.setAlpha((float) 0.7);
                    imageView1.setVisibility(View.VISIBLE);
                    imageView2.setVisibility(View.INVISIBLE);
                    imageView3.setVisibility(View.INVISIBLE);
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }

                return  false;
            }
        });


        Button bts2 = (Button)findViewById(R.id.page2BT_s2);
        bts2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercustom = "2";
                    view.setAlpha((float) 0.7);
                    imageView1.setVisibility(View.INVISIBLE);
                    imageView2.setVisibility(View.VISIBLE);
                    imageView3.setVisibility(View.INVISIBLE);
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }

                return  false;
            }
        });

        Button bts3 = (Button)findViewById(R.id.page2BT_s3);
        bts3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercustom = "3";
                    view.setAlpha((float) 0.7);
                    imageView1.setVisibility(View.INVISIBLE);
                    imageView2.setVisibility(View.INVISIBLE);
                    imageView3.setVisibility(View.VISIBLE);
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }

                return  false;
            }
        });

    }


    public void showpage3()
    {

        watercustom = "2";
        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.page3, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);
        ImageView base1 = (ImageView)findViewById(R.id.page3imgv);
        base1.setImageDrawable(getDrawable(R.drawable.screen5));

        final ImageView imageView1 = (ImageView)findViewById(R.id.page3imageV1);
        imageView1.setImageDrawable(getDrawable(R.drawable.bticon1));

        final ImageView imageView2 = (ImageView)findViewById(R.id.page3imageV2);
        imageView2.setImageDrawable(getDrawable(R.drawable.bticon1));

        final ImageView imageView3 = (ImageView)findViewById(R.id.page3imageV3);
        imageView3.setImageDrawable(getDrawable(R.drawable.bticon1));


        Button bt_cm = (Button)findViewById(R.id.page3BT2);
        bt_cm.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    view.setAlpha((float) 0.7);
                    showpagecustom2();
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }
                return  false;
            }
        });


        Button bt = (Button)findViewById(R.id.page3BT);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watertype = "CD";
                    view.setAlpha((float) 0.7);
                    showcupchoose();
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }
                return  false;
            }
        });

        Button bt1 = (Button)findViewById(R.id.page3BT_s1);
        bt1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercustom = "1";
                    view.setAlpha((float) 0.7);
                    imageView1.setVisibility(View.VISIBLE);
                    imageView2.setVisibility(View.INVISIBLE);
                    imageView3.setVisibility(View.INVISIBLE);
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }

                return  false;
            }
        });


        Button bt2 = (Button)findViewById(R.id.page3BT_s2);
        bt2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercustom = "2";
                    view.setAlpha((float) 0.7);
                    imageView1.setVisibility(View.INVISIBLE);
                    imageView2.setVisibility(View.VISIBLE);
                    imageView3.setVisibility(View.INVISIBLE);
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }

                return  false;
            }
        });

        Button bt3 = (Button)findViewById(R.id.page3BT_s3);
        bt3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercustom = "3";
                    view.setAlpha((float) 0.7);
                    imageView1.setVisibility(View.INVISIBLE);
                    imageView2.setVisibility(View.INVISIBLE);
                    imageView3.setVisibility(View.VISIBLE);
                    return false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    view.setAlpha(0);
                    return false;
                }

                return  false;
            }
        });

    }


    public void showcupchoose() {
        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.cupchoose, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);
        ImageView base1 = (ImageView) findViewById(R.id.cupchoose_imgv);
        base1.setImageDrawable(getDrawable(R.drawable.cupchoose));

        final TextView t1 = (TextView)findViewById(R.id.sy1_tx);
        t1.setText(Integer.parseInt(watersy.substring(0,1))*20 + "%");

        final TextView t2 = (TextView)findViewById(R.id.sy2_tx);
        t2.setText(Integer.parseInt(watersy.substring(1,2))*20 + "%");

        final TextView t3 = (TextView)findViewById(R.id.sy3_tx);
        t3.setText(Integer.parseInt(watersy.substring(2,3))*20 + "%");

        final TextView t4 = (TextView)findViewById(R.id.sy4_tx);
        t4.setText(Integer.parseInt(watersy.substring(3,4))*20 + "%");

        final TextView t5 = (TextView)findViewById(R.id.sy5_tx);
        t5.setText(Integer.parseInt(watersy.substring(4,5))*20 + "%");

        final TextView t6 = (TextView)findViewById(R.id.sy6_tx);
        t6.setText(Integer.parseInt(watersy.substring(5,6))*20 + "%");

        final TextView casttx = (TextView)findViewById(R.id.cast_tx);
        casttx.setText(price + "$");


        Button topay_bt = (Button)findViewById(R.id.cuptopay_bt);
        topay_bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    Date now = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String strDate = sdf.format(now);
                    pustr = "QDSC" + MID + strDate + watertype + watercustom + watersy + watercup + "Z";
                    Log.d("aha",pustr);
                    showpagepay();
                    return false;
                }
                return  false;
            }
        });

        final Button lcup_bt;
        final Button scup_bt;
        final Button mcup_bt;
        lcup_bt = (Button)findViewById(R.id.lcup_bt);
        mcup_bt = (Button)findViewById(R.id.mcup_bt);
        scup_bt = (Button)findViewById(R.id.scup_bt);

        scup_bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercup = "1";
                    price = "1";
                    casttx.setText(price + "$");
                    lcup_bt.setAlpha((float)0.2);
                    mcup_bt.setAlpha((float)0.2);
                    scup_bt.setAlpha(0);
                    return false;
                }
                return  false;
            }
        });


        mcup_bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercup = "2";
                    price ="1.25";
                    casttx.setText(price + "$");
                    lcup_bt.setAlpha((float)0.2);
                    mcup_bt.setAlpha(0);
                    scup_bt.setAlpha((float)0.2);
                    return false;
                }
                return  false;
            }
        });


        lcup_bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    watercup = "3";
                    price = "1.75";
                    casttx.setText(price + "$");
                    lcup_bt.setAlpha(0);
                    mcup_bt.setAlpha((float)0.2);
                    scup_bt.setAlpha((float)0.2);
                    return false;
                }
                return  false;
            }
        });

    }


        public void showpagepay()
    {
        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.pay, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);
        ImageView base1 = (ImageView)findViewById(R.id.page3imgv);
        base1.setImageDrawable(getDrawable(R.drawable.screen3));

        TextView price_tx = (TextView)findViewById(R.id.paycast_tx);
        price_tx.setText(price + "$");

        Button bt = (Button)findViewById(R.id.page3BT);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    toQR();
                    return false;
                }
                return  false;
            }
        });

        Button bt2 = (Button)findViewById(R.id.cancelBT);
        bt2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    view.setAlpha((float) 0.7);
                    resetdata();
                    showpage1();
                    return false;
                }
                return  false;
            }
        });

    }




    public void showpagecustom()
    {
        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.drank1, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);

        ImageView base1 = (ImageView)findViewById(R.id.base1);
        base1.setImageDrawable(getDrawable(R.drawable.base1));
        ImageView base2 = (ImageView)findViewById(R.id.base2);
        base2.setImageDrawable(getDrawable(R.drawable.base1));
        ImageView base3 = (ImageView)findViewById(R.id.base3);
        base3.setImageDrawable(getDrawable(R.drawable.base1));
        ImageView base4 = (ImageView)findViewById(R.id.base4);
        base4.setImageDrawable(getDrawable(R.drawable.base1));
        ImageView base5 = (ImageView)findViewById(R.id.base5);
        base5.setImageDrawable(getDrawable(R.drawable.base1));
        ImageView base6 = (ImageView)findViewById(R.id.base6);
        base6.setImageDrawable(getDrawable(R.drawable.base1));

        final ImageView water1 = (ImageView)findViewById(R.id.water1);
        water1.setImageDrawable(getDrawable(R.drawable.energy1));
        final ImageView water2 = (ImageView)findViewById(R.id.water2);
        water2.setImageDrawable(getDrawable(R.drawable.orange));
        final ImageView water3 = (ImageView)findViewById(R.id.water3);
        water3.setImageDrawable(getDrawable(R.drawable.raspberry));
        final ImageView water4 = (ImageView)findViewById(R.id.water4);
        water4.setImageDrawable(getDrawable(R.drawable.cucumber));
        final ImageView water5 = (ImageView)findViewById(R.id.water5);
        water5.setImageDrawable(getDrawable(R.drawable.relax));
        final ImageView water6 = (ImageView)findViewById(R.id.water6);
        water6.setImageDrawable(getDrawable(R.drawable.lemon));



        final ImageView thebt = (ImageView) findViewById(R.id.imgvpaybt);
        thebt.setImageResource(R.drawable.paybt);
        thebt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    try {
                        topay();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
                return  false;
            }
        });
    }


    public void showpagecustom2()
    {
        final ViewGroup mainview = findViewById(R.id.mainview);
        Scene scene = Scene.getSceneForLayout(mainview, R.layout.drank2, getApplicationContext());
        Transition transition = new AutoTransition();
        TransitionManager.go(scene, transition);

        ImageView base1 = (ImageView)findViewById(R.id.base21);
        base1.setImageDrawable(getDrawable(R.drawable.bot1));
        ImageView base2 = (ImageView)findViewById(R.id.base22);
        base2.setImageDrawable(getDrawable(R.drawable.bot2));
        ImageView base3 = (ImageView)findViewById(R.id.base23);
        base3.setImageDrawable(getDrawable(R.drawable.bot3));
        ImageView base4 = (ImageView)findViewById(R.id.base24);
        base4.setImageDrawable(getDrawable(R.drawable.bot4));
        ImageView base5 = (ImageView)findViewById(R.id.base25);
        base5.setImageDrawable(getDrawable(R.drawable.bot5));
        ImageView base6 = (ImageView)findViewById(R.id.base26);
        base6.setImageDrawable(getDrawable(R.drawable.bot6));



        final ImageView thebt = (ImageView) findViewById(R.id.imgvpaybt2);
        thebt.setImageResource(R.drawable.paybt2);
        thebt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    topay2();
                    return false;
                }
                return  false;
            }
        });
    }



    public void loadDS() {

        String url = "http://www.windcoffee.club:8088/mapinfo";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.d("aha", "onResponse: " + response.toString());

                if (response.length() > 0)
                {
                    for (int i=0;i<response.length();i++) {
                        try {
                            JSONObject theobj = response.getJSONObject(i);
                            double la = theobj.getDouble("la");
                            double ln = theobj.getDouble("ln");
                            String DS_id = theobj.getString("MID");
                            String info = theobj.getString("info");
                            if (i==0)
                            {
                                CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(la, ln), 18, 0, 0));
                                mapView.getMap().animateCamera(mCameraUpdate);
                            }
                            mapView.getMap().addMarker(new MarkerOptions().position(new LatLng(la,ln)).title(DS_id).snippet(info));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("aha", error.toString());
            }
        });

        requestQueue.add(jsonArrayRequest);
    }


    public void saveorder(JSONObject obj) {
        String url = "http://www.windcoffee.club:8088/saveorder";

        JsonObjectRequest request = new JsonObjectRequest(url, obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("aha", "onResponse: " + response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("aha", "onResponse: " + error.toString());
            }
        }

        );
        requestQueue.add(request);
    }


   public  void saveinfo(String str)
   {
       String filename = "myinfo.json";
       String fileContents = str;
       FileOutputStream outputStream;

       File file = new File(getFilesDir(), filename);
       if (file.delete()) {
           Log.d("aha", "file delete");
           try {
               outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
               outputStream.write(fileContents.getBytes());
               outputStream.close();
           } catch (Exception e) {
               e.printStackTrace();
           }
       }
   }

   public String loadinfo()
   {
       String jsonstr = "{\"name\":\"\"}";
       String filename = "myinfo.json";

       File directory = this.getFilesDir();
       File file = new File(directory, filename);

       if(file.exists()) {
           try {
               FileInputStream is = openFileInput(filename);

               int size = is.available();

               byte[] buffer = new byte[size];

               is.read(buffer);

               is.close();

               jsonstr = new String(buffer, "UTF-8");


           } catch (IOException ex) {
               ex.printStackTrace();
               return jsonstr;
           }
       }
        return jsonstr;
   }

}
