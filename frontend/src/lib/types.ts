// Auth Types
export interface SignUpRequest {
  email: string;
  password: string;
  nickname: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
}

export interface UserResponse {
  id: number;
  email: string;
  nickname: string;
}

// Post Types
export interface Post {
  id: number;
  content: string;
  author: string;
  createdAt: string;
  modifiedAt: string;
}

export interface PostRequest {
  content: string;
  author: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

// AI Types
export interface SentimentResult {
  sentiment: 'positive' | 'negative' | 'neutral';
  score: number;
  emoji: string;
}

export interface AnalyzeResult {
  sentiment: SentimentResult;
  hashtags: string[];
}
