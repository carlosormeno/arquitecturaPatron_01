
Este paquete agrega un cliente Alfresco (WebClient) listo para usar en tu DMS.
Copia el contenido de `src/main/java` dentro de tu proyecto y asegúrate de tener en `application.yml`:

app:
  alfresco:
    base-url: http://alfresco-repository:8080/alfresco
    username: admin
    password: admin

Uso en servicios:
- Inyecta `AlfrescoClient` en `ExpedientesServiceImpl` y `DocumentosServiceImpl`.
  Ejemplos:
    var node = alfrescoClient.createFolder(parentId, name, props);
    var uploaded = alfrescoClient.uploadFile(parentId, filename, bytes, "application/pdf", props);
    var existing = alfrescoClient.getNode(nodeId);
    var results = alfrescoClient.searchLucene("PATH:"/app:company_home//*"", 25, 0);
    alfrescoClient.createRendition(nodeId, "pdf"); // o "doclib" para thumbnail

Endpoints base utilizados:
- /api/-default-/public/alfresco/versions/1/nodes/{id}
- /api/-default-/public/alfresco/versions/1/nodes/{id}/children
- /api/-default-/public/alfresco/versions/1/nodes/{id}/content
- /api/-default-/public/alfresco/versions/1/nodes/{id}/renditions
- /api/-default-/public/search/versions/1/search
