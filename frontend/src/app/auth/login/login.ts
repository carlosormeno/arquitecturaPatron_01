import { Component, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  login(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/productos';
    this.authService.login().subscribe({
      next: () => {
        //this.router.navigate(['/productos']);
        this.router.navigateByUrl(returnUrl);
      }
    });
  }
}
