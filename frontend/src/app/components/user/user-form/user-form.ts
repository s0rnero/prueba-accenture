import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { AppButton } from '../../common/button/app-button';
import { AppInput } from '../../common/input/app-input';
import { AppModal } from '../../common/modal/app-modal';

const PASSWORD_POLICY = /^(?=.*[0-9])(?=.*[^A-Za-z0-9\s]).{8,72}$/;

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, AppModal, AppInput, AppButton],
  templateUrl: './user-form.html',
})
export class UserForm {
  readonly open = input(false);
  readonly closed = output<void>();

  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    password: [''],
  });

  constructor() {
    effect(() => {
      if (!this.open()) {
        return;
      }
      this.error.set(null);
      this.form.reset({ displayName: this.auth.session()?.displayName ?? '', password: '' });
    });
  }

  cancel(): void {
    this.closed.emit();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { displayName, password } = this.form.getRawValue();
    if (password && !PASSWORD_POLICY.test(password)) {
      this.error.set('La contraseña debe tener 8+ caracteres, un número y un símbolo');
      return;
    }

    this.saving.set(true);
    this.auth.updateProfile(password ? { displayName, password } : { displayName }).subscribe({
      next: () => {
        this.saving.set(false);
        this.closed.emit();
      },
      error: (error: { error?: { detail?: string } }) => {
        this.saving.set(false);
        this.error.set(error.error?.detail ?? 'No se pudo guardar el perfil');
      },
    });
  }
}
