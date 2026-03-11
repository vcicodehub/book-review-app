import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'history'
  },
  {
    path: 'upload',
    loadComponent: () => import('./features/upload/upload-page.component').then(m => m.UploadPageComponent)
  },
  {
    path: 'summary/:id',
    loadComponent: () => import('./features/summary/summary-page.component').then(m => m.SummaryPageComponent)
  },
  {
    path: 'history',
    loadComponent: () => import('./features/history/history-page.component').then(m => m.HistoryPageComponent)
  }
];
