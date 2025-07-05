import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { WebTracerProvider, BatchSpanProcessor } from '@opentelemetry/sdk-trace-web';
import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { Resource } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION, SEMRESATTRS_DEPLOYMENT_ENVIRONMENT } from '@opentelemetry/semantic-conventions';
import { trace, context, SpanStatusCode } from '@opentelemetry/api';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { FileLogger } from './services/file-logger';

@Injectable({
  providedIn: 'root'
})
export class TelemetryService {
  //private provider: WebTracerProvider;
  private provider!: WebTracerProvider;
  private tracer: any;

  private logBuffer: any[] = [];
  private readonly batchSize = 10;
  private readonly flushInterval = 5000; // 5 segundos

  // ✅ Endpoint del backend para logs
  private readonly logEndpoint = 'http://locahost:8000/api/logs/frontend';
  private readonly logBatchEndpoint = 'http://localhost:8000/api/logs/frontend/batch';


  constructor(private http: HttpClient) {
    this.initializeTracing();
    this.startBatchTimer();
  }

  /*constructor(private fileLogger: FileLogger) {
    this.initializeTracing();
  }*/

  private initializeTracing() {
    // Configurar el exporter OTLP
    const traceExporter = new OTLPTraceExporter({
      url: 'http://localhost:4318/v1/traces',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const resource = new Resource({
      [ATTR_SERVICE_NAME]: 'frontend-angular',
      [ATTR_SERVICE_VERSION]: '1.0.0',
      [SEMRESATTRS_DEPLOYMENT_ENVIRONMENT ]: 'development'
    });


    // Crear el WebTracerProvider
    this.provider = new WebTracerProvider({
      resource
    });

    // Agregar el processor con el exporter
    this.provider.addSpanProcessor(
      new BatchSpanProcessor(traceExporter)
    );

    // Registrar el provider globalmente
    this.provider.register();

    // Registrar instrumentaciones automáticas
    registerInstrumentations({
      instrumentations: [
        getWebAutoInstrumentations({
          '@opentelemetry/instrumentation-fetch': {
            propagateTraceHeaderCorsUrls: [
              'http://kong:8000', // Kong Gateway
              'http://comando-microservicio:8081', // Command Service
              'http://consulta-microservicio:8082',  // Query Service
              'http://otel-collector:4318'     // ✅ OTLP Collector
            ],
            clearTimingResources: true
          },
          '@opentelemetry/instrumentation-xml-http-request': {
            propagateTraceHeaderCorsUrls: [
              'http://kong:8000', // Kong Gateway
              'http://comando-microservicio:8081', // Command Service
              'http://consulta-microservicio:8082',  // Query Service
              'http://otel-collector:4318'     // ✅ OTLP Collector
            ]
          },
          '@opentelemetry/instrumentation-user-interaction': {
            enabled: true
          },
          '@opentelemetry/instrumentation-document-load': {
            enabled: true
          }
        })
      ]
    });

    this.tracer = trace.getTracer('frontend-angular', '1.0.0');

    console.log('✅ OpenTelemetry initialized successfully');
  }

  // Crear un span personalizado
  createSpan(spanName: string, operation: () => void | Promise<void>) {
    const span = this.tracer.startSpan(spanName);

    return context.with(trace.setSpan(context.active(), span), async () => {
      try {
        span.setAttributes({
          'user.id': this.getCurrentUserId() || 'anonymous',
          'app.version': '1.0.0'
        });

        const result = await operation();
        span.setStatus({ code: SpanStatusCode.OK });
        return result;
      } catch (error: any) {
        span.setStatus({
          code: SpanStatusCode.ERROR,
          message: error.message
        });
        span.setAttribute('error', true);
        span.setAttribute('error.message', error.message);
        throw error;
      } finally {
        span.end();
      }
    });
  }

  // Método para logs estructurados
  logToConsole(level: 'info' | 'warn' | 'error', message: string, data?: any) {
    const activeSpan = trace.getActiveSpan();
    const spanContext = activeSpan?.spanContext();

    const logEntry = {
      timestamp: new Date().toISOString(),
      level,
      service: 'frontend-angular',
      message,
      data,
      traceId: spanContext?.traceId,
      spanId: spanContext?.spanId,
      userId: this.getCurrentUserId(),
      correlationId: this.generateCorrelationId()
    };

    // Log a consola (que puede ser capturado por herramientas)
    console.log(JSON.stringify(logEntry));

    // ✅ Escribir a archivo para que Promtail lo lea
    //this.writeLogToFile(logEntry);
    this.sendLogToBackend(logEntry);

    // Opcional: enviar directamente a Loki
    //this.sendLogToLoki(logEntry);
  }

  // ✅ NUEVO: Escribir logs a archivo local para Promtail
  private async writeLogToFile(logEntry: any): Promise<void> {
    try {
      // ✅ En desarrollo, simular escritura a archivo
      if (this.isDevelopment()) {
        console.log(`📁 [File Log] ${JSON.stringify(logEntry)}`);
      }

      //await this.fileLogger.writeLog(logEntry);

      // ✅ TODO: Implementar escritura real a archivo en producción
      // Por ahora, los logs van a console y Promtail puede leerlos desde browser logs

    } catch (error) {
      console.warn('Failed to write log to file:', error);
    }
  }

  // ✅ NUEVO: Enviar log al backend
  private async sendLogToBackend(logEntry: any): Promise<void> {
    try {
      // ✅ Añadir al buffer para batching
      this.logBuffer.push(logEntry);

      // ✅ Si el buffer está lleno, enviar inmediatamente
      if (this.logBuffer.length >= this.batchSize) {
        await this.flushLogs();
      }

    } catch (error) {
      console.warn('Failed to buffer log for backend:', error);
      // ✅ Fallback: continúa funcionando sin backend
    }
  }

  private getCurrentTraceId(): string | undefined {
    const activeSpan = trace.getActiveSpan();
    return activeSpan?.spanContext()?.traceId;
  }

  private getCurrentUserId(): string | undefined {
    return localStorage.getItem('user_id') || localStorage.getItem('jwt_token')?.split('.')[1] || undefined;
  }

  private parseJwtUserId(token: string | null): string | undefined {
    if (!token) return undefined;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || payload.user_id || payload.id;
    } catch {
      return undefined;
    }
  }

  private isDevelopment(): boolean {
    return window.location.hostname === 'localhost' ||
           window.location.hostname === '127.0.0.1';
  }

  private async sendLogToLoki(logEntry: any) {
    try {
      //const response = await fetch('http://localhost:3100/loki/api/v1/push', {
      const response = await fetch('/api/loki/v1/push', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          streams: [{
            stream: {
              job: 'frontend-angular',
              level: logEntry.level,
              service: 'frontend'
            },
            values: [[
              (Date.now() * 1000000).toString(), // timestamp en nanosegundos
              JSON.stringify(logEntry)
            ]]
          }]
        })
      });

      if (!response.ok) {
        console.warn('Failed to send log to Loki:', response.statusText);
      }
    } catch (error) {
      // Fallar silenciosamente para no interrumpir la app
      console.warn('Error sending log to Loki:', error);
    }
  }

  // ✅ NUEVO: Enviar logs en lotes al backend
  private async flushLogs(): Promise<void> {
    if (this.logBuffer.length === 0) return;

    const logsToSend = [...this.logBuffer];
    this.logBuffer = [];

    try {
      // ✅ Enviar al backend en lote
      await this.http.post(this.logBatchEndpoint, { logs: logsToSend }).toPromise();
      console.debug(`✅ Sent ${logsToSend.length} logs to backend`);

    } catch (error) {
      console.warn('Failed to send logs to backend, retrying individually:', error);

      // ✅ Fallback: enviar logs individualmente
      for (const log of logsToSend) {
        try {
          await this.http.post(this.logEndpoint, log).toPromise();
        } catch (individualError) {
          console.warn('Failed to send individual log:', individualError);
          // ✅ Log solo a console como último recurso
          console.log(`[FALLBACK_LOG] ${JSON.stringify(log)}`);
        }
      }
    }
  }

  // ✅ NUEVO: Timer para flush automático
  private startBatchTimer(): void {
    setInterval(() => {
      this.flushLogs();
    }, this.flushInterval);

    // ✅ Flush antes de cerrar la página
    window.addEventListener('beforeunload', () => {
      this.flushLogs();
    });
  }

  // Método para shutdown limpio
  shutdown() {
    this.flushLogs();
    return this.provider.shutdown();
  }

  // ✅ NUEVOS: Métodos de conveniencia para logging
  logInfo(message: string, data?: any): void {
    this.logToConsole('info', message, data);
  }

  logWarn(message: string, data?: any): void {
    this.logToConsole('warn', message, data);
  }

  logError(message: string, data?: any): void {
    this.logToConsole('error', message, data);
  }

  // ✅ NUEVO: Log HTTP requests con trace context
  logHttpRequest(method: string, url: string, headers?: any): void {
    this.logInfo('HTTP Request Started', {
      method,
      url,
      headers: this.sanitizeHeaders(headers),
      correlationId: this.generateCorrelationId()
    });
  }

  logHttpResponse(method: string, url: string, status: number, responseTime?: number): void {
    const level = status >= 400 ? 'error' : status >= 300 ? 'warn' : 'info';
    this.logToConsole(level, 'HTTP Response', {
      method,
      url,
      status,
      responseTime: responseTime ? `${responseTime}ms` : undefined
    });
  }

  logHttpError(method: string, url: string, error: any): void {
    this.logError('HTTP Request Failed', {
      method,
      url,
      error: error.message || error,
      status: error.status
    });
  }

  // ✅ Helper methods
  private sanitizeHeaders(headers: any): any {
    if (!headers) return {};
    const sanitized = { ...headers };
    // Remove sensitive headers
    delete sanitized.authorization;
    delete sanitized.Authorization;
    return sanitized;
  }

  generateCorrelationId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  // ✅ NUEVO: Obtener trace context actual
  getCurrentTraceContext(): { traceId?: string; spanId?: string } {
    const activeSpan = trace.getActiveSpan();
    const spanContext = activeSpan?.spanContext();
    return {
      traceId: spanContext?.traceId,
      spanId: spanContext?.spanId
    };
  }

}
