package com.example.clase4;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroActivity extends AppCompatActivity {

    private static final int REQ_DNI_FRENTE = 1001;
    private static final int REQ_DNI_DORSO = 1002;

    private EditText etDocumento;
    private EditText etNombre;
    private EditText etApellido;
    private EditText etDireccion;
    private EditText etEmail;
    private EditText etTelefono;
    private EditText etPais;
    private EditText etDocumentoPaso2;
    private EditText etClavePaso2;
    private TextView txtDniFrente;
    private TextView txtDniDorso;
    private TextView txtMensajeRegistro;
    private TextView txtMensajePaso2;
    private Button btnSeleccionarDniFrente;
    private Button btnSeleccionarDniDorso;
    private Button btnRegistrar;
    private Button btnCompletarRegistro;

    private String fotoDniFrenteBase64;
    private String fotoDniDorsoBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etDocumento = findViewById(R.id.etDocumento);
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etDireccion = findViewById(R.id.etDireccion);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etPais = findViewById(R.id.etPais);
        etDocumentoPaso2 = findViewById(R.id.etDocumentoPaso2);
        etClavePaso2 = findViewById(R.id.etClavePaso2);
        txtDniFrente = findViewById(R.id.txtDniFrente);
        txtDniDorso = findViewById(R.id.txtDniDorso);
        txtMensajeRegistro = findViewById(R.id.txtMensajeRegistro);
        txtMensajePaso2 = findViewById(R.id.txtMensajePaso2);
        btnSeleccionarDniFrente = findViewById(R.id.btnSeleccionarDniFrente);
        btnSeleccionarDniDorso = findViewById(R.id.btnSeleccionarDniDorso);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnCompletarRegistro = findViewById(R.id.btnCompletarRegistro);

        String documentoPendiente = getIntent().getStringExtra("documento");
        if (documentoPendiente != null) {
            etDocumentoPaso2.setText(documentoPendiente);
        }

        btnSeleccionarDniFrente.setOnClickListener(v -> seleccionarImagen(REQ_DNI_FRENTE));
        btnSeleccionarDniDorso.setOnClickListener(v -> seleccionarImagen(REQ_DNI_DORSO));
        btnRegistrar.setOnClickListener(v -> enviarPaso1());
        btnCompletarRegistro.setOnClickListener(v -> enviarPaso2());
    }

    private void seleccionarImagen(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        try {
            String base64 = leerImagenBase64(uri);

            if (requestCode == REQ_DNI_FRENTE) {
                fotoDniFrenteBase64 = base64;
                txtDniFrente.setText("Frente seleccionado");
            } else if (requestCode == REQ_DNI_DORSO) {
                fotoDniDorsoBase64 = base64;
                txtDniDorso.setText("Dorso seleccionado");
            }
        } catch (Exception e) {
            txtMensajeRegistro.setText("No se pudo leer la imagen seleccionada.");
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

    private void enviarPaso1() {
        String documento = etDocumento.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String paisTexto = etPais.getText().toString().trim();

        if (documento.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || direccion.isEmpty()) {
            txtMensajeRegistro.setText("Completá documento, nombre, apellido y domicilio legal.");
            return;
        }

        if (fotoDniFrenteBase64 == null || fotoDniDorsoBase64 == null) {
            txtMensajeRegistro.setText("Seleccioná frente y dorso del DNI.");
            return;
        }

        Integer numeroPais = null;
        if (!paisTexto.isEmpty()) {
            try {
                numeroPais = Integer.parseInt(paisTexto);
            } catch (Exception e) {
                txtMensajeRegistro.setText("El código de país debe ser numérico.");
                return;
            }
        }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Enviando...");
        txtMensajeRegistro.setText("");

        RegistroRequest request = new RegistroRequest(
                documento,
                nombre,
                apellido,
                direccion,
                email,
                telefono,
                numeroPais,
                fotoDniFrenteBase64,
                fotoDniDorsoBase64
        );

        ApiClient.getClient().create(ApiService.class).registroPaso1(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnRegistrar.setEnabled(true);
                btnRegistrar.setText("Enviar solicitud");

                if (response.isSuccessful()) {
                    Intent intent = new Intent(RegistroActivity.this, PendingVerificationActivity.class);
                    intent.putExtra("documento", documento);
                    startActivity(intent);
                    finish();
                } else if (response.code() == 409) {
                    txtMensajeRegistro.setText("Ya existe una solicitud con ese documento.");
                } else {
                    txtMensajeRegistro.setText("No se pudo enviar la solicitud. Revisá los datos.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnRegistrar.setEnabled(true);
                btnRegistrar.setText("Enviar solicitud");
                txtMensajeRegistro.setText("No se pudo conectar con el servidor.");
            }
        });
    }

    private void enviarPaso2() {
        String documento = etDocumentoPaso2.getText().toString().trim();
        String clave = etClavePaso2.getText().toString().trim();

        if (documento.isEmpty() || clave.isEmpty()) {
            txtMensajePaso2.setText("Ingresá documento y clave.");
            return;
        }

        btnCompletarRegistro.setEnabled(false);
        btnCompletarRegistro.setText("Generando...");
        txtMensajePaso2.setText("");

        RegistroPaso2Request request = new RegistroPaso2Request(documento, clave);

        ApiClient.getClient().create(ApiService.class).registroPaso2(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnCompletarRegistro.setEnabled(true);
                btnCompletarRegistro.setText("Generar clave");

                if (response.isSuccessful()) {
                    txtMensajePaso2.setText("Clave generada. Ya podés iniciar sesión.");
                    startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
                    finish();
                } else if (response.code() == 403) {
                    txtMensajePaso2.setText("La empresa todavía no aprobó esta cuenta.");
                } else {
                    txtMensajePaso2.setText("No se pudo generar la clave.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnCompletarRegistro.setEnabled(true);
                btnCompletarRegistro.setText("Generar clave");
                txtMensajePaso2.setText("No se pudo conectar con el servidor.");
            }
        });
    }
}
