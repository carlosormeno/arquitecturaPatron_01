import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
// Si tienes un servicio de telemetría real, impórtalo aquí
// ✅ Importa el servicio REAL (ajusta la ruta según tu proyecto)
import { TelemetryService } from '../telemetry.service';


//class TelemetryService { logHttp(_: any) {} }

export const telemetryInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const telemetry = inject(TelemetryService);
  const correlationId = (globalThis.crypto && 'randomUUID' in globalThis.crypto) ? crypto.randomUUID() : Math.random().toString(36).slice(2);
  const startedAt = performance.now();
  const tracedReq = req.clone({ setHeaders: { 'X-Correlation-Id': correlationId } });

  return next(tracedReq).pipe(
    tap((event) => {
      if (event instanceof HttpResponse) {
        const duration = performance.now() - startedAt;
        try { telemetry.logHttp({ url: tracedReq.url, method: tracedReq.method, status: event.status, duration, correlationId }); } catch {}
      }
    }),
    catchError((err) => {
      const duration = performance.now() - startedAt;
      try { telemetry.logHttp({ url: tracedReq.url, method: tracedReq.method, status: err?.status ?? 0, duration, correlationId, error: true }); } catch {}
      return throwError(() => err);
    })
  );
};
