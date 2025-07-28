import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors} from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './auth/auth.interceptor';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { TelemetryService } from './telemetry.service';
//import { provideKeycloak } from 'keycloak-angular';
import {
  provideKeycloak} from 'keycloak-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
    provideAnimations(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
    }),
    provideAnimationsAsync(),

    // ✅ Telemetry service (se inicializará automáticamente)
    TelemetryService,
    // ✅ Keycloak configuration para Angular standalone
    provideKeycloak({
      config: {
        url: 'http://localhost:8080',
        realm: 'arquitecturaTI',
        clientId: 'angular-app'
      },
      initOptions: {
        onLoad: 'check-sso',
        // The silent SSO check file is served from the root path
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      }
    })
  ]
};
