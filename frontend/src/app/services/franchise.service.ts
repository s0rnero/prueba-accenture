import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
};

export type Franchise = {
  id: string;
  name: string;
  version: number;
  branchCount: number;
  productCount: number;
  totalStock: number;
};

export type Branch = {
  id: string;
  name: string;
  version: number;
  productCount: number;
};

@Injectable({ providedIn: 'root' })
export class FranchiseService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/franchises';

  page(page: number, size: number): Observable<Page<Franchise>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Franchise>>(this.base, { params });
  }

  get(id: string): Observable<Franchise> {
    return this.http.get<Franchise>(`${this.base}/${id}`);
  }

  create(name: string): Observable<Franchise> {
    return this.http.post<Franchise>(this.base, { name });
  }

  rename(id: string, name: string, version: number): Observable<Franchise> {
    return this.http.patch<Franchise>(`${this.base}/${id}?version=${version}`, { name });
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  branches(id: string): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.base}/${id}/branches`);
  }

  createBranch(id: string, name: string): Observable<Branch> {
    return this.http.post<Branch>(`${this.base}/${id}/branches`, { name });
  }

  renameBranch(
    id: string,
    branchId: string,
    name: string,
    version: number,
  ): Observable<Branch> {
    return this.http.patch<Branch>(`${this.base}/${id}/branches/${branchId}`, { name, version });
  }

  removeBranch(id: string, branchId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}/branches/${branchId}`);
  }
}
