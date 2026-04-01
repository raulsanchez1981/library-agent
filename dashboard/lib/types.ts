export type RecommendationStatus = "NUEVA" | "VISTA" | "DESCARTADA";

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

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
