
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import pe.gob.onp.arquitectura.dms.api.dto.DocumentoDtos.*;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.ArrayList;

@Service
public class ExpedientesServiceImpl implements ExpedientesService {

    // Agregar esta constante al inicio de la clase
    private static final String REPOSITORIO_ID = "02dbd884-06fe-41a2-9bd8-8406fef1a234";

    private final AlfrescoClient alfrescoClient;
    private final String rootPath;
    private static final Logger log = LoggerFactory.getLogger(ExpedientesServiceImpl.class);

    //private static final String DMS_NAMESPACE = "{http://www.onp.gob.pe/model/dms/1.0}";
    private static final String DMS_NAMESPACE = "dms:";


    private static final String PROP_NUMERO_EXPEDIENTE = DMS_NAMESPACE + "numeroExpediente";
    private static final String PROP_ESTADO = DMS_NAMESPACE + "estado";
    private static final String PROP_FECHA_CREACION = DMS_NAMESPACE + "fechaCreacion";
    private static final String PROP_CARPETA_BASE = DMS_NAMESPACE + "carpetaBase";


    // Agregar estas constantes después de las existentes:
    private static final String PROP_TIPO_DOCUMENTO = DMS_NAMESPACE + "tipoDocumento";
    private static final String PROP_SUBCARPETA = DMS_NAMESPACE + "subcarpeta";
    private static final String PROP_EXPEDIENTE_ID = DMS_NAMESPACE + "expedienteId";
    private static final String PROP_FECHA_SUBIDA = DMS_NAMESPACE + "fechaSubida";
    private static final String PROP_USUARIO_SUBIDA = DMS_NAMESPACE + "usuarioSubida";

    private static final Set<String> SUBCARPETAS_VALIDAS = Set.of(
            "Documentos", "Anexos", "Comunicaciones", "Resoluciones"
    );

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
            //String expedienteFolderId = alfrescoClient.ensurePath(expedientePath);
            // DESPUÉS - separar creación de carpeta padre y expediente final:
            String parentPath = req.carpetaBase(); // Solo la carpeta padre: "Expedientes"
            String parentFolderId = alfrescoClient.ensurePath(parentPath); // Crear carpeta padre como cm:folder

            // Crear el expediente como dms:expediente
            Map<String, Object> expedienteProps = construirPropiedadesExpediente(req, expedienteCodigo);
            NodeEntry expedienteNode = alfrescoClient.createExpediente(parentFolderId, expedienteCodigo, expedienteProps);
            String expedienteFolderId = expedienteNode.entry().id();

            log.debug("Carpeta creada con ID: {}", expedienteFolderId);

            // Configurar propiedades del expediente
            /*Map<String, Object> properties = new HashMap<>();
            properties.put("cm:title", req.titulo());
            properties.put("cm:description", req.descripcion() != null ? req.descripcion() : "");
            properties.put(PROP_NUMERO_EXPEDIENTE, expedienteCodigo);
            properties.put(PROP_ESTADO, "VIGENTE");*/

            // Agregar metadatos del request
            /*if (req.metadatos() != null) {
                req.metadatos().forEach((key, value) -> {
                    properties.put("dms:" + key, value);
                });
            }*/
            // Agregar metadatos del request
            /*if (req.metadatos() != null) {
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
            alfrescoClient.updateNode(expedienteFolderId, properties);*/
            log.info("Propiedades actualizadas para expediente: {}", expedienteCodigo);

            // Crear subcarpetas estándar
            crearSubcarpetasEstandar(expedienteFolderId);

            log.info("Expediente creado exitosamente en '{}': codigo {}, ID {}", req.carpetaBase(), expedienteCodigo,expedienteFolderId);
            //return new Expediente(expedienteFolderId, expedienteCodigo, req.titulo(), "VIGENTE", req.metadatos());
            return new Expediente(expedienteFolderId, expedienteCodigo, req.titulo(), "INICIADO", req.metadatos());

        } catch (Exception e) {
            log.error("Error creando expediente en carpeta '{}': {}",
                    req.carpetaBase(), e.getMessage(), e);
            throw new RuntimeException("Error al crear expediente: " + e.getMessage(), e);
        }
    }

    // AGREGAR este método en ExpedientesServiceImpl.java

    private Map<String, Object> construirPropiedadesExpediente(CreateRequest req, String expedienteCodigo) {
        log.debug("Construyendo propiedades DMS para expediente: {}", expedienteCodigo);

        Map<String, Object> properties = new HashMap<>();

        // PROPIEDADES ESTÁNDAR DE ALFRESCO
        properties.put("cm:title", req.titulo());
        properties.put("cm:description", req.descripcion() != null ? req.descripcion() : "");

        // USAR LAS CONSTANTES EXISTENTES PARA CONSISTENCIA
        properties.put(PROP_NUMERO_EXPEDIENTE, expedienteCodigo);
        properties.put(PROP_ESTADO, "INICIADO"); // Default del modelo (cambié de VIGENTE a INICIADO)
        properties.put(PROP_CARPETA_BASE, req.carpetaBase());

        // PROPIEDADES ADICIONALES OBLIGATORIAS SEGÚN MODELO
        properties.put("dms:area", req.area() != null ? req.area() : "GENERAL");
        properties.put("dms:fechaInicio", java.time.LocalDate.now().toString());
        properties.put("dms:asunto", req.asunto() != null ? req.asunto() : req.titulo());
        properties.put("dms:solicitante", req.solicitante() != null ? req.solicitante() : "SISTEMA");

        // PROPIEDADES OPCIONALES CON DEFAULTS
        properties.put("dms:prioridad", req.prioridad() != null ? req.prioridad() : "NORMAL");
        properties.put("dms:confidencialidad", req.confidencialidad() != null ? req.confidencialidad() : "PUBLICO");

        // Solo agregar si no son null
        if (req.categoria() != null && !req.categoria().trim().isEmpty()) {
            properties.put("dms:categoria", req.categoria());
        }
        if (req.observaciones() != null && !req.observaciones().trim().isEmpty()) {
            properties.put("dms:observaciones", req.observaciones());
        }

        // METADATOS ADICIONALES del request
        if (req.metadatos() != null) {
            log.debug("Agregando {} metadatos adicionales", req.metadatos().size());
            req.metadatos().forEach((key, value) -> {
                if (value != null && !value.trim().isEmpty()) {
                    properties.put("dms:" + key, value);
                }
            });
        }

        log.debug("Propiedades DMS construidas: {} propiedades", properties.size());
        return properties;
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
            //if (id.matches("[a-f0-9-]{36}")) {
            if (id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")) {
                log.debug("Buscando por nodeId UUID: {}", id);
                node = alfrescoClient.getNode(id);
            } else if (id.startsWith("EXP-")) {
                // Es número de expediente
                log.debug("Buscando por número de expediente: {}", id);
                node = buscarExpedientePorNumero(id);
            } else {
                throw new IllegalArgumentException("Formato de ID no válido: " + id);
            }

             //       = alfrescoClient.getNode(id);
            Map<String, Object> props = node.entry().properties();

            // Extraer propiedades
            String titulo = getStringProperty(props, "cm:title", "Sin título");
            //String estado = getStringProperty(props, "dms:estado", "VIGENTE");
            //String numeroExpediente = getStringProperty(props, "dms:numeroExpediente", node.entry().name());
            //String estado = getStringProperty(props, PROP_ESTADO, "VIGENTE");
            //String estado = getStringProperty(props, PROP_ESTADO, "INICIADO");

            // Agregar antes de la validación del estado:
            log.debug("=== DEBUG PROPIEDADES EXPEDIENTE {} ===", id);
            log.debug("Total propiedades encontradas: {}", props.size());
            log.debug("Buscando propiedad: {}", PROP_ESTADO);
            log.debug("Propiedad encontrada: {}", props.containsKey(PROP_ESTADO) ? "SÍ" : "NO");

            Object estadoObj = props.get(PROP_ESTADO);
            if (estadoObj == null) {
                log.error("ERROR CRÍTICO: Expediente {} no tiene propiedad dms:estado", id);
                log.error("Propiedades disponibles: {}", props.keySet());
                throw new RuntimeException("Expediente sin estado válido");
            }
            String estado = estadoObj.toString();

            String numeroExpediente = getStringProperty(props, PROP_NUMERO_EXPEDIENTE, node.entry().name());
            String numeroExpedienteId = getStringProperty(props, PROP_EXPEDIENTE_ID, node.entry().id());

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
                metadatos.put("tamaño", formatearTamano(content.size()));
                metadatos.put("tipoMime", content.mimeType() != null ? content.mimeType() : "N/A");
            } else {
                metadatos.put("tamaño", "Carpeta");
                metadatos.put("tipoMime", "folder");
            }

            Expediente expediente = new Expediente(numeroExpedienteId, numeroExpediente, titulo, estado, metadatos);

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

    private String formatearTamano(Long size) {
        if (size == null) return "0 bytes";
        if (size < 1024) return size + " bytes";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    /*private NodeEntry buscarExpedientePorNumeroConSolr(String numeroExpediente) {
        try {
            log.info("=== INICIANDO BÚSQUEDA POR NÚMERO ===");
            log.info("Número de expediente buscado: '{}'", numeroExpediente);
            // Búsqueda por propiedad dms:numeroExpediente
            //String query = "TYPE:\"dms:folder\" AND @dms\\:numeroExpediente:\"" + numeroExpediente + "\"";
            String query = "TYPE:\"cm:folder\" AND cm:name:\"" + numeroExpediente + "\"";
            log.info("Query de búsqueda construida: '{}'", query);

            SearchResponse results = alfrescoClient.searchLucene(query, 1, 0);
            log.info("Búsqueda ejecutada. Verificando resultados...");

            --------if (results.list() == null || results.list().isEmpty()) {
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }

            // Obtener el nodeId del primer resultado
            String nodeId = results.list().get(0).entry().id();------

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

            ------if (results.list() == null || results.list().entries() == null || results.list().entries().isEmpty()) {
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }-----

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
    }*/

    /*private NodeEntry buscarExpedientePorNumeroConSolr(String numeroExpediente) {
        try {
            log.info("=== BÚSQUEDA CON SOLR ===");
            log.info("Número de expediente buscado: '{}'", numeroExpediente);

            // Intentar primero con dms:numeroExpediente
            //String query = "TYPE:\"cm:folder\" AND @dms\\:numeroExpediente:\"" + numeroExpediente + "\"";
            String query = "TYPE:\"dms:expediente\" AND @dms\\:numeroExpediente:\"" + numeroExpediente + "\"";
            log.info("Query 1 (dms:numeroExpediente): '{}'", query);

            SearchResponse results = alfrescoClient.searchLucene(query, 1, 0);

            // Si no encuentra con dms:numeroExpediente, intentar con cm:name
            if (results == null || results.list() == null ||
                    results.list().entries() == null || results.list().entries().isEmpty()) {

                log.info("No encontrado con dms:numeroExpediente, intentando con cm:name...");
                //query = "TYPE:\"cm:folder\" AND cm:name:\"" + numeroExpediente + "\"";
                query = "TYPE:\"dms:expediente\" AND cm:name:\"" + numeroExpediente + "\"";
                log.info("Query 2 (cm:name): '{}'", query);

                results = alfrescoClient.searchLucene(query, 1, 0);
            }

            // Si tampoco encuentra con cm:name, intentar case-insensitive
            if (results == null || results.list() == null ||
                    results.list().entries() == null || results.list().entries().isEmpty()) {

                log.info("No encontrado con cm:name exacto, intentando case-insensitive...");
                query = "TYPE:\"cm:folder\" AND cm:name:\"" + numeroExpediente.toLowerCase() + "\"";
                log.info("Query 3 (lowercase): '{}'", query);

                results = alfrescoClient.searchLucene(query, 1, 0);
            }

            // Validar resultados finales
            if (results == null || results.list() == null ||
                    results.list().entries() == null || results.list().entries().isEmpty()) {

                log.warn("No se encontraron resultados con ninguna estrategia para: {}", numeroExpediente);
                throw new AlfrescoException("Expediente no encontrado: " + numeroExpediente);
            }

            String nodeId = results.list().entries().get(0).entry().id();
            log.info("NodeId encontrado con Solr: '{}'", nodeId);

            NodeEntry node = alfrescoClient.getNode(nodeId);
            log.info("=== BÚSQUEDA SOLR COMPLETADA EXITOSAMENTE ===");
            return node;

        } catch (Exception e) {
            log.error("=== ERROR EN BÚSQUEDA SOLR ===");
            log.error("Número buscado: {}", numeroExpediente);
            log.error("Tipo de error: {}", e.getClass().getSimpleName());
            log.error("Mensaje de error: {}", e.getMessage());
            log.error("=== FIN ERROR SOLR ===", e);
            throw new RuntimeException("No se pudo encontrar expediente: " + numeroExpediente, e);
        }
    }*/

    private NodeEntry buscarExpedientePorNumeroConSolr(String numeroExpediente) {
        try {
            log.info("=== BÚSQUEDA COMPATIBLE CON AMBOS TIPOS ===");
            log.info("Número de expediente buscado: '{}'", numeroExpediente);

            // ESTRATEGIA 1: Buscar en nuevos expedientes (dms:expediente)
            String query = "TYPE:\"dms:expediente\" AND @dms\\:numeroExpediente:\"" + numeroExpediente + "\"";
            log.info("Query 1 (nuevos expedientes): '{}'", query);

            SearchResponse results = alfrescoClient.searchLucene(query, 1, 0);
            if (results != null && results.list() != null &&
                    results.list().entries() != null && !results.list().entries().isEmpty()) {

                String nodeId = results.list().entries().get(0).entry().id();
                log.info("✓ Expediente NUEVO encontrado (dms:expediente): '{}'", nodeId);
                return alfrescoClient.getNode(nodeId);
            }

            // ESTRATEGIA 2: Buscar en nuevos expedientes por nombre
            log.info("Buscando nuevos expedientes por cm:name...");
            query = "TYPE:\"dms:expediente\" AND cm:name:\"" + numeroExpediente + "\"";
            log.info("Query 2 (nuevos por nombre): '{}'", query);

            results = alfrescoClient.searchLucene(query, 1, 0);
            if (results != null && results.list() != null &&
                    results.list().entries() != null && !results.list().entries().isEmpty()) {

                String nodeId = results.list().entries().get(0).entry().id();
                log.info("✓ Expediente NUEVO encontrado por nombre: '{}'", nodeId);
                return alfrescoClient.getNode(nodeId);
            }

            // ESTRATEGIA 3: COMPATIBILIDAD - Buscar expedientes antiguos (cm:folder)
            log.info("Buscando expedientes ANTIGUOS (compatibilidad cm:folder)...");
            query = "TYPE:\"cm:folder\" AND cm:name:\"" + numeroExpediente + "\"";
            log.info("Query 3 (compatibilidad cm:folder): '{}'", query);

            results = alfrescoClient.searchLucene(query, 1, 0);
            if (results != null && results.list() != null &&
                    results.list().entries() != null && !results.list().entries().isEmpty()) {

                String nodeId = results.list().entries().get(0).entry().id();
                log.info("✓ Expediente ANTIGUO encontrado (cm:folder): '{}'", nodeId);
                return alfrescoClient.getNode(nodeId);
            }

            // ESTRATEGIA 4: Case-insensitive para ambos tipos
            log.info("Última búsqueda case-insensitive...");
            query = "(TYPE:\"dms:expediente\" OR TYPE:\"cm:folder\") AND cm:name:\"" +
                    numeroExpediente.toLowerCase() + "\"";
            log.info("Query 4 (case-insensitive combinada): '{}'", query);

            results = alfrescoClient.searchLucene(query, 1, 0);
            if (results != null && results.list() != null &&
                    results.list().entries() != null && !results.list().entries().isEmpty()) {

                String nodeId = results.list().entries().get(0).entry().id();
                String tipo = results.list().entries().get(0).entry().nodeType();
                log.info("✓ Expediente encontrado case-insensitive: '{}' (tipo: {})", nodeId, tipo);
                return alfrescoClient.getNode(nodeId);
            }

            throw new AlfrescoException("Expediente no encontrado con ninguna estrategia: " + numeroExpediente);

        } catch (Exception e) {
            log.error("Error en búsqueda compatible: {}", e.getMessage());
            throw new RuntimeException("No se pudo encontrar expediente: " + numeroExpediente, e);
        }
    }

    private NodeEntry buscarExpedientePorNumero(String numeroExpediente) {
        try {
            log.info("=== BÚSQUEDA DE EXPEDIENTE POR NÚMERO ===");
            log.info("Número buscado: '{}'", numeroExpediente);

            // MÉTODO PRINCIPAL: Usar Solr (más eficiente)
            log.info("Intentando búsqueda con Solr...");
            return buscarExpedientePorNumeroConSolr(numeroExpediente);

        } catch (Exception e) {
            log.warn("Búsqueda con Solr falló: {}", e.getMessage());
            log.info("Iniciando búsqueda manual como fallback...");

            try {
                // FALLBACK: búsqueda manual solo si Solr falla
                return buscarExpedienteManualmente(numeroExpediente);

            } catch (Exception fallbackError) {
                log.error("También falló la búsqueda manual: {}", fallbackError.getMessage());
                throw new RuntimeException("No se pudo encontrar expediente con ningún método: " + numeroExpediente, fallbackError);
            }
        }
    }

    private NodeEntry buscarExpedienteManualmente(String numeroExpediente) {
        try {
            log.info("=== BÚSQUEDA MANUAL POR CARPETAS ===");
            log.info("Número buscado: '{}'", numeroExpediente);

            // Buscar en carpetas base conocidas
            List<String> carpetasBase = alfrescoClient.getChildrenNames("-root-");
            Set<String> carpetasRelevantes = Set.of("Repositorio", "Expedientes", "Archivo");

            for (String carpetaBase : carpetasBase) {
                if (!carpetasRelevantes.contains(carpetaBase)) continue;

                try {
                    String carpetaBaseId = findChildIdByName("-root-", carpetaBase);
                    if (carpetaBaseId == null) continue;

                    log.debug("Buscando en carpeta '{}' (ID: {})", carpetaBase, carpetaBaseId);

                    String expedienteId = findChildIdByName(carpetaBaseId, numeroExpediente);

                    if (expedienteId != null) {
                        log.info("✓ Expediente encontrado manualmente en '{}': {}", carpetaBase, expedienteId);
                        return alfrescoClient.getNode(expedienteId);
                    }

                } catch (Exception e) {
                    log.warn("Error buscando en {}: {}", carpetaBase, e.getMessage());
                }
            }

            throw new AlfrescoException("Expediente no encontrado en búsqueda manual: " + numeroExpediente);

        } catch (Exception e) {
            log.error("Error en búsqueda manual: {}", e.getMessage());
            throw new RuntimeException("No se pudo encontrar expediente manualmente: " + numeroExpediente, e);
        }
    }

    // Agregar este método auxiliar
    /*private String findChildIdByName(String parentId, String name) {
        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(parentId, null);

            if (children != null && children.getList() != null &&
                    children.getList().getEntries() != null) {

                return children.getList().getEntries().stream()
                        .filter(e -> name.equalsIgnoreCase(e.getEntry().name()))
                        .map(e -> e.getEntry().id())
                        .findFirst()
                        .orElse(null);
            }
            return null;

        } catch (Exception e) {
            log.warn("Error buscando hijo {}: {}", name, e.getMessage());
            return null;
        }
    }*/

    private String findChildIdByName(String parentId, String name) {
        log.debug(">>> FIND CHILD: '{}' en parent '{}'", name, parentId);

        if (name == null || name.isBlank()) {
            log.warn("Nombre vacío en findChildIdByName");
            return null;
        }

        try {
            // INTENTO 1: Método optimizado con WHERE (puede fallar en algunas versiones de Alfresco)
            log.debug("Intentando búsqueda optimizada con WHERE...");
            String whereClause = String.format("(name='%s')", name.replace("'", "\\'"));
            log.debug("Where clause construido: {}", whereClause);

            NodeChildrenList children = alfrescoClient.getNodeChildren(parentId, whereClause);

            if (children != null && children.getList() != null &&
                    children.getList().getEntries() != null && !children.getList().getEntries().isEmpty()) {

                String nodeId = children.getList().getEntries().get(0).getEntry().id();
                log.debug("<<< FIND CHILD RESULT (OPTIMIZADO): ENCONTRADO con ID '{}'", nodeId);
                return nodeId;
            }

            log.debug("WHERE no devolvió resultados, probando método manual...");
            throw new Exception("WHERE sin resultados");

        } catch (Exception e) {
            // FALLBACK: Método que ya funcionaba - traer todo y filtrar en memoria
            log.debug("WHERE falló ({}), usando método manual conocido...", e.getMessage());
            return findChildIdByNameManual(parentId, name);
        }
    }

    private String findChildIdByNameManual(String parentId, String name) {
        try {
            int maxItems = 100;  // Procesar de a 100 elementos
            int skipCount = 0;

            log.debug("Iniciando búsqueda paginada para '{}' en parent '{}'", name, parentId);

            while (true) {
                // Construir parámetros de paginación
                String paginationParams = String.format("skipCount=%d&maxItems=%d", skipCount, maxItems);
                log.debug("Página actual: skip={}, max={}", skipCount, maxItems);

                NodeChildrenList children = alfrescoClient.getNodeChildrenPaginated(parentId, null, skipCount, maxItems);

                if (children == null || children.getList() == null ||
                        children.getList().getEntries() == null || children.getList().getEntries().isEmpty()) {
                    log.debug("No hay más elementos para procesar. Búsqueda terminada sin resultados.");
                    break;
                }

                // Buscar en esta página
                String childId = children.getList().getEntries().stream()
                        .filter(e -> name.equalsIgnoreCase(e.getEntry().name()))
                        .map(e -> e.getEntry().id())
                        .findFirst()
                        .orElse(null);

                if (childId != null) {
                    log.debug("<<< FIND CHILD RESULT (PAGINADO): ENCONTRADO en página skip={} con ID '{}'", skipCount, childId);
                    return childId;
                }

                // Si esta página tiene menos elementos que maxItems, es la última página
                if (children.getList().getEntries().size() < maxItems) {
                    log.debug("Última página procesada (solo {} elementos). Elemento no encontrado.",
                            children.getList().getEntries().size());
                    break;
                }

                // Avanzar a la siguiente página
                skipCount += maxItems;

                // Protección: máximo 50 páginas (5000 elementos)
                if (skipCount > 5000) {
                    log.warn("Límite de búsqueda alcanzado (5000 elementos). Deteniendo búsqueda.");
                    break;
                }
            }

            log.debug("<<< FIND CHILD RESULT (PAGINADO): NO ENCONTRADO después de {} elementos procesados", skipCount);
            return null;

        } catch (Exception e) {
            log.warn("<<< FIND CHILD ERROR (PAGINADO): {} - RETORNANDO NULL", e.getMessage());
            return null;
        }
    }

    private String findChildIdByNameCaseInsensitive(String parentId, String name) {
        try {
            // FALLBACK: Si falla el filtrado optimizado, usar método actual
            NodeChildrenList children = alfrescoClient.getNodeChildren(parentId, null);

            if (children != null && children.getList() != null &&
                    children.getList().getEntries() != null) {

                return children.getList().getEntries().stream()
                        .filter(e -> name.equalsIgnoreCase(e.getEntry().name()))
                        .map(e -> e.getEntry().id())
                        .findFirst()
                        .orElse(null);
            }
            return null;

        } catch (Exception e) {
            log.warn("<<< FIND CHILD ERROR: {} - RETORNANDO NULL", e.getMessage());
            return null;
        }
    }

    /*private NodeEntry buscarExpedientePorNumero(String numeroExpediente) {
        try {
            log.info("=== INICIANDO BÚSQUEDA POR NÚMERO (NODE API) ===");
            log.info("Número de expediente buscado: '{}'", numeroExpediente);
            log.info("REPOSITORIO_ID: '{}'", REPOSITORIO_ID);

            // Usar Node Children API en lugar de Search API
            String whereClause = String.format("(name='%s')", numeroExpediente.replace("'", "\\'"));
            log.info("Where clause construida: '{}'", whereClause);

            NodeChildrenList children = alfrescoClient.getNodeChildren(REPOSITORIO_ID, whereClause);
            log.info("Resultado búsqueda - children: {}", children != null ? "no null" : "null");

            /*if (children != null && children.getList() != null) {
                log.info("Entradas encontradas: {}",
                        children.getList().getEntries() != null ? children.getList().getEntries().size() : 0);
            }

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
            return node;------

            if (children != null && children.getList() != null &&
                    children.getList().getEntries() != null && !children.getList().getEntries().isEmpty()) {

                String nodeId = children.getList().getEntries().get(0).getEntry().id();
                log.info("NodeId encontrado con propiedad custom: '{}'", nodeId);

                NodeEntry node = alfrescoClient.getNode(nodeId);
                log.info("=== BÚSQUEDA POR PROPIEDAD CUSTOM EXITOSA ===");
                return node;
            }

            log.warn("No encontrado con dms:numeroExpediente, probando con cm:name...");

            // Si no funciona con dms:numeroExpediente, lanzar excepción para ir al catch
            throw new AlfrescoException("No encontrado con propiedad custom:" + numeroExpediente);

        } catch (Exception e) {
            log.error("=== ERROR EN BÚSQUEDA POR NODE API ===");
            log.error("Número buscado: {}", numeroExpediente);
            log.error("Error: {}", e.getMessage());
            log.error("=== FIN ERROR ===");

            // Fallback opcional a Solr si quieres mantenerlo
            log.warn("Intentando fallback con Solr...");
            return buscarExpedientePorNumeroConSolr(numeroExpediente);
        }
    }*/

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
        String ano = String.valueOf(java.time.LocalDateTime.now().getYear());
        long secuencial = System.currentTimeMillis() % 1000000;
        return String.format("EXP-%s-%06d", ano, secuencial);
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

    @Override
    public UploadResponseCompleto uploadDocumento(String expedienteId, UploadDocumentoRequest request) {
        try {
            log.info("Subiendo documento - Expediente: {}, Archivo: {}, Subcarpeta: {}",
                    expedienteId, request.nombreArchivo(), request.subcarpeta());

            // Validaciones
            validarRequestUpload(request);

            // Buscar expediente
            NodeEntry expedienteNode = buscarExpediente(expedienteId);
            log.debug("Expediente encontrado: {}", expedienteNode.entry().name());

            // Buscar o crear subcarpeta
            String subcarpetaId = buscarOCrearSubcarpeta(expedienteNode.entry().id(), request.subcarpeta());
            log.debug("Subcarpeta obtenida: {}", subcarpetaId);

            // Construir propiedades del documento (sin usar propiedades DMS por ahora)
            /*Map<String, Object> propiedades = Map.of(
                    "cm:title", request.nombreArchivo(),
                    "cm:description", request.descripcion() != null ? request.descripcion() : ""
            );*/

            Map<String, Object> propiedades = construirPropiedadesDocumento(request, expedienteId);

            // Subir archivo
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

    private Map<String, Object> construirPropiedadesDocumento(UploadDocumentoRequest request, String expedienteId) {
        Map<String, Object> properties = new HashMap<>();

        // Propiedades estándar
        properties.put("cm:title", request.nombreArchivo());
        properties.put("cm:description", request.descripcion() != null ? request.descripcion() : "");

        // Propiedades DMS obligatorias según tu modelo
        properties.put("dms:tipoDocumento", request.tipoDocumento());
        properties.put("dms:fechaDocumento", java.time.LocalDate.now().toString());

        // Propiedades DMS opcionales
        properties.put("dms:subcarpeta", request.subcarpeta());
        properties.put("dms:expedienteId", expedienteId);
        properties.put("dms:fechaSubida", java.time.OffsetDateTime.now().toString());
        properties.put("dms:usuarioSubida", "system"); // O del contexto de seguridad

        // Metadatos adicionales
        if (request.metadatos() != null) {
            request.metadatos().forEach((key, value) -> {
                if (value != null && !value.trim().isEmpty()) {
                    properties.put("dms:" + key, value);
                }
            });
        }

        return properties;
    }

    @Override
    public PageDocumento<DocumentoInfo> getDocumentos(String expedienteId, String subcarpeta, int page, int size) {
        try {
            log.debug("Obteniendo documentos - Expediente: {}, Subcarpeta: {}, Page: {}, Size: {}",
                    expedienteId, subcarpeta, page, size);

            NodeEntry expedienteNode = buscarExpediente(expedienteId);
            List<DocumentoInfo> documentos = new ArrayList<>();

            if (subcarpeta != null && !subcarpeta.trim().isEmpty()) {
                documentos.addAll(getDocumentosDeSubcarpeta(expedienteNode.entry().id(), subcarpeta));
            } else {
                // Obtener documentos de todas las subcarpetas
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

            log.debug("Documentos encontrados: {} total, {} en página actual",
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
            log.debug("Obteniendo subcarpetas del expediente: {}", expedienteId);

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

            log.debug("Subcarpetas obtenidas: {}", subcarpetas.size());
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

// Métodos auxiliares privados:

    private void validarRequestUpload(UploadDocumentoRequest request) {
        if (!isSubcarpetaValida(request.subcarpeta())) {
            throw new IllegalArgumentException("Subcarpeta no válida: " + request.subcarpeta() +
                    ". Válidas: " + SUBCARPETAS_VALIDAS);
        }

        ConfiguracionSubcarpeta config = CONFIGURACION_SUBCARPETAS.get(request.subcarpeta());
        if (config != null) {
            long tamanoMB = request.contenido().length / (1024 * 1024);
            if (tamanoMB > config.tamanoMaximoMB()) {
                throw new IllegalArgumentException("Archivo demasiado grande: " + tamanoMB +
                        "MB. Máximo permitido: " + config.tamanoMaximoMB() + "MB");
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

        log.debug("Creando subcarpeta {}", nombreSubcarpeta);
        Map<String, Object> props = Map.of(
                "cm:title", nombreSubcarpeta,
                "cm:description", "Subcarpeta para " + nombreSubcarpeta.toLowerCase()
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

    private UploadResponseCompleto construirUploadResponse(NodeEntry documentoNode, UploadDocumentoRequest request) {
        if (documentoNode == null || documentoNode.entry() == null) {
            log.error("NodeEntry o su entry es null");
            throw new RuntimeException("Error: Respuesta inválida de Alfresco");
        }

        NodeEntry.Node node = documentoNode.entry();

        log.info("=== DEBUG UPLOAD RESPONSE ===");
        log.info("Node ID: '{}'", node.id());
        log.info("Node name: '{}'", node.name());
        log.info("Node type: '{}'", node.nodeType());

        // Validar que los campos críticos no sean null
        if (node.id() == null) {
            log.error("CRÍTICO: node.id() es null en la respuesta de Alfresco");
            // Intentar obtener el nodo recién creado
            return construirResponseAlternativo(request);
        }

        return new UploadResponseCompleto(
                node.id(),
                "1.0",
                node.name() != null ? node.name() : request.nombreArchivo(),
                request.subcarpeta(),
                request.tipoDocumento(),
                request.contenido().length,
                request.mimeType(),
                OffsetDateTime.now(),
                "system"
        );
    }

    private UploadResponseCompleto construirResponseAlternativo(UploadDocumentoRequest request) {
        log.warn("Construyendo respuesta alternativa debido a NodeEntry con valores null");

        return new UploadResponseCompleto(
                "temp-" + System.currentTimeMillis(), // ID temporal
                "1.0",
                request.nombreArchivo(),
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

    private DocumentoInfo construirDocumentoInfo(NodeEntry.Node node, String subcarpeta) {
        return new DocumentoInfo(
                node.id(),
                node.name(),
                "DOCUMENTO", // Valor por defecto ya que no usamos propiedades DMS aún
                subcarpeta,
                node.content() != null ? node.content().size() : 0L,
                node.content() != null ? node.content().mimeType() : "application/octet-stream",
                getStringProperty(node.properties(), "cm:created", ""),
                getStringProperty(node.properties(), "cm:creator", "system")
        );
    }

    private SubcarpetaInfo construirSubcarpetaInfo(String subcarpetaId, String nombre) {
        try {
            NodeChildrenList children = alfrescoClient.getNodeChildren(subcarpetaId, null);

            int cantidadDocs = 0;
            long tamanoTotal = 0L;

            if (children != null && children.getList() != null && children.getList().getEntries() != null) {
                for (var entry : children.getList().getEntries()) {
                    if ("cm:content".equals(entry.getEntry().nodeType())) {
                        cantidadDocs++;
                        if (entry.getEntry().content() != null && entry.getEntry().content().size() != null) {
                            tamanoTotal += entry.getEntry().content().size();
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
                    tamanoTotal,
                    OffsetDateTime.now()
            );

        } catch (Exception e) {
            log.warn("Error construyendo info de subcarpeta {}: {}", nombre, e.getMessage());
            return new SubcarpetaInfo(nombre, subcarpetaId, "Error obteniendo información", 0, 0L, OffsetDateTime.now());
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
    }

}
