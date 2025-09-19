
package pe.gob.onp.arquitectura.dms.service.impl;

import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;
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
import java.util.*;

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

    private static final String PROP_AREA = DMS_NAMESPACE + "area";
    private static final String PROP_FECHA_INICIO = DMS_NAMESPACE + "fechaInicio";
    private static final String PROP_ASUNTO = DMS_NAMESPACE + "asunto";
    private static final String PROP_CATEGORIA = DMS_NAMESPACE + "categoria";
    private static final String PROP_SOLICITANTE = DMS_NAMESPACE + "solicitante";
    private static final String PROP_OBSERVACIONES = DMS_NAMESPACE + "observaciones";
    private static final String PROP_CONFIDENCIALIDAD = DMS_NAMESPACE + "confidencialidad";
    private static final String PROP_PRIORIDAD = DMS_NAMESPACE + "prioridad";

    // Propiedades de documento
    private static final String PROP_FECHA_DOCUMENTO = DMS_NAMESPACE + "fechaDocumento";
    private static final String PROP_TIPO_DOCUMENTO = DMS_NAMESPACE + "tipoDocumento";
    private static final String PROP_NUMERO_DOCUMENTO = DMS_NAMESPACE + "numeroDocumento";
    private static final String PROP_DESTINATARIO = DMS_NAMESPACE + "destinatario";
    private static final String PROP_EMISOR = DMS_NAMESPACE + "emisor";
    private static final String PROP_SUBCARPETA = DMS_NAMESPACE + "subcarpeta";
    private static final String PROP_EXPEDIENTE_ID = DMS_NAMESPACE + "expedienteId";
    private static final String PROP_FECHA_SUBIDA = DMS_NAMESPACE + "fechaSubida";
    private static final String PROP_USUARIO_SUBIDA = DMS_NAMESPACE + "usuarioSubida";

    private static final Set<String> SUBCARPETAS_VALIDAS = Set.of(
            "Documentos", "Anexos", "Comunicaciones", "Resoluciones"
    );

    // Configuración actualizada con tipos permitidos del modelo
    private static final Map<String, ConfiguracionSubcarpeta> CONFIGURACION_SUBCARPETAS = Map.of(
            "Documentos", new ConfiguracionSubcarpeta(
                    "Documentos",
                    "Documentos principales del expediente",
                    new String[]{"SOLICITUD", "RESOLUCION", "OFICIO", "INFORME", "MEMORANDUM"},
                    new String[]{".pdf", ".doc", ".docx", ".txt"},
                    50L,
                    false
            ),
            "Anexos", new ConfiguracionSubcarpeta(
                    "Anexos",
                    "Documentos de apoyo y anexos",
                    new String[]{"ANEXO", "ADJUNTO", "SOPORTE", "EVIDENCIA"},
                    new String[]{".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".png"},
                    100L,
                    false
            ),
            "Comunicaciones", new ConfiguracionSubcarpeta(
                    "Comunicaciones",
                    "Correspondencia y comunicaciones oficiales",
                    new String[]{"CARTA", "EMAIL", "FAX", "NOTIFICACION", "OFICIO"},
                    new String[]{".pdf", ".doc", ".docx", ".msg", ".eml"},
                    25L,
                    false
            ),
            "Resoluciones", new ConfiguracionSubcarpeta(
                    "Resoluciones",
                    "Resoluciones y decisiones oficiales",
                    new String[]{"RESOLUCION", "DECRETO", "SENTENCIA", "DICTAMEN"},
                    new String[]{".pdf", ".doc", ".docx"},
                    50L,
                    true
            )
    );



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

            if (req.metadatos() == null || !req.metadatos().containsKey("area") ||
                    req.metadatos().get("area") == null || req.metadatos().get("area").trim().isEmpty()) {
                throw new IllegalArgumentException("metadatos.area es requerido para crear expediente");
            }

            if (req.metadatos() == null || !req.metadatos().containsKey("solicitante") ||
                    req.metadatos().get("solicitante") == null || req.metadatos().get("solicitante").trim().isEmpty()) {
                throw new IllegalArgumentException("metadatos.solicitante es requerido para crear expediente");
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

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
    }

    private String formatearTamaño(Long size) {
        if (size == null) return "0 bytes";
        if (size < 1024) return size + " bytes";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private UploadResponseCompleto construirUploadResponse(NodeEntry documentoNode, UploadDocumentoRequest request) {
        NodeEntry.Node node = documentoNode.entry();

        return new UploadResponseCompleto(
                node.id(),
                "1.0",
                node.name(),
                request.subcarpeta(),
                request.tipoDocumento(),
                request.contenido().length,
                request.mimeType(),
                OffsetDateTime.now(),
                "system"
        );
    }

    private List<DocumentoInfo> getDocumentosDeSubcarpeta(String expedienteId, String subcarpeta) {
        String subcarpetaId = buscarSubcarpeta(expedienteId, subcarpeta);
        if (subcarpetaId == null) {
            return List.of();
        }

        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(subcarpetaId, null);
            if (children == null || children.getList() == null || children.getList().getEntries() == null) {
                return List.of();
            }

            return children.getList().getEntries().stream()
                    .filter(entry -> "dms:documento".equals(entry.getEntry().nodeType())) // FILTRAR POR dms:documento
                    .map(entry -> construirDocumentoInfo(entry.getEntry(), subcarpeta))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Error obteniendo documentos de subcarpeta {}: {}", subcarpeta, e.getMessage());
            return List.of();
        }
    }

    private DocumentoInfo construirDocumentoInfo(NodeEntry.Node node, String subcarpeta) {
        Map<String, Object> props = node.properties();

        return new DocumentoInfo(
                node.id(),
                node.name(),
                getStringProperty(props, PROP_TIPO_DOCUMENTO, "DOCUMENTO"),
                subcarpeta,
                node.content() != null ? node.content().size() : 0L,
                node.content() != null ? node.content().mimeType() : "application/octet-stream",
                getStringProperty(props, "cm:created", ""),
                getStringProperty(props, "cm:creator", "system")
        );
    }

    private SubcarpetaInfo construirSubcarpetaInfo(String subcarpetaId, String nombre) {
        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(subcarpetaId, null);

            int cantidadDocs = 0;
            long tamañoTotal = 0L;

            if (children != null && children.getList() != null && children.getList().getEntries() != null) {
                for (var entry : children.getList().getEntries()) {
                    if ("dms:documento".equals(entry.getEntry().nodeType())) { // CONTAR SOLO dms:documento
                        cantidadDocs++;
                        if (entry.getEntry().content() != null && entry.getEntry().content().size() != null) {
                            tamañoTotal += entry.getEntry().content().size();
                        }
                    }
                }
            }

            ConfiguracionSubcarpeta config = CONFIGURACION_SUBCARPETAS.get(nombre);
            String descripcion = config != null ? config.descripcion() : "Subcarpeta de " + nombre;

            return new SubcarpetaInfo(
                    nombre,
                    subcarpetaId,
                    descripcion,
                    cantidadDocs,
                    tamañoTotal,
                    OffsetDateTime.now()
            );

        } catch (Exception e) {
            log.warn("Error construyendo info de subcarpeta {}: {}", nombre, e.getMessage());
            return new SubcarpetaInfo(nombre, subcarpetaId, "Error obteniendo información", 0, 0L, OffsetDateTime.now());
        }
    }

    // NUEVO MÉTODO para crear expediente con tipo dms:expediente:
    private String crearExpedienteConTipo(String carpetaBase, String expedienteCodigo, CreateRequest req) {
        try {
            // Buscar carpeta base
            String carpetaBaseId = findChildIdByName("-root-", carpetaBase);
            if (carpetaBaseId == null) {
                throw new AlfrescoException("Carpeta base no encontrada: " + carpetaBase);
            }

            // Crear propiedades del expediente
            Map<String, Object> properties = construirPropiedadesExpediente(expedienteCodigo, req);

            // Crear nodo como dms:expediente
            NodeBodyCreate body = new NodeBodyCreate(expedienteCodigo, "dms:expediente", properties);

            NodeEntry expediente = alfrescoClient.exchange(
                    alfrescoClient.client.post().uri(alfrescoClient.API_V1 + "/nodes/{id}/children", carpetaBaseId)
                            .contentType(MediaType.APPLICATION_JSON).bodyValue(body),
                    NodeEntry.class
            ).block();

            return expediente.entry().id();

        } catch (Exception e) {
            log.error("Error creando expediente tipo dms:expediente: {}", e.getMessage(), e);
            throw new AlfrescoException("No se pudo crear expediente: " + e.getMessage(), e);
        }
    }

    // NUEVO MÉTODO para construir propiedades de expediente:
    private Map<String, Object> construirPropiedadesExpediente(String expedienteCodigo, CreateRequest req) {
        Map<String, Object> properties = new HashMap<>();

        // Propiedades básicas
        properties.put("cm:title", req.titulo());
        properties.put("cm:description", req.descripcion() != null ? req.descripcion() : "");

        // Propiedades obligatorias de dms:expediente
        properties.put(PROP_NUMERO_EXPEDIENTE, expedienteCodigo);
        properties.put(PROP_ESTADO, "INICIADO");
        properties.put(PROP_FECHA_INICIO, LocalDate.now().toString());
        properties.put(PROP_ASUNTO, req.titulo());
        properties.put(PROP_AREA, req.metadatos().get("area"));
        properties.put(PROP_SOLICITANTE, req.metadatos().get("solicitante"));
        properties.put(PROP_CARPETA_BASE, req.carpetaBase());

        // Propiedades opcionales
        if (req.metadatos() != null) {
            req.metadatos().forEach((key, value) -> {
                switch (key) {
                    case "categoria" -> properties.put(PROP_CATEGORIA, value);
                    case "observaciones" -> properties.put(PROP_OBSERVACIONES, value);
                    case "confidencialidad" -> properties.put(PROP_CONFIDENCIALIDAD, value);
                    case "prioridad" -> properties.put(PROP_PRIORIDAD, value);
                    // area y solicitante ya procesados arriba
                }
            });
        }

        return properties;
    }

    // MÉTODO uploadDocumento CORREGIDO:
    @Override
    public UploadResponseCompleto uploadDocumento(String expedienteId, UploadDocumentoRequest request) {
        try {
            log.info("=== UPLOAD DOCUMENTO ===");
            log.info("Expediente: {}, Archivo: {}, Subcarpeta: {}",
                    expedienteId, request.nombreArchivo(), request.subcarpeta());

            validarRequestUpload(request);
            NodeEntry expedienteNode = buscarExpediente(expedienteId);
            log.info("Expediente encontrado: {}", expedienteNode.entry().name());
            String subcarpetaId = buscarOCrearSubcarpeta(expedienteNode.entry().id(), request.subcarpeta());
            log.info("Subcarpeta obtenida: {}", subcarpetaId);

            Map<String, Object> propiedades = construirPropiedadesDocumento(request, expedienteId);

            // Crear documento como dms:documento
            /*NodeEntry documentoNode = crearDocumentoConTipo(
                    subcarpetaId,
                    request.nombreArchivo(),
                    request.contenido(),
                    request.mimeType(),
                    construirPropiedadesDocumento(request, expedienteId)
            );*/

            NodeEntry documentoNode = alfrescoClient.uploadFile(
                    subcarpetaId,
                    request.nombreArchivo(),
                    request.contenido(),
                    request.mimeType(),
                    propiedades
            );

            log.info("Documento subido exitosamente: {}", documentoNode.entry().id());
            return construirUploadResponse(documentoNode, request);

        } catch (Exception e) {
            log.error("Error subiendo documento: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir documento: " + e.getMessage(), e);
        }
    }

    // NUEVO MÉTODO para crear documento con tipo dms:documento:
    private NodeEntry crearDocumentoConTipo(String parentId, String filename, byte[] content,
                                            String mimeType, Map<String, Object> props) {
        try {
            LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("name", filename);
            form.add("nodeType", "dms:documento"); // TIPO ESPECÍFICO

            if (props != null && !props.isEmpty()) {
                HttpHeaders jsonHeaders = new HttpHeaders();
                jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
                form.add("properties", new HttpEntity<>(toJson(props), jsonHeaders));
            }

            HttpHeaders fileHeaders = new HttpHeaders();
            if (mimeType != null && !mimeType.isBlank()) {
                fileHeaders.setContentType(MediaType.parseMediaType(mimeType));
                form.add("mimeType", mimeType);
            }

            ByteArrayResource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            form.add("filedata", new HttpEntity<>(resource, fileHeaders));

            return alfrescoClient.exchange(
                    alfrescoClient.client.post()
                            .uri(alfrescoClient.API_V1 + "/nodes/{id}/children?autoRename=true", parentId)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(form)),
                    NodeEntry.class
            ).block();

        } catch (Exception e) {
            log.error("Error creando documento dms:documento: {}", e.getMessage(), e);
            throw new AlfrescoException("No se pudo crear documento: " + e.getMessage(), e);
        }
    }

    // MÉTODO construirPropiedadesDocumento CORREGIDO:
    private Map<String, Object> construirPropiedadesDocumento(UploadDocumentoRequest request, String expedienteId) {
        Map<String, Object> props = new HashMap<>();

        // Propiedades básicas
        props.put("cm:title", request.nombreArchivo());
        props.put("cm:description", request.descripcion() != null ? request.descripcion() : "");

        // Propiedades obligatorias de dms:documento
        props.put(PROP_FECHA_DOCUMENTO, LocalDate.now().toString());
        props.put(PROP_TIPO_DOCUMENTO, request.tipoDocumento());

        // Propiedades específicas para expedientes
        props.put(PROP_SUBCARPETA, request.subcarpeta());
        props.put(PROP_EXPEDIENTE_ID, expedienteId);
        props.put(PROP_FECHA_SUBIDA, Instant.now().toString());
        props.put(PROP_USUARIO_SUBIDA, "system"); // TODO: obtener usuario actual

        // Propiedades opcionales según el modelo
        if (request.metadatos() != null) {
            request.metadatos().forEach((key, value) -> {
                switch (key) {
                    case "numeroDocumento" -> props.put(PROP_NUMERO_DOCUMENTO, value);
                    case "destinatario" -> props.put(PROP_DESTINATARIO, value);
                    case "emisor" -> props.put(PROP_EMISOR, value);
                }
            });
        }

        return props;
    }

    // MÉTODOS AUXILIARES NECESARIOS:
    private static String toJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception ex) {
            throw new AlfrescoException("Error building JSON properties", ex);
        }
    }

    private String findChildIdByName(String parentId, String name) {
        try {
            NodeChildrenList res = alfrescoClient.exchange(
                    alfrescoClient.client.get().uri(alfrescoClient.API_V1 + "/nodes/{pid}/children", parentId),
                    NodeChildrenList.class
            ).block();

            if (res == null || res.getList() == null || res.getList().getEntries() == null) {
                return null;
            }

            return res.getList().getEntries().stream()
                    .filter(entry -> name.equals(entry.getEntry().name()))
                    .map(entry -> entry.getEntry().id())
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.warn("Error buscando hijo '{}' en parent '{}': {}", name, parentId, e.getMessage());
            return null;
        }
    }

    @Override
    public PageDocumento<DocumentoInfo> getDocumentos(String expedienteId, String subcarpeta, int page, int size) {
        try {
            log.info("Obteniendo documentos - Expediente: {}, Subcarpeta: {}, Page: {}, Size: {}",
                    expedienteId, subcarpeta, page, size);

            NodeEntry expedienteNode = buscarExpediente(expedienteId);
            List<DocumentoInfo> documentos = new ArrayList<>();

            if (subcarpeta != null && !subcarpeta.trim().isEmpty()) {
                documentos.addAll(getDocumentosDeSubcarpeta(expedienteNode.entry().id(), subcarpeta));
            } else {
                for (String sub : SUBCARPETAS_VALIDAS) {
                    try {
                        documentos.addAll(getDocumentosDeSubcarpeta(expedienteNode.entry().id(), sub));
                    } catch (Exception e) {
                        log.warn("Error obteniendo documentos de subcarpeta {}: {}", sub, e.getMessage());
                    }
                }
            }

            // Paginación manual
            int start = page * size;
            int end = Math.min(start + size, documentos.size());
            List<DocumentoInfo> paginatedDocs = start < documentos.size()
                    ? documentos.subList(start, end)
                    : List.of();

            log.info("Documentos encontrados: {} total, {} en página actual",
                    documentos.size(), paginatedDocs.size());

            return new PageDocumento<>(paginatedDocs, page, size, documentos.size());

        } catch (Exception e) {
            log.error("Error obteniendo documentos de expediente {}: {}", expedienteId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener documentos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SubcarpetaInfo> getSubcarpetas(String expedienteId) {
        try {
            log.info("Obteniendo subcarpetas del expediente: {}", expedienteId);

            NodeEntry expedienteNode = buscarExpediente(expedienteId);
            List<SubcarpetaInfo> subcarpetas = new ArrayList<>();

            for (String nombreSubcarpeta : SUBCARPETAS_VALIDAS) {
                try {
                    String subcarpetaId = buscarSubcarpeta(expedienteNode.entry().id(), nombreSubcarpeta);
                    if (subcarpetaId != null) {
                        SubcarpetaInfo info = construirSubcarpetaInfo(subcarpetaId, nombreSubcarpeta);
                        subcarpetas.add(info);
                    }
                } catch (Exception e) {
                    log.warn("Error procesando subcarpeta {}: {}", nombreSubcarpeta, e.getMessage());
                }
            }

            log.info("Subcarpetas obtenidas: {}", subcarpetas.size());
            return subcarpetas;

        } catch (Exception e) {
            log.error("Error obteniendo subcarpetas de expediente {}: {}", expedienteId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener subcarpetas: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isSubcarpetaValida(String subcarpeta) {
        return subcarpeta != null && SUBCARPETAS_VALIDAS.contains(subcarpeta.trim());
    }

    private void validarRequestUpload(UploadDocumentoRequest request) {
        if (!isSubcarpetaValida(request.subcarpeta())) {
            throw new IllegalArgumentException("Subcarpeta no válida: " + request.subcarpeta() +
                    ". Válidas: " + SUBCARPETAS_VALIDAS);
        }

        ConfiguracionSubcarpeta config = CONFIGURACION_SUBCARPETAS.get(request.subcarpeta());
        if (config != null) {
            long tamañoMB = request.contenido().length / (1024 * 1024);
            if (tamañoMB > config.tamañoMaximoMB()) {
                throw new IllegalArgumentException("Archivo demasiado grande: " + tamañoMB +
                        "MB. Máximo permitido: " + config.tamañoMaximoMB() + "MB");
            }

            String extension = obtenerExtension(request.nombreArchivo());
            if (!Arrays.asList(config.extensionesPermitidas()).contains(extension.toLowerCase())) {
                throw new IllegalArgumentException("Extensión no permitida: " + extension +
                        ". Permitidas: " + Arrays.toString(config.extensionesPermitidas()));
            }
        }
    }

    private NodeEntry buscarExpediente(String expedienteId) {
        try {
            if (expedienteId.matches("[a-f0-9-]{36}")) {
                return alfrescoClient.getNode(expedienteId);
            } else {
                return buscarExpedientePorNumero(expedienteId);
            }
        } catch (Exception e) {
            throw new AlfrescoException("Expediente no encontrado: " + expedienteId);
        }
    }

    private String buscarOCrearSubcarpeta(String expedienteId, String nombreSubcarpeta) {
        String subcarpetaId = buscarSubcarpeta(expedienteId, nombreSubcarpeta);

        if (subcarpetaId != null) {
            log.debug("Subcarpeta {} ya existe: {}", nombreSubcarpeta, subcarpetaId);
            return subcarpetaId;
        }

        log.info("Creando subcarpeta {}", nombreSubcarpeta);
        Map<String, Object> props = Map.of(
                "cm:title", nombreSubcarpeta,
                "cm:description", "Subcarpeta para " + nombreSubcarpeta.toLowerCase(),
                PROP_CARPETA_BASE, nombreSubcarpeta
        );

        NodeEntry subcarpeta = alfrescoClient.createFolder(expedienteId, nombreSubcarpeta, props);
        return subcarpeta.entry().id();
    }

    private String buscarSubcarpeta(String expedienteId, String nombreSubcarpeta) {
        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(
                    expedienteId,
                    String.format("(name='%s')", nombreSubcarpeta.replace("'", "\\'"))
            );

            if (children != null && children.getList() != null &&
                    children.getList().getEntries() != null && !children.getList().getEntries().isEmpty()) {
                return children.getList().getEntries().get(0).getEntry().id();
            }

            return null;
        } catch (Exception e) {
            log.warn("Error buscando subcarpeta {}: {}", nombreSubcarpeta, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> construirPropiedadesDocumento(UploadDocumentoRequest request, String expedienteId) {
        Map<String, Object> props = new HashMap<>();

        props.put("cm:title", request.nombreArchivo());
        props.put("cm:description", request.descripcion() != null ? request.descripcion() : "");

        props.put(DMS_NAMESPACE + "tipoDocumento", request.tipoDocumento());
        props.put(DMS_NAMESPACE + "subcarpeta", request.subcarpeta());
        props.put(DMS_NAMESPACE + "expedienteId", expedienteId);
        props.put(DMS_NAMESPACE + "fechaSubida", java.time.Instant.now().toString());

        if (request.metadatos() != null) {
            request.metadatos().forEach((key, value) -> {
                props.put(DMS_NAMESPACE + key, value);
            });
        }

        return props;
    }

    private UploadResponseCompleto construirUploadResponse(NodeEntry documentoNode, UploadDocumentoRequest request) {
        NodeEntry.Node node = documentoNode.entry();

        return new UploadResponseCompleto(
                node.id(),
                "1.0",
                node.name(),
                request.subcarpeta(),
                request.tipoDocumento(),
                request.contenido().length,
                request.mimeType(),
                OffsetDateTime.now(),
                "system"
        );
    }

    private List<DocumentoInfo> getDocumentosDeSubcarpeta(String expedienteId, String subcarpeta) {
        String subcarpetaId = buscarSubcarpeta(expedienteId, subcarpeta);
        if (subcarpetaId == null) {
            return List.of();
        }

        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(subcarpetaId, null);
            if (children == null || children.getList() == null || children.getList().getEntries() == null) {
                return List.of();
            }

            return children.getList().getEntries().stream()
                    .filter(entry -> "cm:content".equals(entry.getEntry().nodeType()))
                    .map(entry -> construirDocumentoInfo(entry.getEntry(), subcarpeta))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Error obteniendo documentos de subcarpeta {}: {}", subcarpeta, e.getMessage());
            return List.of();
        }
    }

    private SubcarpetaInfo construirSubcarpetaInfo(String subcarpetaId, String nombre) {
        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(subcarpetaId, null);

            int cantidadDocs = 0;
            long tamañoTotal = 0L;

            if (children != null && children.getList() != null && children.getList().getEntries() != null) {
                for (var entry : children.getList().getEntries()) {
                    if ("cm:content".equals(entry.getEntry().nodeType())) {
                        cantidadDocs++;
                        if (entry.getEntry().content() != null && entry.getEntry().content().size() != null) {
                            tamañoTotal += entry.getEntry().content().size();
                        }
                    }
                }
            }

            ConfiguracionSubcarpeta config = CONFIGURACION_SUBCARPETAS.get(nombre);
            String descripcion = config != null ? config.descripcion() : "Subcarpeta de " + nombre;

            return new SubcarpetaInfo(
                    nombre,
                    subcarpetaId,
                    descripcion,
                    cantidadDocs,
                    tamañoTotal,
                    OffsetDateTime.now()
            );

        } catch (Exception e) {
            log.warn("Error construyendo info de subcarpeta {}: {}", nombre, e.getMessage());
            return new SubcarpetaInfo(nombre, subcarpetaId, "Error obteniendo información", 0, 0L, OffsetDateTime.now());
        }
    }

}
