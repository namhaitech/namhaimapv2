package com.namhaigroup.map;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.namhaigroup.map.system.AppSettings;
import com.namhaigroup.map.system.UserInformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Home_Activity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String Lat, Long, currentSpeedLimit = "0", maxSpeedLimit = "0";
    private boolean isHaveExtratags = false, isResidentialSounded = false, isResidential = false, oneway = false, isStartScan = false;
    private int lanes = 0;
    private final int delay = 3000;
    TextView tvAddress, tvLat,  tvLong,  tvCurrentSpeed;
    ImageView imgTrafficSign, imgTrafficSign2;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private AdView mAdView;
    ImageButton btnMenu;
    AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvAddress = findViewById(R.id.tvAddress);
        tvLat = findViewById(R.id.tvLat);
        tvLong = findViewById(R.id.tvLong);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        adView = findViewById(R.id.adView);
        btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void CreateLocationServices() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled == true) {
            int fineLocPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
            int coarseLocPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
            if (fineLocPermission == PackageManager.PERMISSION_GRANTED && coarseLocPermission == PackageManager.PERMISSION_GRANTED) {
                LocationListener locationListener = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float speed = location.getSpeed();
                        double speedKmh = (speed * 3600) / 1000;
                        speedKmh = ((double) Math.round(speedKmh * 10) / 10);

                        if(UserInformation.permission != 2) {
                            Lat = String.valueOf(latitude);
                            Long = String.valueOf(longitude);
                        }

                        tvLat.setText("Kinh độ: " + Lat);
                        tvLong.setText("Vĩ độ: " + Long);
                        tvCurrentSpeed.setText("Tốc độ: " + speedKmh + " km/h");
                    }
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }
                    public void onProviderEnabled(String provider) {
                    }
                    public void onProviderDisabled(String provider) {
                    }
                };
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                GetLocationInformation();
                isStartScan = true;
            }
            else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Cấp quyền truy cập vị trí cho ứng dụng").setMessage("Vui lòng cấp quyền truy cập vị trí cho ứng dụng này")
                        .setPositiveButton("Cài đặt", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        }).setNegativeButton("Hủy", null).show();
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Bật quyền truy cập vị trí").setMessage("Vui lòng bật quyền truy cập vị trí để sử dụng tính năng này")
                    .setPositiveButton("Cài đặt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(intent);
                        }
                    }).setNegativeButton("Hủy", null).show();
        }
    }

    private void CreateADSServices() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void GetLocationInformation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new DownloadJsonTask().execute("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + Lat + "&lon=" + Long + "&accept-language=vi&extratags=1");
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private class DownloadJsonTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                inputStream.close();

                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String json) {
            if (json != null) {
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String displayName = jsonObject.getString("display_name");
                    tvAddress.setText(displayName);

                    if (jsonObject.has("extratags") && !jsonObject.isNull("extratags")) { // Get extratags
                        JSONObject extraTags = jsonObject.getJSONObject("extratags");
                        if (extraTags.has("maxspeed")) { //Check max speed
                            isHaveExtratags = true;
                            maxSpeedLimit = extraTags.getString("maxspeed");
                        } else {
                            isHaveExtratags = false;
                        }

                        if (extraTags.has("population")) { //Check high population
                            if (extraTags.getString("population").equals("yes")) {
                                isResidential = true;
                            } else {
                                isResidential = false;
                            }
                        } else {
                            isResidential = false;
                        }

                        if (extraTags.has("oneway")) {
                            oneway = extraTags.getString("oneway").equals("yes");
                        } else {
                            oneway = false;
                        }
                        if (extraTags.has("lanes")) {
                            lanes = Integer.parseInt(extraTags.getString("lanes"));
                        } else {
                            lanes = 0;
                        }
                    } else {
                        isHaveExtratags = false;
                        isResidential = false;
                        oneway = false;
                        lanes = 0;
                    }

                    if (isHaveExtratags == false) { // Calculate speed limit if don't have extratags
                        String name = jsonObject.getString("name");
                        String roadType = jsonObject.getString("type");
                        String residential = jsonObject.getString("place_rank");
                        if(!roadType.equals("bridge")) {
                            if (roadType.equals("trunk")) {
                                if (isResidential == false && Integer.parseInt(residential) >= 30) {
                                    isResidential = true;
                                } else {
                                    isResidential = false;
                                    isResidentialSounded = false;
                                    imgTrafficSign2 = findViewById(R.id.imgTrafficSign2);
                                    imgTrafficSign2.setImageResource(com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_light_normal_background);
                                }
                                if (isResidential == true && oneway == true) {
                                    maxSpeedLimit = "60";
                                }  else if (isResidential == true && oneway == false) {
                                    maxSpeedLimit = "50";
                                } else if (isResidential == false && oneway == true) {
                                    maxSpeedLimit = "90";
                                } else if (isResidential == false && oneway == false) {
                                    maxSpeedLimit = "80";
                                }
                            } else if (roadType.equals("primary") || roadType.equals("secondary")) {
                                if (isResidential == true && oneway == true) {
                                    maxSpeedLimit = "60";
                                }  else if (isResidential == true && oneway == false) {
                                    maxSpeedLimit = "50";
                                } else if (isResidential == false && oneway == true) {
                                    maxSpeedLimit = "60";
                                } else if (isResidential == false && oneway == false) {
                                    maxSpeedLimit = "50";
                                }
                            }
                            else if (roadType.equals("residential") || roadType.equals("tertiary")) {
                                maxSpeedLimit = "50";
                            } else if (roadType.equals("motorway")) {
                                if (name.toLowerCase().contains("cao lãnh - vàm cống")) {
                                    maxSpeedLimit = "90";
                                } else if (name.toLowerCase().contains("cao tốc lộ tẻ - rạch sỏi")) {
                                    maxSpeedLimit = "80";
                                } else {
                                    if (lanes > 2) {
                                        maxSpeedLimit = "120";
                                    } else {
                                        maxSpeedLimit = "80";
                                    }
                                }
                            } else if (roadType.equals("motorway link")) {
                                maxSpeedLimit = "40";
                            } else {
                                maxSpeedLimit = "50";
                            }
                        }
                    }

                    if (isResidential == true) { // Residential play sound
                        if (isResidentialSounded == false) {
                            if (!mediaPlayer.isPlaying()) {
                                isResidentialSounded = true;
                                imgTrafficSign2 = findViewById(R.id.imgTrafficSign2);
                                imgTrafficSign2.setImageResource(R.drawable.population);
                                PlayOtherSound("population");
                            }
                        }
                    } else {
                        isResidentialSounded = false;
                        imgTrafficSign2 = findViewById(R.id.imgTrafficSign2);
                        imgTrafficSign2.setImageResource(com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_light_normal_background);
                    }

                    if (!currentSpeedLimit.equals(maxSpeedLimit)) { // Set speed limit and play sound
                        if (!mediaPlayer.isPlaying()) {
                            if (maxSpeedLimit.equals("40")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_40);
                                PlaySpeedLimitSound(40);
                            } else if (maxSpeedLimit.equals("50")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_50);
                                PlaySpeedLimitSound(50);
                            } else if (maxSpeedLimit.equals("60")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_60);
                                PlaySpeedLimitSound(60);
                            } else if (maxSpeedLimit.equals("70")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_70);
                                PlaySpeedLimitSound(70);
                            } else if (maxSpeedLimit.equals("80")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_80);
                                PlaySpeedLimitSound(80);
                            } else if (maxSpeedLimit.equals("90")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_90);
                                PlaySpeedLimitSound(90);
                            } else if (maxSpeedLimit.equals("100")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_100);
                                PlaySpeedLimitSound(100);
                            } else if (maxSpeedLimit.equals("110")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_110);
                                PlaySpeedLimitSound(110);
                            } else if (maxSpeedLimit.equals("120")) {
                                currentSpeedLimit = maxSpeedLimit;
                                imgTrafficSign = findViewById(R.id.imgTrafficSign);
                                imgTrafficSign.setImageResource(R.drawable.img_sign_120);
                                PlaySpeedLimitSound(120);
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void PlaySpeedLimitSound(int speed) {
        if (speed == 40) {
            if(AppSettings.isAlert40km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed40);
                mediaPlayer.start();
            }
        } else if (speed == 50) {
            if(AppSettings.isAlert50km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed50);
                mediaPlayer.start();
            }
        } else if (speed == 60) {
            if(AppSettings.isAlert60km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed60);
                mediaPlayer.start();
            }
        } else if (speed == 70) {
            if(AppSettings.isAlert70km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed70);
                mediaPlayer.start();
            }
        } else if (speed == 80) {
            if(AppSettings.isAlert80km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed80);
                mediaPlayer.start();
            }
        } else if (speed == 90) {
            if(AppSettings.isAlert90km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed90);
                mediaPlayer.start();
            }
        } else if (speed == 100) {
            if(AppSettings.isAlert100km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed100);
                mediaPlayer.start();
            }
        } else if (speed == 110) {
            if(AppSettings.isAlert110km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed110);
                mediaPlayer.start();
            }
        } else if (speed == 120) {
            if(AppSettings.isAlert120km == true) {
                mediaPlayer = MediaPlayer.create(this, R.raw.speed120);
                mediaPlayer.start();
            }
        }
    }

    private void PlayOtherSound(String type) {
        if (type == "population") {
            mediaPlayer = MediaPlayer.create(this, R.raw.in_high_population);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isStartScan == false) {
            tvAddress.setText("Đang xác định vị trí, vui lòng đợi...");
            CreateLocationServices();
        }

        if(UserInformation.permission == 1) {
            adView.setVisibility(View.GONE);
        } else if(UserInformation.permission == 2) {
            adView.setVisibility(View.VISIBLE);
            CreateADSServices();
        }
        else {
            adView.setVisibility(View.VISIBLE);
        }

        if(UserInformation.isPremium == true) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);
            CreateADSServices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}