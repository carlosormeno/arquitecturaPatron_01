import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  //styleUrl: './app.scss'
  styleUrls: ['./app.scss']
})
export class App {
  protected title = 'frontend';

  private router = inject(Router);
  private authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
