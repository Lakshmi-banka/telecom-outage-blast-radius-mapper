from collections import defaultdict
from datetime import timedelta
from app.schemas import AlarmCorrelationRequest

RANK = {"CRITICAL": 4, "HIGH": 3, "MEDIUM": 2, "LOW": 1, "INFO": 0}

def correlate_alarms(request: AlarmCorrelationRequest) -> dict:
    buckets = defaultdict(list)
    for alarm in request.alarms:
        key = (alarm.component_id, alarm.alarm_type) if request.group_by == "component_and_type" else (alarm.component_id,)
        buckets[key].append(alarm)
    groups = []
    for key, alarms in buckets.items():
        alarms.sort(key=lambda a: a.occurred_at)
        current = []
        for alarm in alarms:
            if not current or alarm.occurred_at - current[-1].occurred_at <= timedelta(minutes=request.window_minutes):
                current.append(alarm)
            else:
                groups.append(_group(key, current)); current = [alarm]
        if current:
            groups.append(_group(key, current))
    groups.sort(key=lambda g: g["first_alarm_at"])
    return {"correlation_window_minutes": request.window_minutes, "group_by": request.group_by,
            "input_alarm_count": len(request.alarms), "correlated_group_count": len(groups), "groups": groups}

def _group(key, alarms):
    highest = max(alarms, key=lambda a: RANK.get(a.severity.upper(), 0))
    return {"correlation_key": "|".join(key), "component_id": alarms[0].component_id,
            "alarm_ids": [a.alarm_id for a in alarms], "alarm_count": len(alarms),
            "alarm_codes": sorted({a.alarm_code for a in alarms}),
            "alarm_types": sorted({a.alarm_type for a in alarms}), "highest_severity": highest.severity,
            "first_alarm_at": alarms[0].occurred_at, "last_alarm_at": alarms[-1].occurred_at,
            "suggested_root_alarm_id": alarms[0].alarm_id,
            "open_alarm_count": sum(a.alarm_status.upper() not in {"CLEARED", "CLOSED"} for a in alarms)}
