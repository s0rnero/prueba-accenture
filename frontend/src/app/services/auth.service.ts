import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, of, switchMap, tap } from 'rxjs';

export type Session = {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
  permissions: string[];
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly session = signal<Session | null>(null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.roles.includes('ADMIN') ?? false);

  bootstrap(): Observable<unknown> {
    return this.http.get<void>('/api/v1/auth/csrf').pipe(
      switchMap(() => this.me()),
      catchError(() => {
        this.session.set(null);
        return of(null);
      }),
    );
  }

  login(credentials: { email: string; password: string }): Observable<Session> {
    return this.http
      .post<Session>('/api/v1/auth/login', credentials)
      .pipe(tap((session) => this.session.set(session)));
  }

  register(payload: { displayName: string; email: string; password: string }): Observable<Session> {
    return this.http
      .post<Session>('/api/v1/auth/register', payload)
      .pipe(tap((session) => this.session.set(session)));
  }

  me(): Observable<Session> {
    return this.http
      .get<Session>('/api/v1/auth/me')
      .pipe(tap((session) => this.session.set(session)));
  }

  updateProfile(payload: { displayName: string; password?: string }): Observable<Session> {
    return this.http
      .patch<Session>('/api/v1/auth/me', payload)
      .pipe(tap((session) => this.session.set(session)));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', {}).pipe(
      tap(() => {
        this.session.set(null);
        this.router.navigateByUrl('/login');
      }),
    );
  }

  clear(): void {
    this.session.set(null);
  }
}
