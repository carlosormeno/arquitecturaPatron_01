# 📋 Stack de Reglas de Negocio - GoRules

## 🎯 Descripción General

El **docker-compose-business.yml** implementa un sistema completo de gestión de reglas de negocio usando GoRules. Proporciona un editor visual para crear reglas, un motor de ejecución de alta performance y APIs para integración con aplicaciones.

## 🏗️ Servicios Incluidos

### 📝 **Business Rules Management**
- **GoRules Editor** - Editor visual de reglas de negocio
- **GoRules BRMS** - Business Rules Management System (motor de ejecución)

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| GoRules Editor | 3003 | HTTP | Editor visual de reglas |
| GoRules BRMS | 8180 | HTTP | API del motor de reglas |

## 🔑 Accesos por Defecto

### GoRules Editor
- **URL:** http://localhost:3003
- **Interfaz:** Editor visual drag & drop
- **Documentación:** http://localhost:3003/docs

### GoRules BRMS
- **API:** http://localhost:8180
- **Health:** http://localhost:8180/health
- **Metrics:** http://localhost:8180/metrics
- **Swagger:** http://localhost:8180/swagger

## ⚙️ Variables de Entorno Requeridas

```bash
# GoRules Editor
EDITOR_PORT=3000
RULES_STORAGE_PATH=/app/rules
EDITOR_MEMORY_LIMIT=512m

# GoRules BRMS
BRMS_PORT=8080
LOG_LEVEL=info
RULES_PATH=/app/rules
RULES_HOT_RELOAD=true
WORKERS=4
CACHE_ENABLED=true
CACHE_TTL=300
METRICS_ENABLED=true
HEALTH_CHECK_ENABLED=true

# Resource Limits
CPU_LIMIT_SMALL=0.5
CPU_LIMIT_MEDIUM=1.0
MEMORY_LIMIT_SMALL=512m
MEMORY_LIMIT_MEDIUM=1g
```

## 🚀 Casos de Uso

### 1. **Reglas de Pricing Dinámico**

#### Crear regla de descuentos por volumen
```json
{
  "ruleName": "descuentos_volumen",
  "description": "Aplicar descuentos basados en cantidad de productos",
  "version": "1.0",
  "rules": [
    {
      "when": {
        "quantity": {">=": 100}
      },
      "then": {
        "discount_percentage": 15,
        "discount_reason": "Descuento por volumen alto"
      }
    },
    {
      "when": {
        "quantity": {">=": 50, "<": 100}
      },
      "then": {
        "discount_percentage": 10,
        "discount_reason": "Descuento por volumen medio"
      }
    },
    {
      "when": {
        "quantity": {">=": 10, "<": 50}
      },
      "then": {
        "discount_percentage": 5,
        "discount_reason": "Descuento por volumen bajo"
      }
    }
  ]
}
```

#### Ejecutar regla via API
```bash
# Test de regla de descuentos
curl -X POST "http://localhost:8180/api/rules/descuentos_volumen/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "quantity": 75,
      "unit_price": 100.00,
      "customer_type": "premium",
      "product_category": "electronics"
    }
  }'

# Respuesta esperada:
# {
#   "output": {
#     "discount_percentage": 10,
#     "discount_reason": "Descuento por volumen medio",
#     "final_price": 90.00,
#     "savings": 10.00
#   },
#   "execution_time_ms": 12
# }
```

### 2. **Reglas de Validación de Productos**

#### Validación compleja de productos
```json
{
  "ruleName": "validacion_productos",
  "description": "Validar productos antes de guardar en inventario",
  "version": "2.1",
  "rules": [
    {
      "when": {
        "and": [
          {"product_name": {"length": {"<": 3}}},
          {"product_name": {"not_empty": true}}
        ]
      },
      "then": {
        "valid": false,
        "error": "Nombre de producto muy corto",
        "error_code": "INVALID_NAME_LENGTH"
      }
    },
    {
      "when": {
        "and": [
          {"price": {"<=": 0}},
          {"product_type": {"!=": "free_sample"}}
        ]
      },
      "then": {
        "valid": false,
        "error": "Precio debe ser mayor que 0 para productos comerciales",
        "error_code": "INVALID_PRICE"
      }
    },
    {
      "when": {
        "and": [
          {"category": {"in": ["electronics", "computers"]}},
          {"warranty_months": {"<": 12}}
        ]
      },
      "then": {
        "valid": false,
        "error": "Productos electrónicos requieren mínimo 12 meses de garantía",
        "error_code": "INSUFFICIENT_WARRANTY"
      }
    },
    {
      "when": {
        "and": [
          {"product_name": {"length": {">=": 3}}},
          {"price": {">": 0}},
          {"category": {"not_empty": true}}
        ]
      },
      "then": {
        "valid": true,
        "message": "Producto válido para registrar"
      }
    }
  ]
}
```

#### Integración en microservicio
```java
// ProductValidationService.java
@Service
public class ProductValidationService {
    
    @Value("${gorules.brms.url}")
    private String gorulesUrl;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public ValidationResult validateProduct(Product product) {
        String url = gorulesUrl + "/api/rules/validacion_productos/execute";
        
        Map<String, Object> input = Map.of(
            "product_name", product.getName(),
            "price", product.getPrice(),
            "category", product.getCategory(),
            "warranty_months", product.getWarrantyMonths(),
            "product_type", product.getType()
        );
        
        Map<String, Object> request = Map.of("input", input);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> output = (Map<String, Object>) response.getBody().get("output");
            
            return ValidationResult.builder()
                .valid((Boolean) output.get("valid"))
                .message((String) output.getOrDefault("message", output.get("error")))
                .errorCode((String) output.get("error_code"))
                .build();
                
        } catch (Exception e) {
            log.error("Error validating product with GoRules: {}", e.getMessage());
            throw new BusinessRuleException("Error en validación de producto", e);
        }
    }
}
```

### 3. **Reglas de Routing y Asignación**

#### Asignación automática de pedidos a almacenes
```json
{
  "ruleName": "asignacion_almacenes",
  "description": "Determinar qué almacén debe procesar cada pedido",
  "version": "1.5",
  "rules": [
    {
      "when": {
        "and": [
          {"customer_location": {"in": ["Lima", "Callao", "San Isidro"]}},
          {"order_weight": {"<=": 10}},
          {"priority": {"!=": "express"}}
        ]
      },
      "then": {
        "assigned_warehouse": "WAREHOUSE_LIMA_CENTRAL",
        "estimated_delivery_hours": 24,
        "shipping_cost": 15.00
      }
    },
    {
      "when": {
        "and": [
          {"customer_location": {"in": ["Arequipa", "Cusco", "Tacna"]}},
          {"order_value": {">": 500}}
        ]
      },
      "then": {
        "assigned_warehouse": "WAREHOUSE_AREQUIPA",
        "estimated_delivery_hours": 48,
        "shipping_cost": 25.00,
        "special_handling": true
      }
    },
    {
      "when": {
        "priority": {"==": "express"}
      },
      "then": {
        "assigned_warehouse": "WAREHOUSE_EXPRESS_HUB",
        "estimated_delivery_hours": 4,
        "shipping_cost": 50.00,
        "express_service": true
      }
    }
  ]
}
```

### 4. **Reglas de Aprobación de Crédito**

#### Evaluación automática de crédito
```json
{
  "ruleName": "aprobacion_credito",
  "description": "Evaluar solicitudes de crédito automáticamente",
  "version": "3.0",
  "rules": [
    {
      "when": {
        "and": [
          {"credit_score": {">=": 750}},
          {"monthly_income": {">=": 5000}},
          {"debt_to_income_ratio": {"<=": 0.3}},
          {"employment_years": {">=": 2}}
        ]
      },
      "then": {
        "approved": true,
        "credit_limit": 50000,
        "interest_rate": 12.5,
        "approval_reason": "Excelente perfil crediticio",
        "requires_manual_review": false
      }
    },
    {
      "when": {
        "and": [
          {"credit_score": {">=": 650, "<": 750}},
          {"monthly_income": {">=": 3000}},
          {"debt_to_income_ratio": {"<=": 0.4}}
        ]
      },
      "then": {
        "approved": true,
        "credit_limit": 25000,
        "interest_rate": 15.5,
        "approval_reason": "Buen perfil crediticio",
        "requires_manual_review": false
      }
    },
    {
      "when": {
        "or": [
          {"credit_score": {"<": 600}},
          {"debt_to_income_ratio": {">": 0.5}},
          {"bankruptcy_history": {"==": true}}
        ]
      },
      "then": {
        "approved": false,
        "credit_limit": 0,
        "denial_reason": "Perfil de riesgo alto",
        "requires_manual_review": true
      }
    }
  ]
}
```

### 5. **Reglas de Marketing y Promociones**

#### Segmentación dinámica de clientes
```json
{
  "ruleName": "segmentacion_marketing",
  "description": "Segmentar clientes para campañas específicas",
  "version": "2.3",
  "rules": [
    {
      "when": {
        "and": [
          {"total_purchases": {">": 10000}},
          {"last_purchase_days": {"<=": 30}},
          {"customer_tier": {"==": "premium"}}
        ]
      },
      "then": {
        "segment": "VIP_ACTIVE",
        "recommended_campaign": "exclusive_offers",
        "discount_eligibility": 20,
        "personal_manager": true
      }
    },
    {
      "when": {
        "and": [
          {"age": {">=": 25, "<=": 35}},
          {"purchase_category": {"contains": "technology"}},
          {"mobile_user": {"==": true}}
        ]
      },
      "then": {
        "segment": "TECH_MILLENNIALS",
        "recommended_campaign": "mobile_tech_deals",
        "preferred_channel": "mobile_push",
        "best_contact_time": "evening"
      }
    },
    {
      "when": {
        "last_purchase_days": {">": 90}
      },
      "then": {
        "segment": "INACTIVE_CUSTOMERS",
        "recommended_campaign": "win_back",
        "special_offer": "comeback_discount",
        "contact_priority": "high"
      }
    }
  ]
}
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar business rules stack
./manage-stack.sh start business

# Ver logs de GoRules
docker logs gorules-editor -f
docker logs gorules-brms -f

# Restart servicios
docker restart gorules-editor
docker restart gorules-brms
```

### Administración de Reglas
```bash
# Listar todas las reglas
curl "http://localhost:8180/api/rules" | jq '.rules[].name'

# Obtener regla específica
curl "http://localhost:8180/api/rules/descuentos_volumen" | jq '.'

# Crear nueva regla
curl -X POST "http://localhost:8180/api/rules" \
  -H "Content-Type: application/json" \
  -d @nueva-regla.json

# Actualizar regla existente
curl -X PUT "http://localhost:8180/api/rules/descuentos_volumen" \
  -H "Content-Type: application/json" \
  -d @regla-actualizada.json

# Eliminar regla
curl -X DELETE "http://localhost:8180/api/rules/regla_obsoleta"
```

### Testing de Reglas
```bash
# Test batch de múltiples casos
curl -X POST "http://localhost:8180/api/rules/validacion_productos/test" \
  -H "Content-Type: application/json" \
  -d '{
    "test_cases": [
      {
        "name": "producto_valido",
        "input": {
          "product_name": "Laptop Gaming",
          "price": 1500,
          "category": "electronics",
          "warranty_months": 24
        },
        "expected_output": {
          "valid": true
        }
      },
      {
        "name": "precio_invalido",
        "input": {
          "product_name": "Mouse",
          "price": 0,
          "category": "accessories"
        },
        "expected_output": {
          "valid": false,
          "error_code": "INVALID_PRICE"
        }
      }
    ]
  }'

# Performance test
curl -X POST "http://localhost:8180/api/rules/descuentos_volumen/benchmark" \
  -H "Content-Type: application/json" \
  -d '{
    "iterations": 1000,
    "input": {
      "quantity": 75,
      "unit_price": 100.00
    }
  }'
```

### Metrics y Monitoring
```bash
# Ver métricas de performance
curl "http://localhost:8180/metrics" | grep gorules

# Estadísticas de ejecución
curl "http://localhost:8180/api/stats" | jq '.'

# Health check detallado
curl "http://localhost:8180/health" | jq '.'

# Ver cache stats
curl "http://localhost:8180/api/cache/stats" | jq '.'
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Reglas no se cargan**
   ```bash
   # Verificar directorio de reglas
   docker exec gorules-brms ls -la /app/rules/
   
   # Ver logs de carga
   docker logs gorules-brms | grep "loading"
   
   # Validar sintaxis JSON
   curl -X POST "http://localhost:8180/api/rules/validate" \
     -H "Content-Type: application/json" \
     -d @mi-regla.json
   ```

2. **Performance lenta**
   ```bash
   # Ver estadísticas de cache
   curl "http://localhost:8180/api/cache/stats"
   
   # Limpiar cache si es necesario
   curl -X POST "http://localhost:8180/api/cache/clear"
   
   # Verificar uso de CPU/memoria
   docker stats gorules-brms
   ```

3. **Editor no guarda reglas**
   ```bash
   # Verificar permisos del volumen
   docker exec gorules-editor ls -la /app/rules/
   
   # Ver logs del editor
   docker logs gorules-editor | grep "save"
   
   # Verificar conectividad editor → brms
   docker exec gorules-editor curl -f http://gorules-brms:8080/health
   ```

4. **Errores de ejecución de reglas**
   ```bash
   # Habilitar debug logging
   # En docker-compose: LOG_LEVEL=debug
   
   # Ver trace de ejecución
   curl -X POST "http://localhost:8180/api/rules/mi-regla/execute?debug=true" \
     -H "Content-Type: application/json" \
     -d '{"input": {...}}'
   ```

## 📊 Monitoreo y Métricas

### Performance Metrics
```bash
# Métricas de ejecución
curl "http://localhost:8180/metrics" | grep -E "(execution_time|rule_hits|cache_hits)"

# Top reglas más usadas
curl "http://localhost:8180/api/stats/top-rules" | jq '.'

# Errores de ejecución
curl "http://localhost:8180/api/stats/errors" | jq '.'
```

### Business Metrics Dashboard
```json
{
  "business_rules_dashboard": {
    "panels": [
      {
        "title": "Rules Execution Rate",
        "query": "rate(gorules_executions_total[5m])"
      },
      {
        "title": "Average Execution Time",
        "query": "gorules_execution_duration_seconds"
      },
      {
        "title": "Cache Hit Rate",
        "query": "gorules_cache_hits_total / gorules_cache_requests_total"
      },
      {
        "title": "Rule Errors",
        "query": "rate(gorules_errors_total[5m])"
      }
    ]
  }
}
```

### Audit Trail
```bash
# Ver historial de cambios en reglas
curl "http://localhost:8180/api/audit/rules" | jq '.'

# Audit de ejecuciones (si está habilitado)
curl "http://localhost:8180/api/audit/executions?rule=descuentos_volumen&limit=100" | jq '.'
```

## 🔐 Seguridad y Gobernanza

### Control de Acceso
```json
{
  "rule_permissions": {
    "roles": {
      "business_analyst": {
        "permissions": ["read", "create", "edit"],
        "rule_patterns": ["marketing_*", "pricing_*"]
      },
      "developer": {
        "permissions": ["read", "execute"],
        "rule_patterns": ["*"]
      },
      "admin": {
        "permissions": ["read", "create", "edit", "delete", "execute"],
        "rule_patterns": ["*"]
      }
    }
  }
}
```

### Versionado y Deployment
```bash
# Crear versión de regla
curl -X POST "http://localhost:8180/api/rules/descuentos_volumen/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "2.0",
    "description": "Agregados descuentos para clientes premium",
    "changes": ["Added premium customer logic", "Updated discount tiers"]
  }'

# Rollback a versión anterior
curl -X POST "http://localhost:8180/api/rules/descuentos_volumen/rollback" \
  -H "Content-Type: application/json" \
  -d '{"target_version": "1.5"}'

# A/B testing de reglas
curl -X POST "http://localhost:8180/api/rules/descuentos_volumen/split-test" \
  -H "Content-Type: application/json" \
  -d '{
    "traffic_split": 50,
    "version_a": "1.5",
    "version_b": "2.0",
    "duration_hours": 72
  }'
```

## 🏗️ Arquitectura de Integración

### Integración con Microservicios
```java
// Configuration para Spring Boot
@Configuration
public class GoRulesConfig {
    
    @Bean
    public GoRulesClient goRulesClient(@Value("${gorules.url}") String baseUrl) {
        return GoRulesClient.builder()
            .baseUrl(baseUrl)
            .timeout(Duration.ofSeconds(5))
            .retries(3)
            .circuitBreaker(true)
            .build();
    }
}

// Service wrapper
@Service
public class BusinessRulesService {
    
    @Autowired
    private GoRulesClient goRulesClient;
    
    @Cacheable("business-rules")
    public <T> T executeRule(String ruleName, Object input, Class<T> outputType) {
        try {
            RuleExecutionRequest request = RuleExecutionRequest.builder()
                .input(input)
                .build();
                
            RuleExecutionResponse response = goRulesClient.executeRule(ruleName, request);
            return objectMapper.convertValue(response.getOutput(), outputType);
            
        } catch (Exception e) {
            log.error("Error executing rule {}: {}", ruleName, e.getMessage());
            throw new BusinessRuleException("Failed to execute business rule", e);
        }
    }
}
```

### Event-Driven Rules
```bash
# Configurar webhook para cambios en reglas
curl -X POST "http://localhost:8180/api/webhooks" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "http://api-gateway:8000/webhook/rules-changed",
    "events": ["rule.created", "rule.updated", "rule.deleted"],
    "secret": "webhook-secret-key"
  }'
```

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (redes)

### Integra con:
- `docker-compose-apps.yml` (reglas en microservicios)
- `docker-compose-gateway.yml` (reglas de routing)
- `docker-compose-observability.yml` (métricas de reglas)
- `docker-compose-workflow.yml` (reglas en workflows)

## 📚 Enlaces Útiles

- [GoRules Documentation](https://gorules.io/docs)
- [Business Rules Management](https://gorules.io/docs/user-guide/introduction)
- [GoRules API Reference](https://gorules.io/docs/api/overview)
- [Decision Tables Guide](https://gorules.io/docs/user-guide/decision-tables)
- [Expression Language](https://gorules.io/docs/user-guide/expressions)
- [Integration Examples](https://github.com/gorules/gorules-examples)

## 🎯 Próximos Pasos

1. **Configurar versionado** automático de reglas
2. **Implementar A/B testing** para reglas críticas
3. **Configurar alertas** para ejecuciones fallidas
4. **Integrar con CI/CD** para deployment de reglas
5. **Configurar backup** automático de reglas