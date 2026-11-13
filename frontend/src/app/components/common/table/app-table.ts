import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, input, output } from '@angular/core';
import { AppButton } from '../button/app-button';

@Component({
  selector: 'app-table',
  imports: [AppButton, NgTemplateOutlet],
  templateUrl: './app-table.html',
})
export class AppTable {
  readonly columns = input<{ key: string; label: string }[]>([]);
  readonly rows = input<Record<string, unknown>[]>([]);
  readonly page = input(0);
  readonly total = input(0);
  readonly totalPages = input(0);
  readonly rowActions = input<TemplateRef<{ $implicit: Record<string, unknown> }>>();

  readonly pageChange = output<number>();
}
