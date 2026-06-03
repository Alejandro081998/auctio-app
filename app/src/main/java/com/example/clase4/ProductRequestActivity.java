package com.example.clase4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRequestActivity extends AppCompatActivity {

    private static final int REQ_FOTO_BASE = 3100;
    private static final int TOTAL_FOTOS_REQUERIDAS = 6;

    private EditText edtDescripcionCatalogo;
    private EditText edtDescripcionCompleta;
    private EditText edtHistoria;
    private EditText edtArtista;
    private EditText edtPrecioBaseSugerido;
    private TextView[] txtFotos;
    private String[] fotosBase64;
    private CheckBox chkPropiedad;
    private CheckBox chkOrigenLicito;
    private TextView txtMensajeSolicitud;
    private Button btnEnviarSolicitud;
    private Button btnVolverSolicitud;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_request);

        edtDescripcionCatalogo = findViewById(R.id.edtDescripcionCatalogo);
        edtDescripcionCompleta = findViewById(R.id.edtDescripcionCompleta);
        edtHistoria = findViewById(R.id.edtHistoria);
        edtArtista = findViewById(R.id.edtArtista);
        edtPrecioBaseSugerido = findViewById(R.id.edtPrecioBaseSugerido);
        chkPropiedad = findViewById(R.id.chkPropiedad);
        chkOrigenLicito = findViewById(R.id.chkOrigenLicito);
        txtMensajeSolicitud = findViewById(R.id.txtMensajeSolicitud);
        btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);
        btnVolverSolicitud = findViewById(R.id.btnVolverSolicitud);

        fotosBase64 = new String[TOTAL_FOTOS_REQUERIDAS];
        txtFotos = new TextView[]{
                findViewById(R.id.txtFoto1),
                findViewById(R.id.txtFoto2),
                findViewById(R.id.txtFoto3),
                findViewById(R.id.txtFoto4),
                findViewById(R.id.txtFoto5),
                findViewById(R.id.txtFoto6)
        };

        Button[] botonesFotos = new Button[]{
                findViewById(R.id.btnFoto1),
                findViewById(R.id.btnFoto2),
                findViewById(R.id.btnFoto3),
                findViewById(R.id.btnFoto4),
                findViewById(R.id.btnFoto5),
                findViewById(R.id.btnFoto6)
        };

        for (int i = 0; i < botonesFotos.length; i++) {
            final int indice = i;
            botonesFotos[i].setOnClickListener(v -> seleccionarFoto(indice));
        }

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnEnviarSolicitud.setOnClickListener(v -> validarYEnviarSolicitud());
        btnVolverSolicitud.setOnClickListener(v -> finish());
    }

    private void seleccionarFoto(int indice) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_FOTO_BASE + indice);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        int indice = requestCode - REQ_FOTO_BASE;
        if (indice < 0 || indice >= TOTAL_FOTOS_REQUERIDAS) {
            return;
        }

        try {
            Uri uri = data.getData();
            fotosBase64[indice] = leerImagenBase64(uri);
            txtFotos[indice].setText("Foto " + (indice + 1) + " seleccionada");
        } catch (Exception e) {
            txtMensajeSolicitud.setText("No se pudo leer la foto seleccionada.");
        }
    }

    private String leerImagenBase64(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while (inputStream != null && (bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private void validarYEnviarSolicitud() {
        String descripcionCatalogo = edtDescripcionCatalogo.getText().toString().trim();
        String descripcionCompleta = edtDescripcionCompleta.getText().toString().trim();
        String historia = edtHistoria.getText().toString().trim();
        String artista = edtArtista.getText().toString().trim();
        String precioBaseSugerido = edtPrecioBaseSugerido.getText().toString().trim();
        JSONArray fotos = new JSONArray();

        if (descripcionCatalogo.isEmpty()) {
            txtMensajeSolicitud.setText("Ingresá un título corto para el catálogo.");
            return;
        }

        if (descripcionCompleta.isEmpty()) {
            txtMensajeSolicitud.setText("Ingresá una descripción completa del artículo.");
            return;
        }

        for (int i = 0; i < TOTAL_FOTOS_REQUERIDAS; i++) {
            if (fotosBase64[i] == null) {
                txtMensajeSolicitud.setText("Seleccioná las 6 fotos mínimas del bien.");
                return;
            }
            fotos.put(fotosBase64[i]);
        }

        if (!precioBaseSugerido.isEmpty()) {
            try {
                Double.parseDouble(precioBaseSugerido);
            } catch (Exception e) {
                txtMensajeSolicitud.setText("El precio base sugerido debe ser numérico.");
                return;
            }
        }

        if (!chkPropiedad.isChecked()) {
            txtMensajeSolicitud.setText("Debés declarar que el bien te pertenece.");
            return;
        }

        if (!chkOrigenLicito.isChecked()) {
            txtMensajeSolicitud.setText("Debés declarar el origen lícito del bien.");
            return;
        }

        txtMensajeSolicitud.setText("");
        btnEnviarSolicitud.setEnabled(false);
        btnEnviarSolicitud.setText("Enviando...");

        enviarSolicitud(
                descripcionCatalogo,
                descripcionCompleta,
                historia,
                artista,
                precioBaseSugerido,
                fotos
        );
    }

    private void enviarSolicitud(
            String descripcionCatalogo,
            String descripcionCompleta,
            String historia,
            String artista,
            String precioBaseSugerido,
            JSONArray fotos
    ) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/products");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("duenio", userId);
                body.put("descripcionCatalogo", descripcionCatalogo);
                body.put("descripcionCompleta", descripcionCompleta);
                body.put("historia", historia);
                body.put("artistaDiseniador", artista);
                if (!precioBaseSugerido.isEmpty()) {
                    body.put("precioBaseSugerido", Double.parseDouble(precioBaseSugerido));
                }
                body.put("fotos", fotos);
                body.put("declaracionPropiedad", "si");
                body.put("origenLicito", "si");

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

                if (statusCode == 202 || statusCode == 201 || statusCode == 200) {
                    String mensaje = json.optString("mensaje", "Solicitud enviada correctamente");

                    mainHandler.post(() -> {
                        btnEnviarSolicitud.setEnabled(true);
                        btnEnviarSolicitud.setText("Enviar solicitud");
                        txtMensajeSolicitud.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        txtMensajeSolicitud.setText(mensaje);
                        limpiarFormulario();
                    });
                } else {
                    String error = json.optString("error", "No se pudo enviar la solicitud");

                    mainHandler.post(() -> {
                        btnEnviarSolicitud.setEnabled(true);
                        btnEnviarSolicitud.setText("Enviar solicitud");
                        txtMensajeSolicitud.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        txtMensajeSolicitud.setText(error);
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnEnviarSolicitud.setEnabled(true);
                    btnEnviarSolicitud.setText("Enviar solicitud");
                    txtMensajeSolicitud.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    txtMensajeSolicitud.setText("No se pudo conectar con el servidor.");
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void limpiarFormulario() {
        edtDescripcionCatalogo.setText("");
        edtDescripcionCompleta.setText("");
        edtHistoria.setText("");
        edtArtista.setText("");
        edtPrecioBaseSugerido.setText("");
        fotosBase64 = new String[TOTAL_FOTOS_REQUERIDAS];
        for (int i = 0; i < txtFotos.length; i++) {
            txtFotos[i].setText("Foto " + (i + 1) + " pendiente");
        }
        chkPropiedad.setChecked(false);
        chkOrigenLicito.setChecked(false);
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
