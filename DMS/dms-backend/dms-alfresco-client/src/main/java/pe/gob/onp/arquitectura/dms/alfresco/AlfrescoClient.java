
package pe.gob.onp.arquitectura.dms.alfresco;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

@Component
public class AlfrescoClient {

    private static final String API_V1 = "/api/-default-/public/alfresco/versions/1";
    private static final String SEARCH_V1 = "/api/-default-/public/search/versions/1/search";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        NodeBodyCreate body = new NodeBodyCreate(name, "cm:folder", properties);
        return exchange(
                client.post().uri(API_V1 + "/nodes/{id}/children", parentId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body),
                NodeEntry.class
        ).block();
    }

    public NodeEntry uploadFile(String parentId, String filename, byte[] content, String mimeType, Map<String, Object> props) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("name", filename);
        form.add("nodeType", "cm:content");

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
        SearchRequest req = new SearchRequest(new SearchRequest.Query(luceneQuery), maxItems, skip);
        return exchange(
                client.post().uri(SEARCH_V1).contentType(MediaType.APPLICATION_JSON).bodyValue(req),
                SearchResponse.class
        ).block();
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

    private <T> Mono<T> exchange(WebClient.RequestHeadersSpec<?> spec, Class<T> bodyType) {
        return spec.retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> new AlfrescoException("HTTP %s: %s".formatted(resp.statusCode(), body)))
                )
                .bodyToMono(bodyType);
    }

    /**
     * Crea la ruta completa bajo -root- si no existe.
     * Ej: "/Company Home/Expedientes/EXP-2025-000123"
     * Devuelve el nodeId del ÚLTIMO segmento.
     */
    public String ensurePath(String absolutePathFromRoot) {
        String parentId = "-root-";
        if (absolutePathFromRoot == null || absolutePathFromRoot.isBlank()) {
            throw new AlfrescoException("Path vacío en ensurePath");
        }

        String[] segments = absolutePathFromRoot.split("/");
        for (String seg : segments) {
            if (seg == null || seg.isBlank()) continue;

            // 1) Intentar encontrar hijo con ese nombre
            String childId = findChildIdByName(parentId, seg);

            // 2) Si no existe, crearlo
            if (childId == null) {
                try {
                    NodeEntry created = createFolder(parentId, seg, Map.of("cm:title", seg));
                    // asumiendo que NodeEntry tiene getEntry().getId()
                    childId = created.entry().id();
                } catch (RuntimeException e) {
                    // Posible carrera: otro proceso la creó; reintenta resolver por nombre
                    childId = findChildIdByName(parentId, seg);
                    if (childId == null) {
                        throw e;
                    }
                }
            }

            parentId = childId;
        }

        return parentId;
    }

    /**
     * Busca un hijo por nombre usando: GET /nodes/{parentId}/children?where=(name='...')&include=properties
     * Devuelve el nodeId o null si no existe.
     */
    private String findChildIdByName(String parentId, String name) {
        String where = "(name='{n}')".replace("{n}", name.replace("'", "\\'"));

        NodeChildrenList res = client.get()
                .uri(uri -> uri
                        .path(API_V1 + "/nodes/{pid}/children")
                        .queryParam("where", where)
                        .queryParam("include", "properties")
                        .build(parentId))
                .retrieve()
                .bodyToMono(NodeChildrenList.class)
                .block();

        if (res == null || res.getList() == null || res.getList().getEntries() == null || res.getList().getEntries().isEmpty()) {
            return null;
        }
        return res.getList().getEntries().get(0).getEntry().id();
    }


}
