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

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {

    public MqttAndroidClient client;

    public IMqttMessageListener iMqttMessageListener;

    public IMqttToken iMqttToken;

    public  String pustr;

    public  String your_name;

    public  String your_weight;

    public TextToSpeech textToSpeech;

    private BluetoothDevice esp32_ble;

    private BluetoothLeScanner scanner;

    private BluetoothAdapter bluetoothAdapter;

    private boolean mScanning;

    private final static String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";

    private MapView mapView;

    private RequestQueue requestQueue;



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
                speakhello("Hi hunry finded " + esp32_ble.getName() + ". Can i help you?");


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



        Scene scene;
        Transition transition = new AutoTransition();

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

        final ViewGroup mainview = findViewById(R.id.mainview);
        scene = Scene.getSceneForLayout(mainview, R.layout.drank, getApplicationContext());
        TransitionManager.go(scene, transition);


        final TextView textView = (TextView)findViewById(R.id.aha);
        if (your_name != null)
        {
            textView.setText("Hello " + your_name + " !");
        }

        final ImageView imageView = (ImageView)findViewById(R.id.tree);
        imageView.setImageDrawable(getDrawable(R.drawable.tree));

        final Button thebt = (Button) findViewById(R.id.bt_custom);
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
        long time = new Date().getTime();

        Scene scene;
        Transition transition = new AutoTransition();

        final ViewGroup mainview = findViewById(R.id.mainview);


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

        pustr = "QDRIN_SUCC:" + Integer.toString(syrup1) + "_" + Integer.toString(syrup2) + "_"  + Integer.toString(syrup3) + "_" + Integer.toString(syrup4) + "_" + Integer.toString(syrup5) + "_" + Integer.toString(syrup6);
        //mqttpub(Integer.toString(syrup1) + "_" + Integer.toString(syrup2) + "_"  + Integer.toString(syrup3) + "_" + Integer.toString(syrup4) );

        scene = Scene.getSceneForLayout(mainview, R.layout.pay, getApplicationContext());
        TransitionManager.go(scene, transition);

        final Button bt1 = (Button) findViewById(R.id.bt_pay1);

        final Button bt2 = (Button) findViewById(R.id.bt_pay2);

        bt1.setOnTouchListener(new View.OnTouchListener() {
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

        bt2.setOnTouchListener(new View.OnTouchListener() {
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


        //makeQR("ok");
        //QREEncoder qreEncoder = new

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

            ImageView imgv = (ImageView) findViewById(R.id.imagev_cup);
            imgv.setImageDrawable(getDrawable(R.drawable.cup));

           //LinearLayout linearLayout = findViewById(R.id.showused);
            //DayDrinkView myView = new DayDrinkView(this);
            //linearLayout.addView(myView);

        }else if (id == R.id.menu_info)
        {
            scene = Scene.getSceneForLayout(mainview, R.layout.myinfo, getApplicationContext());
            TransitionManager.go(scene, transition); final Button bt1 = (Button) findViewById(R.id.bt_pay1);

            final EditText t1 = (EditText) findViewById(R.id.username);
            final EditText t2 = (EditText) findViewById(R.id.userweight);

            if (your_name != null)
            {
                t1.setText(your_name);
            }

            if (your_weight != null)
            {
                t2.setText(your_weight);
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
                    your_name = t1.getText().toString();
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
                    your_weight = t2.getText().toString();
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
            scene = Scene.getSceneForLayout(mainview, R.layout.drank, getApplicationContext());
            TransitionManager.go(scene, transition);

            Log.d("aha", "speak ?");

            Log.d("aha", "yes speak");

            final TextView textView = (TextView)findViewById(R.id.aha);
            if (your_name != null)
            {
                textView.setText("Hello " + your_name + " !");
            }

            final ImageView imageView = (ImageView)findViewById(R.id.tree);
            imageView.setImageDrawable(getDrawable(R.drawable.tree));

            final Button thebt = (Button) findViewById(R.id.bt_custom);
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
        makeQR(pustr);
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


    public void speakhello(String str) {
        Bundle bundle = new Bundle();
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, bundle, null);
    }


    public void loadDS() {

        String url = "http://www.windcoffee.club/mapinfo";

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
                            String DS_id = theobj.getString("DS_id");
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


        /*
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("aha", "onResponse: " + response.toString());


                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("aha", error.toString());

                    }
                });
                */

        requestQueue.add(jsonArrayRequest);

    }


}
