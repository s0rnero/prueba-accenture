import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'franchises' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./views/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./views/register/register').then((m) => m.Register),
  },
  {
    path: 'franchises',
    canActivate: [authGuard],
    loadComponent: () => import('./views/franchises/franchises').then((m) => m.Franchises),
  },
  {
    path: 'branches',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./views/branches/branches').then((m) => m.Branches),
  },
  {
    path: 'branches/:fid',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./views/branches/branches').then((m) => m.Branches),
  },
  {
    path: 'branches/:fid/:bid',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./views/branch-detail/branch-detail').then((m) => m.BranchDetail),
  },
  {
    path: 'users',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./views/users/users').then((m) => m.Users),
  },
  {
    path: 'sin-acceso',
    canActivate: [authGuard],
    loadComponent: () => import('./views/no-access/no-access').then((m) => m.NoAccess),
  },
  {
    path: '**',
    loadComponent: () => import('./views/not-found/not-found').then((m) => m.NotFound),
  },
];
