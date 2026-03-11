# 🔐 Stack de Identidad - Keycloak SSO y Gestión de Identidad

## 🎯 Descripción General

El **docker-compose-identity.yml** implementa una solución completa de gestión de identidad y acceso (IAM) usando Keycloak. Proporciona Single Sign-On (SSO), autenticación multi-factor, federación de identidades y gestión centralizada de usuarios y roles.

## 🛡️ Servicios Incluidos

### 🔑 **Identity Provider**
- **Keycloak** - Servidor de identidad empresarial
- **Keycloak Database** - PostgreSQL dedicada para Keycloak

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| Keycloak | 8080 | HTTP | Interfaz de administración |
| Keycloak DB | 5438 | TCP | Base de datos PostgreSQL |

## 🔑 Accesos por Defecto

### Keycloak Admin Console
- **URL:** http://localhost:8080
- **Admin Console:** http://localhost:8080/admin
- **Usuario:** admin
- **Password:** admin123
- **Health Check:** http://localhost:8080/health/ready

### Keycloak Database
- **Host:** localhost:5438
- **Usuario:** keycloak
- **Password:** keycloak123
- **Base de datos:** keycloak

## ⚙️ Variables de Entorno Requeridas

```bash
# Keycloak Database
KEYCLOAK_DB_NAME=keycloak
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak123

# Keycloak Admin
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=admin123

# Optional - SSL Configuration
KEYCLOAK_HOSTNAME=localhost
KEYCLOAK_HTTPS_CERTIFICATE_FILE=/opt/keycloak/certs/server.crt
KEYCLOAK_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/certs/server.key
```

## 🚀 Casos de Uso

### 1. **Configuración Inicial y Realm**

#### Crear realm para aplicaciones
```bash
# Via Admin Console:
# 1. Acceder a http://localhost:8080/admin
# 2. Login con admin/admin123
# 3. Create Realm → "empresa-realm"

# Via REST API:
curl -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "empresa-realm",
    "enabled": true,
    "displayName": "Empresa Realm",
    "registrationAllowed": true,
    "resetPasswordAllowed": true
  }'
```

#### Configurar tema personalizado
```bash
# Subir tema personalizado
docker cp ./themes/empresa-theme keycloak:/opt/keycloak/themes/

# Restart Keycloak
docker restart keycloak

# Aplicar tema al realm
# Admin Console → Realm Settings → Themes → Login Theme: empresa-theme
```

### 2. **Gestión de Usuarios y Roles**

#### Crear usuarios programáticamente
```bash
# Obtener token de admin
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123&grant_type=password&client_id=admin-cli" \
  | jq -r '.access_token')

# Crear usuario
curl -X POST http://localhost:8080/admin/realms/empresa-realm/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juan.perez",
    "email": "juan.perez@empresa.com",
    "firstName": "Juan",
    "lastName": "Pérez",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "temporal123",
      "temporary": true
    }]
  }'
```

#### Configurar roles y grupos
```bash
# Crear roles de aplicación
curl -X POST http://localhost:8080/admin/realms/empresa-realm/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "admin",
    "description": "Administrador del sistema"
  }'

curl -X POST http://localhost:8080/admin/realms/empresa-realm/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "user",
    "description": "Usuario estándar"
  }'

# Crear grupo
curl -X POST http://localhost:8080/admin/realms/empresa-realm/groups \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "desarrolladores",
    "attributes": {
      "department": ["IT"],
      "location": ["Lima"]
    }
  }'
```

### 3. **Configuración de Clientes (Aplicaciones)**

#### Cliente para aplicación web (Authorization Code Flow)
```bash
# Crear cliente para frontend
curl -X POST http://localhost:8080/admin/realms/empresa-realm/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "frontend-app",
    "enabled": true,
    "publicClient": true,
    "protocol": "openid-connect",
    "redirectUris": ["http://localhost:4200/*"],
    "webOrigins": ["http://localhost:4200"],
    "standardFlowEnabled": true,
    "directAccessGrantsEnabled": false
  }'
```

#### Cliente para microservicio (Client Credentials Flow)
```bash
# Crear cliente para backend service
curl -X POST http://localhost:8080/admin/realms/empresa-realm/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "comando-service",
    "enabled": true,
    "publicClient": false,
    "protocol": "openid-connect",
    "serviceAccountsEnabled": true,
    "authorizationServicesEnabled": true,
    "standardFlowEnabled": false,
    "directAccessGrantsEnabled": false
  }'
```

### 4. **Integración con Aplicaciones**

#### Configuración en Spring Boot
```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/empresa-realm
      client:
        registration:
          keycloak:
            client-id: comando-service
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            authorization-grant-type: client_credentials
            scope: openid
        provider:
          keycloak:
            issuer-uri: http://localhost:8080/realms/empresa-realm
```

#### Configuración en Angular
```typescript
// app.module.ts
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';

const keycloakConfig = {
  url: 'http://localhost:8080',
  realm: 'empresa-realm',
  clientId: 'frontend-app'
};

function initializeKeycloak(keycloak: KeycloakService) {
  return () =>
    keycloak.init({
      config: keycloakConfig,
      initOptions: {
        onLoad: 'login-required',
        checkLoginIframe: false
      }
    });
}
```

### 5. **Federación de Identidades**

#### Configurar LDAP/Active Directory
```bash
# Via Admin Console:
# Identity Providers → User Federation → Add provider... → ldap

# Configuración típica LDAP:
# - Connection URL: ldap://ldap.empresa.com:389
# - Users DN: ou=users,dc=empresa,dc=com
# - Username LDAP attribute: uid
# - Bind DN: cn=admin,dc=empresa,dc=com
```

#### Configurar Social Login (Google)
```bash
# Via Admin Console:
# Identity Providers → Add provider... → Google

# Configuración:
# - Client ID: from Google Console
# - Client Secret: from Google Console
# - Default Scopes: openid profile email
```

### 6. **Autenticación Multi-Factor (MFA)**

#### Configurar OTP
```bash
# Via Admin Console:
# Authentication → Flows → Browser Flow
# Agregar "OTP Form" execution
# Configurar como REQUIRED

# Para usuarios individuales:
# Users → Select User → Credentials → Configure OTP
```

#### Configurar WebAuthn
```bash
# Via Admin Console:
# Authentication → Flows → Browser Flow
# Agregar "WebAuthn Authenticator" execution
# Configurar política WebAuthn:
# - Resident Key Requirement: Not specified
# - User Verification Requirement: Preferred
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar identity stack
./manage-stack.sh start identity

# Ver logs de Keycloak
docker logs keycloak -f

# Backup de configuración
docker exec keycloak /opt/keycloak/bin/kc.sh export \
  --realm empresa-realm --file /tmp/realm-backup.json

# Restaurar configuración
docker exec keycloak /opt/keycloak/bin/kc.sh import \
  --file /tmp/realm-backup.json
```

### Administración de Keycloak
```bash
# Restart Keycloak (para aplicar cambios)
docker restart keycloak

# Ver configuración actual
curl http://localhost:8080/realms/empresa-realm/.well-known/openid-configuration

# Test de conectividad LDAP
docker exec keycloak /opt/keycloak/bin/kc.sh build --db=postgres

# Cache clear
curl -X POST http://localhost:8080/admin/realms/empresa-realm/clear-realm-cache \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Testing de Autenticación
```bash
# Test login directo
curl -X POST http://localhost:8080/realms/empresa-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=juan.perez&password=password123&grant_type=password&client_id=frontend-app"

# Validar token JWT
curl -X POST http://localhost:8080/realms/empresa-realm/protocol/openid-connect/token/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=$JWT_TOKEN&client_id=frontend-app"

# Logout
curl -X POST http://localhost:8080/realms/empresa-realm/protocol/openid-connect/logout \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "refresh_token=$REFRESH_TOKEN&client_id=frontend-app"
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Keycloak no inicia**
   ```bash
   # Verificar logs
   docker logs keycloak --tail 100
   
   # Verificar conectividad a BD
   docker exec keycloak nc -zv keycloak-db 5432
   
   # Verificar variables de entorno
   docker exec keycloak env | grep KEYCLOAK
   ```

2. **Problemas de autenticación**
   ```bash
   # Verificar configuración del cliente
   curl http://localhost:8080/admin/realms/empresa-realm/clients \
     -H "Authorization: Bearer $ADMIN_TOKEN"
   
   # Verificar que el usuario existe
   curl http://localhost:8080/admin/realms/empresa-realm/users?username=juan.perez \
     -H "Authorization: Bearer $ADMIN_TOKEN"
   ```

3. **CORS issues en aplicaciones web**
   ```bash
   # Verificar Web Origins en cliente
   # Admin Console → Clients → [client-id] → Settings → Web Origins
   # Agregar: http://localhost:4200
   ```

4. **Tokens inválidos**
   ```bash
   # Verificar configuración del issuer
   curl http://localhost:8080/realms/empresa-realm/.well-known/openid-configuration
   
   # Verificar tiempo de vida de tokens
   # Admin Console → Realm Settings → Tokens
   ```

## 📊 Métricas y Monitoreo

### Health Checks
```bash
# Health general
curl http://localhost:8080/health

# Health específico
curl http://localhost:8080/health/ready
curl http://localhost:8080/health/live

# Métricas (si están habilitadas)
curl http://localhost:8080/metrics
```

### Eventos de Auditoría
```bash
# Habilitar eventos de login
# Admin Console → Events → Config → Login Events → ON

# Ver eventos recientes
# Admin Console → Events → Login Events

# Eventos de admin
# Admin Console → Events → Admin Events
```

### Logs Importantes
```bash
# Logs de autenticación
docker logs keycloak | grep "LOGIN"

# Logs de errores
docker logs keycloak | grep "ERROR"

# Logs de performance
docker logs keycloak | grep "WARN.*slow"
```

## 🔐 Seguridad y Mejores Prácticas

### Configuración de Producción
```bash
# Habilitar HTTPS
KEYCLOAK_HTTPS_CERTIFICATE_FILE=/opt/keycloak/certs/server.crt
KEYCLOAK_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/certs/server.key

# Configurar hostname
KEYCLOAK_HOSTNAME=sso.empresa.com

# Deshabilitar development mode
KEYCLOAK_HOSTNAME_STRICT=true
```

### Políticas de Password
```json
{
  "realm": "empresa-realm",
  "passwordPolicy": "length(8) and digits(1) and lowerCase(1) and upperCase(1) and specialChars(1) and notUsername(undefined) and notEmail(undefined) and passwordAge(365) and passwordHistory(3)"
}
```

### Session Management
```yaml
# Configuración de sesiones
sso-session-idle-timeout: 30m
sso-session-max-lifespan: 10h
offline-session-idle-timeout: 30d
access-token-lifespan: 15m
refresh-token-max-reuse: 0
```

## 🏗️ Arquitectura de Integración

### Flujo de Autenticación
```
User → Frontend App → Keycloak (Auth) → JWT Token → Backend API
                        ↓
                   Identity Store
                   (LDAP/DB/Social)
```

### Integración con la Capa de API Management

> Histórico:
> En la primera fase esta integración se planteó con `Kong`.
> La arquitectura objetivo actual reemplaza esa integración por `WSO2 API Manager`, manteniendo `Keycloak` como proveedor de identidad.

### Ejemplo histórico con Kong
```bash
# Configurar Kong con OIDC plugin
curl -X POST http://localhost:8001/plugins \
  -d "name=oidc" \
  -d "config.client_id=frontend-app" \
  -d "config.discovery=http://keycloak:8080/realms/empresa-realm/.well-known/openid-configuration"
```

## 🔗 Dependencias

### Prerequisitos:
- `docker-compose-base.yml` (redes base)

### Integra con:
- `docker-compose-gateway.yml` (protección de APIs; fase 1 con Kong, fase objetivo con WSO2 APIM)
- `docker-compose-apps.yml` (autenticación de microservicios)
- `docker-compose-observability.yml` (métricas de autenticación)

## 📚 Enlaces Útiles

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [OpenID Connect Specification](https://openid.net/connect/)
- [OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/oauth2/)
- [Keycloak Angular Adapter](https://github.com/mauriciovigolo/keycloak-angular)

## 🎯 Próximos Pasos

1. **Configurar certificados SSL** para producción
2. **Implementar federación** con LDAP corporativo
3. **Configurar social login** (Google, Microsoft)
4. **Implementar MFA** obligatorio para admins
5. **Configurar políticas** de password complejas
