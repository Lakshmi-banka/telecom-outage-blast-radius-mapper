package com.telecom.outage.dto;
import jakarta.validation.constraints.*;
public class Requests {
 public record Login(@NotBlank @Email String email,@NotBlank String password){}
 public record ComponentCreate(@NotBlank String componentCode,@NotBlank String componentName,@NotBlank String componentType,String location,java.math.BigDecimal latitude,java.math.BigDecimal longitude,@NotBlank String status,@NotBlank String criticality){}
 public record StatusUpdate(@NotBlank String status){}
 public record IncidentCreate(@NotBlank String incidentCode,@NotBlank String title,@NotBlank String severity,@NotNull Long rootComponentId,String description){}
 public record IncidentStatusUpdate(@NotBlank String incidentStatus){}
}
