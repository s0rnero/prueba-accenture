import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AppButton } from '../../components/common/button/app-button';
import { AppInput } from '../../components/common/input/app-input';
import { AppModal } from '../../components/common/modal/app-modal';
import { GridCard } from '../../components/layout/content/grid-elements/grid-card';
import { GridCreateCard } from '../../components/layout/content/grid-elements/grid-create-card';
import { GridElements } from '../../components/layout/content/grid-elements/grid-elements';
import { AuthService } from '../../services/auth.service';
import { Franchise, FranchiseService } from '../../services/franchise.service';

@Component({
  selector: 'app-franchises',
  imports: [
    ReactiveFormsModule,
    AppButton,
    AppInput,
    AppModal,
    GridElements,
    GridCard,
    GridCreateCard,
  ],
  templateUrl: './franchises.html',
})
export class Franchises {
  private readonly service = inject(FranchiseService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly items = signal<Franchise[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<Franchise | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.page(0, 50).subscribe({
      next: (result) => {
        this.items.set(result.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron cargar las franquicias');
      },
    });
  }

  open(franchise: Franchise): void {
    void this.router.navigate(['/branches', franchise.id]);
  }

  startCreate(): void {
    this.editing.set(null);
    this.error.set(null);
    this.form.reset({ name: '' });
    this.modalOpen.set(true);
  }

  edit(franchise: Franchise): void {
    this.editing.set(franchise);
    this.error.set(null);
    this.form.setValue({ name: franchise.name });
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

    const name = this.form.getRawValue().name;
    const current = this.editing();
    const request = current
      ? this.service.rename(current.id, name, current.version)
      : this.service.create(name);

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
        this.error.set(error.error?.detail ?? 'No se pudo guardar la franquicia');
      },
    });
  }

  remove(franchise: Franchise): void {
    this.service.remove(franchise.id).subscribe({
      next: () => this.load(),
      error: (error: { error?: { detail?: string } }) =>
        this.error.set(error.error?.detail ?? 'No se pudo eliminar la franquicia'),
    });
  }
}
