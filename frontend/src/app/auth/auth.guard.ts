import { inject } from '@angular/core';
import { Router, UrlTree, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
//import { CanActivateFn } from '@angular/router';
//import { AuthService } from './auth';
//import { Router } from '@angular/router';
//import { KeycloakService } from 'keycloak-angular';
import { createAuthGuard, AuthGuardData } from 'keycloak-angular';

/*export const authGuard: CanActivateFn = async (route, state) => {

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  console.log('[LOGIN] init');
  const isLoggedIn = await keycloak.isLoggedIn();
  console.log('[LOGIN] isLoggedIn?', isLoggedIn);

  if (isLoggedIn) {
    return true;
  }

  /*await keycloak.login({
    redirectUri: window.location.origin + state.url,
  });*/
  //router.navigate(['/login'], { queryParams: { returnUrl: state.url } });

  //return false;
//};*/

const isAccessAllowed = async (route: ActivatedRouteSnapshot, state: RouterStateSnapshot, data: AuthGuardData)
: Promise<boolean | UrlTree> => {
  const router = inject(Router);
  if (data.authenticated) return true;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const authGuard = createAuthGuard(isAccessAllowed);
