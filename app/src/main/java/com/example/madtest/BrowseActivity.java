package com.example.tasktodo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BrowseActivity extends AppCompatActivity {

    private static final String API_KEY = "06f2baf535174636b7d73005250105";
    private static final String WEATHER_URL = "https://api.weatherapi.com/v1/forecast.json";

    private TextView tvLocation;
    private TextView tvDate;
    private TextView tvTemperature;
    private TextView tvCondition;
    private TextView tvHumidity;
    private TextView tvWindSpeed;
    private ImageView ivWeatherIcon;
    private TextView tvFeelsLike;
    private LinearLayout forecastContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        // Initialize views
        tvLocation = findViewById(R.id.tvLocation);
        tvDate = findViewById(R.id.tvDate);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvCondition = findViewById(R.id.tvCondition);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        forecastContainer = findViewById(R.id.forecastContainer);

        // Set up bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_browse);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_today) {
                // Navigate to Today page
                Intent intent = new Intent(BrowseActivity.this, MainActivity.class);
                intent.putExtra("select_tab", "today");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_upcoming) {
                // Navigate to Upcoming page
                Intent intent = new Intent(BrowseActivity.this, MainActivity.class);
                intent.putExtra("select_tab", "upcoming");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_search) {
                // Navigate to Search page
                startActivity(new Intent(BrowseActivity.this, SearchActivity.class));
                return true;
            } else if (itemId == R.id.nav_browse) {
                // Already on Browse page
                return true;
            }
            return false;
        });

        // Fetch weather data for Kuala Lumpur with forecast for 5 days
        fetchWeatherData("Kuala Lumpur");
    }

    private void fetchWeatherData(String location) {
        OkHttpClient client = new OkHttpClient();

        // Request forecast for 5 days
        String url = WEATHER_URL + "?key=" + API_KEY + "&q=" + location + "&days=6&aqi=no&tp=24";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(BrowseActivity.this,
                            "Failed to fetch weather data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject locationObj = jsonObject.getJSONObject("location");
                        JSONObject current = jsonObject.getJSONObject("current");
                        JSONObject condition = current.getJSONObject("condition");
                        JSONObject forecast = jsonObject.getJSONObject("forecast");
                        JSONArray forecastDays = forecast.getJSONArray("forecastday");

                        String locationName = locationObj.getString("name") + ", " + locationObj.getString("country");
                        double tempC = current.getDouble("temp_c");
                        String conditionText = condition.getString("text");
                        String iconUrl = "https:" + condition.getString("icon");
                        int humidity = current.getInt("humidity");
                        double windKph = current.getDouble("wind_kph");
                        double feelsLikeC = current.getDouble("feelslike_c");

                        // Get current date in Kuala Lumpur timezone
                        String timezone = locationObj.getString("tz_id");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM - EEEE", Locale.getDefault());
                        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
                        String currentDate = dateFormat.format(new Date());

                        runOnUiThread(() -> {
                            tvLocation.setText(locationName);
                            tvDate.setText(currentDate);
                            tvTemperature.setText(String.format("%.1f°C", tempC));
                            tvCondition.setText(conditionText);
                            tvHumidity.setText("Humidity: " + humidity + "%");
                            tvWindSpeed.setText("Wind: " + String.format("%.1f", windKph) + " km/h");
                            tvFeelsLike.setText("Feels like: " + String.format("%.1f°C", feelsLikeC));

                            // Load weather icon using Picasso
                            Picasso.get().load(iconUrl).into(ivWeatherIcon);

                            // Clear previous forecasts
                            forecastContainer.removeAllViews();

                            // Add forecast for next 5 days (skip today)
                            for (int i = 1; i < Math.min(forecastDays.length(), 6); i++) {
                                try {
                                    JSONObject dayForecast = forecastDays.getJSONObject(i);
                                    String date = dayForecast.getString("date");
                                    JSONObject dayObj = dayForecast.getJSONObject("day");
                                    double maxTemp = dayObj.getDouble("maxtemp_c");
                                    double minTemp = dayObj.getDouble("mintemp_c");
                                    JSONObject dayCondition = dayObj.getJSONObject("condition");
                                    String dayConditionText = dayCondition.getString("text");
                                    String dayIconUrl = "https:" + dayCondition.getString("icon");

                                    // Format the date
                                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM - EEE", Locale.getDefault());
                                    Date forecastDate = inputFormat.parse(date);
                                    String formattedDate = outputFormat.format(forecastDate);

                                    // Add forecast card
                                    addForecastCard(formattedDate, maxTemp, minTemp, dayConditionText, dayIconUrl);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(BrowseActivity.this,
                                    "Error parsing weather data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(BrowseActivity.this,
                                "Failed to fetch weather data. Status code: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void addForecastCard(String date, double maxTemp, double minTemp, String condition, String iconUrl) {
        View forecastView = getLayoutInflater().inflate(R.layout.item_forecast, forecastContainer, false);

        TextView tvForecastDate = forecastView.findViewById(R.id.tvForecastDate);
        TextView tvForecastTemp = forecastView.findViewById(R.id.tvForecastTemp);
        TextView tvForecastCondition = forecastView.findViewById(R.id.tvForecastCondition);
        ImageView ivForecastIcon = forecastView.findViewById(R.id.ivForecastIcon);

        tvForecastDate.setText(date);
        tvForecastTemp.setText(String.format("%.1f°C / %.1f°C", maxTemp, minTemp));
        tvForecastCondition.setText(condition);

        // Load forecast icon
        Picasso.get().load(iconUrl).into(ivForecastIcon);

        forecastContainer.addView(forecastView);
    }
}