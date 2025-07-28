import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { catchError, tap, switchMap } from 'rxjs/operators';
import { throwError, from } from 'rxjs';
import { TelemetryService } from '../telemetry.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloakService = inject(KeycloakService);
  const telemetry = inject(TelemetryService);

  // Generar correlation ID único para cada request
  const correlationId = crypto.randomUUID();

  // ✅ Obtener token de Keycloak de forma asíncrona si es necesario
  return from(keycloakService.getToken()).pipe(
    switchMap(token => {
      const authReq = req.clone({
        setHeaders: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          'X-Correlation-ID': correlationId,
          'X-User-Agent': 'frontend-angular/1.0.0'
        }
      });

      const startTime = Date.now();

      telemetry.logToConsole('info', 'HTTP Request Started', {
        method: req.method,
        url: req.url,
        correlationId,
        headers: req.headers.keys(),
        hasToken: !!token
      });

      return next(authReq).pipe(
        tap(event => {
          if (event instanceof HttpResponse) {
            const duration = Date.now() - startTime;
            telemetry.logToConsole('info', 'HTTP Request Completed', {
              method: req.method,
              url: req.url,
              status: event.status,
              statusText: event.statusText,
              duration,
              correlationId,
              responseSize: event.body ? JSON.stringify(event.body).length : 0
            });
          }
        }),
        catchError(err => {
          const duration = Date.now() - startTime;
          telemetry.logToConsole('error', 'HTTP Request Failed', {
            method: req.method,
            url: req.url,
            status: err.status,
            error: err.message,
            duration,
            correlationId
          });

          // ✅ Si hay error 401/403, redirigir a Keycloak login
          if (err.status === 401 || err.status === 403) {
            keycloakService.login();
          }
          return throwError(() => err);
        })
      );
    })
  );
};