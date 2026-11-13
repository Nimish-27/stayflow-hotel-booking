const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

// Every failure (server error body or network problem) reaches the UI as an ApiError.
export class ApiError extends Error {
  constructor(status, message, fieldErrors = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }

  // Turns the backend's [{field, message}] list into { field: message }.
  fieldMessages() {
    return Object.fromEntries(this.fieldErrors.map(({ field, message }) => [field, message]));
  }
}

function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      query.append(key, String(value).trim());
    }
  });
  const text = query.toString();
  return text ? `?${text}` : '';
}

async function parseBody(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export async function request(path, { method = 'GET', params, body } = {}) {
  let response;
  try {
    response = await fetch(`${API_URL}${path}${buildQuery(params)}`, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Please check that the backend is running.');
  }

  const data = await parseBody(response);
  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.message ?? `Request failed (${response.status})`,
      data?.fieldErrors ?? [],
    );
  }
  return data;
}