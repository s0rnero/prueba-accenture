import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-button',
  templateUrl: './app-button.html',
})
export class AppButton {
  readonly label = input('');
  readonly type = input<'button' | 'submit'>('button');
  readonly variant = input<'primary' | 'ghost' | 'danger'>('primary');
  readonly disabled = input(false);
  readonly full = input(false);
  readonly pressed = output<void>();

  classes(): string {
    const base =
      'inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-40';
    const variants: Record<string, string> = {
      primary: 'bg-accenture text-white hover:bg-accenture-violet',
      ghost: 'border border-neutral-300 bg-white text-accenture-ink hover:border-accenture',
      danger: 'border border-red-200 bg-white text-red-600 hover:bg-red-50',
    };
    return `${base} ${variants[this.variant()]} ${this.full() ? 'w-full' : ''}`;
  }
}
