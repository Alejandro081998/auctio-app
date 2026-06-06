# Segunda entrega - flujo demo integrado

Este documento describe el flujo completo para demostrar que el frontend Android, el backend Node.js y la base SQL Server estan conectados.

## Objetivo de la demo

Mostrar un circuito funcional de punta a punta:

`Registro KYC -> aprobacion interna -> generacion de clave -> login -> carga de medio de pago -> verificacion interna -> ingreso a subasta -> catalogo -> puja`

## Preparacion

1. Ejecutar `database/SCRIPT.txt` en SQL Server.
2. Levantar el backend desde la carpeta `backend`:

```bash
node index.js
```

3. Verificar la URL del backend en `app/src/main/java/com/example/clase4/ApiConfig.java`.

Para emulador Android:

```java
public static final String BASE_URL = "http://10.0.2.2:3000";
```

Para celular fisico:

```java
public static final String BASE_URL = "http://IP_DE_LA_PC:3000";
```

4. Compilar y ejecutar la app desde Android Studio.

## Usuarios base

- Admin/verificador: `20000111` / `1234`
- Cliente ya habilitado: `30123456` / `1234`
- Cliente ya habilitado: `30999888` / `1234`
- Cliente con multa pendiente: `31888777` / `1234`
- Usuario pendiente precargado: `40000111` / `1234`

## Flujo completo desde registro nuevo

Este es el flujo principal recomendado para la defensa.

### 1. Registrar un nuevo postor

Desde Login:

1. Tocar `Solicita tu validacion KYC` o entrar a Registro.
2. Completar datos personales.
3. Cargar:
   - Documento.
   - Nombre.
   - Apellido.
   - Domicilio legal.
   - Email.
   - Telefono.
   - Pais, por ejemplo `32`.
4. Seleccionar foto frente del DNI.
5. Seleccionar foto dorso del DNI.
6. Tocar `Enviar solicitud`.

Datos sugeridos para no chocar con usuarios existentes:

- Documento: `50000123`
- Nombre: `Demo`
- Apellido: `Postor`
- Domicilio: `Av. Demo 123`
- Email: `demo50000123@test.com`
- Telefono: `1150000123`
- Pais: `32`

Resultado esperado:

- La app informa que la solicitud fue recibida.
- El usuario queda pendiente de verificacion.
- Todavia no puede iniciar sesion como cliente habilitado.

### 2. Aprobar el usuario desde panel interno

Salir del registro e iniciar sesion como admin:

- Documento: `20000111`
- Clave: `1234`

Resultado esperado:

- La app abre `Panel interno`.
- En `Pendientes internos` aparece el usuario registrado.

Operacion:

1. Tocar `Actualizar pendientes internos`.
2. Buscar el usuario nuevo por documento, por ejemplo `50000123`.
3. Copiar su `ID`.
4. Cargar ese ID en `ID de usuario a verificar`.
5. Elegir categoria, por ejemplo `oro` o `plata`.
6. Tocar `Aprobar usuario y asignar categoria`.

Resultado esperado:

- El backend actualiza `Users.estado = activo`.
- El backend actualiza `Clients.admitido = si`.
- El cliente queda habilitado para completar la segunda etapa de registro.

### 3. Generar clave del usuario aprobado

Volver a la pantalla de Registro.

En el bloque `Paso 3: generar clave`:

1. Ingresar el documento aprobado, por ejemplo `50000123`.
2. Ingresar una clave, por ejemplo `1234`.
3. Tocar `Generar clave`.

Resultado esperado:

- La app confirma que la clave fue generada.
- El usuario ya puede iniciar sesion.

### 4. Login del cliente nuevo

Ir a Login e ingresar:

- Documento: `50000123`
- Clave: `1234`

Resultado esperado:

- La app abre Home.
- Se muestra el nombre del usuario.
- Se muestra su categoria.
- Se listan subastas disponibles.

### 5. Cargar un metodo de pago

Desde Home:

1. Entrar a `PAGOS`.
2. Elegir `Agregar tarjeta de credito`, `Agregar cuenta bancaria` o `Agregar cheque certificado`.
3. Completar datos.

Ejemplo para una tarjeta en pesos:

- Moneda: `pesos`
- Extranjera: `no`
- Entidad: `Visa Demo`
- Referencia: `**** **** **** 9999`

Ejemplo para subastas en dolares:

- Moneda: `dolares`
- Extranjera: `si`
- Entidad: `Visa International`
- Referencia: `**** **** **** 8888`

Resultado esperado:

- El medio de pago se registra.
- Queda pendiente de verificacion interna.
- Aparece en el listado de medios del cliente.

### 6. Verificar el medio de pago desde admin

Cerrar sesion e ingresar nuevamente como admin:

- Documento: `20000111`
- Clave: `1234`

En Panel interno:

1. Tocar `Actualizar pendientes internos`.
2. Buscar el medio de pago pendiente.
3. Copiar el ID del medio.
4. Cargarlo en `ID de medio de pago`.
5. Tocar `Verificar medio de pago`.

Resultado esperado:

- El medio pasa a estar verificado.
- El usuario ya puede pujar si la categoria de la subasta lo permite y no tiene multas pendientes.

### 7. Entrar a subasta y ver catalogo

Volver a iniciar sesion como cliente nuevo:

- Documento: `50000123`
- Clave: `1234`

Desde Home:

1. Elegir una subasta disponible.
2. Tocar `ENTRAR AL CATALOGO` o `VER CATALOGO`.

Resultado esperado:

- Se muestra el detalle de la subasta.
- Se muestra el estado en vivo.
- Se muestran los lotes del catalogo.
- Cada lote muestra descripcion, precio base, comision, mejor oferta e imagen.

Nota:

- Si las fotos guardadas en SQL son imagenes validas, se muestra la foto real.
- Si la base tiene placeholders binarios, la app muestra el logo como imagen fallback para que la pantalla no quede vacia.

### 8. Realizar una puja

Desde un lote habilitado:

1. Tocar `PUJAR`.
2. Revisar el resumen del lote.
3. Revisar la puja minima y maxima.
4. Ingresar un importe permitido.
5. Tocar `ENVIAR PUJA`.

Ejemplo para un lote con precio base `10000` y mejor oferta `12200`:

- Puja minima: `12300`
- Puja maxima: `14200`

Resultado esperado:

- El boton se bloquea mientras espera confirmacion.
- El backend valida reglas de categoria, medio de pago, moneda, multas y rango de puja.
- La puja queda registrada.
- La app vuelve al detalle del lote.
- El estado en vivo puede reflejar la nueva mejor oferta.

## Flujo rapido con usuario precargado

Usar este flujo si se quiere ahorrar tiempo durante la defensa.

1. Login con cliente `30123456` / `1234`.
2. Entrar a `PAGOS`.
3. Cargar un nuevo medio de pago.
4. Login con admin `20000111` / `1234`.
5. Verificar el medio desde Panel interno.
6. Volver al cliente `30123456` / `1234`.
7. Entrar a una subasta.
8. Abrir catalogo.
9. Pujar.

## Errores que conviene mostrar

- Login incorrecto: muestra error de credenciales.
- Usuario pendiente: no permite iniciar sesion como cliente habilitado.
- Medio de pago sin verificar: permite ver subasta, pero bloquea puja.
- Multa pendiente: bloquea puja.
- Categoria insuficiente: permite ver o bloquea segun corresponda.
- Puja menor al minimo: muestra error de validacion.
- Puja mayor al maximo en categorias no premium: muestra error.
- Error de conexion: muestra mensaje de servidor no disponible.

## Alcance declarado para segunda entrega

La segunda entrega requiere backend y frontend funcionando al menos al 50%, con un circuito completo integrado.

Este flujo cubre:

- Registro KYC en dos pasos.
- Aprobacion interna de usuario.
- Login conectado al backend.
- Medios de pago conectados al backend.
- Verificacion interna de medios de pago.
- Subastas y catalogo conectados al backend.
- Imagenes/fallback visual de lotes.
- Estado en vivo via SSE.
- Puja con validacion frontend y backend.
- Panel interno basico.
- Manejo de errores visibles para el usuario.
