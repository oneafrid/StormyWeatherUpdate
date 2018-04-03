package com.example.priyanka.stormy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    double latitude;
    double longitude;

    public static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather mCurrentWeather;
    static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;

    @BindView(R.id.timeTextView) TextView mTimelabel;
    @BindView(R.id.temperaturetextView) TextView mTemperatureLabel;
    @BindView(R.id.humidityValueTextView) TextView mHumidityValue;
    @BindView(R.id.percipValue) TextView mPrecipValue;
    @BindView(R.id.summarytextView) TextView mSummaryLabel;
    @BindView(R.id.iconimageView) ImageView mIconImageView;
    @BindView(R.id.refreshimageView) ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.locationTextView) TextView mlocation;
    @BindView(R.id.windSpeedTextViewValue) TextView mWindSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getLocation();

        mProgressBar.setVisibility(View.INVISIBLE);

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocation();
                getForecast();
            }
        });
        getForecast();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }
    }

    private String getLocationDetails(double latitude, double longitude) throws IOException {
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        addresses = geocoder.getFromLocation(latitude, longitude, 1);

        String city = addresses.get(0).getLocality();
        return city;
    }

    private void getForecast() {

        String forecastURL = "https://api.darksky.net/forecast/582cb3a77cc9a6ffd73b06b85c04ad8a/" + latitude + "," + longitude;

        if (isNetworkAvailable()) {
            toggleRefresh();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(forecastURL).build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertDialogToUser();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData = response.body().string();
                        if (response.isSuccessful()){
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        updateDispaly();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else {
                            alertDialogToUser();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Exception : ", e);
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Exception : ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, "No Internet Connection Available", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility()  == View.INVISIBLE){
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDispaly() throws IOException {

        mTemperatureLabel.setText(mCurrentWeather.getmTemperature() + "");
        mHumidityValue.setText(mCurrentWeather.getmHumidity() + "%");
        mPrecipValue.setText(mCurrentWeather.getmPercipChance() + "%");
        mSummaryLabel.setText("Today It Will Be: " + mCurrentWeather.getmSummary());

        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconID());
        mIconImageView.setImageDrawable(drawable);

        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        String time = formatter.format(mCurrentWeather.getmTime()*1000);
        mTimelabel.setText(" Current Temperature At " + time);

        mlocation.setText(getLocationDetails(latitude,longitude));

        mWindSpeed.setText(mCurrentWeather.getmWindSpeed() + "");

    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {

        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setmHumidity(currently.getDouble("humidity"));
        currentWeather.setmIcon(currently.getString("icon"));
        currentWeather.setmPercipChance(currently.getDouble("precipProbability"));
        currentWeather.setmSummary(currently.getString("summary"));
        currentWeather.setmTemperature(currently.getDouble("temperature"));
        currentWeather.setmTime(currently.getLong("time"));
        currentWeather.setmWindSpeed(currently.getDouble("windSpeed"));

        return currentWeather;
    }

    private boolean isNetworkAvailable(){

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;

    }

    private void alertDialogToUser() {

        AlertDialogFragment dialogFragment = new AlertDialogFragment();
        dialogFragment.show(getFragmentManager(), "error_dialog");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_LOCATION:
                getLocation();
                break;
        }
    }
}
