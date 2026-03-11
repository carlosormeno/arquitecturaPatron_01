# Transicion de API Gateway: Kong -> WSO2 API Manager

## Objetivo

Dejar trazabilidad documental de la evolucion de la capa gateway del proyecto:

- **Fase 1:** `Kong` como API Gateway evaluado e implementado inicialmente
- **Fase actual objetivo:** `WSO2 API Manager` como plataforma principal de API Management
- **Componente que se mantiene:** `Nginx Edge` como reverse proxy

## Decision

La arquitectura actual del proyecto considera que:

- `Kong` ya no es la solucion objetivo
- `WSO2 API Manager` reemplaza a `Kong`
- `Nginx Edge` no se elimina

Arquitectura objetivo:

```text
Cliente -> Nginx Edge -> WSO2 API Manager -> Microservicios / DMS
```

## Motivo del Cambio

La evaluacion funcional y de licenciamiento llevo a preferir `WSO2 API Manager` como solucion gratuita y mas completa para API Management en el contexto del proyecto.

## Alcance

### A nivel documental
- Actualizar README troncales
- Marcar `Kong` como componente historico
- Reposicionar `WSO2` como target arquitectonico

### A nivel tecnico
- Pendiente en una fase posterior
- Incluye `docker-compose`, configuracion `nginx`, monitoreo, rutas, health checks y referencias de codigo

## Estado Actual

- La documentacion ya refleja la transicion
- El codigo y algunos artefactos operativos todavia siguen referenciando `Kong`
- La migracion tecnica todavia no se ha ejecutado
