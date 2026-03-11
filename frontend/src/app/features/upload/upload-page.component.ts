import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import type { KindleBookInfo } from '../../core/services/api.models';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <section class="w-full">
      <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-2xl font-bold text-slate-900">Upload a PDF</h2>
        <p class="mt-2 text-slate-600">
          Upload a PDF, summarize it with AI, then draft and save an Amazon review.
        </p>

        <form class="mt-6 space-y-5" [formGroup]="form" (ngSubmit)="submit()">
          <div>
            <label class="mb-2 block text-sm font-medium text-slate-700">Amazon URL</label>
            <div class="flex gap-2">
              <input formControlName="amazonUrl" type="url" class="flex-1 rounded-xl border border-slate-300 px-4 py-3 outline-none ring-0 transition focus:border-slate-500" placeholder="https://www.amazon.com/dp/..." />
              <button
                type="button"
                [disabled]="isFetching() || !form.get('amazonUrl')?.value?.trim()"
                (click)="fetchFromAmazon()"
                class="rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {{ isFetching() ? 'Fetching...' : 'Fetch' }}
              </button>
            </div>
            <p class="mt-1 text-xs text-slate-500">Paste an Amazon book URL to auto-fill title, author, and book size.</p>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-slate-700">Book title</label>
            <input formControlName="bookTitle" type="text" class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none ring-0 transition focus:border-slate-500" placeholder="Optional title" />
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-slate-700">Author</label>
            <input formControlName="author" type="text" class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none ring-0 transition focus:border-slate-500" placeholder="Optional author" />
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-slate-700">Book size</label>
            <input formControlName="bookSize" type="text" class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none ring-0 transition focus:border-slate-500" placeholder="e.g. 6 x 9 inches (from Product Dimensions)" />
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-slate-700">Category</label>
            <select formControlName="category" class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none ring-0 transition focus:border-slate-500">
              <option value="book">Book</option>
              <option value="kindle">Kindle</option>
              <option value="puzzles">Puzzles</option>
              <option value="activity">Activity</option>
              <option value="coloring">Coloring</option>
            </select>
          </div>

          @if (form.get('category')?.value !== 'kindle') {
          <div>
            <label class="mb-2 block text-sm font-medium text-slate-700">PDF file</label>
            <label class="flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center transition hover:border-slate-500 hover:bg-slate-100">
              <span class="text-base font-medium text-slate-700">Choose a PDF file</span>
              <span class="mt-1 text-sm text-slate-500">Only PDF files are accepted</span>
              <input class="hidden" type="file" accept="application/pdf" (change)="onFileSelected($event)" />
            </label>
            @if (selectedFileName()) {
              <p class="mt-3 text-sm text-slate-600">Selected: <span class="font-medium">{{ selectedFileName() }}</span></p>
            }
          </div>
          } @else {
          <div class="space-y-4">
            <div class="flex items-center gap-3">
              <button
                type="button"
                [disabled]="isLoadingKindleBooks()"
                (click)="loadKindleBooks()"
                class="rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {{ isLoadingKindleBooks() ? 'Loading...' : 'Find downloaded Kindle books' }}
              </button>
              @if (kindleContentPath()) {
                <span class="text-xs text-slate-500">From: {{ kindleContentPath() }}</span>
              }
            </div>
            @if (kindleBooks().length > 0) {
              <div>
                <label class="mb-2 block text-sm font-medium text-slate-700">Select a book to import and summarize</label>
                <select
                  [(ngModel)]="selectedKindlePath"
                  [ngModelOptions]="{standalone: true}"
                  class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none ring-0 transition focus:border-slate-500"
                >
                  <option value="">-- Choose a book --</option>
                  @for (book of kindleBooks(); track book.absolutePath) {
                    <option [value]="book.absolutePath">{{ book.fileName }}</option>
                  }
                </select>
                <p class="mt-1 text-xs text-slate-500">Title and author above are optional; they will be read from the book if left blank.</p>
              </div>
            } @else if (hasLoadedKindleBooks() && kindleBooks().length === 0) {
              <p class="text-sm text-amber-600">No ebook files found. Download a book in the Kindle app first, or set KINDLE_CONTENT_PATH to your folder.</p>
            }
          </div>
          }

          @if (errorMessage()) {
            <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {{ errorMessage() }}
            </div>
          }

          <button
            type="submit"
            [disabled]="isSubmitting()"
            class="inline-flex items-center rounded-xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            @if (form.get('category')?.value === 'kindle') {
              @if (selectedKindlePath) {
                {{ isSubmitting() ? 'Importing and summarizing...' : 'Import and summarize' }}
              } @else {
                {{ isSubmitting() ? 'Saving...' : 'Save book (no file)' }}
              }
            } @else {
              {{ isSubmitting() ? 'Uploading and summarizing...' : 'Upload and summarize' }}
            }
          </button>
        </form>
      </div>
    </section>
  `
})
export class UploadPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly isFetching = signal(false);
  protected readonly selectedFileName = signal('');
  protected readonly errorMessage = signal('');
  protected readonly kindleBooks = signal<KindleBookInfo[]>([]);
  protected readonly kindleContentPath = signal('');
  protected readonly isLoadingKindleBooks = signal(false);
  protected readonly hasLoadedKindleBooks = signal(false);
  protected selectedKindlePath = '';
  private selectedFile: File | null = null;

  protected readonly form = this.fb.group({
    amazonUrl: [''],
    bookTitle: [''],
    author: [''],
    bookSize: [''],
    category: ['book']
  });

  protected loadKindleBooks() {
    this.errorMessage.set('');
    this.isLoadingKindleBooks.set(true);
    console.debug('[Kindle] loadKindleBooks: Fetching books from API...');
    this.api.listKindleBooks().subscribe({
      next: res => {
        console.debug('[Kindle] API response:', { contentPath: res.contentPath, books: res.books });
        console.info(`[Kindle] Loaded ${res.books.length} book(s) from ${res.contentPath}`);
        this.kindleBooks.set(res.books);
        this.kindleContentPath.set(res.contentPath);
        this.hasLoadedKindleBooks.set(true);
        this.isLoadingKindleBooks.set(false);
      },
      error: err => {
        console.error('[Kindle] Failed to load books:', err);
        this.isLoadingKindleBooks.set(false);
        this.errorMessage.set(err.error?.message ?? err.message ?? 'Could not load Kindle books.');
      }
    });
  }

  protected fetchFromAmazon() {
    const url = this.form.get('amazonUrl')?.value?.trim();
    if (!url) return;

    this.errorMessage.set('');
    this.isFetching.set(true);

    this.api.fetchAmazonBookInfo(url).subscribe({
      next: info => {
        this.form.patchValue({
          bookTitle: info.bookTitle ?? '',
          author: info.author ?? '',
          bookSize: info.bookSize ?? ''
        });
        this.isFetching.set(false);
      },
      error: err => {
        this.isFetching.set(false);
        const msg = err.error?.message ?? err.message ?? 'Could not fetch book info from Amazon.';
        this.errorMessage.set(msg);
      }
    });
  }

  protected onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.selectedFile = null;
      this.selectedFileName.set('');
      return;
    }

    if (file.type !== 'application/pdf') {
      this.errorMessage.set('Please choose a valid PDF file.');
      this.selectedFile = null;
      this.selectedFileName.set('');
      return;
    }

    this.errorMessage.set('');
    this.selectedFile = file;
    this.selectedFileName.set(file.name);
  }

  protected submit() {
    const { bookTitle, author, bookSize, category } = this.form.getRawValue();
    const isKindle = category === 'kindle';

    if (!isKindle && !this.selectedFile) {
      this.errorMessage.set('Please select a PDF file first.');
      return;
    }

    this.errorMessage.set('');
    this.isSubmitting.set(true);

    if (isKindle) {
      const path = this.selectedKindlePath;
      if (path) {
        this.api.importKindleFromPath({
          filePath: path,
          bookTitle: bookTitle ?? '',
          author: author ?? '',
          bookSize: bookSize ?? '',
          amazonUrl: this.form.get('amazonUrl')?.value?.trim() ?? ''
        })
          .pipe(finalize(() => this.isSubmitting.set(false)))
          .subscribe({
            next: document => this.router.navigate(['/summary', document.id]),
            error: err => {
              console.error(err);
              this.errorMessage.set(err.error?.message ?? err.message ?? 'Could not import. The file may be DRM-protected.');
            }
          });
      } else {
        this.api.saveKindle(bookTitle ?? '', author ?? '', bookSize ?? '', this.form.get('amazonUrl')?.value?.trim() ?? '')
          .pipe(finalize(() => this.isSubmitting.set(false)))
          .subscribe({
            next: document => this.router.navigate(['/summary', document.id]),
            error: error => {
              console.error(error);
              this.errorMessage.set('Could not save the Kindle book. Check the backend logs.');
            }
          });
      }
    } else {
      this.api.uploadPdf(this.selectedFile!, bookTitle ?? '', author ?? '', bookSize ?? '', category ?? 'book', this.form.get('amazonUrl')?.value?.trim() ?? '')
        .pipe(finalize(() => this.isSubmitting.set(false)))
        .subscribe({
          next: document => this.router.navigate(['/summary', document.id]),
          error: error => {
            console.error(error);
            this.errorMessage.set('The PDF could not be processed. Check the backend logs and AI settings.');
          }
        });
    }
  }
}
