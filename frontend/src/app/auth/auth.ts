import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})




export class AuthService {

  //private readonly apiUrl = 'http://localhost:8081/api/auth/login'; // Ajusta si es necesario

  private http = inject(HttpClient);

  /*login(username: string, password: string): Observable<any> {
    return this.http.post(this.apiUrl, { username, password }).pipe(
      tap((response: any) => {
        localStorage.setItem('token', response.token); // Ajusta si el backend devuelve otro campo
      })
    );
  }*/

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
  }

  //constructor() { }
}
