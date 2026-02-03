import {
  SignUpRequest,
  LoginRequest,
  TokenResponse,
  UserResponse,
  Post,
  PostRequest,
  PageResponse,
  ApiResponse,
  AnalyzeResult,
} from './types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const AI_URL = process.env.NEXT_PUBLIC_AI_URL || 'http://localhost:8000';

// Token 관리
export const getToken = (): string | null => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('token');
  }
  return null;
};

export const setToken = (token: string): void => {
  localStorage.setItem('token', token);
};

export const removeToken = (): void => {
  localStorage.removeItem('token');
};

// API 요청 헬퍼
async function fetchApi<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'API 요청 실패');
  }

  return response.json();
}

// Auth API
export const authApi = {
  signUp: async (data: SignUpRequest): Promise<ApiResponse<UserResponse>> => {
    return fetchApi(`${API_URL}/api/auth/signup`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  login: async (data: LoginRequest): Promise<ApiResponse<TokenResponse>> => {
    return fetchApi(`${API_URL}/api/auth/login`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// Post API
export const postApi = {
  getAll: async (page = 0, size = 10): Promise<ApiResponse<PageResponse<Post>>> => {
    return fetchApi(`${API_URL}/api/posts?page=${page}&size=${size}`);
  },

  getById: async (id: number): Promise<ApiResponse<Post>> => {
    return fetchApi(`${API_URL}/api/posts/${id}`);
  },

  create: async (data: PostRequest): Promise<ApiResponse<Post>> => {
    return fetchApi(`${API_URL}/api/posts`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  update: async (id: number, data: PostRequest): Promise<ApiResponse<Post>> => {
    return fetchApi(`${API_URL}/api/posts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  delete: async (id: number): Promise<void> => {
    await fetchApi(`${API_URL}/api/posts/${id}`, {
      method: 'DELETE',
    });
  },
};

// AI API
export const aiApi = {
  analyze: async (content: string): Promise<AnalyzeResult> => {
    return fetchApi(`${AI_URL}/api/analyze`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    });
  },
};
