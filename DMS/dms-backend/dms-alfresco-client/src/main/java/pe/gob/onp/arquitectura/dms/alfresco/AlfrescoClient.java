
package pe.gob.onp.arquitectura.dms.alfresco;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.w3c.dom.NodeList;
import pe.gob.onp.arquitectura.dms.alfresco.dto.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeChildrenList;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class AlfrescoClient {

    private static final String API_V1 = "/api/-default-/public/alfresco/versions/1";
    private static final String SEARCH_V1 = "/api/-default-/public/search/versions/1/search";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AlfrescoClient.class);

    private final WebClient client;

    public AlfrescoClient(WebClient alfrescoWebClient) {
        this.client = alfrescoWebClient;
    }

    public NodeEntry getNode(String nodeId) {
        return exchange(
                client.get().uri(API_V1 + "/nodes/{id}", nodeId),
                NodeEntry.class
        ).blockOptional().orElseThrow(() -> new AlfrescoException("Node not found: " + nodeId));
    }

    public NodeEntry createFolder(String parentId, String name, Map<String, Object> properties) {
        log.info("Creando el Folder");
        log.debug("parentId: {}", parentId);
        log.debug("name: {}", name);
        log.debug("properties: {}", properties);
        //NodeBodyCreate body = new NodeBodyCreate(name, "cm:folder", properties);
        NodeBodyCreate body = new NodeBodyCreate(name, "dms:expediente", properties);
        log.debug("body: {}", body);
        try {
            log.info("uri1: {}", API_V1 + "/nodes/{id}/children");
            return exchange(
                    client.post().uri(API_V1 + "/nodes/{id}/children", parentId)
                            .contentType(MediaType.APPLICATION_JSON).bodyValue(body),
                    NodeEntry.class
            ).block();
        } catch (AlfrescoException e) {
            // Si es error de duplicado, buscar la carpeta existente
            if (e.getMessage() != null && e.getMessage().contains("Duplicate child name not allowed")) {
                log.warn("Carpeta '{}' ya existe en parent '{}', buscando existente", name, parentId);
                String existingId = findChildIdByName(parentId, name);
                if (existingId != null) {
                    return getNode(existingId);
                }
            }
            throw e; // Re-lanzar otros errores
        }
    }

    public NodeEntry uploadFile(String parentId, String filename, byte[] content, String mimeType, Map<String, Object> props) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("name", filename);
        //form.add("nodeType", "cm:content");
        form.add("nodeType", "dms:documento");

        if (props != null && !props.isEmpty()) {
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            form.add("properties", new HttpEntity<>(toJson(props), jsonHeaders));
        }

        HttpHeaders fileHeaders = new HttpHeaders();
        if (mimeType != null && !mimeType.isBlank()) {
            fileHeaders.setContentType(MediaType.parseMediaType(mimeType));
            form.add("mimeType", mimeType); // compat extra
        }

        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        form.add("filedata", new HttpEntity<>(resource, fileHeaders));

        return exchange(
                client.post()
                        .uri(API_V1 + "/nodes/{id}/children?autoRename=true", parentId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(form)),
                NodeEntry.class
        ).block();
    }

    public SearchResponse searchLucene(String luceneQuery, int maxItems, int skip) {
        log.info("=== INICIANDO BÚSQUEDA LUCENE ===");
        log.info("Query: '{}'", luceneQuery);
        log.info("MaxItems: {}, Skip: {}", maxItems, skip);

        SearchRequest req = new SearchRequest(
                new SearchRequest.Query(luceneQuery),
                new SearchRequest.Paging(skip, maxItems)
        );

        log.info("SearchRequest construido: {}", req);
        log.info("Enviando request a: {}", SEARCH_V1);

        try {
            SearchResponse response = exchange(
                    client.post().uri(SEARCH_V1).contentType(MediaType.APPLICATION_JSON).bodyValue(req),
                    SearchResponse.class
            ).block();

            log.info("=== RESPUESTA LUCENE RECIBIDA ===");
            if (response == null) {
                log.error("SearchResponse es NULL");
            } else {
                log.info("SearchResponse recibido exitosamente");
                if (response.list() == null) {
                    log.warn("response.list() es NULL");
                } else {
                    if (response.list().entries() == null) {
                        log.warn("response.list().entries() es NULL");
                    } else {
                        log.info("Cantidad de resultados encontrados: {}", response.list().entries().size());

                        // Log de cada resultado encontrado
                        for (int i = 0; i < response.list().entries().size(); i++) {
                            var entry = response.list().entries().get(i);
                            log.info("Resultado #{}: ID='{}', Name='{}', Type='{}'",
                                    i + 1,
                                    entry.entry().id(),
                                    entry.entry().name(),
                                    entry.entry().nodeType());
                        }
                    }
                }
            }
            log.info("=== FIN RESPUESTA LUCENE ===");

            return response;

        } catch (Exception e) {
            log.error("=== ERROR EN BÚSQUEDA LUCENE ===");
            log.error("Query que falló: '{}'", luceneQuery);
            log.error("Error: {}", e.getMessage());
            log.error("=== FIN ERROR LUCENE ===");
            throw e;
        }
    }

    public void setContent(String nodeId, byte[] content, String mimeType) {
        exchange(
                client.put()
                        .uri(API_V1 + "/nodes/{id}/content?majorVersion=true", nodeId)
                        .contentType(MediaType.parseMediaType(mimeType))
                        .bodyValue(content),
                Void.class
        ).block();
    }

    public void createRendition(String nodeId, String renditionId) {
        exchange(
                client.post()
                        .uri(API_V1 + "/nodes/{id}/renditions", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("id", renditionId)),
                Void.class
        ).block();
    }


    private static String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new AlfrescoException("Error building JSON properties", ex);
        }
    }

    /*private <T> Mono<T> exchange(WebClient.RequestHeadersSpec<?> spec, Class<T> bodyType) {
        return spec.retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> new AlfrescoException("HTTP %s: %s".formatted(resp.statusCode(), body)))
                )
                .bodyToMono(bodyType);
    }*/

    private <T> Mono<T> exchange(WebClient.RequestHeadersSpec<?> spec, Class<T> bodyType) {
        log.info("Entramos a exchange");
        log.info("=== ALFRESCO REQUEST START ===");
        log.info("Target body type: {}", bodyType.getSimpleName());

        return spec.exchangeToMono(response -> {
                    log.info("Request URL: {}", response.request().getURI());
                    log.info("Request method: {}", response.request().getMethod());
                    log.info("Response status: {} {}",
                            response.statusCode().value(), response.statusCode());
                    log.info("Response headers: {}", response.headers().asHttpHeaders());

                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        log.error("=== ALFRESCO ERROR RESPONSE ===");
                        log.error("HTTP Status: {} {}", response.statusCode().value(), response.statusCode());

                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .doOnNext(errorBody -> {
                                    log.error("Error body: {}", errorBody);
                                    log.error("=== END ERROR RESPONSE ===");
                                })
                                .flatMap(body -> {
                                    String errorMsg = "HTTP %s: %s".formatted(response.statusCode(), body);
                                    log.error("Creando AlfrescoException: {}", errorMsg);
                                    return Mono.error(new AlfrescoException(errorMsg));
                                });
                    } else {
                        return response.bodyToMono(bodyType)
                                .doOnSuccess(result -> {
                                    log.debug("=== ALFRESCO SUCCESS RESPONSE ===");
                                    log.debug("Response type: {}", result != null ? result.getClass().getSimpleName() : "null");
                                    log.debug("=== END SUCCESS RESPONSE ===");
                                });
                    }
                })
                .doOnError(throwable -> {
                    log.error("=== ALFRESCO REQUEST ERROR ===");
                    log.error("Error type: {}", throwable.getClass().getSimpleName());
                    log.error("Error message: {}", throwable.getMessage());
                    log.error("=== END REQUEST ERROR ===");
                })
                .doFinally(signalType -> {
                    log.debug("=== ALFRESCO REQUEST END ===");
                    log.debug("Signal type: {}", signalType);
                    log.debug("=============================");
                });
    }

    /**
     * Crea la ruta completa bajo -root- si no existe.
     * Ej: "/Company Home/Expedientes/EXP-2025-000123"
     * Devuelve el nodeId del ÚLTIMO segmento.
     */
    /*public String ensurePath(String absolutePathFromRoot) {
        log.info("Creando/verificando path: {}", absolutePathFromRoot);

        String parentId = "-root-";
        if (absolutePathFromRoot == null || absolutePathFromRoot.isBlank()) {
            throw new AlfrescoException("Path vacío en ensurePath");
        }

        String[] segments = absolutePathFromRoot.split("/");
        for (String seg : segments) {
            if (seg == null || seg.isBlank()) continue;

            log.debug("Procesando segmento: '{}' en parent: {}", seg, parentId);

            String childId = findChildIdByName(parentId, seg);
            log.info("Vamos a ver el valor de childId '{}'",childId);
            if (childId == null) {
                log.info("Creando carpeta '{}' en parent '{}'", seg, parentId);
                /*try {
                    NodeEntry created = createFolder(parentId, seg, Map.of("cm:title", seg));
                    childId = created.entry().id();
                    log.info("Carpeta '{}' creada exitosamente con ID: {}", seg, childId);
                } catch (RuntimeException e) {
                    log.warn("Error creando '{}', reintentando búsqueda: {}", seg, e.getMessage());
                    childId = findChildIdByName(parentId, seg);
                    if (childId == null) {
                        log.error("No se pudo crear ni encontrar carpeta '{}'", seg);
                        throw e;
                    }
                    log.info("Carpeta '{}' encontrada en segundo intento", seg);
                }}*/
    // Crear carpeta con retry en caso de concurrencia
                /*NodeEntry created = createFolderWithRetry(parentId, seg, Map.of("cm:title", seg));
                childId = created.entry().id();
                log.info("Carpeta '{}' creada exitosamente con ID: {}... a donde iré???", seg, childId);
            } else {
                log.debug("Carpeta '{}' ya existe con ID: {}", seg, childId);
            }


            parentId = childId;
        }

        log.info("Path completado exitosamente. ID final: {}", parentId);
        return parentId;
    }*/
    public String ensurePath(String absolutePathFromRoot) {
        log.info("=== ENSURE PATH START ===");
        log.info("Path solicitado: '{}'", absolutePathFromRoot);

        String parentId = "-root-";
        log.info("Iniciando desde parentId: '{}'", parentId);

        if (absolutePathFromRoot == null || absolutePathFromRoot.isBlank()) {
            log.error("Path vacío o null recibido");
            throw new AlfrescoException("Path vacío en ensurePath");
        }

        String[] segments = absolutePathFromRoot.split("/");
        log.info("Path dividido en {} segmentos: {}", segments.length, java.util.Arrays.toString(segments));

        int segmentIndex = 0;
        for (String seg : segments) {
            segmentIndex++;

            if (seg == null || seg.isBlank()) {
                log.debug("Saltando segmento vacío #{}: '{}'", segmentIndex, seg);
                continue;
            }

            log.info("--- PROCESANDO SEGMENTO #{} ---", segmentIndex);
            log.info("Segmento: '{}' | Parent actual: '{}'", seg, parentId);

            // Buscar si ya existe
            log.debug("Buscando carpeta existente '{}'...", seg);
            String childId = findChildIdByName(parentId, seg);

            log.info("Resultado búsqueda para '{}': childId = '{}'", seg, childId);

            if (childId == null) {
                log.warn("Carpeta '{}' NO EXISTE - procediendo a crear", seg);
                log.info("Creando carpeta '{}' en parent '{}'", seg, parentId);

                try {
                    log.debug("Llamando createFolderWithRetry...");
                    NodeEntry created = createFolderWithRetry(parentId, seg, Map.of("cm:title", seg));
                    childId = created.entry().id();
                    log.info("✓ Carpeta '{}' CREADA exitosamente con ID: '{}'", seg, childId);
                    log.debug("Nodo creado - Name: '{}', Type: '{}'",
                            created.entry().name(), created.entry().nodeType());

                } catch (Exception e) {
                    log.error("✗ ERROR creando carpeta '{}' en parent '{}': {}", seg, parentId, e.getMessage());
                    log.error("Tipo de excepción: {}", e.getClass().getSimpleName());
                    throw e;
                }
            } else {
                log.info("✓ Carpeta '{}' YA EXISTE con ID: '{}'", seg, childId);
            }

            // Cambiar al siguiente nivel
            String previousParentId = parentId;
            parentId = childId;
            log.info("Avanzando nivel: '{}' -> '{}'", previousParentId, parentId);
            log.info("--- FIN SEGMENTO #{} ---", segmentIndex);
        }

        log.info("=== ENSURE PATH COMPLETADO ===");
        log.info("Path final construido: '{}'", absolutePathFromRoot);
        log.info("ID final retornado: '{}'", parentId);
        log.info("===============================");

        return parentId;
    }

    private NodeEntry createFolderWithRetry(String parentId, String name, Map<String, Object> properties) {
        log.debug("=== CREATE FOLDER WITH RETRY ===");
        log.debug("Parent: '{}', Name: '{}', Properties: {}", parentId, name, properties);

        try {
            log.debug("Primer intento de creación...");
            NodeEntry result = createFolder(parentId, name, properties);
            log.debug("✓ Carpeta creada exitosamente en primer intento");
            return result;

        } catch (AlfrescoException e) {
            log.warn("Primer intento falló: {}", e.getMessage());

            if (e.getMessage() != null && e.getMessage().contains("Duplicate child name not allowed")) {
                log.info("Error de duplicado detectado - buscando carpeta existente...");

                String existingId = findChildIdByName(parentId, name);
                if (existingId != null) {
                    log.info("✓ Carpeta encontrada tras error de duplicado: '{}'", existingId);
                    NodeEntry existing = getNode(existingId);
                    log.debug("Nodo existente recuperado: {}", existing.entry().name());
                    return existing;
                } else {
                    log.error("✗ No se encontró carpeta tras error de duplicado");
                }
            }

            log.error("✗ Error no recuperable en createFolderWithRetry");
            throw e;
        }
    }
    /*public String ensurePath(String absolutePathFromRoot) {
        log.info("Creando/verificando path: {}", absolutePathFromRoot);

        String parentId = "-root-";
        if (absolutePathFromRoot == null || absolutePathFromRoot.isBlank()) {
            throw new AlfrescoException("Path vacío en ensurePath");
        }

        String[] segments = absolutePathFromRoot.split("/");
        for (String seg : segments) {
            if (seg == null || seg.isBlank()) continue;

            log.debug("Procesando segmento: '{}' en parent: {}", seg, parentId);

            String childId = findChildIdByName(parentId, seg);

            if (childId == null) {
                log.info("Creando carpeta '{}' en parent '{}'", seg, parentId);
                try {
                    NodeEntry created = createFolder(parentId, seg, Map.of("cm:title", seg));
                    childId = created.entry().id();
                    log.info("Carpeta '{}' creada con ID: {}", seg, childId);
                } catch (RuntimeException e) {
                    log.warn("Error creando '{}', reintentando búsqueda: {}", seg, e.getMessage());
                    childId = findChildIdByName(parentId, seg);
                    if (childId == null) {
                        log.error("No se pudo crear ni encontrar carpeta '{}'", seg);
                        throw e;
                    }
                }
            }

            parentId = childId;
        }

        log.info("Path completado. ID final: {}", parentId);
        return parentId;
    }*/

    /**
     * Busca un hijo por nombre usando: GET /nodes/{parentId}/children?where=(name='...')&include=properties
     * Devuelve el nodeId o null si no existe.
     */

// Reemplazar el método findChildIdByName existente
    private String findChildIdByName(String parentId, String name) {
        log.debug(">>> FIND CHILD: '{}' en parent '{}'", name, parentId);

        if (name == null || name.isBlank()) {
            log.warn("Nombre vacío en findChildIdByName");
            return null;
        }

        try {
            NodeChildrenList res = exchange(
                    client.get().uri(API_V1 + "/nodes/{pid}/children", parentId),
                    NodeChildrenList.class
            ).block();

            if (res == null || res.getList() == null || res.getList().getEntries() == null) {
                log.debug("<<< FIND CHILD RESULT: NO ENCONTRADO (lista vacía)");
                return null;
            }

            // CAMBIO CRÍTICO: Búsqueda case-insensitive
            String childId = res.getList().getEntries().stream()
                    .filter(entry -> name.equalsIgnoreCase(entry.getEntry().name()))
                    .map(entry -> entry.getEntry().id())
                    .findFirst()
                    .orElse(null);

            if (childId != null) {
                log.debug("<<< FIND CHILD RESULT: ENCONTRADO con ID '{}'", childId);
            } else {
                log.debug("<<< FIND CHILD RESULT: NO ENCONTRADO en lista de {} elementos",
                        res.getList().getEntries().size());
            }
            return childId;

        } catch (Exception e) {
            log.warn("<<< FIND CHILD ERROR: {} - RETORNANDO NULL", e.getMessage());
            return null;
        }
    }

    /*private String findChildIdByName(String parentId, String name) {
        log.debug(">>> FIND CHILD: '{}' en parent '{}'", name, parentId);

        // PROTECCIÓN: evitar bucles infinitos
        if (name == null || name.isBlank()) {
            log.warn("Nombre vacío en findChildIdByName");
            return null;
        }

        String where = "(name='{n}')".replace("{n}", name.replace("'", "\\'"));
        log.debug("Query where construido: {}", where);

        try {
            NodeChildrenList res = exchange(
                    client.get()
                            /*--------.uri(uri -> uri
                                    .path(API_V1 + "/nodes/{pid}/children")
                                    .queryParam("where", where)
                                    .queryParam("include", "properties")
                                    .build(parentId)),------------
                            .uri(API_V1 + "/nodes/{pid}/children", parentId),
                    NodeChildrenList.class
            ).block();

            /*if (res == null || res.getList() == null ||
                    res.getList().getEntries() == null ||
                    res.getList().getEntries().isEmpty()) {
                log.debug("<<< FIND CHILD RESULT: NO ENCONTRADO");
                return null;
            }-------

            if (res == null || res.getList() == null || res.getList().getEntries() == null) {
                log.debug("<<< FIND CHILD RESULT: NO ENCONTRADO (lista vacía)");
                return null;
            }

            //String childId = res.getList().getEntries().get(0).getEntry().id();
            //log.debug("<<< FIND CHILD RESULT: ENCONTRADO con ID '{}'", childId);

            // Buscar el nombre en la lista de hijos
            String childId = res.getList().getEntries().stream()
                    .filter(entry -> name.equals(entry.getEntry().name()))
                    .map(entry -> entry.getEntry().id())
                    .findFirst()
                    .orElse(null);

            if (childId != null) {
                log.debug("<<< FIND CHILD RESULT: ENCONTRADO con ID '{}'", childId);
            } else {
                log.debug("<<< FIND CHILD RESULT: NO ENCONTRADO en lista de {} elementos",
                        res.getList().getEntries().size());
            }
            return childId;

        } catch (Exception e) {  // Cambié AlfrescoException por Exception
            log.warn("<<< FIND CHILD ERROR: {} - RETORNANDO NULL", e.getMessage());
            return null;  // NO reintentar, devolver null
        }
    }*/

    /*private String findChildIdByName(String parentId, String name) {
        log.debug("Buscando hijo '{}' en parent '{}'", name, parentId);

        String where = "(name='{n}')".replace("{n}", name.replace("'", "\\'"));
        log.debug("Query where: {}", where);

        try {
            NodeChildrenList res = exchange(
                    client.get()
                            .uri(uri -> uri
                                    .path(API_V1 + "/nodes/{pid}/children")
                                    .queryParam("where", where)
                                    .queryParam("include", "properties")
                                    .build(parentId)),
                    NodeChildrenList.class
            ).block();

            if (res == null || res.getList() == null ||
                    res.getList().getEntries() == null ||
                    res.getList().getEntries().isEmpty()) {
                log.debug("No se encontró hijo '{}' en parent '{}'", name, parentId);
                return null;
            }

            String childId = res.getList().getEntries().get(0).getEntry().id();
            log.debug("Encontrado hijo '{}' con ID: {}", name, childId);
            return childId;

        } catch (AlfrescoException e) {
            log.warn("Error buscando hijo '{}' en parent '{}': {}", name, parentId, e.getMessage());
            return null;
        }
    }*/

    public NodeEntry updateNode(String nodeId, Map<String, Object> properties) {
        NodeBodyUpdate body = new NodeBodyUpdate(null, properties);
        return exchange(
                client.put().uri(API_V1 + "/nodes/{id}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body),
                NodeEntry.class
        ).block();
    }

    public NodeEntry renameNode(String nodeId, String newName) {
        NodeBodyUpdate body = new NodeBodyUpdate(newName, null);
        return exchange(
                client.put().uri(API_V1 + "/nodes/{id}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body),
                NodeEntry.class
        ).block();
    }

    public List<String> getChildrenNames(String parentId) {
        log.debug("Obteniendo nombres de hijos para parent: {}", parentId);

        try {
            NodeChildrenList res = exchange(
                    client.get()
                            .uri(API_V1 + "/nodes/{id}/children", parentId),
                    NodeChildrenList.class
            ).block();

            if (res == null || res.getList() == null || res.getList().getEntries() == null) {
                return List.of();
            }

            List<String> names = res.getList().getEntries().stream()
                    .map(entry -> entry.getEntry().name())
                    .sorted()
                    .toList();

            log.debug("Encontrados {} hijos en parent {}: {}", names.size(), parentId, names);
            return names;

        } catch (AlfrescoException e) {
            log.error("Error obteniendo hijos de parent '{}': {}", parentId, e.getMessage());
            return List.of();
        }
    }

    // Agrega este método al final de tu clase AlfrescoClient
    public void debugRootContent() {
        log.info("Entramos a debugRootContent");
        try {
            log.info("=== VERIFICANDO CONTENIDO DE -root- ===");

            NodeChildrenList res = exchange(
                    client.get().uri(API_V1 + "/nodes/-root-/children"),
                    NodeChildrenList.class
            ).block();

            if (res != null && res.getList() != null && res.getList().getEntries() != null) {
                List<String> names = res.getList().getEntries().stream()
                        .map(entry -> entry.getEntry().name())
                        .toList();
                log.info("Carpetas encontradas en -root-: {}", names);
            } else {
                log.warn("No se encontraron carpetas en -root-");
            }

        } catch (Exception e) {
            log.error("Error verificando contenido root: {}", e.getMessage());
        }
    }

    public NodeChildrenList getNodeChildren(String parentId, String whereClause) {
        log.info("=== OBTENIENDO HIJOS DE NODO ===");
        log.info("Parent ID: '{}', Where: '{}'", parentId, whereClause);

        String uri = API_V1 + "/nodes/{id}/children";
        log.info("URI0: '{}'", uri);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            uri += "?where=" + whereClause;
        }
        log.info("URI1: '{}'", uri);
        try {
            log.info("Entramos al Try del getNodeChildren");
            NodeChildrenList result = exchange(
                    client.get().uri(uri, parentId), NodeChildrenList.class
            ).block();

            /*log.info("Resultado: {} hijos encontrados",
                    result != null && result.getList() != null && result.getList().getEntries() != null
                            ? result.getList().getEntries().size() : 0);*/

            log.info("=== DESPUÉS DEL EXCHANGE ===");
            log.info("Resultado obtenido: {}", result != null ? "no null" : "null");

            if (result != null) {
                log.info("Result.getList(): {}", result.getList() != null ? "no null" : "null");
                if (result.getList() != null) {
                    log.info("Result.getList().getEntries(): {}",
                            result.getList().getEntries() != null ? "no null" : "null");
                    if (result.getList().getEntries() != null) {
                        log.info("Número total de entradas: {}", result.getList().getEntries().size());

                        // Log de las primeras entradas encontradas
                        for (int i = 0; i < Math.min(3, result.getList().getEntries().size()); i++) {
                            var entry = result.getList().getEntries().get(i);
                            log.info("Entrada {}: name='{}', id='{}'",
                                    i, entry.getEntry().name(), entry.getEntry().id());
                        }
                    }
                }
            }

            log.info("=== FIN OBTENCIÓN HIJOS ===");
            return result;

        } catch (Exception e) {
            log.error("Error obteniendo hijos de {}: {}", parentId, e.getMessage());
            throw new AlfrescoException("No se pudieron obtener hijos de: " + parentId, e);
        }
    }

    public byte[] downloadFile(String nodeId) {
        log.debug("Descargando archivo con nodeId: {}", nodeId);

        return exchange(
                client.get().uri(API_V1 + "/nodes/{id}/content", nodeId),
                byte[].class
        ).block();
    }

    public byte[] getRendition(String nodeId, String renditionId) {
        log.debug("Obteniendo rendición '{}' para nodo '{}'", renditionId, nodeId);

        try {
            return exchange(
                    client.get().uri(API_V1 + "/nodes/{id}/renditions/{renditionId}/content",
                            nodeId, renditionId),
                    byte[].class
            ).block();
        } catch (Exception e) {
            log.warn("No se pudo obtener rendición '{}' para nodo '{}': {}", renditionId, nodeId, e.getMessage());
            return null;
        }
    }


    // Agregar este método en AlfrescoClient.java

    public NodeChildrenList getNodeChildrenPaginated(String parentId, String whereClause, int skipCount, int maxItems) {
        log.debug("=== OBTENIENDO HIJOS PAGINADOS ===");
        log.debug("Parent ID: '{}', Where: '{}', Skip: {}, Max: {}", parentId, whereClause, skipCount, maxItems);

        try {
            String uri = API_V1 + "/nodes/{id}/children";

            // Construir parámetros de consulta
            StringBuilder queryParams = new StringBuilder();
            queryParams.append("skipCount=").append(skipCount);
            queryParams.append("&maxItems=").append(maxItems);

            if (whereClause != null && !whereClause.trim().isEmpty()) {
                queryParams.append("&where=").append(whereClause);
            }

            uri += "?" + queryParams.toString();

            log.debug("URI paginada construida: '{}'", uri);

            NodeChildrenList result = exchange(
                    client.get().uri(uri, parentId),
                    NodeChildrenList.class
            ).block();

            log.debug("Página obtenida: {} elementos",
                    result != null && result.getList() != null && result.getList().getEntries() != null
                            ? result.getList().getEntries().size() : 0);

            return result;

        } catch (Exception e) {
            log.error("Error obteniendo hijos paginados de {}: {}", parentId, e.getMessage());
            throw new AlfrescoException("Error en paginación: " + e.getMessage(), e);
        }
    }
}
