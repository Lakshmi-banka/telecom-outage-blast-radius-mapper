from datetime import datetime, timezone, timedelta
from app.sample_data import sample_topology
from app.graph_engine import analyze_blast_radius, graph_summary
from app.schemas import AlarmIn, AlarmCorrelationRequest
from app.correlation import correlate_alarms

def test_sample_graph_counts():
    result = graph_summary(sample_topology())
    assert result["component_count"] == 6
    assert result["active_dependency_count"] == 5

def test_bfs_blast_radius():
    result = analyze_blast_radius(sample_topology(), "CORE-RTR-01", "bfs")
    assert set(result["visited_order"]) == {"AGG-RTR-01", "BTS-001", "BTS-002", "FIBER-001", "BTS-003"}
    assert result["affected_component_count"] == 5
    assert result["affected_service_count"] == 2
    assert result["estimated_affected_customers"] == 30000

def test_dfs_same_reachable_nodes():
    bfs = analyze_blast_radius(sample_topology(), "CORE-RTR-01", "bfs")
    dfs = analyze_blast_radius(sample_topology(), "CORE-RTR-01", "dfs")
    assert set(bfs["visited_order"]) == set(dfs["visited_order"])

def test_alarm_correlation():
    now = datetime.now(timezone.utc)
    alarms = [
        AlarmIn(alarm_id="A1", component_id="CORE-RTR-01", alarm_code="LINK_DOWN", severity="CRITICAL", occurred_at=now),
        AlarmIn(alarm_id="A2", component_id="CORE-RTR-01", alarm_code="PACKET_LOSS", severity="HIGH", occurred_at=now + timedelta(minutes=3))]
    result = correlate_alarms(AlarmCorrelationRequest(alarms=alarms, window_minutes=10))
    assert result["correlated_group_count"] == 1
    assert result["groups"][0]["highest_severity"] == "CRITICAL"
