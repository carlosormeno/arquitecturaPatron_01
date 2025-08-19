import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize, tap, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';

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
  /*private queryUrl = 'http://localhost:8000/api/mongoProductos';
  private commandUrl = 'http://localhost:8000/api/productos';*/

  private queryUrl = `${environment.apiBase}/mongoProductos`;
  private commandUrl = `${environment.apiBase}/productos`;

  getProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.queryUrl);
  }

  getProducto(id: string): Observable<Producto> {
    return this.http.get<Producto>(`${this.queryUrl}/${id}`);
  }

  /*crearProducto(producto: Producto): Observable<any> {
    console.log('📤 Enviando producto al backend:', producto);
    console.log('📤 Enviando producto al backend:22 ', this.commandUrl);
    return this.http.post(this.commandUrl, producto).pipe(
    tap((respuesta) => console.log('✅ Respuesta del backend (crearProducto):', respuesta))
    );
  }*/

  crearProducto(producto: Producto) {
  console.log('📤 Enviando producto al backend:', producto, this.commandUrl);

  return this.http.post(this.commandUrl, producto, { observe: 'events', reportProgress: true }).pipe(
    tap(event => {
      switch (event.type) {
        case HttpEventType.Sent: console.log('🚀 Sent'); break;
        case HttpEventType.ResponseHeader: console.log('📥 ResponseHeader'); break;
        case HttpEventType.UploadProgress: console.log('⬆️ Upload'); break;
        case HttpEventType.DownloadProgress: console.log('⬇️ Download'); break;
        case HttpEventType.Response: console.log('✅ Response:', event.body); break;
      }
    }),
    timeout(15000),
    catchError(err => { console.error('❌ Error crearProducto:', err); return throwError(() => err); }),
    finalize(() => console.log('🧹 finalize crearProducto')),
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
