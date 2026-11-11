from fastapi import FastAPI, Depends, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.config import get_settings
from app.schemas import TopologyRequest, AlarmCorrelationRequest, OutageSimulationRequest
from app.graph_engine import analyze_blast_radius, graph_summary
from app.correlation import correlate_alarms
from app.sample_data import sample_topology
from app.synthetic import choose_failure_candidates

settings = get_settings()
app = FastAPI(title=settings.app_name, version="1.0.0",
              description="Alarm correlation, graph traversal, BFS/DFS blast-radius analysis and outage simulation.")
from fastapi import Request

@app.middleware("http")
async def log_request_details(request: Request, call_next):

    print("\n========== INCOMING REQUEST ==========")
    print("METHOD:", request.method)
    print("PATH:", request.url.path)
    print("CONTENT TYPE:", request.headers.get("content-type"))
    print("CONTENT LENGTH:", request.headers.get("content-length"))
    print("TRANSFER ENCODING:",
          request.headers.get("transfer-encoding"))

    try:
        response = await call_next(request)
        print("PYTHON RESPONSE STATUS:", response.status_code)
        print("=====================================\n")
        return response

    except Exception as exc:
        print("PYTHON APPLICATION EXCEPTION:", repr(exc))
        raise
app.add_middleware(CORSMiddleware, allow_origins=settings.cors_origin_list, allow_credentials=True,
                   allow_methods=["GET", "POST"], allow_headers=["Authorization", "Content-Type", "X-Internal-API-Key"])

def verify_internal_key(x_internal_api_key: str | None = Header(default=None)):
    if settings.python_engine_api_key and x_internal_api_key != settings.python_engine_api_key:
        raise HTTPException(status_code=401, detail="Invalid or missing internal API key")

@app.get("/health")
def health():
    return {"status":"UP", "service":settings.app_name, "environment":settings.app_env}

@app.get("/sample/topology", dependencies=[Depends(verify_internal_key)])
def sample():
    return sample_topology().model_dump(mode="json")

@app.post("/graph/analyze", dependencies=[Depends(verify_internal_key)])
def graph_analyze(request: TopologyRequest):
    try: return graph_summary(request)
    except ValueError as exc: raise HTTPException(status_code=422, detail=str(exc))

@app.post("/blast-radius/bfs", dependencies=[Depends(verify_internal_key)])
def bfs(request: TopologyRequest):
    if not request.root_component_id: raise HTTPException(status_code=422, detail="root_component_id is required")
    try: return analyze_blast_radius(request, request.root_component_id, "bfs")
    except ValueError as exc: raise HTTPException(status_code=422, detail=str(exc))

@app.post("/blast-radius/dfs", dependencies=[Depends(verify_internal_key)])
def dfs(request: TopologyRequest):
    if not request.root_component_id: raise HTTPException(status_code=422, detail="root_component_id is required")
    try: return analyze_blast_radius(request, request.root_component_id, "dfs")
    except ValueError as exc: raise HTTPException(status_code=422, detail=str(exc))

@app.post("/alarms/correlate", dependencies=[Depends(verify_internal_key)])
def alarm_correlation(request: AlarmCorrelationRequest):
    return correlate_alarms(request)

@app.post("/simulation/outage", dependencies=[Depends(verify_internal_key)])
def simulate(request: OutageSimulationRequest):
    per_root = []
    combined_components, combined_services = set(), {}
    for root in request.failed_component_ids:
        try: result = analyze_blast_radius(request, root, request.traversal)
        except ValueError as exc: raise HTTPException(status_code=422, detail=str(exc))
        per_root.append(result)
        combined_components.update(x["component_id"] for x in result["affected_components"])
        for service in result["affected_services"]: combined_services[service["service_id"]] = service
    if request.include_root_in_affected: combined_components.update(request.failed_component_ids)
    return {"simulation":"OUTAGE", "failed_component_ids":request.failed_component_ids,
            "traversal":request.traversal.upper(), "per_root_results":per_root,
            "combined_affected_component_ids":sorted(combined_components),
            "combined_affected_component_count":len(combined_components),
            "combined_affected_services":list(combined_services.values()),
            "combined_affected_service_count":len(combined_services),
            "estimated_affected_customers":sum(s["estimated_customer_count"] for s in combined_services.values()),
            "note":"Each affected service is counted once; overlapping subscriber populations may still be double-counted."}


@app.post("/simulation/synthetic-failures", dependencies=[Depends(verify_internal_key)])
def synthetic_failures(payload: dict):
    component_ids = payload.get("component_ids", [])
    count = int(payload.get("count", 1))
    seed = payload.get("seed")
    try:
        return {"candidates": choose_failure_candidates(component_ids, count, seed)}
    except (ValueError, TypeError) as exc:
        raise HTTPException(status_code=422, detail=str(exc))
