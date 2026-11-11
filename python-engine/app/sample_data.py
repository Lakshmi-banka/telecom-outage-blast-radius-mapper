from app.schemas import TopologyRequest

def sample_topology() -> TopologyRequest:
    return TopologyRequest(
        root_component_id="CORE-RTR-01",
        components=[
            {"component_id":"CORE-RTR-01","name":"Core Router 01","status":"DOWN","criticality":"CRITICAL"},
            {"component_id":"AGG-RTR-01","name":"Aggregation Router 01","status":"UP","criticality":"HIGH"},
            {"component_id":"BTS-001","name":"Base Tower 001","status":"UP","criticality":"HIGH"},
            {"component_id":"BTS-002","name":"Base Tower 002","status":"UP","criticality":"HIGH"},
            {"component_id":"FIBER-001","name":"Fiber Link 001","status":"UP","criticality":"MEDIUM"},
            {"component_id":"BTS-003","name":"Base Tower 003","status":"UP","criticality":"HIGH"}],
        dependencies=[
            {"source_component_id":"CORE-RTR-01","dependent_component_id":"AGG-RTR-01"},
            {"source_component_id":"AGG-RTR-01","dependent_component_id":"BTS-001"},
            {"source_component_id":"AGG-RTR-01","dependent_component_id":"BTS-002"},
            {"source_component_id":"AGG-RTR-01","dependent_component_id":"FIBER-001"},
            {"source_component_id":"FIBER-001","dependent_component_id":"BTS-003"}],
        services=[
            {"service_id":"SVC-VOICE","service_name":"Mobile Voice","estimated_customer_count":12000,"component_ids":["BTS-001","BTS-002"]},
            {"service_id":"SVC-DATA","service_name":"Mobile Data","estimated_customer_count":18000,"component_ids":["BTS-002","BTS-003"]}])
