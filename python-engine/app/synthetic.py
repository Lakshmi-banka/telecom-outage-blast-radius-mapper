"""Small, deterministic helpers for synthetic network failure candidates.

This module does not connect to PostgreSQL and does not alter the live topology.
"""
from random import Random

FAILURE_TYPES = ("LINK_FAILURE", "ROUTER_FAILURE", "POWER_FAILURE", "HIGH_LATENCY")


def choose_failure_candidates(component_ids: list[str], count: int = 1, seed: int | None = None) -> list[dict]:
    if not component_ids:
        raise ValueError("At least one component is required")
    if count < 1 or count > len(component_ids):
        raise ValueError("count must be between 1 and the number of components")
    rng = Random(seed)
    selected = rng.sample(component_ids, count)
    return [
        {"failed_component_id": component_id,
         "failure_type": rng.choice(FAILURE_TYPES)}
        for component_id in selected
    ]
