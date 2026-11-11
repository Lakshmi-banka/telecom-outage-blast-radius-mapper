# Telecom Outage Mapper – Python Engine

FastAPI microservice for alarm correlation, directed network graph traversal, BFS/DFS blast-radius calculation and outage simulation.

## Architecture
- PostgreSQL remains the source of truth.
- Spring Boot remains the main backend and owns authentication, authorization and persistence.
- Python receives topology, service mappings and alarms from Spring Boot, computes results, and returns JSON.
- Included sample data is local-development data, not a replacement for your database records.

## Setup (Windows CMD)
From this folder:

```cmd
py -3 -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload
```
If `py -3` is unavailable, use `python -m venv .venv` instead. Python 3.10+ is recommended.

Open Swagger: http://127.0.0.1:8001/docs
Health: http://127.0.0.1:8001/health

## Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Health check |
| GET | `/sample/topology` | Six-node sample topology |
| POST | `/graph/analyze` | Graph summary and reachability |
| POST | `/blast-radius/bfs` | Breadth-first blast-radius |
| POST | `/blast-radius/dfs` | Depth-first blast-radius |
| POST | `/alarms/correlate` | Time-window alarm grouping |
| POST | `/simulation/outage` | One or more failed-component simulation |

## Sample topology
The six components and five directed dependencies match the project context:
CORE-RTR-01 -> AGG-RTR-01 -> BTS-001, BTS-002, FIBER-001 -> BTS-003.
Edges point from upstream component to downstream dependent component. Traversal follows outgoing edges.

## Example BFS body
```json
{
  "root_component_id": "CORE-RTR-01",
  "components": [
    {"component_id":"CORE-RTR-01","name":"Core Router 01","status":"DOWN","criticality":"CRITICAL"},
    {"component_id":"AGG-RTR-01","name":"Aggregation Router 01","status":"UP","criticality":"HIGH"},
    {"component_id":"BTS-001","name":"Base Tower 001","status":"UP","criticality":"HIGH"}
  ],
  "dependencies": [
    {"source_component_id":"CORE-RTR-01","dependent_component_id":"AGG-RTR-01","is_active":true},
    {"source_component_id":"AGG-RTR-01","dependent_component_id":"BTS-001","is_active":true}
  ],
  "services": [
    {"service_id":"SVC-001","service_name":"Mobile Voice","estimated_customer_count":12000,"component_ids":["BTS-001"],"status":"UP"}
  ]
}
```

## Alarm correlation rules
Alarms are grouped by component (or component plus alarm type) when consecutive alarm timestamps fall within the requested time window. The group reports highest severity and open alarm count. This is a deterministic rule-based baseline, not machine learning and not proof of root cause.

## Integration with Spring Boot
Spring Boot should query PostgreSQL for components, active dependencies, services/service-component mappings and alarms, then call this service over HTTP. Spring Boot should persist incidents and impact records. Do not make Python a second writer to the production database in this first integration phase. Secure service-to-service calls by setting `PYTHON_ENGINE_API_KEY` in `.env` and sending it as `X-Internal-API-Key`; keep `.env` out of Git.

`estimated_affected_customers` sums affected service estimates. It is not a unique subscriber count and can double-count people who use multiple affected services.

## Tests
```cmd
python -m pytest
```
