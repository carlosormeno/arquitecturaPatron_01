import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductoService, Producto } from '../producto';
import { RouterModule, Route, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { Location } from '@angular/common';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-lista-productos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './lista-productos.html',
  styleUrl: './lista-productos.scss',
})
export class ListaProductos implements OnInit {
  productos: Producto[] = [];

  private productoService = inject(ProductoService);
  private router = inject(Router);
  private location = inject(Location);
  private toastr = inject(ToastrService);
  //mensaje: string | null = null;

  ngOnInit(): void {
    this.cargarProductos();

    //const state = this.location.getState() as { mensaje?: string };
    const state = this.location.getState() as { mensaje?: string, refrescar?: boolean };
    if (state?.mensaje) {
          this.toastr.success(state.mensaje, 'Éxito');
          history.replaceState({}, '',location.href);
    }

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.cargarProductos();
      });
  }

  cargarProductos(): void {
    this.productoService.getProductos().subscribe({
      next: (data) => (this.productos = data),
      error: () => alert('Error al obtener productos'),
    });
  }

  eliminarProducto(id: string): void {
    if (confirm('¿Deseas eliminar este producto?')) {
      this.productoService.eliminarProducto(id).subscribe({
        next: () => {
          this.toastr.success('Producto eliminado');
          this.cargarProductos();
        },
        error: () => alert('Error al eliminar producto'),
      });
    }
  }
}
