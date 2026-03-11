export interface ReviewRecord {
  id?: number;
  documentId: number;
  starRating: number;
  tone?: string;
  reviewTitle?: string;
  reviewBody: string;
  notesForAi?: string;
  posted?: boolean;
  postedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DocumentImageRecord {
  id: number;
  documentId: number;
  originalFileName?: string;
  createdAt: string;
}

export interface DocumentRecord {
  id: number;
  fileName: string;
  originalFileName?: string;
  bookTitle?: string;
  author?: string;
  bookSize?: string;
  category?: string;
  summary: string;
  createdAt: string;
  review?: ReviewRecord | null;
  images?: DocumentImageRecord[];
  amazonUrl?: string;
}

export interface GenerateReviewRequest {
  documentId: number;
  starRating: number;
  tone?: string;
  summary?: string;
  notes?: string;
}

export interface GenerateReviewResponse {
  reviewTitle: string;
  reviewBody: string;
}

export interface AmazonBookInfo {
  bookTitle: string;
  author: string;
  bookSize?: string;
}

export interface KindleBookInfo {
  fileName: string;
  absolutePath: string;
  lastModified: number;
}

export interface KindleBooksResponse {
  contentPath: string;
  books: KindleBookInfo[];
}

export interface ImportKindleRequest {
  filePath: string;
  bookTitle?: string;
  author?: string;
  bookSize?: string;
  amazonUrl?: string;
}
