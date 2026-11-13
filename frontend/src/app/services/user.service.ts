import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type UserRow = {
  id: string;
  email: string;
  displayName: string;
  enabled: boolean;
  roles: string[];
  version: number;
  createdAt: string;
};

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
};

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  page(page: number, size: number): Observable<Page<UserRow>> {
    return this.http.get<Page<UserRow>>('/api/v1/users', { params: { page, size } });
  }

  create(body: {
    email: string;
    password: string;
    displayName: string;
    roles: string[];
  }): Observable<UserRow> {
    return this.http.post<UserRow>('/api/v1/users', body);
  }

  update(
    id: string,
    body: { displayName?: string; enabled?: boolean; roles?: string[]; version: number },
  ): Observable<UserRow> {
    return this.http.patch<UserRow>(`/api/v1/users/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/users/${id}`);
  }
}
