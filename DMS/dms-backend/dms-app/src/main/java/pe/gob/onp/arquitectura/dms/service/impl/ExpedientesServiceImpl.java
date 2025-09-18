
package pe.gob.onp.arquitectura.dms.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoException;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeChildrenList;
import pe.gob.onp.arquitectura.dms.alfresco.dto.NodeEntry;
import pe.gob.onp.arquitectura.dms.alfresco.dto.SearchResponse;
import pe.gob.onp.arquitectura.dms.api.dto.ExpedienteDtos.*;
import pe.gob.onp.arquitectura.dms.alfresco.AlfrescoClient;
import pe.gob.onp.arquitectura.dms.service.ExpedientesService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExpedientesServiceImpl implements ExpedientesService {

    // Agregar esta constante al inicio de la clase
    private static final String REPOSITORIO_ID = "02dbd884-06fe-41a2-9bd8-8406fef1a234";

    private final AlfrescoClient alfrescoClient;
    private final String rootPath;
    private static final Logger log = LoggerFactory.getLogger(ExpedientesServiceImpl.class);

    private static final String DMS_NAMESPACE = "{http://www.onp.gob.pe/model/dms/1.0}";


    private static final String PROP_NUMERO_EXPEDIENTE = DMS_NAMESPACE + "numeroExpediente";
    private static final String PROP_ESTADO = DMS_NAMESPACE + "estado";
    private static final String PROP_FECHA_CREACION = DMS_NAMESPACE + "fechaCreacion";
    private static final String PROP_CARPETA_BASE = DMS_NAMESPACE + "carpetaBase";

    public ExpedientesServiceImpl(
            AlfrescoClient alfrescoClient,
            //@Value("${app.alfresco.root-path:/Repository}") String rootPath
            @Value("${app.alfresco.root-path:}") String rootPath
    ) {
        this.alfrescoClient = alfrescoClient;
        this.rootPath = rootPath;

        // DEBUG: Verificar estructura al inicializar
        log.info("Inicializando ExpedientesService con rootPath: {}", rootPath);
        try {
            alfrescoClient.debugRootContent();
        } catch (Exception e) {
            log.warn("No se pudo verificar estructura root: {}", e.getMessage());
        }
    }

    /*@Override
    public Expediente create(CreateRequest req) {
        try {
            log.info("Creando expediente: {}", req.titulo());

            // 1. Generar código único
            String expedienteCodigo = generarCodigoExpediente();
            log.debug("Código generado: {}", expedienteCodigo);

            // 2. Crear carpeta del expediente
            String expedienteFolderId = ensureExpedienteFolder(expedienteCodigo).block();
            log.debug("Carpeta creada con ID: {}", expedienteFolderId);

            // 3. Configurar propiedades del expediente
            Map<String, Object> properties = new HashMap<>();
            properties.put("cm:title", req.titulo());
            properties.put("cm:description", req.descripcion() != null ? req.descripcion() : "");
            properties.put("dms:numeroExpediente", expedienteCodigo);
            properties.put("dms:estado", "VIGENTE");
            properties.put("dms:fechaCreacion", java.time.Instant.now().toString());

            // Agregar metadatos del request
            if (req.metadatos() != null) {
                req.metadatos().forEach((key, value) -> {
                    properties.put("dms:" + key, value);
                });
            }

            // 4. Actualizar nodo con propiedades
            alfrescoClient.updateNode(expedienteFolderId, properties);
            log.info("Propiedades actualizadas para expediente: {}", expedienteCodigo);

            // 5. Crear subcarpetas estándar
            crearSubcarpetasEstandar(expedienteFolderId);

            log.info("Expediente creado exitosamente: {}", expedienteCodigo);
            return new Expediente(expedienteCodigo, req.titulo(), "VIGENTE", req.metadatos());

        } catch (Exception e) {
            log.error("Error creando expediente: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear expediente: " + e.getMessage(), e);
        }
    }*/

    @Override
    public Expediente create(CreateRequest req) {
        log.info("Entramos a create de Expediente en ExpedientesServiceImpl");
        try {
            log.info("Creando expediente en carpeta base '{}': título='{}'",
                    req.carpetaBase(), req.titulo());

            // Validar carpeta base
            if (req.carpetaBase() == null || req.carpetaBase().trim().isEmpty()) {
                throw new IllegalArgumentException("carpetaBase es requerida");
            }

            // Verificar que la carpeta base existe
            List<String> carpetasDisponibles = alfrescoClient.getChildrenNames("-root-");
            if (!carpetasDisponibles.contains(req.carpetaBase())) {
                throw new IllegalArgumentException("Carpeta base '" + req.carpetaBase() + "' no existe");
            }

            // Generar código único
            String expedienteCodigo = generarCodigoExpediente();
            log.debug("Código generado: {}", expedienteCodigo);

            // Construir path dinámico basado en carpeta seleccionada
            log.info("Configuración para creación: variableRootPath='{}', carpetaBase='{}'", rootPath, req.carpetaBase());
            //String expedientePath = rootPath + "/" + req.carpetaBase() + "/" + expedienteCodigo;
            //String expedientePath = req.carpetaBase() + "/" + expedienteCodigo;

            String expedientePath;
            if (rootPath == null || rootPath.trim().isEmpty()) {
                expedientePath = req.carpetaBase() + "/" + expedienteCodigo;
            } else {
                expedientePath = rootPath + "/" + req.carpetaBase() + "/" + expedienteCodigo;
            }

            log.info("Path completo a crear: {}", expedientePath);

            //String expedientePath = COMPANY_HOME + "/" + req.carpetaBase() + "/" + expedienteCodigo;
            log.debug("Path del expediente: {}", expedientePath);

            // Crear carpeta del expediente
            log.info("antes de enviar el expedientePath a ensurePath");
            String expedienteFolderId = alfrescoClient.ensurePath(expedientePath);
            log.debug("Carpeta creada con ID: {}", expedienteFolderId);

            // Configurar propiedades del expediente
            Map<String, Object> properties = new HashMap<>();
            properties.put("cm:title", req.titulo());
            properties.put("cm:description", req.descripcion() != null ? req.descripcion() : "");
            //properties.put("dms:numeroExpediente", expedienteCodigo);
            //properties.put("dms:estado", "VIGENTE");
            //properties.put("dms:fechaCreacion", java.time.Instant.now().toString());
            //properties.put("dms:carpetaBase", req.carpetaBase()); // Nuevo: guardar carpeta base
            properties.put(PROP_NUMERO_EXPEDIENTE, expedienteCodigo);
            properties.put(PROP_ESTADO, "VIGENTE");
            //properties.put(PROP_FECHA_CREACION, java.time.Instant.now().toString());
            //properties.put(PROP_CARPETA_BASE, req.carpetaBase());
            //properties.put("cm:created", java.time.Instant.now().toString());

            // Agregar metadatos del request
            /*if (req.metadatos() != null) {
                req.metadatos().forEach((key, value) -> {
                    properties.put("dms:" + key, value);
                });
            }*/
            // Agregar metadatos del request
            if (req.metadatos() != null) {
                log.info("=== METADATOS RECIBIDOS ===");
                log.info("Cantidad de metadatos: {}", req.metadatos().size());
                log.info("Metadatos completos: {}", req.metadatos());

                req.metadatos().forEach((key, value) -> {
                    log.info("Procesando metadato: '{}' = '{}'", key, value);
                    log.info("Propiedad a crear: 'dms:{}' = '{}'", key, value);
                    properties.put("dms:" + key, value);
                });

                log.info("=== FIN METADATOS ===");
            } else {
                log.info("No se recibieron metadatos (req.metadatos() es null)");
            }

            // Actualizar nodo con propiedades
            alfrescoClient.updateNode(expedienteFolderId, properties);
            log.info("Propiedades actualizadas para expediente: {}", expedienteCodigo);

            // Crear subcarpetas estándar
            crearSubcarpetasEstandar(expedienteFolderId);

            log.info("Expediente creado exitosamente en '{}': codigo {}, ID {}", req.carpetaBase(), expedienteCodigo,expedienteFolderId);
            return new Expediente(expedienteCodigo, req.titulo(), "VIGENTE", req.metadatos());

        } catch (Exception e) {
            log.error("Error creando expediente en carpeta '{}': {}",
                    req.carpetaBase(), e.getMessage(), e);
            throw new RuntimeException("Error al crear expediente: " + e.getMessage(), e);
        }
    }

    @Override
    public View view(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID del expediente no puede estar vacío");
            }

            log.debug("Obteniendo expediente: {}", id);

            // Obtener nodo desde Alfresco
            NodeEntry node;

            // Detectar si es nodeId (UUID) o número de expediente
            if (id.matches("[a-f0-9-]{36}")) {
                log.debug("Buscando por nodeId: {}", id);
                node = alfrescoClient.getNode(id);
            } else {
                log.debug("Buscando por número de expediente: {}", id);
                node = buscarExpedientePorNumero(id);
            }

             //       = alfrescoClient.getNode(id);
            Map<String, Object> props = node.entry().properties();

            // Extraer propiedades
            String titulo = getStringProperty(props, "cm:title", "Sin título");
            //String estado = getStringProperty(props, "dms:estado", "VIGENTE");
            //String numeroExpediente = getStringProperty(props, "dms:numeroExpediente", node.entry().name());
            String estado = getStringProperty(props, PROP_ESTADO, "VIGENTE");
            String numeroExpediente = getStringProperty(props, PROP_NUMERO_EXPEDIENTE, node.entry().name());

            // Crear expediente
            Map<String, String> metadatos = extraerMetadatos(props);

            // Agregar información del nodo actual
            metadatos.put("nodeId", node.entry().id());
            metadatos.put("tipoNodo", node.entry().nodeType());
            metadatos.put("carpetaPadre", obtenerRutaCarpetaPadre(node.entry().id()));

            // Información de fechas desde propiedades si están disponibles
            metadatos.put("fechaCreacion", getStringProperty(props, "cm:created", "No disponible"));
            metadatos.put("fechaModificacion", getStringProperty(props, "cm:modified", "No disponible"));
            metadatos.put("creadoPor", getStringProperty(props, "cm:creator", "Sistema"));
            metadatos.put("modificadoPor", getStringProperty(props, "cm:modifier", "Sistema"));

            // Información de contenido si está disponible
            if (node.entry().content() != null) {
                NodeEntry.Content content = node.entry().content();
                metadatos.put("tamaño", formatearTamaño(content.size()));
                metadatos.put("tipoMime", content.mimeType() != null ? content.mimeType() : "N/A");
            } else {
                metadatos.put("tamaño", "Carpeta");
                metadatos.put("tipoMime", "folder");
            }

            Expediente expediente = new Expediente(numeroExpediente, titulo, estado, metadatos);

            // Por ahora página vacía de documentos
            PageDocumento page = new PageDocumento(List.of(), 0, 20, 0);

            log.debug("Expediente obtenido: {}", numeroExpediente);
            return new View(expediente, page);

        } catch (Exception e) {
            log.error("Error obteniendo expediente {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error al obtener expediente: " + e.getMessage(), e);
        }
    }

    private String obtenerRutaCarpetaPadre(String parentId) {
        try {
            if (parentId == null) return "Raíz";

            NodeEntry parent = alfrescoClient.getNode(parentId);
            return (String) parent.entry().properties().getOrDefault("cm:title", parent.entry().name());
        } catch (Exception e) {
            log.warn("No se pudo obtener información del padre: {}", e.getMessage());
            return parentId;
        }
    }

    private String formatearTamaño(Long size) {
        if (size == null) return "0 bytes";
        if (size < 1024) return size + " bytes";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private NodeEntry buscarExpedientePorNumeroConSolr(String numeroExpediente) {
        try {
            log.info("=== INICIANDO BÚSQUEDA POR NÚMERO ===");
            log.info("Número de expediente buscado: '{}'", numeroExpediente);
            // Búsqueda por propiedad dms:numeroExpediente
            //String query = "TYPE:\"dms:folder\" AND @dms\\:numeroExpediente:\"" + numeroExpediente + "\"";
            String query = "TYPE:\"cm:folder\" AND cm:name:\"" + numeroExpediente + "\"";
            log.info("Query de búsqueda construida: '{}'", query);

            SearchResponse results = alfrescoClient.searchLucene(query, 1, 0);
            log.info("Búsqueda ejecutada. Verificando resultados...");

            /*if (results.list() == null || results.list().isEmpty()) {
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }

            // Obtener el nodeId del primer resultado
            String nodeId = results.list().get(0).entry().id();*/

            // Log detallado de la respuesta
            if (results == null) {
                log.error("SearchResponse es NULL");
                throw new AlfrescoException("Respuesta de búsqueda nula para: " + numeroExpediente);
            }

            log.info("SearchResponse recibido: {}", results);

            if (results.list() == null) {
                log.error("results.list() es NULL");
                throw new AlfrescoException("Lista de resultados nula para: " + numeroExpediente);
            }

            log.info("Lista de resultados existe. Verificando entries...");

            if (results.list().entries() == null) {
                log.error("results.list().entries() es NULL");
                throw new AlfrescoException("Entries nulo para: " + numeroExpediente);
            }

            log.info("Entries existe. Cantidad de resultados: {}", results.list().entries().size());

            if (results.list().entries().isEmpty()) {
                log.warn("No se encontraron resultados para el número: {}", numeroExpediente);
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }

            /*if (results.list() == null || results.list().entries() == null || results.list().entries().isEmpty()) {
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }*/

            // Obtener el nodeId del primer resultado
            String nodeId = results.list().entries().get(0).entry().id();
            log.info("NodeId encontrado: '{}'", nodeId);

            // Obtener el nodo completo con todas sus propiedades
            NodeEntry node = alfrescoClient.getNode(nodeId);
            log.info("=== BÚSQUEDA COMPLETADA EXITOSAMENTE ===");
            return node;

        } catch (Exception e) {
            //log.error("Error buscando expediente por número {}: {}", numeroExpediente, e.getMessage());
            log.error("=== ERROR EN BÚSQUEDA ===");
            log.error("Número buscado: {}", numeroExpediente);
            log.error("Tipo de error: {}", e.getClass().getSimpleName());
            log.error("Mensaje de error: {}", e.getMessage());
            log.error("=== FIN ERROR ===", e);
            throw new RuntimeException("No se pudo encontrar expediente: " + numeroExpediente, e);
        }
    }

    private NodeEntry buscarExpedientePorNumero(String numeroExpediente) {
        try {
            log.info("=== INICIANDO BÚSQUEDA POR NÚMERO (NODE API) ===");
            log.info("Número de expediente buscado: '{}'", numeroExpediente);

            // Usar Node Children API en lugar de Search API
            String whereClause = String.format("(name='%s')", numeroExpediente.replace("'", "\\'"));
            log.info("Where clause construida: '{}'", whereClause);

            NodeChildrenList children = alfrescoClient.getNodeChildren(REPOSITORIO_ID, whereClause);

            if (children == null || children.getList() == null ||
                    children.getList().getEntries() == null || children.getList().getEntries().isEmpty()) {
                log.warn("No se encontraron resultados para: {}", numeroExpediente);
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }

            // Obtener el primer resultado
            String nodeId = children.getList().getEntries().get(0).getEntry().id();
            log.info("NodeId encontrado: '{}'", nodeId);

            // Obtener el nodo completo
            NodeEntry node = alfrescoClient.getNode(nodeId);
            log.info("=== BÚSQUEDA COMPLETADA EXITOSAMENTE ===");
            return node;

        } catch (Exception e) {
            log.error("=== ERROR EN BÚSQUEDA POR NODE API ===");
            log.error("Número buscado: {}", numeroExpediente);
            log.error("Error: {}", e.getMessage());
            log.error("=== FIN ERROR ===");

            // Fallback opcional a Solr si quieres mantenerlo
            log.warn("Intentando fallback con Solr...");
            return buscarExpedientePorNumeroConSolr(numeroExpediente);
        }
    }

    @Override
    public Mono<String> ensureExpedienteFolder(String expedienteCodigo) {
        String code = sanitizeSegment(expedienteCodigo);
        String path = rootPath.endsWith("/") ? (rootPath + code) : (rootPath + "/" + code);

        // Como el cliente es bloqueante, lo movemos a un scheduler apto para tareas bloqueantes
        log.error("No se como llegué aquí.....ensureExpedienteFolder");
        return Mono.fromCallable(() -> alfrescoClient.ensurePath(path))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String sanitizeSegment(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("expedienteCodigo vacío");
        }
        String trimmed = s.trim();
        if (trimmed.contains("/") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("expedienteCodigo no debe contener / ni \\");
        }
        return trimmed;
    }

    // Métodos auxiliares - agregar al final de la clase
    private String generarCodigoExpediente() {
        String año = String.valueOf(java.time.LocalDateTime.now().getYear());
        long secuencial = System.currentTimeMillis() % 1000000;
        return String.format("EXP-%s-%06d", año, secuencial);
    }

    private void crearSubcarpetasEstandar(String expedienteFolderId) {
        String[] subcarpetas = {"Documentos", "Anexos", "Comunicaciones", "Resoluciones"};

        for (String subcarpeta : subcarpetas) {
            try {
                Map<String, Object> props = Map.of(
                        "cm:title", subcarpeta,
                        "cm:description", "Carpeta para " + subcarpeta.toLowerCase()
                );
                alfrescoClient.createFolder(expedienteFolderId, subcarpeta, props);
                log.debug("Subcarpeta creada: {}", subcarpeta);
            } catch (Exception e) {
                log.warn("No se pudo crear subcarpeta {}: {}", subcarpeta, e.getMessage());
                // No fallar por esto
            }
        }
    }

    // Métodos auxiliares adicionales
    private String getStringProperty(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Map<String, String> extraerMetadatos(Map<String, Object> properties) {
        Map<String, String> metadatos = new HashMap<>();
        properties.forEach((key, value) -> {
            /*if (key.startsWith("dms:") && value != null) {
                String metaKey = key.substring(4);
                metadatos.put(metaKey, value.toString());
            }*/
            if (key.startsWith(DMS_NAMESPACE) && value != null) {
                String metaKey = key.substring(DMS_NAMESPACE.length()); // Remover namespace completo
                metadatos.put(metaKey, value.toString());
            }
        });
        return metadatos;
    }

    public List<CarpetaBase> getCarpetasBase() {
        try {
            log.info("=== DEBUG: Verificando estructura antes de getCarpetasBase ===");
            alfrescoClient.debugRootContent();  // Llamada de debug

            log.info("Obteniendo carpetas base disponibles desde Alfresco");
            List<String> carpetasDisponibles = alfrescoClient.getChildrenNames("-root-");

            // Filtrar solo las carpetas relevantes para expedientes
            // Excluir carpetas del sistema como "Data Dictionary", "Guest Home", etc.
            Set<String> carpetasExcluidas = Set.of(
                    "Data Dictionary", "Guest Home", "Imap Attachments",
                    "IMAP Home", "User Homes", "Sites", "Company Home", "Shared"
            );

            List<CarpetaBase> carpetasBase = carpetasDisponibles.stream()
                    .filter(nombre -> !carpetasExcluidas.contains(nombre))
                    .map(this::crearCarpetaBase)
                    .sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()))
                    .toList();

            log.info("Carpetas base disponibles: {}",
                    carpetasBase.stream().map(CarpetaBase::nombre).toList());

            return carpetasBase;

        } catch (Exception e) {
            log.error("Error obteniendo carpetas base: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener carpetas base: " + e.getMessage(), e);
        }
    }

    private CarpetaBase crearCarpetaBase(String nombre) {
        // Por ahora descripción simple, puedes mejorar esto más tarde
        String descripcion = switch (nombre.toLowerCase()) {
            case "expedientes" -> "Expedientes administrativos y legales";
            case "repositorio" -> "Repositorio de documentos generales";
            case "plantillas" -> "Plantillas y formatos estándar";
            case "archivo" -> "Archivo histórico de documentos";
            default -> "Área de trabajo para " + nombre;
        };

        // Por ahora total 0, puedes implementar conteo real más tarde
        return new CarpetaBase(nombre, descripcion, 0);
    }

}
