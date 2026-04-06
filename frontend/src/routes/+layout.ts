import type { User } from '$lib/api/types';

export async function load({ fetch }) {
	try {
		const res = await fetch('/api/auth/me', { credentials: 'include' });
		if (!res.ok) return { user: null };
		const user: User | null = await res.json();
		return { user };
	} catch {
		return { user: null };
	}
}
