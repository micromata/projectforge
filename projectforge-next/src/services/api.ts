/**
 * API Service for ProjectForge
 * Handles API requests with proper error handling
 */

// Use the Next.js proxy for API requests to avoid CORS issues
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || '/rs';

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: string;
  statusCode?: number;
};

/**
 * ProjectForge uses a PostData wrapper for most requests
 */
export interface PostData<T> {
  data: T;
  watchFieldsTriggered?: string[];
  serverData?: {
    csrfToken?: string;
    returnToCaller?: string;
    returnToCallerParams?: Record<string, string>;
  };
}

/**
 * Generic fetch handler with error handling
 */
async function fetchFromApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      credentials: 'include', // Include cookies for authentication
    });

    // Handle API errors
    if (!response.ok) {
      return {
        success: false,
        statusCode: response.status,
        error: `API Error: ${response.status} ${response.statusText}`,
      };
    }

    // Parse JSON response
    const data = await response.json();
    return {
      success: true,
      data,
    };
  } catch (error) {
    console.error('API request failed:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

// Store the most recently received CSRF token
let currentCsrfToken: string | null = null;

// Update the CSRF token when we receive a new one from the server
export function updateCsrfToken(token: string) {
  currentCsrfToken = token;
  console.log('CSRF token updated:', token);
}

/**
 * Wraps data in the required ProjectForge PostData format
 */
function wrapInPostData<T>(data: T): PostData<T> {
  return {
    data: data,
    watchFieldsTriggered: [],
    serverData: {
      csrfToken: currentCsrfToken || undefined
    }
  };
}

/**
 * API service for making requests to the ProjectForge backend
 */
const api = {
  /**
   * GET request
   */
  get: <T>(endpoint: string, params?: Record<string, string>): Promise<ApiResponse<T>> => {
    const url = params ? `${endpoint}?${new URLSearchParams(params)}` : endpoint;
    return fetchFromApi<T>(url);
  },

  /**
   * POST request with JSON body
   */
  post: <T>(endpoint: string, data?: any): Promise<ApiResponse<T>> => {
    // If this is a save/update operation, wrap in PostData format
    const isSaveOrUpdate = endpoint.includes('saveorupdate');
    const formattedData = isSaveOrUpdate && data ? wrapInPostData(data) : data;
    
    return fetchFromApi<T>(endpoint, {
      method: 'POST',
      body: formattedData ? JSON.stringify(formattedData) : undefined,
    });
  },

  /**
   * PUT request with JSON body
   */
  put: <T>(endpoint: string, data?: any): Promise<ApiResponse<T>> => {
    // If this is a save/update operation, wrap in PostData format
    const isSaveOrUpdate = endpoint.includes('saveorupdate');
    const formattedData = isSaveOrUpdate && data ? wrapInPostData(data) : data;
    
    return fetchFromApi<T>(endpoint, {
      method: 'PUT',
      body: formattedData ? JSON.stringify(formattedData) : undefined,
    });
  },

  /**
   * DELETE request
   */
  delete: <T>(endpoint: string): Promise<ApiResponse<T>> => {
    return fetchFromApi<T>(endpoint, {
      method: 'DELETE',
    });
  },
};

export default api;