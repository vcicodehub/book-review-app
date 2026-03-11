import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  template: `
    <div class="flex items-center gap-1">
      @for (star of stars; track star) {
        <button
          type="button"
          class="text-2xl transition hover:scale-110"
          [class.text-amber-400]="star <= value"
          [class.text-slate-300]="star > value"
          (click)="select(star)"
          [attr.aria-label]="'Select ' + star + ' stars'"
        >★</button>
      }
      <span class="ml-2 text-sm text-slate-500">{{ value }} / 5</span>
    </div>
  `
})
export class StarRatingComponent {
  @Input() value = 5;
  @Output() valueChange = new EventEmitter<number>();

  protected readonly stars = [1, 2, 3, 4, 5];

  protected select(value: number) {
    this.valueChange.emit(value);
  }
}
