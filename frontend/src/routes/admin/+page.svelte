<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import type { User } from '$lib/api/types';

	let settings: Record<string, string> = $state({});
	let users: User[] = $state([]);
	let recalculating = $state(false);
	let importResult: { imported: number; skipped: number; errors: string[] } | null = $state(null);
	let importing = $state(false);
	let loadError: string | null = $state(null);

	onMount(async () => {
		try {
			const [s, u] = await Promise.all([
				api.get<Record<string, string>>('/api/admin/settings'),
				api.get<User[]>('/api/users')
			]);
			settings = s;
			users = u;
		} catch (e) {
			loadError = 'Failed to load admin settings.';
		}
	});

	async function updateLongTermAlgorithm(algorithm: string) {
		await api.put('/api/admin/settings/long-term-algorithm', { algorithm });
		settings = { ...settings, long_term_algorithm: algorithm };
	}

	async function updateMonthlyAlgorithm(algorithm: string) {
		await api.put('/api/admin/settings/monthly-algorithm', { algorithm });
		settings = { ...settings, monthly_algorithm: algorithm };
	}

	async function toggleRole(user: User) {
		const newRole = user.role === 'ADMIN' ? 'PLAYER' : 'ADMIN';
		const updated = await api.put<User>(`/api/admin/users/${user.id}/role`, { role: newRole });
		users = users.map(u => u.id === updated.id ? updated : u);
	}

	async function recalculate() {
		recalculating = true;
		await api.post('/api/admin/rankings/recalculate');
		recalculating = false;
	}

	async function handleImport(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files?.length) return;
		importing = true;
		const formData = new FormData();
		formData.append('file', input.files[0]);
		try {
			importResult = await api.upload('/api/admin/import', formData);
		} catch (e) {
			importResult = { imported: 0, skipped: 0, errors: [(e as Error).message] };
		}
		importing = false;
		input.value = '';
	}
</script>

<div class="space-y-6">
	<h1 class="text-2xl font-black text-gray-900 brand-heading">Admin Panel</h1>

	{#if loadError}
		<div class="bg-red-50 text-red-700 rounded-lg p-4 text-sm border border-red-100">{loadError}</div>
	{/if}

	<!-- Algorithm Settings -->
	<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 animate-fade-in-up">
		<h2 class="font-bold text-gray-900 mb-4">Ranking Algorithms</h2>

		<div class="space-y-4">
			<div>
				<label class="text-sm font-semibold text-gray-700 mb-2 block">Long-Term Ranking</label>
				<div class="flex gap-2">
					<button
						onclick={() => updateLongTermAlgorithm('ELO')}
						class="flex-1 py-2 px-3 rounded-lg text-sm font-bold border-2 transition-all
							{settings.long_term_algorithm === 'ELO' ? 'border-brand-blue bg-brand-cloud-blue text-brand-blue' : 'border-brand-gray hover:border-brand-it-blue'}"
					>
						ELO
					</button>
					<button
						onclick={() => updateLongTermAlgorithm('AVG_GOAL_DIFF')}
						class="flex-1 py-2 px-3 rounded-lg text-sm font-bold border-2 transition-all
							{settings.long_term_algorithm === 'AVG_GOAL_DIFF' ? 'border-brand-blue bg-brand-cloud-blue text-brand-blue' : 'border-brand-gray hover:border-brand-it-blue'}"
					>
						Avg Goal Difference
					</button>
				</div>
			</div>

			<div>
				<label class="text-sm font-semibold text-gray-700 mb-2 block">Monthly Competition</label>
				<div class="flex gap-2">
					<button
						onclick={() => updateMonthlyAlgorithm('MONTHLY_ELO_GAIN')}
						class="flex-1 py-2 px-3 rounded-lg text-sm font-bold border-2 transition-all
							{settings.monthly_algorithm === 'MONTHLY_ELO_GAIN' ? 'border-brand-blue bg-brand-cloud-blue text-brand-blue' : 'border-brand-gray hover:border-brand-it-blue'}"
					>
						ELO Gain
					</button>
					<button
						onclick={() => updateMonthlyAlgorithm('MONTHLY_GOALS_SCORED')}
						class="flex-1 py-2 px-3 rounded-lg text-sm font-bold border-2 transition-all
							{settings.monthly_algorithm === 'MONTHLY_GOALS_SCORED' ? 'border-brand-blue bg-brand-cloud-blue text-brand-blue' : 'border-brand-gray hover:border-brand-it-blue'}"
					>
						Goals Scored
					</button>
				</div>
			</div>
		</div>
	</div>

	<!-- Recalculate -->
	<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 animate-fade-in-up" style="--delay: 60ms">
		<h2 class="font-bold text-gray-900 mb-2">Recalculate Rankings</h2>
		<p class="text-sm text-gray-500 mb-3">Replay all matches through the current algorithm.</p>
		<button onclick={recalculate} disabled={recalculating}
			class="px-4 py-2 bg-orange-600 text-white rounded-lg text-sm font-bold hover:bg-orange-700 disabled:bg-brand-gray transition-colors">
			{recalculating ? 'Recalculating...' : 'Recalculate All'}
		</button>
	</div>

	<!-- Import -->
	<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 animate-fade-in-up" style="--delay: 120ms">
		<h2 class="font-bold text-gray-900 mb-2">Import Matches</h2>
		<p class="text-sm text-gray-500 mb-1">
			<strong>CSV:</strong> datum, zluty_obrance, zluty_utocnik, bily_obrance, bily_utocnik, skore_zluty, skore_bily
		</p>
		<p class="text-sm text-gray-500 mb-3">
			<strong>Excel:</strong> Date | Yellow Attacker | Yellow Defender | White Attacker | White Defender | Yellow Score | White Score
		</p>
		<label class="inline-flex items-center gap-2 px-4 py-2 bg-brand-blue text-white rounded-lg text-sm font-bold hover:bg-brand-blue/90 cursor-pointer transition-colors">
			{importing ? 'Importing...' : 'Upload File'}
			<input type="file" accept=".xlsx,.xls,.csv,.txt" onchange={handleImport} class="hidden" disabled={importing} />
		</label>
		{#if importResult}
			<div class="mt-3 p-3 rounded-lg border {importResult.errors.length > 0 ? 'bg-yellow-50 border-yellow-200' : 'bg-green-50 border-green-200'}">
				<p class="text-sm font-bold">
					Imported: {importResult.imported} | Skipped: {importResult.skipped}
				</p>
				{#if importResult.errors.length > 0}
					<ul class="mt-2 text-xs text-red-600 space-y-1">
						{#each importResult.errors.slice(0, 10) as error}
							<li>{error}</li>
						{/each}
					</ul>
				{/if}
			</div>
		{/if}
	</div>

	<!-- User Management -->
	<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 animate-fade-in-up" style="--delay: 180ms">
		<h2 class="font-bold text-gray-900 mb-4">User Management</h2>
		<div class="divide-y divide-brand-cloud-blue">
			{#each users as user}
				<div class="flex items-center justify-between py-3">
					<div class="flex items-center gap-3">
						{#if user.avatarUrl}
							<img src={user.avatarUrl} alt={user.displayName} class="w-8 h-8 rounded-full" />
						{:else}
							<div class="w-8 h-8 rounded-full bg-brand-cloud-blue flex items-center justify-center text-xs font-bold text-brand-blue">
								{user.displayName?.[0] ?? '?'}
							</div>
						{/if}
						<div>
							<p class="font-semibold text-sm text-gray-900">{user.displayName}</p>
							<p class="text-xs text-gray-500">{user.email}</p>
						</div>
					</div>
					<button
						onclick={() => toggleRole(user)}
						class="px-3 py-1 rounded-full text-xs font-bold transition-colors
							{user.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-brand-cloud-blue text-brand-blue'}"
					>
						{user.role}
					</button>
				</div>
			{/each}
		</div>
	</div>
</div>
