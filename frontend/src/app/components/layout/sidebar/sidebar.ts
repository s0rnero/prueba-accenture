import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { AppMenu } from '../../common/menu/app-menu';
import { UserForm } from '../../user/user-form/user-form';

type View = {
  label: string;
  emoji: string;
  link: string | string[];
  active: boolean;
};

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, AppMenu, UserForm],
  templateUrl: './sidebar.html',
})
export class Sidebar {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  readonly menuOpen = signal(false);
  readonly editing = signal(false);

  readonly initials = computed(() => {
    const parts = (this.auth.session()?.displayName ?? '').split(' ').filter(Boolean);
    return (
      parts
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase() ?? '')
        .join('') || '?'
    );
  });

  readonly views = computed<View[]>(() => {
    const url = this.url();
    const list: View[] = [
      {
        label: 'Franquicias',
        emoji: '📊',
        link: '/franchises',
        active: url === '/franchises',
      },
    ];
    list.push({
      label: 'Sucursales',
      emoji: '🏬',
      link: '/branches',
      active: url === '/branches' || url.startsWith('/branches/'),
    });
    if (this.auth.isAdmin()) {
      list.push({
        label: 'Usuarios',
        emoji: '👥',
        link: '/users',
        active: url === '/users',
      });
    }
    return list;
  });

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  openProfile(): void {
    this.closeMenu();
    this.editing.set(true);
  }

  logout(): void {
    this.closeMenu();
    this.auth.logout().subscribe();
  }
}
