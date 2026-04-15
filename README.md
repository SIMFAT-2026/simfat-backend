# SIMFAT Backend (MVP + OpenEO Sync)

Backend de **SIMFAT** (Spring Boot + MongoDB) para monitoreo forestal y dashboard.  
El frontend `simfat-web` consume solo este backend; la integración con openEO se hace internamente via `openeo-service`.

## Arquitectura de integración

Flujo principal:

1. `@Scheduled` (o trigger manual) ejecuta sync por regiones.
2. Se consulta `openeo-service` por indicador con `POST /openeo/indicators/latest/{indicator}`.
3. Se registra traza en `openeo_job_runs`.
4. Si llega valor real, se hace upsert en `openeo_indicator_observations`.
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
- `OPENEO_AOI_BBOX_MAP` (mapa `regionCode -> bbox`, formato `CL-15:-70.8,-19.2,-69.2,-18.1;...`)
- `OPENEO_SYNC_PLACEHOLDER_VALUE_ENABLED` (default `false`; solo testing/demo)

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

- `POST /api/dashboard/sync/run?regionId={id}&from=YYYY-MM-DD&to=YYYY-MM-DD` (`regionId/from/to` opcionales)
- `GET /api/dashboard/indicators/latest?regionId={id}&indicator=NDVI|NDMI`
- `GET /api/dashboard/indicators/series?regionId={id}&indicator=NDVI|NDMI&from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=day|week|month` (`from/to` opcionales, default ultimos 30 dias)
- `GET /api/dashboard/indicators/map?indicator=NDVI|NDMI&from=YYYY-MM-DD&to=YYYY-MM-DD&limit=500`
- `GET /api/dashboard/data-freshness?regionId={id}`

Todas las respuestas usan `ApiResponse<T>`.

Contratos de respuesta dashboard MVP:

- `latest`: `regionId`, `indicator`, `value`, `observedAt`, `source`, `cached`
- `series.points[]`: `ts`, `value`
- `data-freshness`: `regionId`, `lastUpdate`, `ageSeconds`, `status` (`FRESH|STALE|EMPTY`)

## Ejemplos de uso

Trigger manual de sync:

```bash
curl -X POST "http://localhost:8080/api/dashboard/sync/run?regionId=REGION_ID"
```

Trigger manual con ventana explicita:

```bash
curl -X POST "http://localhost:8080/api/dashboard/sync/run?regionId=REGION_ID&from=2026-04-01&to=2026-04-05"
```

Ultimo indicador:

```bash
curl "http://localhost:8080/api/dashboard/indicators/latest?regionId=REGION_ID&indicator=NDVI"
```

Serie semanal:

```bash
curl "http://localhost:8080/api/dashboard/indicators/series?regionId=REGION_ID&indicator=NDMI&from=2026-03-01&to=2026-03-31&granularity=week"
```

Serie diaria (default ultimos 30 dias):

```bash
curl "http://localhost:8080/api/dashboard/indicators/series?regionId=REGION_ID&indicator=NDVI"
```

## Rendimiento y observabilidad

- Lecturas frecuentes con cache local TTL corto (30-90s).
- Payloads de dashboard acotados para UI.
- `map` con limite y tope maximo (`500`).
- Logs de sync y cache con campos de trazabilidad (region, indicador, status, latencia, hit/miss).

## AOI por region

El sync solo persiste datos reales cuando la region tiene AOI bbox configurado.

Formato de `OPENEO_AOI_BBOX_MAP`:

```env
OPENEO_AOI_BBOX_MAP=CL-15:-70.8,-19.2,-69.2,-18.1;CL-1:-70.5,-21.0,-69.8,-20.2
```

- clave: `region.codigo` (ej: `CL-15`)
- orden de coordenadas: `west,south,east,north`

Si falta AOI para una region:
- se registra warning de sync
- se guarda `jobRun` con estado de error controlado
- no se crea observacion fake

## Datos reales vs fallback

- Modo recomendado (real): `OPENEO_SYNC_PLACEHOLDER_VALUE_ENABLED=false`
- Modo testing/demo: `OPENEO_SYNC_PLACEHOLDER_VALUE_ENABLED=true`

Con fallback desactivado, nunca se inventa `value` ni `quality: estimated`.

## Testing

Incluye:

- Unit tests:
  - cliente `openeo-service` con `MockWebServer`
  - sync service
  - snapshot service
- Integration tests:
  - nuevos endpoints dashboard (MockMvc)
  - repositorios e indices de colecciones nuevas
