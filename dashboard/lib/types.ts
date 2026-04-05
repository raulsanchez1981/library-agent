export type RecommendationStatus = "NUEVA" | "VISTA" | "DESCARTADA";

export type Confidence = "HIGH" | "MEDIUM" | "LOW" | "VERIFIED";
export type EnrichmentSource = "SONNET" | "OL_ONLY" | "NONE" | "ADMIN";
export type CdlAutoSearchStatus = "AUTO" | "NOT_FOUND" | "CONFIRMED";

export interface ExtractedBookAdminDto {
  id: string;
  title: string;
  author: string | null;
  isSaga: boolean;
  titleEs: string | null;
  titleEsOl: string | null;
  verifiedTitleName: string | null;
  authorCorrected: string | null;
  authors: string[];
  verifiedTitleId: string | null;
  availableInSpanish: boolean;
  enrichmentSource: EnrichmentSource | null;
  confidence: Confidence | null;
  enriched: boolean;
  enrichedAt: string | null;
  createdAt: string;
  coverUrl: string | null;
  synopsis: string | null;
  cdlEnriched: boolean;
  cdlAutoSearchStatus: CdlAutoSearchStatus | null;
  discarded: boolean;
}

export interface UpdateExtractedBookRequest {
  titleEs?: string | null;
  authorCorrected?: string | null;
  availableInSpanish?: boolean | null;
  isSaga?: boolean | null;
}

export interface RecommendationDto {
  id: string;
  extractedBookId: string;
  bookTitle: string;
  bookAuthor: string | null;
  score: number;
  reasoning: string;
  status: RecommendationStatus;
  scoredAt: string;
}

export interface VerifiedTitleDto {
  id: string;
  name: string;
  authors: string[];
  coverUrl: string | null;
  synopsis: string | null;
  googleBooksId: string | null;
  cdlEnriched: boolean;
  cdlAutoSearchStatus: CdlAutoSearchStatus | null;
  publisher: string | null;
  publishedDate: string | null;
  pageCount: number | null;
  isbn: string | null;
}

export interface GenreDto {
  id: string;
  name: string;
}

export interface AuthorRefDto {
  id: string;
  name: string;
}

export interface VerifiedTitleDetailDto {
  id: string;
  name: string;
  coverUrl: string | null;
  synopsis: string | null;
  technicalSheet: string | null;
  casaDelLibroUrl: string | null;
  genres: GenreDto[];
  authors: AuthorRefDto[];
  cdlAutoSearchStatus: CdlAutoSearchStatus | null;
  publisher: string | null;
  publishedDate: string | null;
  pageCount: number | null;
  isbn: string | null;
}

export interface AutorDto {
  id: string;
  name: string;
  photoUrl: string | null;
  bookCount: number;
}

export interface AutorBookDto {
  id: string;
  name: string;
  coverUrl: string | null;
}

export interface AutorDetailDto {
  id: string;
  name: string;
  photoUrl: string | null;
  bio: string | null;
  openLibraryOlid: string | null;
  books: AutorBookDto[];
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

export interface UserProfileDto {
  id: string;
  email: string;
  preferredLanguage: string | null;
  minScoreThreshold: number | null;
  favoriteGenres: string[];
  favoriteAuthors: string[];
  createdAt: string;
}

export type ReadingStatus = "PENDING" | "IN_PROGRESS" | "READ" | "ABANDONED";

export interface BookSearchResultDto {
  id: string;
  title: string;
  titleEs: string | null;
  author: string | null;
}

export interface ReadingHistoryDto {
  id: string;
  bookTitle: string;
  bookAuthor: string | null;
  status: ReadingStatus;
  startedAt: string | null;
  finishedAt: string | null;
  rating: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}
