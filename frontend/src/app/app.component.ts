import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen">
      <header class="border-b border-slate-200 bg-white shadow-sm">
        <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <div>
            <h1 class="text-xl font-bold text-slate-900">PDF AI Review App</h1>
            <p class="text-sm text-slate-500">Summarize PDFs, draft Amazon reviews, and save locally.</p>
          </div>

          <nav class="flex gap-3">
            <a
              routerLink="/upload"
              routerLinkActive="bg-slate-900 text-white"
              class="rounded-xl px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
            >Upload</a>
            <a
              routerLink="/history"
              routerLinkActive="bg-slate-900 text-white"
              class="rounded-xl px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
            >History</a>
          </nav>
        </div>
      </header>

      <main class="mx-auto max-w-6xl px-6 py-8">
        <router-outlet />
      </main>
    </div>
  `
})
export class AppComponent {}
