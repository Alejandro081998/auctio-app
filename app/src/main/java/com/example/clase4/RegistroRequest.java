package com.example.clase4;

public class RegistroRequest {
    String documento;
    String nombre;
    String apellido;
    String direccion;
    String email;
    String telefono;
    Integer numeroPais;
    String fotoDniFrente;
    String fotoDniDorso;

    public RegistroRequest(String d, String n, String a, String dir) {
        this(d, n, a, dir, "", "", null, "", "");
    }

    public RegistroRequest(
            String documento,
            String nombre,
            String apellido,
            String direccion,
            String email,
            String telefono,
            Integer numeroPais,
            String fotoDniFrente,
            String fotoDniDorso
    ) {
        this.documento = documento;
        this.nombre = nombre;
        this.apellido = apellido;
        this.direccion = direccion;
        this.email = email;
        this.telefono = telefono;
        this.numeroPais = numeroPais;
        this.fotoDniFrente = fotoDniFrente;
        this.fotoDniDorso = fotoDniDorso;
    }
}
