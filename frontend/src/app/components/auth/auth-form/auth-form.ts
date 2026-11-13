import { Component, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppButton } from '../../common/button/app-button';
import { AppInput } from '../../common/input/app-input';

@Component({
  selector: 'app-auth-form',
  imports: [ReactiveFormsModule, AppInput, AppButton],
  templateUrl: './auth-form.html',
})
export class AuthForm {
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly submitted = output<{ email: string; password: string }>();

  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.getRawValue());
  }
}
