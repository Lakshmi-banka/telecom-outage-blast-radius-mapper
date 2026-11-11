from datetime import datetime
from typing import Optional, Literal
from pydantic import BaseModel, Field

class ComponentIn(BaseModel):
    component_id: str = Field(min_length=1)
    name: str = ""
    status: str = "UP"
    criticality: str = "MEDIUM"

class DependencyIn(BaseModel):
    source_component_id: str = Field(min_length=1)
    dependent_component_id: str = Field(min_length=1)
    is_active: bool = True

class ServiceIn(BaseModel):
    service_id: str = Field(min_length=1)
    service_name: str
    estimated_customer_count: int = Field(default=0, ge=0)
    component_ids: list[str] = Field(default_factory=list)
    status: str = "UP"

class TopologyRequest(BaseModel):
    root_component_id: Optional[str] = None
    components: list[ComponentIn]
    dependencies: list[DependencyIn]
    services: list[ServiceIn] = Field(default_factory=list)

class AlarmIn(BaseModel):
    alarm_id: str
    component_id: str
    alarm_code: str
    alarm_type: str = "UNKNOWN"
    severity: str = "MEDIUM"
    description: str = ""
    alarm_status: str = "OPEN"
    occurred_at: datetime

class AlarmCorrelationRequest(BaseModel):
    alarms: list[AlarmIn]
    window_minutes: int = Field(default=10, ge=1, le=1440)
    group_by: Literal["component", "component_and_type"] = "component"

class OutageSimulationRequest(TopologyRequest):
    failed_component_ids: list[str] = Field(min_length=1)
    include_root_in_affected: bool = False
    traversal: Literal["bfs", "dfs"] = "bfs"
