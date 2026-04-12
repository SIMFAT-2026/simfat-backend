# SIMFAT Backend (MVP + OpenEO Sync)

Backend de **SIMFAT** (Spring Boot + MongoDB) para monitoreo forestal y dashboard.  
El frontend `simfat-web` consume solo este backend; la integración con openEO se hace internamente via `openeo-service`.

## Arquitectura de integración

Flujo principal:

1. `@Scheduled` (o trigger manual) ejecuta sync por regiones.
2. Se envian jobs NDVI/NDMI a `openeo-service`.
3. Se registra traza en `openeo_job_runs`.
4. Si llega valor inline, se hace upsert en `openeo_indicator_observations`.
5. Se recalcula `dashboard_region_snapshots`.
6. Endpoints dashboard leen desde Mongo (observations/snapshots), sin consultas live a proveedor por request.

Colecciones nuevas:

- `openeo_job_runs`
- `openeo_indicator_observations`
- `dashboard_region_snapshots`

Indices aplicados:

- `openeo_job_runs`: `jobId` unico, `status + updatedAt(desc)`
- `openeo_indicator_observations`: `regionId + indicator + observedAt(desc)` y unico compuesto `regionId + indicator + observedAt`
- `dashboard_region_snapshots`: `regionId` unico, `computedAt(desc)`

## Variables de entorno

Usa `.env.example` como referencia.

Base:

- `MONGODB_URI`
- `SERVER_PORT`
- `FRONTEND_URL`
- `DEFAULT_REFERENCE_HECTARES`
- `DEFAULT_LOSS_THRESHOLD`
- `DEFAULT_HEAT_EVENTS_THRESHOLD`

Integracion openEO:

- `OPENEO_SERVICE_BASE_URL`
- `OPENEO_SERVICE_TIMEOUT_MS` (default `8000`)
- `OPENEO_SYNC_ENABLED` (default `true`)
- `OPENEO_SYNC_CRON` (default cada 15 min: `0 */15 * * * *`)

## Ejecutar localmente

1. Configura variables de entorno.
2. Compila y prueba:
   - `mvn clean test`
3. Ejecuta:
   - `mvn spring-boot:run`

API por defecto: `http://localhost:8080`.

## Endpoints dashboard

Compatibles existentes:

- `GET /api/dashboard/summary`
- `GET /api/dashboard/critical-regions`
- `GET /api/dashboard/loss-trend`
- `GET /api/dashboard/alerts-summary`

Nuevos MVP:

- `POST /api/dashboard/sync/run?regionId={id}` (`regionId` opcional)
- `GET /api/dashboard/indicators/latest?regionId={id}&indicator=NDVI|NDMI`
- `GET /api/dashboard/indicators/series?regionId={id}&indicator=NDVI|NDMI&from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=day|week|month`
- `GET /api/dashboard/indicators/map?indicator=NDVI|NDMI&from=YYYY-MM-DD&to=YYYY-MM-DD&limit=500`
- `GET /api/dashboard/data-freshness?regionId={id}`

Todas las respuestas usan `ApiResponse<T>`.

## Ejemplos de uso

Trigger manual de sync:

```bash
curl -X POST "http://localhost:8080/api/dashboard/sync/run?regionId=REGION_ID"
```

Ultimo indicador:

```bash
curl "http://localhost:8080/api/dashboard/indicators/latest?regionId=REGION_ID&indicator=NDVI"
```

Serie semanal:

```bash
curl "http://localhost:8080/api/dashboard/indicators/series?regionId=REGION_ID&indicator=NDMI&from=2026-03-01&to=2026-03-31&granularity=week"
```

## Rendimiento y observabilidad

- Lecturas frecuentes con cache local TTL corto (30-90s).
- Payloads de dashboard acotados para UI.
- `map` con limite y tope maximo (`500`).
- Logs de sync y cache con campos de trazabilidad (region, indicador, status, latencia, hit/miss).

## Testing

Incluye:

- Unit tests:
  - cliente `openeo-service` con `MockWebServer`
  - sync service
  - snapshot service
- Integration tests:
  - nuevos endpoints dashboard (MockMvc)
  - repositorios e indices de colecciones nuevas
