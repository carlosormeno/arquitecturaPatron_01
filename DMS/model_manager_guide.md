# Guía Paso a Paso: Crear Modelo de Contenido en Alfresco

## PASO 1: Acceder al Model Manager

1. **Abrir Alfresco Share** en tu navegador:
   ```
   http://localhost:8080/share
   ```

2. **Iniciar sesión** como administrador (usuario: `admin`)

3. **Ir a Admin Tools**:
   - Click en tu nombre de usuario (esquina superior derecha)
   - Seleccionar **"Admin Tools"**

4. **Acceder al Model Manager**:
   - En el panel izquierdo, buscar **"Model Manager"**
   - Click en **"Model Manager"**

## PASO 2: Crear el Modelo Base

1. **Crear nuevo modelo**:
   - Click en el botón **"Create Model"**

2. **Completar la información del modelo**:
   ```
   Namespace:    http://www.onp.gob.pe/model/dms/1.0
   Prefix:       dms
   Name:         dmsModel
   Author:       Sistema DMS ONP
   Description:  Modelo de contenido para sistema de gestión documental
   ```

3. **Guardar**:
   - Click en **"Create"**

## PASO 3: Crear el Tipo "Expediente"

1. **Acceder al modelo creado**:
   - Click en el modelo **"dmsModel"** que acabas de crear

2. **Crear Custom Type**:
   - Click en la pestaña **"Custom Types"**
   - Click en **"Create Custom Type"**

3. **Configurar el tipo Expediente**:
   ```
   Name:              expediente
   Display Label:     Expediente
   Description:       Expediente del sistema DMS
   Parent Type:       cm:folder
   ```

4. **Guardar**:
   - Click en **"Create"**

## PASO 4: Agregar Propiedades al Expediente

### Propiedad 1: Número de Expediente
1. **Dentro del tipo "expediente"**:
   - Click en la pestaña **"Properties"**
   - Click en **"Create Property"**

2. **Configurar**:
   ```
   Name:              numeroExpediente
   Display Label:     Número de Expediente
   Description:       Número único del expediente
   Data Type:         d:text
   Mandatory:         ✓ (marcado)
   Multiple:          ✗ (sin marcar)
   Default Value:     (vacío)
   ```

3. **Guardar**: Click en **"Create"**

### Propiedad 2: Fecha de Inicio
1. **Crear otra propiedad**:
   - Click en **"Create Property"**

2. **Configurar**:
   ```
   Name:              fechaInicio
   Display Label:     Fecha de Inicio
   Description:       Fecha de inicio del expediente
   Data Type:         d:date
   Mandatory:         ✓ (marcado)
   Multiple:          ✗ (sin marcar)
   ```

3. **Guardar**: Click en **"Create"**

### Propiedad 3: Solicitante
1. **Crear propiedad**:
   ```
   Name:              solicitante
   Display Label:     Solicitante
   Description:       Persona o entidad solicitante
   Data Type:         d:text
   Mandatory:         ✓ (marcado)
   Multiple:          ✗ (sin marcar)
   ```

### Propiedad 4: Asunto
1. **Crear propiedad**:
   ```
   Name:              asunto
   Display Label:     Asunto
   Description:       Asunto del expediente
   Data Type:         d:text
   Mandatory:         ✓ (marcado)
   Multiple:          ✗ (sin marcar)
   ```

### Propiedad 5: Estado (con lista de valores)
1. **Crear propiedad**:
   ```
   Name:              estado
   Display Label:     Estado
   Description:       Estado actual del expediente
   Data Type:         d:text
   Mandatory:         ✓ (marcado)
   Multiple:          ✗ (sin marcar)
   Default Value:     INICIADO
   ```

2. **Agregar Constraint (Lista de valores)**:
   - En la misma pantalla, buscar **"Constraint"**
   - Seleccionar **"List of Values"**
   - Agregar los valores:
     ```
     INICIADO
     EN_PROCESO
     PENDIENTE
     FINALIZADO
     ARCHIVADO
     ```

3. **Guardar**: Click en **"Create"**

## PASO 5: Crear Tipo "Documento" (Opcional)

1. **Regresar al modelo**:
   - Click en **"dmsModel"** (breadcrumb superior)

2. **Crear otro Custom Type**:
   ```
   Name:              documento
   Display Label:     Documento DMS
   Description:       Documento del sistema DMS
   Parent Type:       cm:content
   ```

3. **Agregar propiedades al documento**:
   - `numeroDocumento` (d:text, no obligatorio)
   - `tipoDocumento` (d:text, obligatorio, con lista: SOLICITUD, RESOLUCION, OFICIO, INFORME)
   - `fechaDocumento` (d:date, obligatorio)
   - `emisor` (d:text, no obligatorio)
   - `destinatario` (d:text, no obligatorio)

## PASO 6: Activar el Modelo

1. **Regresar a la lista de modelos**:
   - Click en **"Models"** en el breadcrumb superior

2. **Activar el modelo**:
   - Buscar tu modelo **"dmsModel"**
   - En la columna **"Actions"**, click en **"Activate"**
   - Confirmar la activación

## PASO 7: Verificar la Activación

1. **Estado del modelo**:
   - El modelo debe mostrar status **"Active"**

2. **Probar en el código**:
   - Reiniciar tu aplicación
   - El error del namespace debería desaparecer

## PASO 8: Usar el Modelo en tu Código

Ahora puedes usar las propiedades en tu código Java:

```java
// Definir las QNames
public static final String DMS_URI = "http://www.onp.gob.pe/model/dms/1.0";
public static final QName TYPE_EXPEDIENTE = QName.createQName(DMS_URI, "expediente");
public static final QName PROP_NUMERO_EXPEDIENTE = QName.createQName(DMS_URI, "numeroExpediente");
public static final QName PROP_SOLICITANTE = QName.createQName(DMS_URI, "solicitante");
public static final QName PROP_ASUNTO = QName.createQName(DMS_URI, "asunto");
public static final QName PROP_ESTADO = QName.createQName(DMS_URI, "estado");

// Crear expediente
Map<QName, Serializable> properties = new HashMap<>();
properties.put(PROP_NUMERO_EXPEDIENTE, numeroExpediente);
properties.put(PROP_SOLICITANTE, solicitante);
properties.put(PROP_ASUNTO, asunto);
properties.put(PROP_ESTADO, "INICIADO");
properties.put(ContentModel.PROP_NAME, "Expediente " + numeroExpediente);

NodeRef expedienteNode = nodeService.createNode(
    parentNodeRef,
    ContentModel.ASSOC_CONTAINS,
    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "expediente-" + numeroExpediente),
    TYPE_EXPEDIENTE,
    properties
).getChildRef();
```

## Notas Importantes

- **No reinicies Alfresco**: Los modelos creados con Model Manager se activan dinámicamente
- **Backup automático**: El Model Manager crea respaldos automáticamente
- **Versionado**: Puedes crear versiones del modelo para cambios futuros
- **Exportar**: Puedes exportar el modelo como XML si necesitas migrarlo

## Solución de Problemas

Si el modelo no aparece activo:
1. Verificar que no haya errores en los logs de Alfresco
2. Revisar que el namespace sea único
3. Comprobar que las propiedades mandatory tengan valores válidos