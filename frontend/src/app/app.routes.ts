import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'productos', pathMatch: 'full' },

  {
    path: 'productos',
    canMatch: [authGuard],
    canActivate: [authGuard],
    loadComponent: () => import('./productos/lista-productos/lista-productos').then(m => m.ListaProductos)
  },
  {
    path: 'productos/crear',
    canMatch: [authGuard],
    canActivate: [authGuard],
    loadComponent: () => import('./productos/formulario-producto/formulario-producto').then(m => m.FormularioProducto)
  },
  {
    path: 'productos/:id/editar',
    canMatch: [authGuard],
    canActivate: [authGuard],
    loadComponent: () => import('./productos/formulario-producto/formulario-producto').then(m => m.FormularioProducto)
  },

  // Pública
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then(m => m.Login)
  },

  { path: '**', redirectTo: 'productos' }
];
