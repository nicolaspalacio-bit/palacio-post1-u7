# Guía de pruebas manuales

Comandos listos para copiar y pegar (curl) que cubren cada checkpoint de la
Parte 1 y de la Parte 2. Ejecutar `mvn spring-boot:run` primero y correr
estos comandos en orden desde otra terminal; el orden importa porque varios
comandos dependen del `id` devuelto por uno anterior y del conteo acumulado
de multas pendientes por estudiante.

Para dejar evidencia en el repositorio (capturas de Postman o de la
terminal), guardar las imágenes en esta misma carpeta (`docs/`) y
enlazarlas desde el README.

## Parte 1 — Arquitectura en capas

### 1. Listar multas al inicio (lista vacía)

```bash
curl -i http://localhost:8080/api/multas
```

**Esperado:** `200 OK`, cuerpo `[]`.

### 2. Generar una multa válida

```bash
curl -i -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{"estudianteId":"E001","concepto":"Devolucion tardia - Clean Code","diasAtraso":10}'
```

**Esperado:** `201 Created`, cuerpo con `id`, `estado: "PENDIENTE"` y
`monto: 5000` (10 días × 500).

### 3. Generar una multa sin `estudianteId` (validación)

```bash
curl -i -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{"estudianteId":"","concepto":"Prueba","diasAtraso":5}'
```

**Esperado:** `400 Bad Request`, cuerpo con la clave `estudianteId` y el
mensaje de validación.

### 4. Superar el límite de multas pendientes

```bash
curl -s -X POST http://localhost:8080/api/multas -H "Content-Type: application/json" \
  -d '{"estudianteId":"E002","concepto":"Multa 1","diasAtraso":2}' > /dev/null
curl -s -X POST http://localhost:8080/api/multas -H "Content-Type: application/json" \
  -d '{"estudianteId":"E002","concepto":"Multa 2","diasAtraso":2}' > /dev/null
curl -s -X POST http://localhost:8080/api/multas -H "Content-Type: application/json" \
  -d '{"estudianteId":"E002","concepto":"Multa 3","diasAtraso":2}' > /dev/null
curl -i -X POST http://localhost:8080/api/multas -H "Content-Type: application/json" \
  -d '{"estudianteId":"E002","concepto":"Multa 4","diasAtraso":2}'
```

**Esperado:** las tres primeras devuelven `201 Created`; la cuarta devuelve
`409 Conflict` con un mensaje que cita el límite de 3.

### 5. Buscar una multa inexistente

```bash
curl -i http://localhost:8080/api/multas/9999
```

**Esperado:** `404 Not Found`.

### 6. Pagar en ventanilla

```bash
curl -i -X PATCH http://localhost:8080/api/multas/1/pagar
```

**Esperado:** `200 OK`, `estado: "PAGADA"`, `metodoPago: "VENTANILLA"`.

### 7. Pagar dos veces la misma multa

```bash
curl -i -X PATCH http://localhost:8080/api/multas/1/pagar
```

**Esperado:** `409 Conflict` (la multa 1 ya se pagó en el paso anterior).

## Parte 2 — Pago en línea con dos pasarelas

> Los adaptadores llaman por HTTP a las URLs configuradas en
> `app.pagos.pagosudes.url` y `app.pagos.wompi.url`. Sin un simulador
> corriendo en esos puertos, la llamada falla con `RestClientException` y el
> adaptador la traduce a un `ResultadoPago` no exitoso — es decir, el
> endpoint sigue respondiendo con un `402` controlado en vez de un error sin
> manejar, que es precisamente el propósito del `try/catch` de cada
> adaptador. Para una prueba end-to-end completa, levantar un servidor HTTP
> simple (o un mock con WireMock) que responda en esos puertos con el
> formato de cada pasarela.

### 8. Pagar en línea con PagosUDES activo (por defecto)

```bash
curl -i -X POST http://localhost:8080/api/multas/2/pagar-en-linea
```

**Esperado (con el simulador de PagosUDES respondiendo `estadoTransaccion:
"APROBADA"`):** `200 OK`, `estado: "PAGADA"`, `metodoPago: "PAGOSUDES"`.
**Esperado (sin simulador disponible):** `402 Payment Required` con el
mensaje `"PagosUDES no disponible: ..."`.

### 9. Cambiar a Wompi sin recompilar

Detener la aplicación, cambiar en `application.properties`:

```properties
app.pagos.proveedor=wompi
```

Reiniciar (`mvn spring-boot:run`) y repetir la prueba anterior sobre otra
multa pendiente:

```bash
curl -i -X POST http://localhost:8080/api/multas/3/pagar-en-linea
```

**Esperado:** el mismo endpoint ahora resuelve el pago con `WompiAdapter`
(`metodoPago: "WOMPI"` en caso de éxito), sin haber tocado
`MultaController` ni `MultaService`.

### 10. Pagar en línea una multa ya pagada

```bash
curl -i -X POST http://localhost:8080/api/multas/1/pagar-en-linea
```

**Esperado:** `409 Conflict` (la multa 1 ya fue pagada en el paso 6), sin
importar que ahora se intente por el canal en línea.
