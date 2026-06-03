package com.example.clase4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentMethodsActivity extends AppCompatActivity {

    private TextView txtMensajeMediosPago;
    private Spinner spTipoMedioPago;
    private Spinner spMonedaMedioPago;
    private Spinner spEsExtranjera;
    private EditText edtEntidadMedioPago;
    private EditText edtReferenciaMedioPago;
    private EditText edtMontoCheque;
    private Button btnNuevaCuentaBancaria;
    private Button btnNuevaTarjetaCredito;
    private Button btnNuevoChequeCertificado;
    private Button btnAgregarMedioPago;
    private Button btnActualizarMediosPago;
    private Button btnVolverDesdeMediosPago;
    private LinearLayout contenedorMediosPago;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        txtMensajeMediosPago = findViewById(R.id.txtMensajeMediosPago);
        spTipoMedioPago = findViewById(R.id.spTipoMedioPago);
        spMonedaMedioPago = findViewById(R.id.spMonedaMedioPago);
        spEsExtranjera = findViewById(R.id.spEsExtranjera);
        edtEntidadMedioPago = findViewById(R.id.edtEntidadMedioPago);
        edtReferenciaMedioPago = findViewById(R.id.edtReferenciaMedioPago);
        edtMontoCheque = findViewById(R.id.edtMontoCheque);
        btnNuevaCuentaBancaria = findViewById(R.id.btnNuevaCuentaBancaria);
        btnNuevaTarjetaCredito = findViewById(R.id.btnNuevaTarjetaCredito);
        btnNuevoChequeCertificado = findViewById(R.id.btnNuevoChequeCertificado);
        btnAgregarMedioPago = findViewById(R.id.btnAgregarMedioPago);
        btnActualizarMediosPago = findViewById(R.id.btnActualizarMediosPago);
        btnVolverDesdeMediosPago = findViewById(R.id.btnVolverDesdeMediosPago);
        contenedorMediosPago = findViewById(R.id.contenedorMediosPago);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        configurarSpinners();

        btnNuevaCuentaBancaria.setOnClickListener(v -> abrirFlujoMedioPago("cuenta_bancaria"));
        btnNuevaTarjetaCredito.setOnClickListener(v -> abrirFlujoMedioPago("tarjeta_credito"));
        btnNuevoChequeCertificado.setOnClickListener(v -> abrirFlujoMedioPago("cheque_certificado"));
        btnAgregarMedioPago.setOnClickListener(v -> validarYAgregarMedioPago());
        btnActualizarMediosPago.setOnClickListener(v -> cargarMediosPago());
        btnVolverDesdeMediosPago.setOnClickListener(v -> finish());

        cargarMediosPago();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (contenedorMediosPago != null) {
            cargarMediosPago();
        }
    }

    private void abrirFlujoMedioPago(String tipo) {
        Intent intent = new Intent(PaymentMethodsActivity.this, PaymentMethodFormActivity.class);
        intent.putExtra("tipo", tipo);
        startActivity(intent);
    }

    private void configurarSpinners() {
        ArrayAdapter<String> tipos = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Cuenta bancaria", "Tarjeta de crédito", "Cheque certificado"}
        );
        spTipoMedioPago.setAdapter(tipos);

        ArrayAdapter<String> monedas = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Pesos", "Dólares"}
        );
        spMonedaMedioPago.setAdapter(monedas);

        ArrayAdapter<String> extranjera = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nacional", "Extranjera"}
        );
        spEsExtranjera.setAdapter(extranjera);
    }

    private void validarYAgregarMedioPago() {
        String entidad = edtEntidadMedioPago.getText().toString().trim();
        String referencia = edtReferenciaMedioPago.getText().toString().trim();
        String montoCheque = edtMontoCheque.getText().toString().trim();
        String tipo = obtenerTipoSeleccionado();

        if (entidad.isEmpty() || referencia.isEmpty()) {
            txtMensajeMediosPago.setText("Completá entidad y referencia del medio de pago.");
            return;
        }

        if (tipo.equals("cheque_certificado") && montoCheque.isEmpty()) {
            txtMensajeMediosPago.setText("Para cheque certificado informá el monto reservado.");
            return;
        }

        btnAgregarMedioPago.setEnabled(false);
        btnAgregarMedioPago.setText("Registrando...");
        txtMensajeMediosPago.setText("");

        agregarMedioPago(tipo, entidad, referencia, montoCheque);
    }

    private String obtenerTipoSeleccionado() {
        int posicion = spTipoMedioPago.getSelectedItemPosition();
        if (posicion == 1) return "tarjeta_credito";
        if (posicion == 2) return "cheque_certificado";
        return "cuenta_bancaria";
    }

    private String obtenerMonedaSeleccionada() {
        return spMonedaMedioPago.getSelectedItemPosition() == 1 ? "dolares" : "pesos";
    }

    private String obtenerExtranjeraSeleccionada() {
        return spEsExtranjera.getSelectedItemPosition() == 1 ? "si" : "no";
    }

    private void agregarMedioPago(
            String tipo,
            String entidad,
            String referencia,
            String montoCheque
    ) {
        final String moneda = obtenerMonedaSeleccionada();
        final String esExtranjera = obtenerExtranjeraSeleccionada();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/payment-methods");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("tipo", tipo);
                body.put("entidad", entidad);
                body.put("numeroReferencia", referencia);
                body.put("moneda", moneda);
                body.put("esExtranjera", esExtranjera);

                if (tipo.equals("cheque_certificado")) {
                    body.put("montoCheque", Double.parseDouble(montoCheque));
                }

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = connection.getResponseCode();
                InputStream inputStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String respuesta = leerRespuesta(inputStream);
                JSONObject json = new JSONObject(respuesta);

                mainHandler.post(() -> {
                    btnAgregarMedioPago.setEnabled(true);
                    btnAgregarMedioPago.setText("Registrar medio de pago");

                    if (statusCode == 201) {
                        txtMensajeMediosPago.setText(json.optString(
                                "mensaje",
                                "Medio de pago registrado. Queda pendiente de verificación."
                        ));
                        edtEntidadMedioPago.setText("");
                        edtReferenciaMedioPago.setText("");
                        edtMontoCheque.setText("");
                        cargarMediosPago();
                    } else {
                        txtMensajeMediosPago.setText(json.optString(
                                "error",
                                "No se pudo registrar el medio de pago."
                        ));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnAgregarMedioPago.setEnabled(true);
                    btnAgregarMedioPago.setText("Registrar medio de pago");
                    txtMensajeMediosPago.setText("No se pudo conectar con el servidor.");
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void cargarMediosPago() {
        txtMensajeMediosPago.setText("Cargando medios de pago...");
        contenedorMediosPago.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/payment-methods");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();

                InputStream inputStream;

                if (statusCode >= 200 && statusCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                String respuesta = leerRespuesta(inputStream);

                if (statusCode == 200) {
                    JSONArray mediosPago = new JSONArray(respuesta);
                    mainHandler.post(() -> mostrarMediosPago(mediosPago));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar medios de pago");
                    mainHandler.post(() -> txtMensajeMediosPago.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeMediosPago.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarMediosPago(JSONArray mediosPago) {
        contenedorMediosPago.removeAllViews();

        if (mediosPago.length() == 0) {
            txtMensajeMediosPago.setText("No tenés medios de pago registrados.");
            return;
        }

        txtMensajeMediosPago.setText("Medios de pago encontrados: " + mediosPago.length());

        try {
            for (int i = 0; i < mediosPago.length(); i++) {
                JSONObject medio = mediosPago.getJSONObject(i);

                int id = medio.getInt("id");
                String tipo = medio.optString("tipo", "-");
                String entidad = medio.optString("entidad", "-");
                String numeroReferencia = medio.optString("numeroReferencia", "-");
                String esExtranjera = medio.optString("esExtranjera", "-");
                String moneda = medio.optString("moneda", "-");
                String verificado = medio.optString("verificado", "-");
                double montoCheque = medio.optDouble("montoCheque", 0);
                double montoDisponible = medio.optDouble("montoDisponible", 0);

                View card = crearCardMedioPago(
                        id,
                        tipo,
                        entidad,
                        numeroReferencia,
                        esExtranjera,
                        moneda,
                        verificado,
                        montoCheque,
                        montoDisponible
                );

                contenedorMediosPago.addView(card);
            }

        } catch (Exception e) {
            txtMensajeMediosPago.setText("Error mostrando medios de pago.");
        }
    }

    private View crearCardMedioPago(
            int id,
            String tipo,
            String entidad,
            String numeroReferencia,
            String esExtranjera,
            String moneda,
            String verificado,
            double montoCheque,
            double montoDisponible
    ) {
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
        titulo.setText("Medio de pago #" + id);
        titulo.setTextSize(18);
        titulo.setTextColor(Color.parseColor("#0F172A"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detalle = new TextView(this);

        String textoDetalle =
                "Tipo: " + formatearTipo(tipo) + "\n" +
                        "Entidad: " + entidad + "\n" +
                        "Referencia: " + numeroReferencia + "\n" +
                        "Extranjera: " + esExtranjera + "\n" +
                        "Moneda: " + moneda + "\n" +
                        "Verificado: " + verificado;

        if (tipo.equals("cheque_certificado")) {
            textoDetalle += "\nMonto cheque: $" + montoCheque;
            textoDetalle += "\nMonto disponible: $" + montoDisponible;
        }

        detalle.setText(textoDetalle);
        detalle.setTextSize(15);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setPadding(0, 12, 0, 12);

        TextView estado = new TextView(this);

        if (verificado.equals("si")) {
            estado.setText("Medio de pago habilitado");
            estado.setTextColor(Color.parseColor("#16A34A"));
        } else {
            estado.setText("Pendiente de verificación");
            estado.setTextColor(Color.parseColor("#DC2626"));
        }

        estado.setTextSize(14);
        estado.setTypeface(null, android.graphics.Typeface.BOLD);

        card.addView(titulo);
        card.addView(detalle);
        card.addView(estado);

        return card;
    }

    private String formatearTipo(String tipo) {
        if (tipo.equals("tarjeta_credito")) return "Tarjeta de crédito";
        if (tipo.equals("cuenta_bancaria")) return "Cuenta bancaria";
        if (tipo.equals("cheque_certificado")) return "Cheque certificado";
        return tipo;
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
