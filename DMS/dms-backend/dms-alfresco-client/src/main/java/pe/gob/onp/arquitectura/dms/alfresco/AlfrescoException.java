
package pe.gob.onp.arquitectura.dms.alfresco;

public class AlfrescoException extends RuntimeException {
    public AlfrescoException(String message) {
        super(message);
    }

    public AlfrescoException(String message, Throwable cause) {
        super(message, cause);
    }
}
