const apiBase = '/api';

export async function apiRequest(path, { token, ...options } = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${apiBase}${path}`, {
      ...options,
      headers,
      signal: options.signal || AbortSignal.timeout(12000)
    });
  } catch (error) {
    if (error.name === 'TimeoutError' || error.name === 'AbortError') {
      throw new Error('Request timed out. Check whether the backend is running.');
    }
    throw new Error('Cannot connect to the backend.');
  }

  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    const retryAfter = response.headers.get('Retry-After');
    const suffix = response.status === 429 && retryAfter ? ` Try again in ${retryAfter}s.` : '';
    throw new Error(`${data?.error || `HTTP ${response.status}`}${suffix}`);
  }

  return data;
}
