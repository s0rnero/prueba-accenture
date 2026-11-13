import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppButton } from '../../components/common/button/app-button';
import { AppInput } from '../../components/common/input/app-input';
import { AppModal } from '../../components/common/modal/app-modal';
import { AppTable } from '../../components/common/table/app-table';
import { UserRow, UserService } from '../../services/user.service';

const PASSWORD_POLICY = /^(?=.*[0-9])(?=.*[^A-Za-z0-9\s]).{8,72}$/;

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, AppButton, AppInput, AppModal, AppTable],
  templateUrl: './users.html',
})
export class Users {
  private readonly service = inject(UserService);
  private readonly fb = inject(FormBuilder);

  readonly columns = [
    { key: 'email', label: 'Correo' },
    { key: 'displayName', label: 'Nombre' },
    { key: 'roles', label: 'Roles' },
    { key: 'enabled', label: 'Estado' },
  ];

  readonly page = signal(0);
  readonly size = 5;
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly users = signal<UserRow[]>([]);
  readonly error = signal<string | null>(null);
  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<UserRow | null>(null);

  readonly rows = computed(() =>
    this.users().map((user) => ({
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      roles: user.roles.join(', '),
      enabled: user.enabled ? 'Activo' : 'Inactivo',
    })),
  );

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    displayName: ['', [Validators.required]],
    role: ['USER', [Validators.required]],
    enabled: [true],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.service.page(this.page(), this.size).subscribe({
      next: (result) => {
        this.users.set(result.content);
        this.total.set(result.total);
        this.totalPages.set(result.totalPages);
      },
      error: () => this.error.set('No se pudo cargar la lista de usuarios'),
    });
  }

  goTo(page: number): void {
    this.page.set(page);
    this.load();
  }

  startCreate(): void {
    this.editing.set(null);
    this.error.set(null);
    this.form.controls.email.enable();
    this.form.controls.password.setValidators([
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(PASSWORD_POLICY),
    ]);
    this.form.controls.password.updateValueAndValidity();
    this.form.reset({ email: '', password: '', displayName: '', role: 'USER', enabled: true });
    this.modalOpen.set(true);
  }

  edit(user: UserRow): void {
    this.editing.set(user);
    this.error.set(null);
    this.form.controls.email.disable();
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
    this.form.reset({
      email: user.email,
      password: '',
      displayName: user.displayName,
      role: user.roles[0] ?? 'USER',
      enabled: user.enabled,
    });
    this.modalOpen.set(true);
  }

  cancel(): void {
    this.modalOpen.set(false);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const current = this.editing();
    const request = current
      ? this.service.update(current.id, {
          displayName: value.displayName,
          roles: [value.role],
          enabled: value.enabled,
          version: current.version,
        })
      : this.service.create({
          email: value.email,
          password: value.password,
          displayName: value.displayName,
          roles: [value.role],
        });

    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.error.set(null);
        this.modalOpen.set(false);
        this.load();
      },
      error: (error: { error?: { detail?: string } }) => {
        this.saving.set(false);
        this.error.set(error.error?.detail ?? 'No se pudo guardar el usuario');
      },
    });
  }

  remove(user: UserRow): void {
    this.service.remove(user.id).subscribe({
      next: () => this.load(),
      error: (error: { error?: { detail?: string } }) =>
        this.error.set(error.error?.detail ?? 'No se pudo eliminar el usuario'),
    });
  }

  find(id: string): UserRow {
    return this.users().find((user) => user.id === id)!;
  }
}
