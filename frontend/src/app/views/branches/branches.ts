import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map } from 'rxjs';
import { AppButton } from '../../components/common/button/app-button';
import { AppInput } from '../../components/common/input/app-input';
import { AppModal } from '../../components/common/modal/app-modal';
import { GridCard } from '../../components/layout/content/grid-elements/grid-card';
import { GridCreateCard } from '../../components/layout/content/grid-elements/grid-create-card';
import { GridElements } from '../../components/layout/content/grid-elements/grid-elements';
import { AuthService } from '../../services/auth.service';
import { Branch, Franchise, FranchiseService } from '../../services/franchise.service';

type BranchCard = Branch & { franchiseId: string; franchiseName: string };

@Component({
  selector: 'app-branches',
  imports: [
    ReactiveFormsModule,
    AppButton,
    AppInput,
    AppModal,
    GridElements,
    GridCard,
    GridCreateCard,
  ],
  templateUrl: './branches.html',
})
export class Branches {
  readonly auth = inject(AuthService);
  private readonly api = inject(FranchiseService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly franchiseId = this.route.snapshot.paramMap.get('fid');

  readonly franchises = signal<Franchise[]>([]);
  readonly items = signal<BranchCard[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<BranchCard | null>(null);

  readonly form = this.fb.nonNullable.group({
    franchiseId: ['', [Validators.required]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.page(0, 100).subscribe({
      next: (page) => {
        this.franchises.set(page.content);
        const targets = this.franchiseId
          ? page.content.filter((franchise) => franchise.id === this.franchiseId)
          : page.content;

        if (targets.length === 0) {
          this.items.set([]);
          this.loading.set(false);
          return;
        }

        const requests = targets.map((franchise) =>
          this.api.branches(franchise.id).pipe(
            map((branches) =>
              branches.map((branch) => ({
                ...branch,
                franchiseId: franchise.id,
                franchiseName: franchise.name,
              })),
            ),
          ),
        );

        forkJoin(requests).subscribe({
          next: (groups) => {
            this.items.set(groups.flat());
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
            this.error.set('No se pudieron cargar las sucursales');
          },
        });
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron cargar las franquicias');
      },
    });
  }

  open(branch: BranchCard): void {
    void this.router.navigate(['/branches', branch.franchiseId, branch.id]);
  }

  startCreate(): void {
    this.editing.set(null);
    this.error.set(null);
    this.form.controls.franchiseId.enable();
    this.form.reset({
      franchiseId: this.franchiseId ?? this.franchises()[0]?.id ?? '',
      name: '',
    });
    this.modalOpen.set(true);
  }

  edit(branch: BranchCard): void {
    this.editing.set(branch);
    this.error.set(null);
    this.form.setValue({ franchiseId: branch.franchiseId, name: branch.name });
    this.form.controls.franchiseId.disable();
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

    const { franchiseId, name } = this.form.getRawValue();
    const current = this.editing();
    const request = current
      ? this.api.renameBranch(current.franchiseId, current.id, name, current.version)
      : this.api.createBranch(franchiseId, name);

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
        this.error.set(error.error?.detail ?? 'No se pudo guardar la sucursal');
      },
    });
  }

  franchiseName(): string {
    return this.franchises().find((franchise) => franchise.id === this.franchiseId)?.name ?? '';
  }

  back(): void {
    void this.router.navigate(['/franchises']);
  }

  remove(branch: BranchCard): void {
    this.api.removeBranch(branch.franchiseId, branch.id).subscribe({
      next: () => this.load(),
      error: (error: { error?: { detail?: string } }) =>
        this.error.set(error.error?.detail ?? 'No se pudo eliminar la sucursal'),
    });
  }
}
