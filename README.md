# Post-contenido — Unidad 7: Patrones Arquitectónicos I

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![Build](https://img.shields.io/badge/build-Maven-blue)
![DB](https://img.shields.io/badge/DB-H2%20(in--memory)-lightgrey)
![Architecture](https://img.shields.io/badge/architecture-Capas%20%2B%20Puerto%20hexagonal-informational)

## Descripción

Repositorio del post-contenido de la Unidad 7 de Patrones de Diseño de
Software. Un único proyecto Spring Boot (`multas-biblioteca-api`) para la
gestión de multas de biblioteca, desarrollado en dos partes sobre el mismo
código: una API REST en arquitectura en capas (Presentación, Aplicación,
Dominio, Infraestructura) sobre H2, y un canal de pago en línea con dos
pasarelas intercambiables que obligó a decidir, con criterio propio, entre
extender esas mismas capas o introducir un puerto de dominio con adaptadores.

```
Índice
├── Arquitectura general (Parte 1)
├── Estructura del repositorio
├── Cómo ejecutar
├── Referencia de la API
├── Parte 2 — la decisión arquitectónica
├── Decisiones de diseño (ADR-1 a ADR-4)
├── Trade-off considerado
├── Herramientas utilizadas
└── Conclusiones
```

## Arquitectura general (Parte 1)

```
┌──────────────────────────────── PRESENTACIÓN ─────────────────────────────────┐
│  MultaController                         GlobalExceptionHandler               │
│  GET/POST/PATCH  /api/multas/*           404 · 409 · 402 · 400                │
└───────────────────────────────────────┬───────────────────────────────────────┘
                                         │ delega en
┌────────────────────────────────────── APLICACIÓN ─────────────────────────────┐
│  MultaService                                                                 │
│  generar()  ·  pagarEnVentanilla()  ·  pagarConPasarela()                     │
└───────────────┬─────────────────────────────────────────────┬────────────────┘
                 │ usa                                         │ usa (Parte 2)
┌────────────────┴────────────────┐             ┌──────────────┴──────────────────┐
│   DOMINIO — model/               │             │   DOMINIO — domain/  (sin Spring)│
│   Multa (calcularMonto,          │             │   PasarelaPagoPort (interfaz)     │
│   marcarComoPagada)              │             │   ResultadoPago                   │
│   EstadoMulta                    │             │   PagoRechazadoException          │
└────────────────┬─────────────────┘             └──────────────┬──────────────────┘
                 │ implementa                                    │ implementan
┌────────────────┴────────────────┐             ┌──────────────┴──────────────────┐
│   INFRAESTRUCTURA                │             │   INFRAESTRUCTURA (adaptadores)   │
│   MultaRepository                │             │   PagosUdesAdapter                │
│   (Spring Data JPA + H2)         │             │   WompiAdapter                    │
└──────────────────────────────────┘             └───────────────────────────────────┘
```

La mitad izquierda es arquitectura en capas clásica y cubre toda la Parte 1.
La mitad derecha es el único fragmento del proyecto tratado como hexagonal
(Parte 2, ver más abajo): un puerto que el dominio define y dos adaptadores
que lo implementan sin que `MultaService` conozca cuál está activo.

## Estructura del repositorio

```
palacio-post1-u7/
├── README.md
├── docs/
│   └── GUIA_DE_PRUEBAS.md
└── multas-biblioteca-api/
    ├── pom.xml
    └── src/main/java/com/example/multas/
        ├── MultasApplication.java
        ├── controller/                 ← Presentación (Parte 1 + endpoint Parte 2)
        │   ├── MultaController.java
        │   ├── GenerarMultaRequest.java
        │   └── GlobalExceptionHandler.java
        ├── service/                    ← Aplicación (Parte 1 + conexión Parte 2)
        │   └── MultaService.java
        ├── model/                      ← Dominio de la Parte 1
        │   ├── Multa.java
        │   ├── EstadoMulta.java
        │   ├── MultaNotFoundException.java
        │   ├── LimiteMultasPendientesException.java
        │   └── MultaYaPagadaException.java
        ├── repository/                 ← Infraestructura de la Parte 1
        │   └── MultaRepository.java
        ├── domain/                     ← NUEVO (Parte 2) — Java puro, sin Spring
        │   ├── port/PasarelaPagoPort.java
        │   ├── ResultadoPago.java
        │   └── PagoRechazadoException.java
        └── infrastructure/             ← NUEVO (Parte 2) — adaptadores, sí conocen Spring/HTTP
            ├── pago/PagosUdesAdapter.java
            ├── pago/WompiAdapter.java
            └── config/RestTemplateConfig.java
```

## Cómo ejecutar

```bash
$ cd multas-biblioteca-api
$ mvn spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080` y la consola H2 en
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:multas_biblioteca_db`,
usuario `sa`, sin contraseña).

Para alternar la pasarela de pago activa sin tocar `MultaController` ni
`MultaService`, cambiar en `application.properties`:

```properties
app.pagos.proveedor=pagosudes   # o bien: wompi
```

## Referencia de la API

| Método | Endpoint                                | Descripción                                | Éxito | Errores posibles |
|--------|------------------------------------------|---------------------------------------------|-------|-------------------|
| GET    | `/api/multas`                            | Lista todas las multas                      | 200   | —                 |
| GET    | `/api/multas/{id}`                       | Busca una multa por id                      | 200   | 404               |
| GET    | `/api/multas/estudiante/{estudianteId}`  | Lista las multas de un estudiante           | 200   | —                 |
| POST   | `/api/multas`                            | Genera una multa nueva                      | 201   | 400, 409          |
| PATCH  | `/api/multas/{id}/pagar`                 | Paga una multa en ventanilla                | 200   | 404, 409          |
| POST   | `/api/multas/{id}/pagar-en-linea`        | Paga una multa con la pasarela activa       | 200   | 404, 409, 402     |

Los payloads de ejemplo y las respuestas esperadas para cada fila de esta
tabla están en [`docs/GUIA_DE_PRUEBAS.md`](docs/GUIA_DE_PRUEBAS.md).

## Parte 2 — la decisión arquitectónica

La Vicerrectoría exige que, durante un piloto, convivan dos pasarelas de
pago (PagosUDES y Wompi) seleccionables por sede, con la puerta abierta a que
el número de proveedores cambie cuando el piloto termine. El enunciado no
indica qué estilo usar; estas eran las tres rutas evaluadas:

| Opción | Qué implica | Se descartó porque |
|---|---|---|
| A. Rama condicional en `MultaService` | Un `if`/`switch` que llama a un cliente HTTP distinto según configuración. | `MultaService` terminaría conociendo los dos contratos HTTP externos; cada pasarela nueva exige tocar el mismo método. |
| B. Interfaz *Strategy* en `service/` | Una interfaz `PasarelaPago` en el propio paquete de servicio, con dos `@Service` seleccionados por `@ConditionalOnProperty`. | Resuelve la intercambiabilidad, pero sigue dejando el conocimiento de "cómo se ve el pago" dentro de la capa de aplicación en vez de tratarlo como un contrato de dominio explícito. |
| **C. Puerto de dominio con dos adaptadores** *(elegida)* | `PasarelaPagoPort` vive en `domain/`, sin Spring; `PagosUdesAdapter` y `WompiAdapter` lo implementan en `infrastructure/`. | — |

**Decisión tomada: Opción C**, aplicada únicamente a la porción de pago (no
se migró `Multa`, `MultaRepository` ni `MultaController` a hexagonal, algo
que sí habría sido sobre-ingeniería para este alcance). El razonamiento
completo, con los cuatro puntos de decisión de la implementación, está a
continuación.

## Decisiones de diseño

<details>
<summary><strong>ADR-1 — Cálculo del monto: ¿entidad o Service?</strong></summary>

**Contexto.** El monto de una multa se obtiene multiplicando los días de
atraso por un valor fijo, con un tope máximo. No necesita ningún colaborador
externo: ni el `Repository`, ni ningún bean de Spring.

**Decisión.** Implementarlo como un método estático de la propia entidad
(`Multa.calcularMonto`), no como un método privado de `MultaService`.

**Alternativa descartada.** Calcular el monto dentro de
`MultaService.generar()`, dejando a `Multa` como un contenedor de datos sin
comportamiento propio (modelo anémico).

**Por qué se descartó.** Un modelo anémico obliga a cualquier otro punto del
sistema que necesite recalcular un monto —una simulación, un job de
recálculo masivo, un test unitario aislado— a duplicar la fórmula o a pasar
obligatoriamente por `MultaService`, incluso cuando no necesita ninguna de
sus otras dependencias.

**Consecuencias.** `Multa` concentra tanto sus datos como el comportamiento
que le pertenece; `MultaService` queda enfocado en orquestar, no en calcular.
</details>

<details>
<summary><strong>ADR-2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?</strong></summary>

**Contexto.** Antes de generar una multa, el sistema debe saber cuántas
multas pendientes tiene ya el estudiante, para aplicar el límite de tres.

**Decisión.** Resolverlo con la consulta derivada
`countByEstudianteIdAndEstado`, que Spring Data JPA traduce a un `COUNT` de
SQL.

**Alternativa descartada.** Usar `findByEstudianteId` para traer todas las
multas del estudiante a memoria y contarlas con streams de Java dentro de
`MultaService`.

**Por qué se descartó.** La decisión de negocio ("¿se le permite generar una
multa más?") le pertenece a `MultaService`, pero el dato que esa decisión
necesita se resuelve donde es eficiente resolverlo. Con miles de multas por
estudiante, traer la lista completa degradaría el tiempo de respuesta de
cada creación de multa en proporción directa al historial del estudiante.

**Consecuencias.** El `Repository` asume una agregación que le compete al
motor de base de datos; el `Service` queda desacoplado del volumen histórico
de datos.
</details>

<details>
<summary><strong>ADR-3 — Selección del adaptador de pago activo</strong></summary>

**Contexto.** Dos pasarelas deben convivir durante el piloto, seleccionables
por sede mediante configuración, sin cerrar la puerta a un tercer proveedor.

**Decisión.** Usar `@ConditionalOnProperty` para que solo uno de los dos
adaptadores exista como bean según `app.pagos.proveedor`; `MultaService`
recibe un único `PasarelaPagoPort` por constructor, sin `@Qualifier` ni
lógica condicional propia.

**Alternativa descartada.** Inyectar un `Map<String, PasarelaPagoPort>` con
todos los adaptadores disponibles y elegir la clave en tiempo de ejecución
dentro de `MultaService`.

**Por qué se descartó, aunque ofrece más flexibilidad.** El `Map`
permitiría cambiar de proveedor sin reiniciar la aplicación, una ventaja
real. Pero el requisito documentado es una pasarela fija por sede durante el
piloto, no un cambio en caliente; el `Map` habría obligado a `MultaService`
a conocer las claves de configuración de cada proveedor (`"pagosudes"`,
`"wompi"`), una responsabilidad que no le corresponde.

**Consecuencias.** Agregar una tercera pasarela exige un nuevo adaptador y
una nueva condición, no tocar `MultaService`; a cambio, cambiar de proveedor
exige reiniciar la aplicación, un costo aceptado explícitamente aquí.
</details>

<details>
<summary><strong>ADR-4 — Diseño del puerto y el tipo de resultado</strong></summary>

**Contexto.** `PagosUdesAdapter` y `WompiAdapter` reciben y devuelven
formatos completamente distintos (`idTransaccion`/`estadoTransaccion` en un
caso, `reference`/`status` en centavos en el otro).

**Decisión.** Ambos adaptadores traducen su respuesta al mismo tipo de
dominio `ResultadoPago`, con campos neutrales (`proveedor`, `exitoso`,
`referenciaExterna`, `mensaje`) que no favorecen el vocabulario de ninguna
pasarela concreta.

**Alternativa descartada.** Que `PasarelaPagoPort.procesar` devolviera
directamente el DTO específico de cada pasarela, o expusiera dos métodos
distintos, uno por proveedor.

**Por qué se descartó.** Si `ResultadoPago` tuviera, por ejemplo, un campo
llamado `idTransaccion` en vez de `referenciaExterna`, `WompiAdapter`
tendría que inventar un valor o forzar un nombre sobre un dato que Wompi
nunca llama así: una señal de que el tipo no era suficientemente neutral.
Exponer el DTO específico de cada pasarela obligaría a `MultaService` a
conocer y distinguir ambos formatos, y una tercera pasarela exigiría
modificarlo además de agregar un adaptador.

**Consecuencias.** `MultaService` interactúa con un único contrato estable
sin importar cuántas pasarelas existan detrás; el costo de esa neutralidad
lo asume cada adaptador, donde debe vivir el conocimiento del formato
externo.
</details>

## Trade-off considerado

**Opciones en tensión.** Extender la arquitectura en capas con una interfaz
*Strategy* dentro de `service/` (Opción B) frente a introducir un puerto de
dominio con dos adaptadores en `domain/` e `infrastructure/` (Opción C).

**Por qué C y no B.** El criterio de la guía de la unidad es explícito:
cuando el sistema debe soportar múltiples fuentes externas con contratos
distintos y es previsible que el número de proveedores siga cambiando, un
puerto de dominio con adaptadores intercambiables se justifica. Ambas
condiciones se cumplen aquí — PagosUDES y Wompi hablan protocolos HTTP
incompatibles entre sí, y el enunciado deja explícito que, terminado el
piloto, podría agregarse o eliminarse un proveedor.

**Qué se ganó.** `MultaController` y el resto de `MultaService` (`generar`,
`pagarEnVentanilla`) permanecen exactamente iguales a como quedaron en la
Parte 1; `domain/` es Java puro y se puede probar con un adaptador simulado
sin levantar Spring ni HTTP real; agregar una tercera pasarela es una
operación aditiva, no una modificación de `MultaService`.

**Qué costó adicional.** Dos paquetes nuevos que no existían en la Parte 1
(`domain/`, `infrastructure/`); una interfaz y un tipo de dominio que, si
solo existiera una pasarela para siempre, serían indirección sin retorno; y
una curva de aprendizaje menor para distinguir qué va sin Spring (`domain/`)
y qué va con Spring (`infrastructure/`).

**¿Se revertiría si el piloto terminara y solo quedara una pasarela?** Si
desaparece para siempre la necesidad de más de un proveedor, la Opción B
sería suficiente y más simple. Aun así, el puerto ya construido no se
desecharía de inmediato: dejar un único adaptador implementando
`PasarelaPagoPort` no cuesta más mantenimiento que colapsarlo en el Service;
remover la indirección solo se justificaría si el equipo necesitara aligerar
la complejidad cognitiva de mantener `domain/` e `infrastructure/`
separados, exactamente la señal de sobre-ingeniería que advierte la propia
unidad.

## Herramientas utilizadas

- Java 17, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Spring Validation
- H2 Database (en memoria), RestTemplate
- Apache Maven, Postman/curl, Git, GitHub

## Conclusiones

La Parte 1 confirmó que una arquitectura en capas disciplinada no es solo
una convención de paquetes: exige decidir activamente dónde vive cada regla
de negocio, como lo evidencian el cálculo del monto resuelto en la entidad y
el conteo de multas resuelto en el motor de base de datos en lugar de en
memoria. La Parte 2 mostró que la dificultad real no estuvo en escribir un
puerto y dos adaptadores, sino en reconocer cuándo un requisito de negocio
—contratos externos heterogéneos, número de proveedores abierto— cruza el
umbral que justifica esa indirección adicional frente a simplemente extender
el Service con una Strategy. Aislar la decisión arquitectónica a la porción
de pago, sin migrar el resto del proyecto a hexagonal, fue en sí mismo un
ejercicio de aplicar el criterio de la unidad con precisión quirúrgica en
vez de adoptar un estilo arquitectónico por completo. El aprendizaje más
transferible del laboratorio es que capas y hexagonal no compiten como
bloques monolíticos: pueden convivir en el mismo proyecto, cada una donde el
problema concreto lo amerita.
