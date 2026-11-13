import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AppButton } from '../../components/common/button/app-button';
import { AppInput } from '../../components/common/input/app-input';
import { AppModal } from '../../components/common/modal/app-modal';
import { AppTable } from '../../components/common/table/app-table';
import { AuthService } from '../../services/auth.service';
import { Branch, FranchiseService } from '../../services/franchise.service';
import { Product, ProductService } from '../../services/product.service';

@Component({
  selector: 'app-branch-detail',
  imports: [ReactiveFormsModule, AppButton, AppInput, AppModal, AppTable],
  templateUrl: './branch-detail.html',
})
export class BranchDetail {
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(FranchiseService);
  private readonly productsApi = inject(ProductService);

  readonly franchiseId = this.route.snapshot.paramMap.get('fid') ?? '';
  readonly branchId = this.route.snapshot.paramMap.get('bid') ?? '';

  readonly branch = signal<Branch | null>(null);
  readonly products = signal<Product[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<Product | null>(null);
  readonly page = signal(0);
  readonly size = 10;

  readonly columns = [
    { key: 'name', label: 'Producto' },
    { key: 'stock', label: 'Stock' },
    { key: 'price', label: 'Precio' },
  ];

  readonly total = computed(() => this.products().length);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.products().length / this.size)));
  readonly rows = computed(() => {
    const start = this.page() * this.size;
    return this.products()
      .slice(start, start + this.size)
      .map((product) => ({
        id: product.id,
        name: product.name,
        stock: product.stock,
        price: (product.price ?? 0).toFixed(2),
      }));
  });

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    stock: [0, [Validators.required, Validators.min(0)]],
    price: [0, [Validators.required, Validators.min(0)]],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);

    this.api.branches(this.franchiseId).subscribe({
      next: (branches) => this.branch.set(branches.find((b) => b.id === this.branchId) ?? null),
      error: () => this.error.set('No se pudo cargar la sucursal'),
    });

    this.productsApi.page(this.franchiseId, 0, 500).subscribe({
      next: (page) => {
        this.products.set(page.content.filter((product) => product.branchId === this.branchId));
        this.loading.set(false);
        if (this.page() >= this.totalPages()) {
          this.page.set(0);
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron cargar los productos');
      },
    });
  }

  goTo(page: number): void {
    this.page.set(page);
  }

  startCreate(): void {
    this.editing.set(null);
    this.error.set(null);
    this.form.reset({ name: '', stock: 0, price: 0 });
    this.modalOpen.set(true);
  }

  edit(product: Product): void {
    this.editing.set(product);
    this.error.set(null);
    this.form.setValue({ name: product.name, stock: product.stock, price: product.price });
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

    const { name, stock, price } = this.form.getRawValue();
    const current = this.editing();
    const request = current
      ? this.productsApi.update(this.franchiseId, current, name, stock, price)
      : this.productsApi.create(this.franchiseId, this.branchId, name, stock, price);

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
        this.error.set(error.error?.detail ?? 'No se pudo guardar el producto');
      },
    });
  }

  remove(product: Product): void {
    this.productsApi.remove(this.franchiseId, product).subscribe({
      next: () => this.load(),
      error: (error: { error?: { detail?: string } }) =>
        this.error.set(error.error?.detail ?? 'No se pudo eliminar el producto'),
    });
  }

  find(id: string): Product {
    return this.products().find((product) => product.id === id)!;
  }

  back(): void {
    void this.router.navigate(['/branches']);
  }
}
