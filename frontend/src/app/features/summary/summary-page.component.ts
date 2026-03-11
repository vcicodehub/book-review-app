import { Component, computed, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule, Copy, ExternalLink } from 'lucide-angular';
import { forkJoin, switchMap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { DocumentRecord } from '../../core/services/api.models';
import { StarRatingComponent } from '../../shared/components/star-rating.component';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StarRatingComponent, LucideAngularModule],
  template: `
    @if (isLoading()) {
      <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <p class="text-slate-600">Loading summary...</p>
      </div>
    } @else if (document()) {
      <section class="flex flex-col gap-6">
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 class="text-2xl font-bold text-slate-900">{{ titleText() }}</h2>
                <p class="mt-1 text-sm text-slate-500">{{ subtitleText() }}</p>
              </div>
              <span
                class="rounded-full px-3 py-1 text-xs font-medium"
                [class]="document()?.review?.posted
                  ? 'bg-emerald-100 text-emerald-700'
                  : document()?.review
                    ? 'bg-amber-100 text-amber-700'
                    : 'bg-slate-100 text-slate-600'"
              >{{ statusText() }}</span>
            </div>

            <div class="mt-5 rounded-xl bg-slate-50 p-4 text-sm leading-7 text-slate-700 whitespace-pre-wrap">{{ document()!.summary }}</div>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h3 class="text-xl font-semibold text-slate-900">Amazon review draft</h3>
              <p class="mt-1 text-sm text-slate-500">Generate, edit, and save the review locally.</p>
            </div>
          </div>

          <form class="mt-6 space-y-5" [formGroup]="reviewForm" (ngSubmit)="saveReview()">
            <div>
              <label class="mb-2 block text-sm font-medium text-slate-700">Star rating</label>
              <app-star-rating [value]="reviewForm.controls.starRating.value" (valueChange)="reviewForm.controls.starRating.setValue($event)" />
            </div>

            <div>
              <label class="mb-2 block text-sm font-medium text-slate-700">Tone</label>
              <select formControlName="tone" class="w-full rounded-xl border border-slate-300 px-4 py-3">
                <option value="balanced">Balanced</option>
                <option value="warm">Warm</option>
                <option value="professional">Professional</option>
                <option value="enthusiastic">Enthusiastic</option>
              </select>
            </div>

            @if (document()?.category === 'kindle') {
            <div>
              <label class="mb-2 block text-sm font-medium text-slate-700">Book screenshots or images</label>
              <p class="mb-2 text-xs text-slate-500">Upload Kindle highlights, passages, or book-related images. The AI will use them to enrich your review. Unlimited uploads.</p>
              <div class="flex flex-wrap items-center gap-3">
                <label class="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100">
                  <span>Choose images</span>
                  <input class="hidden" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple (change)="onImagesSelected($event)" />
                </label>
                <div
                  tabindex="0"
                  role="button"
                  (paste)="onImagePasted($event)"
                  (click)="focusPasteZone($event)"
                  class="cursor-pointer rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600 outline-none transition hover:border-slate-400 hover:bg-slate-100 focus:ring-2 focus:ring-slate-300"
                >
                  Or paste image (Ctrl+V)
                </div>
                @if (isUploadingImage()) {
                  <span class="text-sm text-slate-500">Uploading...</span>
                }
              </div>
              @if (document()?.images && document()!.images!.length > 0) {
                <div class="mt-3 flex flex-wrap gap-2">
                  @for (img of document()!.images!; track img.id) {
                    <div class="relative inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2">
                      <button
                        type="button"
                        (click)="openLightbox(getDocumentImageUrl(document()!.id, img.id))"
                        class="cursor-pointer overflow-hidden rounded focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-1"
                      >
                        <img [src]="getDocumentImageUrl(document()!.id, img.id)" [alt]="img.originalFileName ?? 'Image'" class="h-12 w-12 rounded object-cover transition hover:opacity-90" />
                      </button>
                      <span class="max-w-24 truncate text-xs text-slate-600">{{ img.originalFileName ?? 'Image' }}</span>
                      <button type="button" (click)="deleteImage(img.id)" class="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-red-600" title="Remove">×</button>
                    </div>
                  }
                </div>
              }
            </div>
            <div>
              <label class="mb-2 block text-sm font-medium text-slate-700">Notes for AI</label>
              <textarea formControlName="kindleNotes" rows="4" class="w-full rounded-xl border border-slate-300 px-4 py-3" placeholder="Enter your notes about the book (plot points, themes, favorite moments, etc.) to help the AI generate a more personalized review."></textarea>
              <p class="mt-1 text-xs text-slate-500">These notes will be passed to the AI when generating the review.</p>
            </div>
            }

            <div class="flex flex-wrap gap-3">
              <button
                type="button"
                (click)="generateReview()"
                [disabled]="isGenerating()"
                class="rounded-xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:bg-slate-400"
              >
                {{ isGenerating() ? 'Generating...' : 'Generate review' }}
              </button>
              @if (document()?.amazonUrl) {
                <a
                  [href]="document()!.amazonUrl!"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center gap-2 rounded-xl border border-amber-300 bg-amber-50 px-5 py-3 text-sm font-medium text-amber-800 transition hover:bg-amber-100"
                >
                  <lucide-icon [img]="ExternalLinkIcon" [size]="16"></lucide-icon>
                  View on Amazon
                </a>
              }
              @if (hasReviewContent()) {
                <button
                  type="button"
                  (click)="shortenReview()"
                  [disabled]="isShortening()"
                  class="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:bg-slate-100 disabled:text-slate-400"
                >
                  {{ isShortening() ? 'Shortening...' : 'Shorten review' }}
                </button>
                <button
                  type="button"
                  (click)="humanizeReview()"
                  [disabled]="isHumanizing()"
                  class="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:bg-slate-100 disabled:text-slate-400"
                >
                  {{ isHumanizing() ? 'Humanizing...' : 'Humanize' }}
                </button>
              }
            </div>

            <div>
              <div class="mb-2 flex items-center justify-between">
                <label class="text-sm font-medium text-slate-700">Review title</label>
                <button
                  type="button"
                  (click)="copyReviewTitle()"
                  [disabled]="!reviewForm.controls.reviewTitle.value.trim()"
                  title="Copy review title"
                  class="flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-800 disabled:opacity-50 disabled:hover:bg-transparent"
                >
                  <lucide-icon [img]="CopyIcon" [size]="16"></lucide-icon>
                  {{ copyTitleSuccess() ? 'Copied!' : 'Copy' }}
                </button>
              </div>
              <input formControlName="reviewTitle" type="text" class="w-full rounded-xl border border-slate-300 px-4 py-3" placeholder="Generated title" />
            </div>

            <div>
              <div class="mb-2 flex items-center justify-between">
                <label class="text-sm font-medium text-slate-700">Review body</label>
                <button
                  type="button"
                  (click)="copyReviewBody()"
                  [disabled]="!hasReviewContent()"
                  title="Copy review body"
                  class="flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-800 disabled:opacity-50 disabled:hover:bg-transparent"
                >
                  <lucide-icon [img]="CopyIcon" [size]="16"></lucide-icon>
                  {{ copySuccess() ? 'Copied!' : 'Copy' }}
                </button>
              </div>
              <textarea formControlName="reviewBody" rows="10" class="w-full rounded-xl border border-slate-300 px-4 py-3" placeholder="Generated review body"></textarea>
            </div>

            <div class="flex flex-col gap-2">
              <div class="flex items-center gap-3">
                <input
                  formControlName="posted"
                  type="checkbox"
                  id="posted"
                  class="h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                />
                <label for="posted" class="text-sm font-medium text-slate-700">Posted to Amazon</label>
              </div>
              @if (reviewForm.controls.posted.value && reviewForm.controls.postedAt.value) {
                <p class="text-sm text-slate-500">Posted on {{ formatPostedDate(reviewForm.controls.postedAt.value) }}</p>
              }
            </div>

            @if (message()) {
              <div class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                {{ message() }}
              </div>
            }

            @if (errorMessage()) {
              <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {{ errorMessage() }}
              </div>
            }

            <button
              type="submit"
              [disabled]="isSaving() || reviewForm.invalid"
              class="rounded-xl bg-emerald-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-500 disabled:bg-emerald-300"
            >
              {{ isSaving() ? 'Saving...' : 'Save review' }}
            </button>
          </form>
        </div>

        @if (lightboxImageUrl()) {
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Image lightbox"
            (click)="closeLightbox()"
            class="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          >
            <button
              type="button"
              (click)="closeLightbox(); $event.stopPropagation()"
              class="absolute right-4 top-4 rounded-full bg-white/10 px-3 py-1.5 text-white transition hover:bg-white/20"
            >
              Close
            </button>
            <img
              [src]="lightboxImageUrl()!"
              alt="Enlarged view"
              (click)="$event.stopPropagation()"
              class="max-h-[90vh] max-w-full rounded-lg object-contain shadow-2xl"
            />
          </div>
        }
      </section>
    }
  `
})
export class SummaryPageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly isLoading = signal(true);
  protected readonly isUploadingImage = signal(false);
  protected readonly isGenerating = signal(false);
  protected readonly isShortening = signal(false);
  protected readonly isHumanizing = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly CopyIcon = Copy;
  protected readonly ExternalLinkIcon = ExternalLink;
  protected readonly document = signal<DocumentRecord | null>(null);
  protected readonly message = signal('');
  protected readonly errorMessage = signal('');
  protected readonly copySuccess = signal(false);
  protected readonly copyTitleSuccess = signal(false);
  protected readonly lightboxImageUrl = signal<string | null>(null);

  @HostListener('document:keydown.escape')
  protected onEscape() {
    this.closeLightbox();
  }

  protected openLightbox(url: string) {
    this.lightboxImageUrl.set(url);
  }

  protected closeLightbox() {
    this.lightboxImageUrl.set(null);
  }

  protected readonly reviewForm = this.fb.nonNullable.group({
    reviewId: [0],
    starRating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    tone: ['balanced', Validators.required],
    kindleNotes: [''],
    reviewTitle: [''],
    reviewBody: ['', Validators.required],
    posted: [false],
    postedAt: ['' as string]
  });

  protected readonly titleText = computed(() => this.document()?.bookTitle || this.document()?.originalFileName || 'Untitled PDF');
  protected readonly statusText = computed(() => {
    const review = this.document()?.review;
    if (review?.posted) return 'Posted';
    if (review) return 'Draft';
    return 'New';
  });
  protected readonly subtitleText = computed(() => {
    const doc = this.document();
    const parts = [doc?.author];
    if (doc?.createdAt) {
      parts.push(this.formatDate(doc.createdAt));
    }
    return parts.join(' • ');
  });

  protected getDocumentImageUrl(documentId: number, imageId: number): string {
    return this.api.getDocumentImageUrl(documentId, imageId);
  }

  protected focusPasteZone(event: Event) {
    (event.currentTarget as HTMLElement)?.focus();
  }

  protected onImagePasted(event: ClipboardEvent) {
    const items = event.clipboardData?.items;
    if (!items || !this.document()) return;

    const imageItem = Array.from(items).find(item => item.type.startsWith('image/'));
    if (!imageItem) return;

    event.preventDefault();
    const file = imageItem.getAsFile();
    if (!file) return;

    this.isUploadingImage.set(true);
    this.api.uploadDocumentImage(this.document()!.id, file).subscribe({
      next: () => {
        this.api.getDocument(this.document()!.id).subscribe({
          next: doc => {
            this.document.set(doc);
            this.isUploadingImage.set(false);
          },
          error: () => this.isUploadingImage.set(false)
        });
      },
      error: err => {
        console.error(err);
        this.errorMessage.set(err.error?.message ?? 'Failed to upload pasted image.');
        this.isUploadingImage.set(false);
      }
    });
  }

  protected onImagesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files?.length || !this.document()) return;

    this.isUploadingImage.set(true);
    const uploads = Array.from(files).map(file =>
      this.api.uploadDocumentImage(this.document()!.id, file)
    );

    forkJoin(uploads).subscribe({
      next: () => {
        this.api.getDocument(this.document()!.id).subscribe({
          next: doc => {
            this.document.set(doc);
            this.isUploadingImage.set(false);
          },
          error: () => this.isUploadingImage.set(false)
        });
        input.value = '';
      },
      error: err => {
        console.error(err);
        this.errorMessage.set(err.error?.message ?? 'Failed to upload images.');
        this.isUploadingImage.set(false);
      }
    });
  }

  protected deleteImage(imageId: number) {
    const doc = this.document();
    if (!doc) return;
    this.api.deleteDocumentImage(doc.id, imageId).subscribe({
      next: () => {
        this.api.getDocument(doc.id).subscribe({ next: d => this.document.set(d) });
      },
      error: err => this.errorMessage.set(err.error?.message ?? 'Failed to delete image.')
    });
  }

  protected hasReviewContent(): boolean {
    const body = this.reviewForm.controls.reviewBody.value;
    return typeof body === 'string' && body.trim().length > 0;
  }

  protected async copyReviewTitle() {
    const title = this.reviewForm.controls.reviewTitle.value;
    if (!title?.trim()) return;

    try {
      await navigator.clipboard.writeText(title);
      this.copyTitleSuccess.set(true);
      setTimeout(() => this.copyTitleSuccess.set(false), 2000);
    } catch {
      this.errorMessage.set('Could not copy to clipboard.');
    }
  }

  protected async copyReviewBody() {
    const body = this.reviewForm.controls.reviewBody.value;
    if (!body?.trim()) return;

    try {
      await navigator.clipboard.writeText(body);
      this.copySuccess.set(true);
      setTimeout(() => this.copySuccess.set(false), 2000);
    } catch {
      this.errorMessage.set('Could not copy to clipboard.');
    }
  }

  protected formatDate(isoString: string): string {
    if (!isoString) return '';
    try {
      return new Date(isoString).toLocaleDateString(undefined, { dateStyle: 'short' });
    } catch {
      return isoString;
    }
  }

  protected formatPostedDate(isoString: string): string {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(undefined, { dateStyle: 'short' });
    } catch {
      return isoString;
    }
  }

  constructor() {
    this.route.paramMap.pipe(
      switchMap(params => this.api.getDocument(Number(params.get('id'))))
    ).subscribe({
      next: document => {
        this.document.set(document);
        if (document.review) {
          this.reviewForm.patchValue({
            reviewId: document.review.id ?? 0,
            starRating: document.review.starRating,
            tone: document.review.tone || 'balanced',
            kindleNotes: document.review.notesForAi ?? '',
            reviewTitle: document.review.reviewTitle || '',
            reviewBody: document.review.reviewBody,
            posted: document.review.posted ?? false,
            postedAt: document.review.postedAt ?? ''
          });
        }
        this.isLoading.set(false);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Could not load the selected document.');
        this.isLoading.set(false);
      }
    });
  }

  protected generateReview() {
    const document = this.document();
    if (!document) {
      return;
    }

    this.message.set('');
    this.errorMessage.set('');
    this.isGenerating.set(true);

    const notes = document.category === 'kindle' ? this.reviewForm.controls.kindleNotes.value : undefined;
    this.api.generateReview({
      documentId: document.id,
      starRating: this.reviewForm.controls.starRating.value,
      tone: this.reviewForm.controls.tone.value,
      summary: document.summary,
      notes: notes?.trim() || undefined
    }).subscribe({
      next: response => {
        this.reviewForm.patchValue({
          reviewTitle: response.reviewTitle,
          reviewBody: response.reviewBody
        });
        this.message.set('Review generated. You can edit it before saving.');
        this.isGenerating.set(false);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Failed to generate the review.');
        this.isGenerating.set(false);
      }
    });
  }

  protected shortenReview() {
    const reviewTitle = this.reviewForm.controls.reviewTitle.value;
    const reviewBody = this.reviewForm.controls.reviewBody.value;
    if (!reviewBody?.trim()) {
      return;
    }

    this.message.set('');
    this.errorMessage.set('');
    this.isShortening.set(true);

    this.api.shortenReview(reviewTitle, reviewBody).subscribe({
      next: response => {
        this.reviewForm.patchValue({
          reviewTitle: response.reviewTitle,
          reviewBody: response.reviewBody
        });
        this.message.set('Review shortened. You can edit it before saving.');
        this.isShortening.set(false);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Failed to shorten the review.');
        this.isShortening.set(false);
      }
    });
  }

  protected humanizeReview() {
    const reviewTitle = this.reviewForm.controls.reviewTitle.value;
    const reviewBody = this.reviewForm.controls.reviewBody.value;
    if (!reviewBody?.trim()) {
      return;
    }

    this.message.set('');
    this.errorMessage.set('');
    this.isHumanizing.set(true);

    this.api.humanizeReview(reviewTitle, reviewBody).subscribe({
      next: response => {
        this.reviewForm.patchValue({
          reviewTitle: response.reviewTitle,
          reviewBody: response.reviewBody
        });
        this.message.set('Review humanized. You can edit it before saving.');
        this.isHumanizing.set(false);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Failed to humanize the review.');
        this.isHumanizing.set(false);
      }
    });
  }

  protected saveReview() {
    const document = this.document();
    if (!document) {
      return;
    }

    this.message.set('');
    this.errorMessage.set('');
    this.isSaving.set(true);

    const payload = {
      documentId: document.id,
      starRating: this.reviewForm.controls.starRating.value,
      tone: this.reviewForm.controls.tone.value,
      reviewTitle: this.reviewForm.controls.reviewTitle.value,
      reviewBody: this.reviewForm.controls.reviewBody.value,
      notesForAi: this.reviewForm.controls.kindleNotes.value?.trim() || undefined,
      posted: this.reviewForm.controls.posted.value
    };

    const reviewId = this.reviewForm.controls.reviewId.value;
    const request$ = reviewId && reviewId > 0
      ? this.api.updateReview(reviewId, { id: reviewId, ...payload })
      : this.api.saveReview(payload);

    request$.subscribe({
      next: saved => {
        this.reviewForm.patchValue({
          reviewId: saved.id ?? 0,
          postedAt: saved.postedAt ?? ''
        });
        this.document.update(d => {
          if (!d) return d;
          const posted = saved.posted ?? this.reviewForm.controls.posted.value;
          const postedAt = saved.postedAt ?? d.review?.postedAt;
          return {
            ...d,
            review: d.review
              ? { ...d.review, id: saved.id, posted, postedAt }
              : {
                  id: saved.id,
                  documentId: d.id,
                  starRating: payload.starRating,
                  reviewBody: payload.reviewBody,
                  posted,
                  postedAt
                }
          };
        });
        this.message.set('Review saved to the local SQLite database.');
        this.isSaving.set(false);
      },
      error: error => {
        console.error(error);
        this.errorMessage.set('Could not save the review.');
        this.isSaving.set(false);
      }
    });
  }
}
