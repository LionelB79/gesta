import type { ErrorResponse } from "./ErrorResponse";
const BASE_URL = "/api";

export class ApiError extends Error {
  response: ErrorResponse;

  constructor(response: ErrorResponse) {
    super(response.message);
    this.response = response;
  }
}

function getCookie(name: string): string | undefined {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(";").shift();
  }
  return undefined;
}

export async function apiClient<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (options.body) {
    headers["Content-Type"] = "application/json";
  }
  const method = options.method ?? "GET";
  if (method !== "GET") {
    const xsrfToken = getCookie("XSRF-TOKEN");
    if (xsrfToken) {
      headers["X-XSRF-TOKEN"] = xsrfToken;
    }
  }

  const response = await fetch(BASE_URL + path, {
    ...options,
    credentials: "include",
    headers,
  });

  if (!response.ok) {
    const text = await response.text();

    let error: ErrorResponse = {
      timestamp: "",
      status: response.status,
      code: "HTTP_ERROR",
      message: response.statusText || `Erreur ${response.status}`,
      path,
    } as ErrorResponse;

    if (text) {
      try {
        const parsed = JSON.parse(text) as ErrorResponse;
        if (parsed?.message) {
          error = parsed;
        }
      } catch {
        // JSON invalide : on garde le repli
      }
    }
    throw new ApiError(error);
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
