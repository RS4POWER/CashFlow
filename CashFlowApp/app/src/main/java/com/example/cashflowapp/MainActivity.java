package com.example.cashflowapp;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.google.android.gms.location.FusedLocationProviderClient;


import com.google.android.gms.location.LocationCallback;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;


import java.io.IOException;
import java.util.List;
import java.util.Locale;



import android.annotation.SuppressLint;
import android.os.AsyncTask;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView tvAdd;
    Button btGetAdd;

    String country;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    int location_request_id = 10;

    EditText amountEditText;
    Spinner currencySpinner;
    TextView resultTextView;

    TextView tvCountry;
    private Button convertButton;

    // Map pentru a ține ratele de conversie
    Map<String, String> currencyCodes = new HashMap<>();

    // Cheia API
    public static final String API_KEY = "6e0c14a87354a24f6487b3c8";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        amountEditText = findViewById(R.id.amountEditText);
        currencySpinner = findViewById(R.id.currencySpinner);
        resultTextView = findViewById(R.id.resultTextView);
        convertButton = findViewById(R.id.convertButton);
        tvCountry = findViewById(R.id.tv_Country);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new ArrayList<>(currencyCodes.keySet())
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);


        // Populăm map-ul cu codurile valutelor
        populateCurrencyCodes();

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        // Setăm un listener pentru butonul de conversie
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FetchExchangeRateTask(MainActivity.this).execute();
            }
        });

        tvAdd = findViewById(R.id.tv_Add);
        btGetAdd = findViewById(R.id.bt_get_Add);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

                if (location != null) {
                    updateUi(location);
                }

            }
        };


        btGetAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocation();
            }
        });
    }


    private void populateCurrencyCodes() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    String apiUrl = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/RON";
                    URL url = new URL(apiUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder stringBuilder = new StringBuilder();

                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }

                        bufferedReader.close();
                        String result = stringBuilder.toString();

                        JSONObject jsonObject = new JSONObject(result);
                        JSONObject rates = jsonObject.getJSONObject("conversion_rates");

                        // Adăugăm toate valutele din API în map
                        Iterator<String> keys = rates.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            currencyCodes.put(key, key);
                        }
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                // După ce am completat map-ul, putem actualiza spinner-ul
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        MainActivity.this,
                        R.layout.spinner_item,
                        new ArrayList<>(currencyCodes.keySet())
                );
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                currencySpinner.setAdapter(adapter);
            }
        }.execute();
    }

//-------------------------LOCATIE


    private void updateUi(Location location) {

        Geocoder geocoder;
        List<Address> addressList;
        geocoder = new Geocoder(this, Locale.getDefault());


        try {
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tvAdd.setText(addressList.get(0).getAddressLine(0));
            country = addressList.get(0).getCountryName();
           String valuta =  getCurrencyForCountry(country);
           tvCountry.setText(valuta);
           // tvCountry.setText(country);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void getLocation() {

        if (checkLocationPermission()) {
            updateAddress();
        } else {
            askLocationPermission();
        }
    }


    private void updateAddress() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        fusedLocationProviderClient = new FusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());


    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void askLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, location_request_id);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == location_request_id) {
            if (grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getLocation();

            }
        }
    }

    private static String getCurrencyForCountry(String country) {
        switch (country.toLowerCase()) {
            case "românia":
                return "RON";
            case "emiratele arabe unite":
                return "AED";
            case "afganistan":
                return "AFN";
            case "albania":
                return "ALL";
            case "armenia":
                return "AMD";
            case "antilele olandeze":
                return "ANG";
            case "angola":
                return "AOA";
            case "argentina":
                return "ARS";
            case "australia":
                return "AUD";
            case "aruba":
                return "AWG";
            case "azerbaidjan":
                return "AZN";
            case "bosnia și herțegovina":
                return "BAM";
            case "barbados":
                return "BBD";
            case "bangladeș":
                return "BDT";
            case "bulgaria":
                return "BGN";
            case "bahrein":
                return "BHD";
            case "burundi":
                return "BIF";
            case "bermude":
                return "BMD";
            case "brunei":
                return "BND";
            case "bolivia":
                return "BOB";
            case "brazilia":
                return "BRL";
            case "bahamas":
                return "BSD";
            case "bhutan":
                return "BTN";
            case "botswana":
                return "BWP";
            case "belarus":
                return "BYN";
            case "belize":
                return "BZD";
            case "canada":
                return "CAD";
            case "congo (RD Congo)":
                return "CDF";
            case "elveția":
                return "CHF";
            case "chile":
                return "CLP";
            case "china":
                return "CNY";
            case "columbia":
                return "COP";
            case "costa rica":
                return "CRC";
            case "cuba":
                return "CUP";
            case "capul verde":
                return "CVE";
            case "republica cehă":
                return "CZK";
            case "djibouti":
                return "DJF";
            case "danemarca":
                return "DKK";
            case "republica dominicană":
                return "DOP";
            case "algeria":
                return "DZD";
            case "egipt":
                return "EGP";
            case "eritreea":
                return "ERN";
            case "etiopia":
                return "ETB";
            case "europa (euro)":
                return "EUR";
            case "fiji":
                return "FJD";
            case "insulele falkland":
                return "FKP";
            case "insulele faroe":
                return "FOK";
            case "regatul unit":
                return "GBP";
            case "georgia":
                return "GEL";
            case "guernsey":
                return "GGP";
            case "ghana":
                return "GHS";
            case "gibraltar":
                return "GIP";
            case "gambia":
                return "GMD";
            case "guineea":
                return "GNF";
            case "guatemala":
                return "GTQ";
            case "guyana":
                return "GYD";
            case "hong kong":
                return "HKD";
            case "honduras":
                return "HNL";
            case "croația":
                return "HRK";
            case "haiti":
                return "HTG";
            case "ungaria":
                return "HUF";
            case "indonezia":
                return "IDR";
            case "israel":
                return "ILS";
            case "insula man":
                return "IMP";
            case "india":
                return "INR";
            case "irak":
                return "IQD";
            case "iran":
                return "IRR";
            case "islanda":
                return "ISK";
            case "jersey":
                return "JEP";
            case "jamaica":
                return "JMD";
            case "iordania":
                return "JOD";
            case "japonia":
                return "JPY";
            case "kenya":
                return "KES";
            case "kârgâzstan":
                return "KGS";
            case "cambodgia":
                return "KHR";
            case "kiribati":
                return "KID";
            case "comore":
                return "KMF";
            case "coreea de sud":
                return "KRW";
            case "kuweit":
                return "KWD";
            case "insula cayman":
                return "KYD";
            case "kazahstan":
                return "KZT";
            case "laos":
                return "LAK";
            case "liban":
                return "LBP";
            case "sri lanka":
                return "LKR";
            case "liberia":
                return "LRD";
            case "lesotho":
                return "LSL";
            case "libia":
                return "LYD";
            case "maroc":
                return "MAD";
            case "moldova":
                return "MDL";
            case "madagascar":
                return "MGA";
            case "macedonia":
                return "MKD";
            case "myanmar":
                return "MMK";
            case "mongolia":
                return "MNT";
            case "macao":
                return "MOP";
            case "mauritania":
                return "MRU";
            case "mauritius":
                return "MUR";
            case "maldives":
                return "MVR";
            case "malawi":
                return "MWK";
            case "mexic":
                return "MXN";
            case "malaezia":
                return "MYR";
            case "mozambic":
                return "MZN";
            case "namibia":
                return "NAD";
            case "nigeria":
                return "NGN";
            case "nicaragua":
                return "NIO";
            case "norvegia":
                return "NOK";
            case "nepal":
                return "NPR";
            case "noua zeelandă":
                return "NZD";
            case "oman":
                return "OMR";
            case "panama":
                return "PAB";
            case "peru":
                return "PEN";
            case "papua noua guinee":
                return "PGK";
            case "filipine":
                return "PHP";
            case "pakistan":
                return "PKR";
            case "polonia":
                return "PLN";
            case "paraguay":
                return "PYG";
            case "qatar":
                return "QAR";
            case "serbia":
                return "RSD";
            case "rusia":
                return "RUB";
            case "rwanda":
                return "RWF";
            case "arabia saudită":
                return "SAR";
            case "insulele solomon":
                return "SBD";
            case "seychelles":
                return "SCR";
            case "sudan":
                return "SDG";
            case "suedia":
                return "SEK";
            case "singapore":
                return "SGD";
            case "saint helena":
                return "SHP";
            case "sierra leone":
                return "SLE";
            case "serra leone":
                return "SLL";
            case "somalia":
                return "SOS";
            case "surinam":
                return "SRD";
            case "sudanul de sud":
                return "SSP";
            case "sao tome și principe":
                return "STN";
            case "siria":
                return "SYP";
            case "eswatini":
                return "SZL";
            case "tailanda":
                return "THB";
            case "tajikistan":
                return "TJS";
            case "turkmenistan":
                return "TMT";
            case "tunisia":
                return "TND";
            case "tonga":
                return "TOP";
            case "turcia":
                return "TRY";
            case "trinidad și tobago":
                return "TTD";
            case "tuvalu":
                return "TVD";
            case "taiwan":
                return "TWD";
            case "tanzania":
                return "TZS";
            case "ucraina":
                return "UAH";
            case "uganda":
                return "UGX";
            case "USA":
                return "USD";
            case "United States":
                return "USD";
            case "uruguay":
                return "UYU";
            case "uzbekistan":
                return "UZS";
            case "venezuela":
                return "VES";
            case "vietnam":
                return "VND";
            case "vanuatu":
                return "VUV";
            case "samoa":
                return "WST";
            case "republica centrafricană":
                return "XAF";
            case "estul caraibelor":
                return "XCD";
            case "drepturile speciale de tragere":
                return "XDR";
            case "vestul africii cfa franc":
                return "XOF";
            case "cfp franc":
                return "XPF";
            case "yemen":
                return "YER";
            case "africa de sud":
                return "ZAR";
            case "zambia":
                return "ZMW";
            case "zimbabwe":
                return "ZWL";
            default:
                return "Valuta necunoscuta";
        }

    }
}
