# Plan de Implementación DMS - Funcionalidad Real con Alfresco

## 1. Configuración de Roles Completos en Keycloak

### Roles del Sistema
```
DMS_ADMIN         - Administrador completo del DMS
EXPEDIENTE_ADMIN  - Administrador de expedientes
EDITOR           - Puede crear/editar documentos
REVISOR          - Puede revisar y aprobar documentos
LECTOR           - Solo lectura de documentos
EXTERNO          - Acceso limitado para usuarios externos
```

### Pasos de Configuración

#### 1.1 Acceder a Keycloak Admin Console
```bash
# URL: http://localhost/auth/admin
# Usuario: admin / Contraseña: admin
```

#### 1.2 Crear Roles de Cliente
1. Ir a `Clients` → `dms-service` → `Roles`
2. Crear cada rol con descripción:
   - **DMS_ADMIN**: "Administrador completo del sistema DMS"
   - **EXPEDIENTE_ADMIN**: "Administrador de expedientes y documentos"
   - **EDITOR**: "Editor de documentos con permisos de escritura"
   - **REVISOR**: "Revisor de documentos con permisos de aprobación"
   - **LECTOR**: "Lector con acceso de solo lectura"
   - **EXTERNO**: "Usuario externo con acceso limitado"

#### 1.3 Configurar Mappers de Token
1. `Clients` → `dms-service` → `Client Scopes` → `dms-service-dedicated`
2. Agregar mapper para roles:
   ```
   Name: client-roles
   Mapper Type: User Client Role
   Client ID: dms-service
   Token Claim Name: resource_access.dms-service.roles
   Add to userinfo: ON
   ```

#### 1.4 Crear Usuarios de Prueba
```
usuario-admin@test.com    → DMS_ADMIN
usuario-expediente@test.com → EXPEDIENTE_ADMIN  
usuario-editor@test.com   → EDITOR
usuario-revisor@test.com  → REVISOR
usuario-lector@test.com   → LECTOR
usuario-externo@test.com  → EXTERNO
```

## 2. Implementación Real del Cliente Alfresco

### 2.1 Configuración Alfresco Repository

#### application.yml Actualizado
```yaml
alfresco:
  base-url: http://alfresco-repository:8080/alfresco
  api:
    version: 1
    timeout: 30s
  auth:
    username: ${ALFRESCO_USER:admin}
    password: ${ALFRESCO_PASSWORD:admin}
  sites:
    default: dms-documents
    expedientes: expedientes-site
  folders:
    templates: /Company Home/Data Dictionary/Space Templates/DMS
    expedientes: /Company Home/Sites/expedientes-site/documentLibrary
```

#### Creación de Site en Alfresco
```bash
# Script para crear el site necesario
curl -X POST "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/sites" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "id": "expedientes-site",
    "title": "Expedientes DMS", 
    "description": "Site para gestión de expedientes",
    "visibility": "PRIVATE"
  }'
```

### 2.2 Actualización del AlfrescoClient

#### Nuevos DTOs
```java
// NodeResponse.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record NodeResponse(
    String id,
    String name,
    String nodeType,
    Instant createdAt,
    Instant modifiedAt,
    String createdByUser,
    String modifiedByUser,
    Map<String, Object> properties,
    ContentInfo content
) {}

// ContentInfo.java
public record ContentInfo(
    String mimeType,
    String encoding,
    Long sizeInBytes
) {}

// CreateNodeRequest.java
public record CreateNodeRequest(
    String name,
    String nodeType,
    Map<String, Object> properties,
    List<String> aspectNames
) {}
```

#### AlfrescoClient Mejorado
```java
@Component
@Slf4j
public class AlfrescoClient {
    
    private final WebClient webClient;
    
    public AlfrescoClient(AlfrescoConfig config) {
        this.webClient = WebClient.builder()
            .baseUrl(config.getBaseUrl() + "/api/-default-/public/alfresco/versions/1")
            .defaultHeaders(headers -> {
                headers.setBasicAuth(config.getAuth().getUsername(), 
                                   config.getAuth().getPassword());
                headers.setContentType(MediaType.APPLICATION_JSON);
            })
            .build();
    }
    
    // Crear nodo (documento/carpeta)
    public Mono<NodeResponse> createNode(String parentId, CreateNodeRequest request) {
        return webClient.post()
            .uri("/nodes/{parentId}/children", parentId)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::handleError)
            .bodyToMono(NodeResponse.class)
            .doOnNext(node -> log.info("Created node: {}", node.id()));
    }
    
    // Obtener nodo por ID
    public Mono<NodeResponse> getNode(String nodeId) {
        return webClient.get()
            .uri("/nodes/{nodeId}?include=properties,content", nodeId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::handleError)
            .bodyToMono(NodeResponse.class);
    }
    
    // Buscar nodos
    public Mono<SearchResponse> searchNodes(String query, int maxItems, int skipCount) {
        var searchRequest = Map.of(
            "query", Map.of(
                "query", query,
                "language", "afts"
            ),
            "paging", Map.of(
                "maxItems", maxItems,
                "skipCount", skipCount
            )
        );
        
        return webClient.post()
            .uri("/search/request")
            .bodyValue(searchRequest)
            .retrieve()
            .bodyToMono(SearchResponse.class);
    }
    
    // Subir contenido
    public Mono<NodeResponse> uploadContent(String parentId, String fileName, 
                                          Resource content, String description) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("filedata", content);
        parts.add("name", fileName);
        parts.add("nodeType", "cm:content");
        parts.add("description", description);
        
        return webClient.post()
            .uri("/nodes/{parentId}/children", parentId)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(parts)
            .retrieve()
            .bodyToMono(NodeResponse.class);
    }
    
    // Descargar contenido
    public Mono<Resource> downloadContent(String nodeId) {
        return webClient.get()
            .uri("/nodes/{nodeId}/content", nodeId)
            .retrieve()
            .bodyToMono(Resource.class);
    }
    
    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .map(error -> new AlfrescoException(
                "Alfresco error: " + response.statusCode() + " - " + error));
    }
}
```

## 3. Implementación de Servicios Reales

### 3.1 ExpedienteService Real
```java
@Service
@Slf4j
@Transactional
public class ExpedienteService {
    
    private final AlfrescoClient alfrescoClient;
    private final AlfrescoConfig alfrescoConfig;
    
    // Crear expediente
    public Mono<Expediente> crearExpediente(CrearExpedienteRequest request, String userId) {
        String folderName = generarNombreExpediente(request);
        
        var createFolderRequest = new CreateNodeRequest(
            folderName,
            "cm:folder",
            Map.of(
                "cm:title", request.titulo(),
                "cm:description", request.descripcion(),
                "dms:numeroExpediente", request.numeroExpediente(),
                "dms:estado", "CREADO",
                "dms:creadoPor", userId,
                "dms:fechaCreacion", Instant.now()
            ),
            List.of("dms:expediente")
        );
        
        return alfrescoClient.createNode(getExpedientesFolderId(), createFolderRequest)
            .map(this::mapToExpediente)
            .doOnNext(exp -> log.info("Expediente creado: {}", exp.getId()));
    }
    
    // Obtener expediente
    public Mono<Expediente> obtenerExpediente(String expedienteId) {
        return alfrescoClient.getNode(expedienteId)
            .map(this::mapToExpediente)
            .switchIfEmpty(Mono.error(
                new ExpedienteNotFoundException("Expediente no encontrado: " + expedienteId)));
    }
    
    // Agregar documento a expediente
    public Mono<Documento> agregarDocumento(String expedienteId, AgregarDocumentoRequest request) {
        return uploadDocumentContent(expedienteId, request)
            .map(this::mapToDocumento);
    }
    
    // Buscar expedientes
    public Mono<Page<Expediente>> buscarExpedientes(String query, Pageable pageable) {
        String alfrescoQuery = buildAlfrescoQuery(query);
        
        return alfrescoClient.searchNodes(alfrescoQuery, 
                                        pageable.getPageSize(), 
                                        (int) pageable.getOffset())
            .map(response -> mapToExpedientePage(response, pageable));
    }
    
    private String buildAlfrescoQuery(String query) {
        return String.format(
            "TYPE:\"cm:folder\" AND ASPECT:\"dms:expediente\" AND (cm:name:\"%s*\" OR cm:title:\"%s*\")",
            query, query);
    }
}
```

### 3.2 Security Configuration Real
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ping", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/expedientes/**")
                    .hasAnyRole("DMS_ADMIN", "EXPEDIENTE_ADMIN", "EDITOR", "REVISOR", "LECTOR", "EXTERNO")
                .requestMatchers(HttpMethod.POST, "/expedientes")
                    .hasAnyRole("DMS_ADMIN", "EXPEDIENTE_ADMIN", "EDITOR")
                .requestMatchers(HttpMethod.PUT, "/expedientes/**")
                    .hasAnyRole("DMS_ADMIN", "EXPEDIENTE_ADMIN", "EDITOR")
                .requestMatchers(HttpMethod.DELETE, "/expedientes/**")
                    .hasAnyRole("DMS_ADMIN", "EXPEDIENTE_ADMIN")
                .requestMatchers("/admin/**")
                    .hasRole("DMS_ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

## 4. Controllers Completos

### 4.1 ExpedientesController Real
```java
@RestController
@RequestMapping("/expedientes")
@Validated
@Slf4j
public class ExpedientesController {
    
    private final ExpedienteService expedienteService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR')")
    public Mono<ResponseEntity<Expediente>> crear(
            @Valid @RequestBody CrearExpedienteRequest request,
            Authentication auth) {
        
        return expedienteService.crearExpediente(request, auth.getName())
            .map(expediente -> ResponseEntity.status(HttpStatus.CREATED).body(expediente))
            .doOnNext(response -> log.info("Expediente creado por: {}", auth.getName()));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR','EXTERNO')")
    public Mono<ResponseEntity<Expediente>> obtener(@PathVariable String id) {
        return expedienteService.obtenerExpediente(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/documentos")
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR')")
    public Mono<ResponseEntity<Documento>> agregarDocumento(
            @PathVariable String id,
            @RequestPart("file") FilePart file,
            @RequestPart("metadata") @Valid AgregarDocumentoRequest metadata,
            Authentication auth) {
        
        return expedienteService.agregarDocumento(id, metadata, file)
            .map(documento -> ResponseEntity.status(HttpStatus.CREATED).body(documento));
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('DMS_ADMIN','EXPEDIENTE_ADMIN','EDITOR','REVISOR','LECTOR')")
    public Mono<ResponseEntity<Page<Expediente>>> buscar(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        
        return expedienteService.buscarExpedientes(q != null ? q : "", pageable)
            .map(ResponseEntity::ok);
    }
}
```

## 5. Testing

### 5.1 Configuración de Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/auth/realms/test",
    "alfresco.base-url=http://localhost:8080/alfresco"
})
class ExpedientesControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private ExpedienteService expedienteService;
    
    @Test
    @WithMockJwt(roles = {"DMS_ADMIN"})
    void crearExpediente_conRolAdmin_debeCrear() {
        // Test implementation
    }
}
```

## 6. Próximos Pasos

1. **Configurar roles en Keycloak** según el plan detallado
2. **Implementar AlfrescoClient** con las operaciones reales
3. **Crear services** con lógica de negocio real
4. **Implementar controllers** completos con validaciones
5. **Agregar testing** unitario e integración
6. **Configurar aspects** de Alfresco personalizados
7. **Implementar métricas** y observabilidad
8. **Documentar APIs** con OpenAPI

¿Por cuál de estos puntos te gustaría empezar?