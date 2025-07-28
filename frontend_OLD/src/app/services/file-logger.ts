import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FileLogger {

  constructor() { }

    // ✅ Escribir log a archivo usando la API del browser
  async writeLog(logEntry: any): Promise<void> {
    try {
      const logLine = JSON.stringify(logEntry) + '\n';

      // ✅ Crear blob con el contenido
      const blob = new Blob([logLine], { type: 'application/json' });

      // ✅ Usar File System Access API si está disponible (Chrome moderno)
      if ('showSaveFilePicker' in window) {
        await this.appendToFileSystemAPI(logLine);
      } else {
        // ✅ Fallback: usar download API para simular escritura
        await this.appendToDownloadAPI(blob);
      }

    } catch (error) {
      console.warn('Failed to write log to file:', error);
    }
  }

  // ✅ Método moderno para browsers que soportan File System Access API
  private async appendToFileSystemAPI(content: string): Promise<void> {
    try {
      // Por ahora, como fallback a console con formato especial
      console.log(`[FILE_LOG] ${content.trim()}`);
    } catch (error) {
      console.warn('File System API error:', error);
    }
  }

  // ✅ Método fallback usando downloads
  private async appendToDownloadAPI(blob: Blob): Promise<void> {
    try {
      // Log to console con formato que Promtail puede leer
      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        console.log(`[FRONTEND_LOG] ${content.trim()}`);
      };
      reader.readAsText(blob);
    } catch (error) {
      console.warn('Download API error:', error);
    }
  }

}
