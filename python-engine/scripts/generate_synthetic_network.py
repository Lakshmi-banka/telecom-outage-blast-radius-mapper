
"""
Generate bulk synthetic telecom network data.

Uses existing tables only:
components
component_dependencies
services
service_components

Idempotent: existing records are not overwritten.
Run from python-engine directory.
"""

import os
import random
import psycopg

random.seed(42)

TOTAL_COMPONENTS = 250
TOTAL_SERVICES = 50
EXTRA_DEPENDENCIES = 100

LOCATIONS = [
    "Chennai North",
    "Chennai South",
    "Chennai Central",
    "Bangalore",
    "Hyderabad",
    "Vijayawada",
    "Tirupati",
    "Mumbai",
    "Delhi",
    "Kolkata"
]

COMPONENT_TYPES = [
    "ROUTER",
    "SWITCH",
    "ACCESS_NODE"
]

CRITICALITIES = [
    "CRITICAL",
    "HIGH",
    "MEDIUM",
    "LOW"
]

SERVICE_TYPES = [
    "BROADBAND",
    "VOICE",
    "MOBILE",
    "DATA"
]


def main():

    dsn = os.environ.get("DATABASE_URL")

    if not dsn:
        raise SystemExit("Set DATABASE_URL before running.")

    with psycopg.connect(dsn) as conn:

        with conn.cursor() as cur:

            # --------------------------------
            # 1. Generate 250 components
            # --------------------------------

            component_codes = [
                f"SYN-NET-{i:03d}"
                for i in range(1, TOTAL_COMPONENTS + 1)
            ]

            for i, code in enumerate(component_codes, start=1):

                cur.execute("""
                    INSERT INTO components (
                        component_code,
                        component_name,
                        component_type,
                        location,
                        status,
                        criticality
                    )
                    VALUES (%s, %s, %s, %s, 'UP', %s)
                    ON CONFLICT (component_code) DO NOTHING
                """, (
                    code,
                    f"Synthetic Network Component {i}",
                    random.choice(COMPONENT_TYPES),
                    random.choice(LOCATIONS),
                    random.choice(CRITICALITIES)
                ))

            # --------------------------------
            # 2. Generate network dependencies
            # --------------------------------

            # Create a connected tree first.
            dependency_pairs = set()

            for i in range(2, TOTAL_COMPONENTS + 1):

                dependent = f"SYN-NET-{i:03d}"
                parent = random.randint(1, i - 1)
                source = f"SYN-NET-{parent:03d}"

                dependency_pairs.add((source, dependent))

            # Add extra connections.
            attempts = 0

            while (
                len(dependency_pairs)
                < TOTAL_COMPONENTS - 1 + EXTRA_DEPENDENCIES
                and attempts < 10000
            ):

                source_index = random.randint(
                    1, TOTAL_COMPONENTS
                )

                dependent_index = random.randint(
                    1, TOTAL_COMPONENTS
                )

                if source_index == dependent_index:
                    attempts += 1
                    continue

                pair = (
                    f"SYN-NET-{source_index:03d}",
                    f"SYN-NET-{dependent_index:03d}"
                )

                dependency_pairs.add(pair)
                attempts += 1

            for source, dependent in dependency_pairs:

                cur.execute("""
                    INSERT INTO component_dependencies (
                        source_component_id,
                        dependent_component_id,
                        dependency_type,
                        is_active
                    )
                    SELECT
                        a.component_id,
                        b.component_id,
                        'REQUIRED',
                        TRUE
                    FROM components a
                    CROSS JOIN components b
                    WHERE a.component_code = %s
                    AND b.component_code = %s
                    ON CONFLICT (
                        source_component_id,
                        dependent_component_id
                    ) DO NOTHING
                """, (source, dependent))

            # --------------------------------
            # 3. Generate 50 services
            # --------------------------------

            service_codes = [
                f"SYN-SERVICE-{i:03d}"
                for i in range(1, TOTAL_SERVICES + 1)
            ]

            for i, code in enumerate(service_codes, start=1):

                cur.execute("""
                    INSERT INTO services (
                        service_code,
                        service_name,
                        service_type,
                        estimated_customer_count,
                        status
                    )
                    VALUES (%s, %s, %s, %s, 'ACTIVE')
                    ON CONFLICT (service_code) DO NOTHING
                """, (
                    code,
                    f"Synthetic Telecom Service {i}",
                    random.choice(SERVICE_TYPES),
                    random.randint(1000, 20000)
                ))

            # --------------------------------
            # 4. Map services to components
            # --------------------------------

            for service_code in service_codes:

                selected_components = random.sample(
                    component_codes, 4
                )

                for component_code in selected_components:

                    cur.execute("""
                        INSERT INTO service_components (
                            service_id,
                            component_id,
                            is_critical
                        )
                        SELECT
                            s.service_id,
                            c.component_id,
                            TRUE
                        FROM services s
                        CROSS JOIN components c
                        WHERE s.service_code = %s
                        AND c.component_code = %s
                        ON CONFLICT (
                            service_id,
                            component_id
                        ) DO NOTHING
                    """, (
                        service_code,
                        component_code
                    ))

        conn.commit()

    print("\nSynthetic network generation completed!")
    print(f"Target components: {TOTAL_COMPONENTS}")
    print(f"Target services: {TOTAL_SERVICES}")
    print(
        f"Target dependencies: "
        f"{TOTAL_COMPONENTS - 1 + EXTRA_DEPENDENCIES}"
    )
    print("Target service mappings: approximately 200")


if __name__ == "__main__":
    main()
