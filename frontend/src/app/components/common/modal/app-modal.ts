import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  templateUrl: './app-modal.html',
})
export class AppModal {
  readonly open = input(false);
  readonly closed = output<void>();
}
