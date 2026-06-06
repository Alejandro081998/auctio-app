package com.example.clase4;

import android.app.AlertDialog;
import android.content.Context;

public class FeedbackDialog {
    public static void ok(Context context, String mensaje) {
        mostrar(context, "Operacion realizada", mensaje);
    }

    public static void error(Context context, String mensaje) {
        mostrar(context, "No se pudo completar", mensaje);
    }

    public static void info(Context context, String titulo, String mensaje) {
        mostrar(context, titulo, mensaje);
    }

    private static void mostrar(Context context, String titulo, String mensaje) {
        new AlertDialog.Builder(context)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show();
    }
}
