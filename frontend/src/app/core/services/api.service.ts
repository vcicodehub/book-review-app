import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  AmazonBookInfo,
  DocumentImageRecord,
  DocumentRecord,
  GenerateReviewRequest,
  GenerateReviewResponse,
  ImportKindleRequest,
  KindleBooksResponse,
  ReviewRecord
} from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8010/api';

  uploadPdf(file: File, bookTitle?: string, author?: string, bookSize?: string, category?: string, amazonUrl?: string) {
    const formData = new FormData();
    formData.append('file', file);
    if (bookTitle?.trim()) {
      formData.append('bookTitle', bookTitle.trim());
    }
    if (author?.trim()) {
      formData.append('author', author.trim());
    }
    if (bookSize?.trim()) {
      formData.append('bookSize', bookSize.trim());
    }
    if (category?.trim()) {
      formData.append('category', category.trim());
    }
    if (amazonUrl?.trim()) {
      formData.append('amazonUrl', amazonUrl.trim());
    }

    return this.http.post<DocumentRecord>(`${this.baseUrl}/documents/upload`, formData);
  }

  saveKindle(bookTitle?: string, author?: string, bookSize?: string, amazonUrl?: string) {
    return this.http.post<DocumentRecord>(`${this.baseUrl}/documents/kindle`, {
      bookTitle: bookTitle?.trim() ?? '',
      author: author?.trim() ?? '',
      bookSize: bookSize?.trim() ?? '',
      amazonUrl: amazonUrl?.trim() ?? ''
    });
  }

  listKindleBooks() {
    return this.http.get<KindleBooksResponse>(`${this.baseUrl}/kindle/books`);
  }

  importKindleFromPath(request: ImportKindleRequest) {
    return this.http.post<DocumentRecord>(`${this.baseUrl}/kindle/import`, {
      filePath: request.filePath,
      bookTitle: request.bookTitle?.trim() ?? '',
      author: request.author?.trim() ?? '',
      bookSize: request.bookSize?.trim() ?? '',
      amazonUrl: request.amazonUrl?.trim() ?? ''
    });
  }

  fetchAmazonBookInfo(url: string) {
    return this.http.get<AmazonBookInfo>(`${this.baseUrl}/amazon/book-info`, {
      params: { url }
    });
  }

  getDocuments() {
    return this.http.get<DocumentRecord[]>(`${this.baseUrl}/documents`);
  }

  getDocument(id: number) {
    return this.http.get<DocumentRecord>(`${this.baseUrl}/documents/${id}`);
  }

  uploadDocumentImage(documentId: number, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentImageRecord>(`${this.baseUrl}/documents/${documentId}/images`, formData);
  }

  listDocumentImages(documentId: number) {
    return this.http.get<DocumentImageRecord[]>(`${this.baseUrl}/documents/${documentId}/images`);
  }

  getDocumentImageUrl(documentId: number, imageId: number): string {
    return `${this.baseUrl}/documents/${documentId}/images/${imageId}`;
  }

  deleteDocumentImage(documentId: number, imageId: number) {
    return this.http.delete(`${this.baseUrl}/documents/${documentId}/images/${imageId}`);
  }

  deleteDocument(documentId: number) {
    return this.http.delete(`${this.baseUrl}/documents/${documentId}`);
  }

  generateReview(request: GenerateReviewRequest) {
    return this.http.post<GenerateReviewResponse>(`${this.baseUrl}/reviews/generate`, request);
  }

  shortenReview(reviewTitle: string, reviewBody: string) {
    return this.http.post<GenerateReviewResponse>(`${this.baseUrl}/reviews/shorten`, {
      reviewTitle: reviewTitle ?? '',
      reviewBody
    });
  }

  humanizeReview(reviewTitle: string, reviewBody: string) {
    return this.http.post<GenerateReviewResponse>(`${this.baseUrl}/reviews/humanize`, {
      reviewTitle: reviewTitle ?? '',
      reviewBody
    });
  }

  saveReview(request: ReviewRecord) {
    return this.http.post<ReviewRecord>(`${this.baseUrl}/reviews`, request);
  }

  updateReview(id: number, request: ReviewRecord) {
    return this.http.put<ReviewRecord>(`${this.baseUrl}/reviews/${id}`, request);
  }

  markReviewPosted(reviewId: number) {
    return this.http.put<ReviewRecord>(`${this.baseUrl}/reviews/${reviewId}/posted`, {});
  }

  deleteReview(reviewId: number) {
    return this.http.delete(`${this.baseUrl}/reviews/${reviewId}`);
  }
}
