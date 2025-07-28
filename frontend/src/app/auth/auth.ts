import { inject, Injectable } from '@angular/core';
//import { HttpClient } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { from, Observable, tap } from 'rxjs';

/*export interface AuthResponse {
  token: string;
}*/

@Injectable({
  providedIn: 'root'
})

export class AuthService {

  /*private readonly apiUrl = 'http://localhost:8081/api/auth/login'; // Ajusta si es necesario

  private http = inject(HttpClient);

  login(username: string, password: string): Observable<any> {
    return this.http.post(this.apiUrl, { username, password }).pipe(
      tap((response: any) => {
        localStorage.setItem('token', response.token); // Ajusta si el backend devuelve otro campo
      })
    );
  }

  login(username: string, password: string): Observable<any> {
  return this.http.post<AuthResponse>('http://localhost:8000/api/auth/login', {
    username,
    password
  }).pipe(
    tap(response => {
      localStorage.setItem('jwt_token', response.token);
    })
  );
}


  logout(): void {
    localStorage.removeItem('jwt_token');
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('jwt_token');
  }

  getToken(): string | null {
    return localStorage.getItem('jwt_token');
  }*/

  //constructor() { }

  private keycloakService = inject(KeycloakService);

  // ✅ Login ahora redirige a Keycloak
  login(): Observable<void> {
    return from(this.keycloakService.login({
      redirectUri: window.location.origin + '/productos'
    }));
  }

  // ✅ Logout desde Keycloak
  logout(): Observable<void> {
    return from(this.keycloakService.logout(window.location.origin));
  }

  // ✅ Verificar si está autenticado
  isAuthenticated(): boolean {
    return this.keycloakService.isLoggedIn();
  }

  // ✅ Obtener token JWT de Keycloak
  getToken(): Promise<string | undefined> {
    return this.keycloakService.getToken();
  }

  // ✅ Obtener información del usuario
  getUserInfo() {
    return this.keycloakService.loadUserProfile();
  }

  // ✅ Obtener username
  getUsername(): string | undefined {
    return this.keycloakService.getUsername();
  }

  // ✅ Verificar roles
  hasRole(role: string): boolean {
    return this.keycloakService.isUserInRole(role);
  }

  // ✅ Método de compatibilidad (ya no se usa)
  loginWithCredentials(username: string, password: string): Observable<any> {
    // Este método ya no se usará con Keycloak SSO
    // Solo lo mantenemos para compatibilidad durante la transición
    console.warn('loginWithCredentials is deprecated. Use login() instead.');
    return this.login();
  }
}
