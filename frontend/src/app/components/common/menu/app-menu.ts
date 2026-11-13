import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-menu',
  templateUrl: './app-menu.html',
})
export class AppMenu {
  readonly open = input(false);
  readonly closed = output<void>();
}
