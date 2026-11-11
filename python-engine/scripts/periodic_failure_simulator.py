"""Periodically create isolated SIMULATED failure_events for existing components.

Does not modify components, alarms, incidents, restoration records, or blast-radius data.
Set DATABASE_URL, SIMULATION_INTERVAL_SECONDS (default 300), and SIMULATION_ENABLED=true.
Stop with Ctrl+C. Use only in a development/demo database.
"""
import os
import random
import time
from datetime import datetime, timedelta
import psycopg

FAILURE_TYPES = ("LINK_FAILURE", "ROUTER_FAILURE", "POWER_FAILURE", "HIGH_LATENCY")


def create_one(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT component_id FROM components WHERE component_code LIKE 'SYN-%' ORDER BY component_id")
        rows = cur.fetchall()
        if not rows:
            print("No SYN-* components found. Run generate_synthetic_network.py first.")
            return
        component_id = random.choice(rows)[0]
        recovery_at = datetime.now() + timedelta(minutes=random.randint(3, 12))
        cur.execute("""INSERT INTO failure_events
            (failed_component_id,event_source,failure_type,event_status,started_at,scheduled_recovery_at)
            VALUES (%s,'SIMULATED',%s,'ACTIVE',CURRENT_TIMESTAMP,%s)""",
            (component_id, random.choice(FAILURE_TYPES), recovery_at))
    conn.commit()
    print(f"Created synthetic failure event for component_id={component_id}")


def main():
    if os.getenv("SIMULATION_ENABLED", "false").lower() != "true":
        raise SystemExit("Set SIMULATION_ENABLED=true to run this simulator")
    dsn = os.environ.get("DATABASE_URL")
    if not dsn:
        raise SystemExit("Set DATABASE_URL before running")
    interval = max(30, int(os.getenv("SIMULATION_INTERVAL_SECONDS", "300")))
    with psycopg.connect(dsn, autocommit=False) as conn:
        print(f"Synthetic failure simulator running; interval={interval}s. Ctrl+C to stop.")
        while True:
            create_one(conn)
            time.sleep(interval)

if __name__ == "__main__":
    main()
