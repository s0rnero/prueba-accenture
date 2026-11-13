import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AppButton } from '../../components/common/button/app-button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-not-found',
  imports: [AppButton],
  templateUrl: './not-found.html',
})
export class NotFound {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly destination = computed(() => (this.auth.isLoggedIn() ? '/franchises' : '/login'));

  readonly action = computed(() => (this.auth.isLoggedIn() ? 'Ir al inicio' : 'Iniciar sesión'));

  go(): void {
    void this.router.navigateByUrl(this.destination());
  }
}
