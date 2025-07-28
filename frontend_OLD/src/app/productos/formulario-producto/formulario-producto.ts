import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProductoService, Producto } from '../producto';

@Component({
  selector: 'app-formulario-producto',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './formulario-producto.html',
  styleUrl: './formulario-producto.scss'
})
export class FormularioProducto implements OnInit {
  producto: Producto = { id: '', nombre: '', precio: 0 };
  modoEdicion = false;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productoService = inject(ProductoService);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.modoEdicion = true;
      this.productoService.getProducto(id).subscribe({
        next: (data) => {
          console.log('Producto recibido para edición:', data);
          this.producto = data},
        error: () => alert('Producto no encontrado')
      });
    }
  }

  guardar(): void {
    if (this.modoEdicion) {
      this.productoService.actualizarProducto(this.producto.id!, this.producto).subscribe({
        next: () => {

          this.router.navigate(['/productos'], {
            state: { mensaje: 'Producto creado exitosamente', refrescar: true }
          });


        },
        error: () => alert('Error al actualizar producto')
      });
    } else {
      console.log('Producto a crear:', this.producto);
      this.productoService.crearProducto(this.producto).subscribe({
        next: () => {
          console.log('Producto creado:', this.producto);
          this.router.navigate(['/productos'], {
            state: { mensaje: 'Producto creado exitosamente', refrescar: true }
          });


        },
        error: () => alert('Error al crear producto')
      });
    }
  }

  cancelar(): void {
    this.router.navigate(['/productos']);
  }
}
