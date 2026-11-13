import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-grid-create-card',
  templateUrl: './grid-create-card.html',
})
export class GridCreateCard {
  readonly label = input.required<string>();
  readonly pressed = output<void>();
}
