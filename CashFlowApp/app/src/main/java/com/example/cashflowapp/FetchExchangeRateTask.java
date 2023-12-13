package com.example.cashflowapp;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class FetchExchangeRateTask extends AsyncTask<Void, Void, String> {
    private MainActivity mainActivity;

    public FetchExchangeRateTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            // Folosește mainActivity pentru a accesa membrii privați
            String selectedCurrencyCode = mainActivity.currencyCodes.get(mainActivity.currencySpinner.getSelectedItem().toString());
            String apiUrl = "https://v6.exchangerate-api.com/v6/" + MainActivity.API_KEY + "/latest/RON";
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
                return stringBuilder.toString();
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject rates = jsonObject.getJSONObject("conversion_rates");

            String selectedCurrencyCode = mainActivity.currencyCodes.get(mainActivity.currencySpinner.getSelectedItem().toString());
            double conversionRate = rates.getDouble(selectedCurrencyCode);

            // Obținem valoarea introdusă în EditText
            String amountString = mainActivity.amountEditText.getText().toString();

            if (!amountString.isEmpty()) {
                // Convertim valoarea la tipul double
                double amount = Double.parseDouble(amountString);

                // Calculăm rezultatul conversiei
                double resultValue = amount * conversionRate;

                // Afișăm rezultatul în TextView
                mainActivity.resultTextView.setText(String.format("%.2f RON = %.2f %s", amount, resultValue, mainActivity.currencySpinner.getSelectedItem().toString()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    }

