package com.example.clase4;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

public class PaymentMethodFormActivity extends AppCompatActivity {

    private TextView txtTituloFlujoPago;
    private TextView txtDescripcionFlujoPago;
    private TextView txtMensajeFlujoPago;
    private Spinner spMonedaFlujoPago;
    private Spinner spExtranjeraFlujoPago;
    private EditText edtEntidadFlujoPago;
    private EditText edtReferenciaFlujoPago;
    private EditText edtMontoChequeFlujoPago;
    private Button btnGuardarFlujoPago;
    private Button btnVolverFlujoPago;

    private int userId;
    private String tipo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_method_form);

        txtTituloFlujoPago = findViewById(R.id.txtTituloFlujoPago);
        txtDescripcionFlujoPago = findViewById(R.id.txtDescripcionFlujoPago);
        txtMensajeFlujoPago = findViewById(R.id.txtMensajeFlujoPago);
        spMonedaFlujoPago = findViewById(R.id.spMonedaFlujoPago);
        spExtranjeraFlujoPago = findViewById(R.id.spExtranjeraFlujoPago);
        edtEntidadFlujoPago = findViewById(R.id.edtEntidadFlujoPago);
        edtReferenciaFlujoPago = findViewById(R.id.edtReferenciaFlujoPago);
        edtMontoChequeFlujoPago = findViewById(R.id.edtMontoChequeFlujoPago);
        btnGuardarFlujoPago = findViewById(R.id.btnGuardarFlujoPago);
        btnVolverFlujoPago = findViewById(R.id.btnVolverFlujoPago);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        tipo = getIntent().getStringExtra("tipo");
        if (tipo == null) {
            tipo = "cuenta_bancaria";
        }

        configurarSpinners();
        configurarTextosPorTipo();

        btnGuardarFlujoPago.setOnClickListener(v -> validarYGuardar());
        btnVolverFlujoPago.setOnClickListener(v -> finish());
    }

    private void configurarSpinners() {
        spMonedaFlujoPago.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Pesos", "Dólares"}
        ));

        spExtranjeraFlujoPago.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nacional", "Extranjera"}
        ));
    }

    private void configurarTextosPorTipo() {
        if (tipo.equals("tarjeta_credito")) {
            txtTituloFlujoPago.setText("Tarjeta de crédito");
            txtDescripcionFlujoPago.setText("Cargá el emisor y una referencia segura de la tarjeta. Queda pendiente de verificación.");
            edtEntidadFlujoPago.setHint("Emisor, por ejemplo Visa");
            edtReferenciaFlujoPago.setHint("Últimos 4 dígitos o referencia segura");
            edtMontoChequeFlujoPago.setVisibility(View.GONE);
        } else if (tipo.equals("cheque_certificado")) {
            txtTituloFlujoPago.setText("Cheque certificado");
            txtDescripcionFlujoPago.setText("Informá banco, número de cheque y monto certificado reservado para subastas.");
            edtEntidadFlujoPago.setHint("Banco certificante");
            edtReferenciaFlujoPago.setHint("Número de cheque certificado");
            edtMontoChequeFlujoPago.setVisibility(View.VISIBLE);
        } else {
            txtTituloFlujoPago.setText("Cuenta bancaria");
            txtDescripcionFlujoPago.setText("Cargá banco y CBU/CVU o número de cuenta. Puede ser nacional o extranjera.");
            edtEntidadFlujoPago.setHint("Banco");
            edtReferenciaFlujoPago.setHint("CBU, CVU o número de cuenta");
            edtMontoChequeFlujoPago.setVisibility(View.GONE);
        }
    }

    private void validarYGuardar() {
        String entidad = edtEntidadFlujoPago.getText().toString().trim();
        String referencia = edtReferenciaFlujoPago.getText().toString().trim();
        String montoCheque = edtMontoChequeFlujoPago.getText().toString().trim();

        if (entidad.isEmpty() || referencia.isEmpty()) {
            txtMensajeFlujoPago.setText("Completá entidad y referencia.");
            return;
        }

        if (tipo.equals("cheque_certificado") && montoCheque.isEmpty()) {
            txtMensajeFlujoPago.setText("Informá el monto certificado.");
            return;
        }

        btnGuardarFlujoPago.setEnabled(false);
        btnGuardarFlujoPago.setText("Registrando...");
        txtMensajeFlujoPago.setText("");

        guardarMedioPago(entidad, referencia, montoCheque);
    }

    private void guardarMedioPago(String entidad, String referencia, String montoCheque) {
        final String moneda = spMonedaFlujoPago.getSelectedItemPosition() == 1 ? "dolares" : "pesos";
        final String esExtranjera = spExtranjeraFlujoPago.getSelectedItemPosition() == 1 ? "si" : "no";

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
                    btnGuardarFlujoPago.setEnabled(true);
                    btnGuardarFlujoPago.setText("Registrar medio");

                    if (statusCode == 201) {
                        txtMensajeFlujoPago.setText(json.optString(
                                "mensaje",
                                "Medio registrado. Queda pendiente de verificación."
                        ));
                        edtEntidadFlujoPago.setText("");
                        edtReferenciaFlujoPago.setText("");
                        edtMontoChequeFlujoPago.setText("");
                    } else {
                        txtMensajeFlujoPago.setText(json.optString(
                                "error",
                                "No se pudo registrar el medio."
                        ));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnGuardarFlujoPago.setEnabled(true);
                    btnGuardarFlujoPago.setText("Registrar medio");
                    txtMensajeFlujoPago.setText("No se pudo conectar con el servidor.");
                });
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
