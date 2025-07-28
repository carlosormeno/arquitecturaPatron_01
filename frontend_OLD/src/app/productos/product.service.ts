import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Producto } from './producto';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  //private queryUrl = 'http://localhost:8082/api/mongoProductos';
  //private commandUrl = 'http://localhost:8081/api/productos';

  //Usando Kong
  private queryUrl = 'http://localhost:8000/api/mongoProductos';
  private commandUrl = 'http://localhost:8000/api/productos';

  constructor(private http: HttpClient) {console.log('🔗 ProductService (Container) usando Kong:', this.queryUrl);}

  getAll(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.queryUrl);
  }

  getById(id: string): Observable<Producto> {
    return this.http.get<Producto>(`${this.queryUrl}/${id}`);
  }

  create(producto: Producto): Observable<any> {
    return this.http.post(this.commandUrl, producto);
  }

  update(id: string, producto: Producto): Observable<any> {
    return this.http.put(`${this.commandUrl}/${id}`, producto);
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.commandUrl}/${id}`);
  }
}
