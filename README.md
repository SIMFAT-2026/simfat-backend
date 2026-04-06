# SIMFAT Backend (MVP)

Backend del proyecto **SIMFAT** (Sistema Integrado de Monitoreo y Alerta Temprana Forestal), orientado a registros de perdida de cobertura forestal, eventos de calor y alertas tempranas.

## Stack

- Java 17
- Spring Boot 3.x
- Maven
- MongoDB Atlas
- API REST

## Arquitectura

Estructura por capas:

- `config`: CORS, seed de datos y placeholders de seguridad
- `controller`: endpoints REST
- `dto`: contratos API (request/response)
- `model`: entidades MongoDB
- `repository`: acceso a datos
- `service`: interfaces de negocio
- `service/impl`: implementaciones de negocio
- `exception`: manejo global de errores

## Variables de entorno

Usa `.env.example` como referencia:

- `MONGODB_URI`
- `SERVER_PORT`
- `FRONTEND_URL`
- `DEFAULT_REFERENCE_HECTARES`
- `DEFAULT_LOSS_THRESHOLD`
- `DEFAULT_HEAT_EVENTS_THRESHOLD`

## Ejecutar localmente

1. Configura variables de entorno.
2. Compila:
   - `mvn clean compile`
3. Ejecuta:
   - `mvn spring-boot:run`

La API quedara disponible por defecto en `http://localhost:8080`.

## Endpoints principales

- `GET /api/regions`
- `GET /api/regions/{id}`
- `POST /api/regions`
- `PUT /api/regions/{id}`
- `DELETE /api/regions/{id}`

- `GET /api/forest-loss`
- `GET /api/forest-loss/{id}`
- `GET /api/forest-loss/region/{regionId}`
- `GET /api/forest-loss/year/{year}`
- `POST /api/forest-loss`
- `PUT /api/forest-loss/{id}`
- `DELETE /api/forest-loss/{id}`

- `GET /api/alerts`
- `GET /api/alerts/{id}`
- `GET /api/alerts/region/{regionId}`
- `POST /api/alerts`
- `PUT /api/alerts/{id}`
- `DELETE /api/alerts/{id}`

- `GET /api/rules`
- `GET /api/rules/{id}`
- `POST /api/rules`
- `PUT /api/rules/{id}`
- `DELETE /api/rules/{id}`

- `GET /api/dashboard/summary`
- `GET /api/dashboard/critical-regions`
- `GET /api/dashboard/loss-trend`
- `GET /api/dashboard/alerts-summary`

## Formato de respuesta

### Exito

```json
{
  "success": true,
  "message": "Regiones obtenidas correctamente",
  "data": [],
  "timestamp": "2026-04-05T20:10:00"
}
```

### Error

```json
{
  "success": false,
  "status": 400,
  "error": "Validation Error",
  "message": "Uno o mas campos son invalidos",
  "path": "/api/regions",
  "timestamp": "2026-04-05T20:12:00",
  "validationErrors": {
    "nombre": "El nombre de la region es obligatorio"
  }
}
```

## Ejemplos Postman / Thunder Client

### Crear region

`POST /api/regions`

```json
{
  "nombre": "Region Sur Bosque Humedo",
  "codigo": "SIM-RS-02",
  "zona": "SUR",
  "hectareasBosqueReferencia": 240000
}
```

### Crear registro forestal

`POST /api/forest-loss`

```json
{
  "regionId": "REGION_ID",
  "anio": 2026,
  "hectareasPerdidas": 960.5,
  "fuente": "Global Forest Watch"
}
```

### Crear alerta de calor

`POST /api/alerts`

```json
{
  "regionId": "REGION_ID",
  "latitud": -39.81,
  "longitud": -73.24,
  "fuente": "NASA FIRMS",
  "descripcion": "Foco de calor detectado en zona de interfaz"
}
```

### Crear regla

`POST /api/rules`

```json
{
  "nombre": "Regla Sur 2026",
  "regionId": "REGION_ID",
  "umbralPorcentajePerdida": 0.8,
  "umbralEventosCalor": 4,
  "activa": true
}
```

## Notas

- Se incluye `CommandLineRunner` de seed para datos iniciales cuando la base esta vacia.
- CORS habilitado para el frontend definido por `FRONTEND_URL`.
- Seguridad JWT no implementada aun (placeholder listo para fase siguiente).
- Integraciones externas (Global Forest Watch y NASA FIRMS) quedan como servicios placeholder.

