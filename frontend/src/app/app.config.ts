import { ApplicationConfig, ErrorHandler, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
/*import { provideHttpClient, withInterceptors} from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './auth/auth.interceptor';
export { telemetryInterceptor as authInterceptor } from './auth/telemetry.interceptor';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { TelemetryService } from './telemetry.service';
import { telemetryInterceptor } from './auth/telemetry.interceptor';
//import { provideKeycloak } from 'keycloak-angular';
//import {provideKeycloak} from 'keycloak-angular';
import {
  provideKeycloak,
  includeBearerTokenInterceptor,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  createInterceptorCondition
} from 'keycloak-angular';
*/

//import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';

import { telemetryInterceptor } from './auth/telemetry.interceptor';

import { GlobalErrorHandler } from './core/global-error.handler';

import {
  provideKeycloak,
  includeBearerTokenInterceptor,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  createInterceptorCondition,
  // Opcional:
  // withAutoRefreshToken, AutoRefreshTokenService, UserActivityService,
} from 'keycloak-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true, runCoalescing: true }),
    provideAnimations(),
    //provideAnimationsAsync(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
    }),
    // ✅ Telemetry service (se inicializará automáticamente)
    //TelemetryService,
    // ✅ Keycloak configuration para Angular standalone
    
    provideKeycloak({
      config: {
        //url: 'http://localhost/realms/arquitecturaTI',
        url: 'http://localhost:8080',
        realm: 'arquitecturaTI',
        clientId: 'angular-app'
      },
      initOptions: {
        //onLoad: 'check-sso',
        onLoad: 'login-required',
        // The silent SSO check file is served from the root path
        //silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      },
      // features: [ withAutoRefreshToken({ onInactivityTimeout: 'logout', sessionTimeout: 300000 }) ],
      // providers: [AutoRefreshTokenService, UserActivityService],
    }),

    /*{provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  useValue: [
    createInterceptorCondition({
      urlPattern: /^https?:\/\/(localhost(:\d+)?|kong(:\d+)?)\//i,
      bearerPrefix: 'Bearer'
    })
  ]
},*/


//    provideHttpClient(withInterceptors([authInterceptor])),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor, telemetryInterceptor])),
    //provideHttpClient(withInterceptors([bearerTokenInterceptor,telemetryInterceptor])),

    {provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  useValue: [
    // Kong local
        createInterceptorCondition({
          urlPattern: /^http:\/\/localhost:8000\/api(\/.*)?$/i,
          bearerPrefix: 'Bearer' // opcional; por defecto ya es 'Bearer'
        }),
        // Si usas hostname "kong" en Docker:
        createInterceptorCondition({ urlPattern: /^http:\/\/kong:8000\/api(\/.*)?$/i,
          bearerPrefix: 'Bearer' // opcional; por defecto ya es 'Bearer' 
        }),
        createInterceptorCondition({ urlPattern: /^https:\/\/localhost:8000\/api(\/.*)?$/i,
          bearerPrefix: 'Bearer' // opcional; por defecto ya es 'Bearer' 

         }),
createInterceptorCondition({ urlPattern: /^https:\/\/kong:8000\/api(\/.*)?$/i,
          bearerPrefix: 'Bearer' // opcional; por defecto ya es 'Bearer' 

}),

        // Si usas IP:
        // createInterceptorCondition({ urlPattern: /^http:\/\/192\.168\.1\.10:8000\/api(\/.*)?$/i }),
      ],
},

    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideRouter(routes),

  ]
};
