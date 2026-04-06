const BASE = '';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
	const res = await fetch(`${BASE}${path}`, {
		credentials: 'include',
		headers: { 'Content-Type': 'application/json', ...options?.headers },
		...options
	});
	if (res.status === 401) {
		window.location.href = '/auth/login';
		throw new Error('Unauthorized');
	}
	if (!res.ok) {
		const body = await res.json().catch(() => ({}));
		throw new Error(body.detail || `Request failed: ${res.status}`);
	}
	if (res.status === 204) return undefined as T;
	return res.json();
}

export const api = {
	get: <T>(path: string) => request<T>(path),
	post: <T>(path: string, body?: unknown) =>
		request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
	put: <T>(path: string, body?: unknown) =>
		request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
	delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
	upload: <T>(path: string, formData: FormData) =>
		request<T>(path, {
			method: 'POST',
			body: formData,
			headers: {} // let browser set Content-Type with boundary
		})
};
