import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthForm } from '../../components/auth/auth-form/auth-form';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [AuthForm, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(credentials: { email: string; password: string }): void {
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(credentials).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl('/franchises');
      },
      error: (error: { status?: number }) => {
        this.loading.set(false);
        this.error.set(
          error.status === 401 ? 'Correo o contraseña incorrectos' : 'No se pudo iniciar sesión',
        );
      },
    });
  }
}
