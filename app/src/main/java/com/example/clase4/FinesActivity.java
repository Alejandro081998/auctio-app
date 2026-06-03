package com.example.clase4;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinesActivity extends AppCompatActivity {

    private TextView txtMensajeMultas;
    private Button btnActualizarMultas;
    private Button btnVolverMultas;
    private LinearLayout contenedorMultas;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fines);

        txtMensajeMultas = findViewById(R.id.txtMensajeMultas);
        btnActualizarMultas = findViewById(R.id.btnActualizarMultas);
        btnVolverMultas = findViewById(R.id.btnVolverMultas);
        contenedorMultas = findViewById(R.id.contenedorMultas);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnActualizarMultas.setOnClickListener(v -> cargarMultas());
        btnVolverMultas.setOnClickListener(v -> finish());

        cargarMultas();
    }

    private void cargarMultas() {
        txtMensajeMultas.setText("Cargando multas...");
        contenedorMultas.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/fines");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();
                InputStream inputStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String respuesta = leerRespuesta(inputStream);

                if (statusCode == 200) {
                    JSONArray multas = new JSONArray(respuesta);
                    mainHandler.post(() -> mostrarMultas(multas));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar multas");
                    mainHandler.post(() -> txtMensajeMultas.setText(error));
                }
            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeMultas.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarMultas(JSONArray multas) {
        contenedorMultas.removeAllViews();

        if (multas.length() == 0) {
            txtMensajeMultas.setText("No tenés multas registradas.");
            return;
        }

        txtMensajeMultas.setText("Multas encontradas: " + multas.length());

        try {
            for (int i = 0; i < multas.length(); i++) {
                JSONObject multa = multas.getJSONObject(i);
                contenedorMultas.addView(crearCardMulta(multa));
            }
        } catch (Exception e) {
            txtMensajeMultas.setText("Error mostrando multas.");
        }
    }

    private View crearCardMulta(JSONObject multa) throws Exception {
        int id = multa.optInt("id", 0);
        int subastaId = multa.optInt("subastaId", 0);
        double monto = multa.optDouble("monto", 0);
        String pagada = multa.optString("pagada", "no");
        String fecha = multa.optString("fechaGeneracion", "-");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 24, 28, 24);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 22);
        card.setLayoutParams(params);
        card.setElevation(4);

        TextView titulo = new TextView(this);
        titulo.setText("Multa #" + id);
        titulo.setTextSize(18);
        titulo.setTextColor(Color.parseColor("#0F172A"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detalle = new TextView(this);
        detalle.setText(
                "Subasta: #" + subastaId + "\n" +
                        "Monto: $" + monto + "\n" +
                        "Estado: " + (pagada.equals("si") ? "Pagada" : "Pendiente") + "\n" +
                        "Fecha: " + fecha
        );
        detalle.setTextSize(15);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setPadding(0, 12, 0, 12);

        Button btnPagar = new Button(this);
        btnPagar.setText(pagada.equals("si") ? "Multa pagada" : "Marcar como pagada");
        btnPagar.setEnabled(!pagada.equals("si"));
        btnPagar.setTextColor(Color.WHITE);
        btnPagar.setBackgroundColor(pagada.equals("si")
                ? Color.parseColor("#64748B")
                : Color.parseColor("#2563EB"));
        btnPagar.setOnClickListener(v -> pagarMulta(id));

        card.addView(titulo);
        card.addView(detalle);
        card.addView(btnPagar);

        return card;
    }

    private void pagarMulta(int fineId) {
        txtMensajeMultas.setText("Registrando pago de multa...");

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/fines/" + fineId + "/pay");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();
                InputStream inputStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String respuesta = leerRespuesta(inputStream);
                JSONObject json = new JSONObject(respuesta);

                mainHandler.post(() -> {
                    txtMensajeMultas.setText(json.optString(
                            statusCode == 200 ? "mensaje" : "error",
                            statusCode == 200 ? "Multa marcada como pagada." : "No se pudo pagar la multa."
                    ));
                    cargarMultas();
                });
            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeMultas.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String leerRespuesta(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder respuesta = new StringBuilder();
        String linea;

        while ((linea = reader.readLine()) != null) {
            respuesta.append(linea);
        }

        return respuesta.toString();
    }
}
