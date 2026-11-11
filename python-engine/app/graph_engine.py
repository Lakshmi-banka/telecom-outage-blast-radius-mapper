from collections import deque
import networkx as nx
from app.schemas import TopologyRequest

def build_graph(data: TopologyRequest) -> nx.DiGraph:
    graph = nx.DiGraph()
    for c in data.components:
        graph.add_node(c.component_id, name=c.name, status=c.status, criticality=c.criticality)
    for dep in data.dependencies:
        if dep.is_active:
            graph.add_edge(dep.source_component_id, dep.dependent_component_id)
    return graph

def traverse(graph: nx.DiGraph, root: str, method: str) -> list[str]:
    if root not in graph:
        raise ValueError(f"Root component '{root}' was not found in the graph")
    visited = {root}
    order: list[str] = []
    if method == "bfs":
        queue = deque([root])
        while queue:
            node = queue.popleft()
            for nxt in graph.successors(node):
                if nxt not in visited:
                    visited.add(nxt); order.append(nxt); queue.append(nxt)
    elif method == "dfs":
        stack = [root]
        while stack:
            node = stack.pop()
            for nxt in reversed(list(graph.successors(node))):
                if nxt not in visited:
                    visited.add(nxt); order.append(nxt); stack.append(nxt)
    else:
        raise ValueError("Traversal must be bfs or dfs")
    return order

def analyze_blast_radius(data: TopologyRequest, root: str, method: str = "bfs") -> dict:
    graph = build_graph(data)
    order = traverse(graph, root, method)
    lookup = {c.component_id: c for c in data.components}
    components = []
    for cid in order:
        c = lookup.get(cid)
        attrs = graph.nodes[cid]
        components.append({"component_id": cid, "name": c.name if c else attrs.get("name", cid),
                           "status": c.status if c else attrs.get("status", "UNKNOWN"),
                           "criticality": c.criticality if c else attrs.get("criticality", "UNKNOWN"),
                           "is_root_cause": False})
    affected = set(order)
    services = []
    customers = 0
    for s in data.services:
        impacted = sorted(set(s.component_ids) & affected)
        if impacted:
            customers += s.estimated_customer_count
            services.append({"service_id": s.service_id, "service_name": s.service_name,
                             "affected_component_ids": impacted,
                             "estimated_customer_count": s.estimated_customer_count, "status": s.status})
    return {"root_component_id": root, "traversal": method.upper(),
            "affected_components": components, "affected_component_count": len(components),
            "affected_services": services, "affected_service_count": len(services),
            "estimated_affected_customers": customers, "visited_order": order}

def graph_summary(data: TopologyRequest) -> dict:
    graph = build_graph(data)
    groups = [sorted(g) for g in nx.connected_components(graph.to_undirected())]
    isolated = sorted(n for n, degree in graph.degree() if degree == 0)
    reachable = traverse(graph, data.root_component_id, "bfs") if data.root_component_id else []
    return {"component_count": graph.number_of_nodes(), "active_dependency_count": graph.number_of_edges(),
            "connected_component_groups": groups, "root_reachable_component_ids": reachable,
            "isolated_component_ids": isolated}
