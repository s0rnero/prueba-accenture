import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from './franchise.service';

export type Product = {
  id: string;
  branchId: string;
  branchName: string;
  name: string;
  stock: number;
  price: number;
  version: number;
};

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/franchises';

  page(franchiseId: string, page: number, size: number, search = ''): Observable<Page<Product>> {
    const params = new HttpParams().set('page', page).set('size', size).set('search', search);
    return this.http.get<Page<Product>>(`${this.base}/${franchiseId}/products`, { params });
  }

  create(
    franchiseId: string,
    branchId: string,
    name: string,
    stock: number,
    price: number,
  ): Observable<Product> {
    return this.http.post<Product>(`${this.base}/${franchiseId}/branches/${branchId}/products`, {
      name,
      stock,
      price,
    });
  }

  update(
    franchiseId: string,
    product: Product,
    name: string,
    stock: number,
    price: number,
  ): Observable<Product> {
    return this.http.patch<Product>(
      `${this.base}/${franchiseId}/branches/${product.branchId}/products/${product.id}`,
      { name, stock, price, version: product.version },
    );
  }

  updateStock(franchiseId: string, product: Product, stock: number): Observable<Product> {
    return this.http.patch<Product>(
      `${this.base}/${franchiseId}/branches/${product.branchId}/products/${product.id}/stock`,
      { stock, version: product.version },
    );
  }

  remove(franchiseId: string, product: Product): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/${franchiseId}/branches/${product.branchId}/products/${product.id}`,
    );
  }
}
