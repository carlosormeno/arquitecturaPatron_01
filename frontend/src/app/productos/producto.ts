import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface Producto {
  id?: string;
  nombre: string;
  precio: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductoService {

  private http = inject(HttpClient);
  private queryUrl = 'http://localhost:8000/api/mongoProductos'; // ajusta si es necesario
  private commandUrl = 'http://localhost:8000/api/productos';

  getProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.queryUrl);
  }

  getProducto(id: string): Observable<Producto> {
    return this.http.get<Producto>(`${this.queryUrl}/${id}`);
  }

  crearProducto(producto: Producto): Observable<any> {
    console.log('📤 Enviando producto al backend:', producto);
    console.log('📤 Enviando producto al backend:22 ', this.commandUrl);
    return this.http.post(this.commandUrl, producto).pipe(
    tap((respuesta) => console.log('✅ Respuesta del backend (crearProducto):', respuesta))
    );
  }

  actualizarProducto(id: string, producto: Producto): Observable<any> {
    return this.http.put(`${this.commandUrl}/${id}`, producto);
  }

  eliminarProducto(id: string): Observable<any> {
    return this.http.delete(`${this.commandUrl}/${id}`);
  }

  //constructor() { }
}
