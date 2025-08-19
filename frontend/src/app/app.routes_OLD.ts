import { Routes } from '@angular/router';
import { Login } from './auth/login/login'; // Ajusta el path si es necesario
import { ListaProductos } from './productos/lista-productos/lista-productos';
import { FormularioProducto } from './productos/formulario-producto/formulario-producto';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'productos', pathMatch: 'full' },
  
  { path: 'productos', component: ListaProductos, canActivate: [authGuard]},

  //{ path: 'productos', component: ListaProductos, canActivate: [authGuard] }

  { path: 'productos/crear', component: FormularioProducto, canActivate: [authGuard] },
  { path: 'productos/:id/editar', component: FormularioProducto, canActivate: [authGuard] },
  //{ path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: '**', redirectTo: 'productos' }

];
