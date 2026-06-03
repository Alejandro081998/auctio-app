# Correcciones de la entrega 1

## Figma y flujo de pantallas

Link de Figma del equipo: **pendiente de pegar URL real del archivo compartido**.

Flujo principal implementado en Android:

1. Splash.
2. Login.
3. Registro KYC paso 1: datos personales, domicilio legal, pais y fotos de DNI frente/dorso.
4. Esperando verificacion: pantalla dedicada de cuenta pendiente.
5. Registro KYC paso 2: generacion de clave una vez aprobada la cuenta.
6. Home: subastas disponibles, servicios, compras, multas y panel interno.
7. Medios de pago:
   - Flujo cuenta bancaria.
   - Flujo tarjeta de credito.
   - Flujo cheque certificado.
8. Detalle de subasta:
   - Catalogo completo.
   - Estado en vivo via SSE.
   - Acceso a puja si categoria, medio de pago y multas lo permiten.
9. Puja:
   - Precio base.
   - Mejor oferta.
   - Puja minima.
   - Puja maxima, excepto oro/platino.
   - Bloqueo del boton mientras se confirma backend.
10. Compras y pagos:
    - Compra adjudicada.
    - Detalle de importe, comision y envio.
    - Registro de pago.
11. Multas e impagos:
    - Listado de multas.
    - Bloqueo de pujas por multas pendientes.
    - Registro de pago de multa.
12. Consignacion:
    - Datos del bien.
    - Precio base sugerido por el usuario.
    - Minimo 6 fotos.
    - Declaracion de propiedad y origen licito.
13. Panel interno:
    - Aprobar/rechazar usuario y asignar categoria.
    - Verificar medio de pago.
    - Aceptar/rechazar consignacion.
    - Asignar producto a subasta con precio base y comision.
    - Cerrar item y generar venta.
    - Crear multa por impago.

## API corregida

La documentacion OpenAPI actualizada esta en `backend/swagger.yaml`.

Se agregaron rutas para:

- Autenticacion con bearerAuth demo.
- Login.
- Registro KYC en dos pasos.
- Verificacion interna de usuario.
- Medios de pago y verificacion interna.
- Catalogo completo por subasta.
- Estado vivo y canal SSE.
- Puja por item dentro de subasta.
- Compra/pago final.
- Multas e impago.
- Consignacion con minimo 6 fotos.
- Revision interna de consignaciones.
- Asignacion de producto aceptado a subasta.
- Cierre de item y adjudicacion.
