import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Send, Trash2, NotepadText, Loader2, Check } from 'lucide-angular';
import { ApiService } from '../../core/services/api.service';
import { DocumentRecord } from '../../core/services/api.models';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  template: `
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl font-bold text-slate-900">Saved history</h2>
          <p class="mt-1 text-sm text-slate-500">Summaries and reviews stored in your local database.</p>
        </div>
      </div>

      @if (isLoading()) {
        <p class="mt-6 text-slate-600">Loading history...</p>
      } @else if (!documents().length) {
        <p class="mt-6 text-slate-600">No saved items yet.</p>
      } @else {
        <div class="mt-6 overflow-hidden rounded-2xl border border-slate-200">
          <table class="min-w-full table-fixed divide-y divide-slate-200 text-sm">
            <thead class="bg-slate-50">
              <tr>
                <th class="w-[440px] px-4 py-3 text-left font-semibold text-slate-700">Title</th>
                <th class="px-4 py-3 text-left font-semibold text-slate-700">Author</th>
                <th class="px-4 py-3 text-left font-semibold text-slate-700">Saved review</th>
                <th class="px-4 py-3 text-left font-semibold text-slate-700">Created</th>
                <th class="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200 bg-white">
              @for (doc of documents(); track doc.id) {
                <tr>
                  <td class="w-[440px] px-4 py-3 text-slate-800">
                    <span class="line-clamp-2 block">{{ doc.bookTitle || doc.originalFileName || doc.fileName }}</span>
                  </td>
                  <td class="px-4 py-3 text-slate-600">{{ doc.author || '—' }}</td>
                  <td class="px-4 py-3 text-slate-600">{{ doc.review ? (doc.review.starRating + ' stars') : 'New' }}</td>
                  <td class="px-4 py-3 text-slate-600">{{ formatDate(doc.createdAt) }}</td>
                  <td class="px-4 py-3">
                    <div class="flex items-center justify-end gap-2">
                      @if (doc.review?.posted) {
                        <span title="Posted to Amazon" class="flex items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 p-2 text-emerald-600">
                          <lucide-icon [img]="CheckIcon" [size]="18"></lucide-icon>
                        </span>
                      } @else if (doc.review && !doc.review.posted) {
                        <button
                          type="button"
                          (click)="markAsPosted(doc)"
                          [disabled]="postingReviewId() === doc.review!.id"
                          [title]="postingReviewId() === doc.review!.id ? 'Updating...' : 'Mark as posted'"
                          class="flex items-center justify-center rounded-lg border border-emerald-600 bg-white p-2 text-emerald-600 hover:bg-emerald-50 disabled:opacity-50"
                        >
                          @if (postingReviewId() === doc.review!.id) {
                            <lucide-icon [img]="Loader2Icon" [size]="18" class="animate-spin"></lucide-icon>
                          } @else {
                            <lucide-icon [img]="SendIcon" [size]="18"></lucide-icon>
                          }
                        </button>
                      }
                      <button
                        type="button"
                        (click)="deleteItem(doc)"
                        [disabled]="deletingId() === doc.id"
                        title="Delete"
                        class="flex items-center justify-center rounded-lg border border-red-300 bg-white p-2 text-red-600 hover:bg-red-50 disabled:opacity-50"
                      >
                        @if (deletingId() === doc.id) {
                          <lucide-icon [img]="Loader2Icon" [size]="18" class="animate-spin"></lucide-icon>
                        } @else {
                          <lucide-icon [img]="Trash2Icon" [size]="18"></lucide-icon>
                        }
                      </button>
                      <a
                        [routerLink]="['/summary', doc.id]"
                        title="Open"
                        class="flex items-center justify-center rounded-lg bg-slate-900 p-2 text-white hover:bg-slate-800"
                      >
                        <lucide-icon [img]="NotepadTextIcon" [size]="18"></lucide-icon>
                      </a>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `
})
export class HistoryPageComponent {
  private readonly api = inject(ApiService);

  protected readonly SendIcon = Send;
  protected readonly CheckIcon = Check;
  protected readonly Trash2Icon = Trash2;
  protected readonly NotepadTextIcon = NotepadText;
  protected readonly Loader2Icon = Loader2;

  protected readonly documents = signal<DocumentRecord[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly postingReviewId = signal<number | null>(null);
  protected readonly deletingId = signal<number | null>(null);

  protected formatDate(isoString: string): string {
    if (!isoString) return '';
    try {
      return new Date(isoString).toLocaleDateString(undefined, { dateStyle: 'medium' });
    } catch {
      return isoString;
    }
  }

  protected markAsPosted(doc: DocumentRecord) {
    const reviewId = doc.review?.id;
    if (!reviewId) return;

    this.postingReviewId.set(reviewId);
    this.api.markReviewPosted(reviewId).subscribe({
      next: updated => {
        this.documents.update(docs =>
          docs.map(d =>
            d.id === doc.id && d.review?.id === reviewId
              ? { ...d, review: { ...d.review!, ...updated } }
              : d
          )
        );
        this.postingReviewId.set(null);
      },
      error: () => this.postingReviewId.set(null)
    });
  }

  protected deleteItem(doc: DocumentRecord) {
    this.deletingId.set(doc.id);
    const request$ = doc.review?.id
      ? this.api.deleteReview(doc.review.id)
      : this.api.deleteDocument(doc.id);

    request$.subscribe({
      next: () => {
        if (doc.review?.id) {
          this.documents.update(docs =>
            docs.map(d => (d.id === doc.id ? { ...d, review: null } : d))
          );
        } else {
          this.documents.update(docs => docs.filter(d => d.id !== doc.id));
        }
        this.deletingId.set(null);
      },
      error: () => this.deletingId.set(null)
    });
  }

  constructor() {
    this.api.getDocuments().subscribe({
      next: documents => {
        this.documents.set(documents);
        this.isLoading.set(false);
      },
      error: error => {
        console.error(error);
        this.isLoading.set(false);
      }
    });
  }
}
