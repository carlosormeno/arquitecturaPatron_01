
package pe.gob.onp.arquitectura.dms.service;
import pe.gob.onp.arquitectura.dms.api.dto.AdminDtos.*;
public interface AdminService {
  PageTemplate listTemplates();
  Template getTemplate(String id);
  Template duplicateTemplate(String id);
  Template resolve(String tenantId, String businessUnit);
  RetentionPolicy retention();
  RetentionPolicy updateRetention(RetentionPolicy p);
  AuditIntegrity integrity();
}
