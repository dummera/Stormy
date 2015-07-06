package comr.example.andrew.stormy.ui;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import comr.example.andrew.stormy.R;
import comr.example.andrew.stormy.weather.Current;
import comr.example.andrew.stormy.weather.Day;
import comr.example.andrew.stormy.weather.Forecast;
import comr.example.andrew.stormy.weather.Hour;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    //public static final String TAG = MainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "DAILY_FORECAST";


    int located = 0;
    double lat;
    double lng;

    private Forecast mForecast;

    @InjectView(R.id.timeLabel) TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue) TextView mHumidityValue;
    @InjectView(R.id.precipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.refreshImageView) ImageView mRefreshImageView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;

    private GoogleApiClient mGoogleApiClient;

    public static final String TAG = MainActivity.class.getSimpleName();
    private Location mLastLocation;
    private TextView mLatitudeText;
    private TextView mLongitudeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //new ServletPostAsyncTask().execute(new Pair<Context, String>(this, "Manfred"));
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mProgressBar.setVisibility(View.INVISIBLE);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();




        final double latitude = 2;
        final double longitude = 2;

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (located == 0){
                getForecast(latitude, longitude);
            }else
                    getForecast(lat, lng);}
        });

        if (located == 0) {
            getForecast(latitude, longitude);
        }else{getForecast(lat, lng);
        Log.d(TAG, "Main UI code is running!");}


    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "27974c4bc33201748eaf542a6769c3b7";
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey +
                "/" + latitude + "," + longitude;

        if (isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        }
        else {
            Toast.makeText(this, getString(R.string.network_unavailable_message),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
       // new ServletPostAsyncTask().execute(new Pair<Context, String>(this, "Manfred"));
        Current current = mForecast.getCurrent();
        mTemperatureLabel.setText(current.getTemperature() + "");
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(current.getHumidity() + "");
        mPrecipValue.setText(current.getPrecipChance() + "%");
        mSummaryLabel.setText(current.getSummary());

        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }


    private Forecast parseForecastDetails(String jsonData)throws JSONException {
        Forecast forecast = new Forecast();

        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));

        return forecast;
    }

    private Day[] getDailyForecast(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");

        Day[] days = new Day[data.length()];

        for (int i =0; i <data.length(); i++) {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();

            day.setSummary(jsonDay.getString("summary"));
            day.setIcon(jsonDay.getString("icon"));
            day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            day.setTime(jsonDay.getLong("time"));
            day.setTimezone(timezone);

            days[i] = day;
        }

        return days;
    }

    private Hour[] getHourlyForecast(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        for (int i = 0; i < data.length(); i++) {
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();

            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimeZone(timezone);

            hours[i] = hour;
        }

        return hours;
    }

    private Current getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        Current current = new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTime(currently.getLong("time"));
        current.setIcon(currently.getString("icon"));
        current.setPrecipChance(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setTimeZone(timezone);

        Log.d(TAG, current.getFormattedTime());

        return current;
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    @OnClick (R.id.dailyButton)
    public void startDailyActivity(View view) {
        EditText usernameField;
        EditText passwordField;
        EditText numberField;
        usernameField=(EditText)findViewById(R.id.editText2);
        passwordField=(EditText)findViewById(R.id.editText3);
        numberField = (EditText)findViewById(R.id.editText4);
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        String number = numberField.getText().toString();
        if (number.equals("")) {
        }else{
            number = "/" + number;
        }
        //Toast.makeText(this, number, Toast.LENGTH_LONG).show();
        if (username.equals("Andrew") && password.equals("password")) {
            new ServletPostAsyncTaskbeerget().execute(new Pair<Context, String>(this, number));
        }else if (username.equals("John") && password.equals("other")){
            new ServletPostAsyncTaskbreweryget().execute(new Pair<Context, String>(this, number));
            //Intent intent = new Intent(this, HourlyForecastActivity.class);
        }else {
            Toast.makeText(this, "Incorrect Username or Password", Toast.LENGTH_LONG).show();
        }
       // Intent intent = new Intent(this, DailyForecastActivity.class);
       // intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
       // startActivity(intent);
    }

    @OnClick (R.id.createAccount)
    public void startAccountActivity(View view) {
        Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_LONG).show();
        //new ServletPostAsyncTask2().execute(new Pair<Context, String>(this, ""));
        // Intent intent = new Intent(this, DailyForecastActivity.class);
        // intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        // startActivity(intent);
    }

    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view) {
        EditText usernameField;
        EditText passwordField;
        usernameField=(EditText)findViewById(R.id.editText2);
        passwordField=(EditText)findViewById(R.id.editText3);
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        String latandlong = lat + ", " + lng;
        if (username.equals("Andrew") && password.equals("password")) {

            new ServletPostAsyncTaskbeerpost().execute(new Pair<Context, String>(this, latandlong));
        }else if (username.equals("John") && password.equals("other")){
            new ServletPostAsyncTaskbrewerypost().execute(new Pair<Context, String>(this, latandlong));
               //Intent intent = new Intent(this, HourlyForecastActivity.class);
        }else {
            Toast.makeText(this, "Incorrect Username or Password", Toast.LENGTH_LONG).show();
        }
       // intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
        //startActivity(intent);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        //LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
     //   Toast.makeText(this, "Hey", Toast.LENGTH_LONG).show();
        if (mLastLocation != null) {
          //  mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
           // mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            String hey = mLastLocation.toString();
           // Toast.makeText(this, hey, Toast.LENGTH_LONG).show();
           // Toast.makeText(this, "Hey", Toast.LENGTH_LONG).show();
            lat = mLastLocation.getLatitude();
            lng = mLastLocation.getLongitude();
            String latandlong = lat + ", " + lng;
            //new ServletPostAsyncTask().execute(new Pair<Context, String>(this, latandlong));

            located = 1;
        }else
        {//Toast.makeText(this, "Sorry", Toast.LENGTH_LONG).show();
        located = 0;}
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }


    /*public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }*/

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        lat = currentLatitude;
        lng = currentLongitude;


    }
    private LocationRequest mLocationRequest;

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onREsume");
        //setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    /**
     * Created by Andrew on 6/4/2015.
     */
    public static class ServletPostAsyncTaskbrewerypost extends AsyncTask<Pair<Context, String>, Void, String> {
        private Context context;

        @Override
        protected String doInBackground(Pair<Context, String>... params) {
            context = params[0].first;
            String name = params[0].second;
            //String

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://southern-splice-90420.appspot.com/brewery"); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("location", name));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        }
    }

    public static class ServletPostAsyncTaskbeerpost extends AsyncTask<Pair<Context, String>, Void, String> {
        private Context context;

        @Override
        protected String doInBackground(Pair<Context, String>... params) {
            context = params[0].first;
            String name = params[0].second;
            //String


            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://southern-splice-90420.appspot.com/beer"); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("location", name));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        }
    }

    public static class ServletPostAsyncTaskbreweryget extends AsyncTask<Pair<Context, String>, Void, String> {
        private Context context;

        @Override
        protected String doInBackground(Pair<Context, String>... params) {
            context = params[0].first;
            String name = params[0].second;
            //String

            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpPost = new HttpGet("http://southern-splice-90420.appspot.com/brewery" + name); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("location", name));
                // httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        }
    }

    public static class ServletPostAsyncTaskbeerget extends AsyncTask<Pair<Context, String>, Void, String> {
        private Context context;

        @Override
        protected String doInBackground(Pair<Context, String>... params) {
            context = params[0].first;
            String name = params[0].second;
            //String


            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpPost = new HttpGet("http://southern-splice-90420.appspot.com/beer" + name); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("location", name));
                // httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        }
    }
}














