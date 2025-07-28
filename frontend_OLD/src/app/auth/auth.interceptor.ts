import { HttpInterceptorFn, HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
//import { catchError, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { TelemetryService } from '../telemetry.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('jwt_token');
  const router = inject(Router);
  const telemetry = inject(TelemetryService);

  // Generar correlation ID único para cada request
  const correlationId = crypto.randomUUID();

  /*const authReq = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;*/

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
    headers: req.headers.keys()
  });

  //return next(authReq);

  /*return next(authReq).pipe(
    catchError(err => {
      if (err.status === 401 || err.status === 403) {
        localStorage.removeItem('jwt_token');
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );*/

  return next(authReq).pipe(
    /*tap(response => {
      const duration = Date.now() - startTime;
      telemetry.logToConsole('info', 'HTTP Request Completed', {
        method: req.method,
        url: req.url,
        status: response.status,
        duration,
        correlationId
      });
    }),*/
    tap(event => {
      // ✅ Verificar que sea HttpResponse antes de acceder a status
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

      if (err.status === 401 || err.status === 403) {
        localStorage.removeItem('jwt_token');
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );

};
