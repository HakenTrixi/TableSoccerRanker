<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import type { User, TeamSuggestion, MatchCreateRequest, Match, MatchPlayer } from '$lib/api/types';

	let users: User[] = $state([]);
	let step: 1 | 2 | 3 | 4 = $state(1);

	// Step 1: selected player IDs
	let selectedIds: string[] = $state([]);

	// Step 2: team suggestion (editable)
	let suggestion: TeamSuggestion | null = $state(null);
	let yellowAttacker: User | null = $state(null);
	let yellowDefender: User | null = $state(null);
	let whiteAttacker: User | null = $state(null);
	let whiteDefender: User | null = $state(null);

	// Step 3: win probability
	let yellowProb: number | null = $state(null);
	let whiteProb: number | null = $state(null);

	// Step 4: score + ELO preview
	let yellowScore: number = $state(0);
	let whiteScore: number = $state(0);
	let saving = $state(false);
	let previewData: Match | null = $state(null);
	let previewTimer: ReturnType<typeof setTimeout> | null = $state(null);
	let previewLoading = $state(false);

	// Derived: does at least one team have 10 goals?
	let hasValidScore = $derived(yellowScore >= 10 || whiteScore >= 10);

	onMount(async () => {
		users = await api.get<User[]>('/api/users');
	});

	function togglePlayer(id: string) {
		if (selectedIds.includes(id)) {
			selectedIds = selectedIds.filter(p => p !== id);
		} else if (selectedIds.length < 4) {
			selectedIds = [...selectedIds, id];
		}
	}

	async function suggestTeams() {
		suggestion = await api.post<TeamSuggestion>('/api/teams/suggest', { playerIds: selectedIds });
		yellowAttacker = suggestion.yellowAttacker;
		yellowDefender = suggestion.yellowDefender;
		whiteAttacker = suggestion.whiteAttacker;
		whiteDefender = suggestion.whiteDefender;
		step = 2;
	}

	function swapPlayers(pos1: string, pos2: string) {
		const players: Record<string, User | null> = {
			ya: yellowAttacker, yd: yellowDefender,
			wa: whiteAttacker, wd: whiteDefender
		};
		const temp = players[pos1];
		players[pos1] = players[pos2];
		players[pos2] = temp;
		yellowAttacker = players.ya;
		yellowDefender = players.yd;
		whiteAttacker = players.wa;
		whiteDefender = players.wd;
	}

	async function confirmTeams() {
		step = 3;
		// Load win probability via preview with 0:0 score
		await loadProbability();
	}

	async function loadProbability() {
		if (!yellowAttacker || !yellowDefender || !whiteAttacker || !whiteDefender) return;
		try {
			const preview = await api.post<Match>('/api/matches/preview', {
				yellowAttacker: yellowAttacker.id,
				yellowDefender: yellowDefender.id,
				whiteAttacker: whiteAttacker.id,
				whiteDefender: whiteDefender.id,
				yellowScore: 0,
				whiteScore: 0
			});
			const yp = preview.players.find(p => p.teamColor === 'YELLOW');
			const wp = preview.players.find(p => p.teamColor === 'WHITE');
			yellowProb = yp?.winProbability ?? null;
			whiteProb = wp?.winProbability ?? null;
		} catch (e) {
			yellowProb = null;
			whiteProb = null;
		}
	}

	function startScoring() {
		step = 4;
		previewData = null;
	}

	// Debounced ELO preview — fires 2s after score stops changing, only if score is valid
	function onScoreChange() {
		if (previewTimer) clearTimeout(previewTimer);
		previewData = null;
		if (yellowScore < 10 && whiteScore < 10) return;
		previewTimer = setTimeout(() => loadPreview(), 2000);
	}

	async function loadPreview() {
		if (!yellowAttacker || !yellowDefender || !whiteAttacker || !whiteDefender) return;
		if (yellowScore < 10 && whiteScore < 10) return;
		previewLoading = true;
		try {
			previewData = await api.post<Match>('/api/matches/preview', {
				yellowAttacker: yellowAttacker.id,
				yellowDefender: yellowDefender.id,
				whiteAttacker: whiteAttacker.id,
				whiteDefender: whiteDefender.id,
				yellowScore,
				whiteScore
			});
		} catch (e) {
			previewData = null;
		}
		previewLoading = false;
	}

	function getPreviewPlayer(teamColor: 'YELLOW' | 'WHITE', role: 'ATTACKER' | 'DEFENDER'): MatchPlayer | undefined {
		return previewData?.players.find(p => p.teamColor === teamColor && p.playerRole === role);
	}

	function setScore(yellow: number, white: number) {
		yellowScore = yellow;
		whiteScore = white;
		onScoreChange();
	}

	function changeYellow(delta: number) {
		yellowScore = Math.max(0, yellowScore + delta);
		onScoreChange();
	}

	function changeWhite(delta: number) {
		whiteScore = Math.max(0, whiteScore + delta);
		onScoreChange();
	}

	async function saveMatch() {
		if (!yellowAttacker || !yellowDefender || !whiteAttacker || !whiteDefender) return;
		saving = true;
		try {
			const request: MatchCreateRequest = {
				yellowAttacker: yellowAttacker.id,
				yellowDefender: yellowDefender.id,
				whiteAttacker: whiteAttacker.id,
				whiteDefender: whiteDefender.id,
				yellowScore,
				whiteScore
			};
			await api.post('/api/matches', request);
			goto('/');
		} finally {
			saving = false;
		}
	}
</script>

<div class="space-y-6">
	<!-- Step indicator -->
	<div class="flex items-center gap-2">
		{#each [1, 2, 3, 4] as s}
			<div class="flex items-center gap-2 {s <= step ? 'text-brand-blue' : 'text-brand-gray'}">
				<div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-black transition-all duration-200
					{s === step ? 'bg-brand-blue text-white shadow-md' : s < step ? 'bg-brand-cloud-blue text-brand-blue' : 'bg-brand-light-gray text-brand-gray'}">
					{s}
				</div>
				{#if s < 4}
					<div class="w-8 h-0.5 transition-colors duration-200 {s < step ? 'bg-brand-blue' : 'bg-brand-gray'}"></div>
				{/if}
			</div>
		{/each}
	</div>

	<!-- Step 1: Select Players -->
	{#if step === 1}
		<div class="animate-fade-in-up">
			<h1 class="text-2xl font-black text-gray-900 mb-1 brand-heading">Select 4 Players</h1>
			<p class="text-gray-500 text-sm mb-4">Choose who's playing the next match</p>

			<div class="grid grid-cols-2 gap-3">
				{#each users as user}
					<button
						onclick={() => togglePlayer(user.id)}
						class="flex items-center gap-3 p-3 rounded-xl border-2 transition-all duration-200 text-left
							{selectedIds.includes(user.id) ? 'border-brand-blue bg-brand-cloud-blue' : 'border-brand-gray hover:border-brand-it-blue'}"
						disabled={selectedIds.length >= 4 && !selectedIds.includes(user.id)}
					>
						{#if user.avatarUrl}
							<img src={user.avatarUrl} alt={user.displayName} class="w-10 h-10 rounded-full" />
						{:else}
							<div class="w-10 h-10 rounded-full bg-brand-cloud-blue flex items-center justify-center text-sm font-bold text-brand-blue">
								{user.displayName?.[0] ?? '?'}
							</div>
						{/if}
						<div class="min-w-0">
							<p class="font-semibold text-gray-900 truncate text-sm">{user.displayName}</p>
							<p class="text-xs text-gray-500">ELO {user.eloRating}</p>
						</div>
					</button>
				{/each}
			</div>

			<button
				onclick={suggestTeams}
				disabled={selectedIds.length !== 4}
				class="w-full mt-4 py-3 rounded-xl font-bold text-white transition-all duration-200
					{selectedIds.length === 4 ? 'bg-brand-blue hover:bg-brand-blue/90' : 'bg-brand-gray cursor-not-allowed'}"
			>
				Suggest Teams ({selectedIds.length}/4)
			</button>
		</div>
	{/if}

	<!-- Step 2: Team Suggestion -->
	{#if step === 2}
		{#snippet swapIcon()}
			<svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24" aria-hidden="true">
				<path stroke-linecap="round" stroke-linejoin="round" d="M7 16h13m0 0-3-3m3 3-3 3M17 8H4m0 0 3 3M4 8l3-3"/>
			</svg>
		{/snippet}

		<div class="animate-fade-in-up">
			<h1 class="text-2xl font-black text-gray-900 mb-1 brand-heading">Team Suggestion</h1>
			<p class="text-gray-500 text-sm mb-4">Balanced by ranking. Tap swap icons to adjust.</p>

			<div class="relative grid grid-cols-2 gap-8">
				<!-- Yellow Team -->
				<div class="bg-[#fac400]/8 border-2 border-[#fac400]/25 rounded-xl p-4">
					<div class="flex items-center gap-2 mb-3">
						<div class="w-4 h-4 rounded-full bg-brand-yellow"></div>
						<span class="font-black text-brand-blue">Yellow</span>
					</div>
					<div class="relative space-y-2">
						<div class="bg-white rounded-lg p-2">
							<p class="text-[10px] text-brand-blue/60 uppercase font-bold">Attacker</p>
							<p class="font-semibold text-sm truncate">{yellowAttacker?.displayName ?? ''}</p>
						</div>
						<div class="bg-white rounded-lg p-2">
							<p class="text-[10px] text-brand-blue/60 uppercase font-bold">Defender</p>
							<p class="font-semibold text-sm truncate">{yellowDefender?.displayName ?? ''}</p>
						</div>
						<!-- Vertical role swap floating inside Yellow box -->
						<button
							onclick={() => swapPlayers('ya', 'yd')}
							aria-label="Swap yellow attacker and defender"
							title="Swap yellow roles"
							class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-12 h-12 rounded-full border border-brand-gray bg-transparent text-brand-blue hover:bg-white/60 hover:border-brand-it-blue active:scale-95 transition-all flex items-center justify-center z-10"
						>
							<span class="rotate-90 flex">{@render swapIcon()}</span>
						</button>
					</div>
				</div>

				<!-- White Team -->
				<div class="bg-brand-light-gray border-2 border-brand-gray rounded-xl p-4">
					<div class="flex items-center gap-2 mb-3">
						<div class="w-4 h-4 rounded-full bg-brand-gray border border-gray-400"></div>
						<span class="font-black text-gray-700">White</span>
					</div>
					<div class="relative space-y-2">
						<div class="bg-white rounded-lg p-2">
							<p class="text-[10px] text-gray-500 uppercase font-bold">Attacker</p>
							<p class="font-semibold text-sm truncate">{whiteAttacker?.displayName ?? ''}</p>
						</div>
						<div class="bg-white rounded-lg p-2">
							<p class="text-[10px] text-gray-500 uppercase font-bold">Defender</p>
							<p class="font-semibold text-sm truncate">{whiteDefender?.displayName ?? ''}</p>
						</div>
						<!-- Vertical role swap floating inside White box -->
						<button
							onclick={() => swapPlayers('wa', 'wd')}
							aria-label="Swap white attacker and defender"
							title="Swap white roles"
							class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-12 h-12 rounded-full border border-brand-gray bg-transparent text-brand-blue hover:bg-white/60 hover:border-brand-it-blue active:scale-95 transition-all flex items-center justify-center z-10"
						>
							<span class="rotate-90 flex">{@render swapIcon()}</span>
						</button>
					</div>
				</div>

				<!-- Horizontal team swap buttons floating over the gap -->
				<button
					onclick={() => swapPlayers('ya', 'wa')}
					aria-label="Swap attackers between teams"
					title="Swap attackers"
					class="absolute left-1/2 top-[5.25rem] -translate-x-1/2 -translate-y-1/2 w-12 h-12 rounded-full border border-brand-gray bg-white text-brand-blue shadow-md hover:bg-brand-cloud-blue hover:border-brand-it-blue active:scale-95 transition-all flex items-center justify-center z-20"
				>
					{@render swapIcon()}
				</button>
				<button
					onclick={() => swapPlayers('yd', 'wd')}
					aria-label="Swap defenders between teams"
					title="Swap defenders"
					class="absolute left-1/2 top-[9.25rem] -translate-x-1/2 -translate-y-1/2 w-12 h-12 rounded-full border border-brand-gray bg-white text-brand-blue shadow-md hover:bg-brand-cloud-blue hover:border-brand-it-blue active:scale-95 transition-all flex items-center justify-center z-20"
				>
					{@render swapIcon()}
				</button>
			</div>

			<div class="flex gap-3 mt-6">
				<button onclick={() => step = 1} class="flex-1 py-3 rounded-xl border border-brand-gray font-bold hover:bg-brand-cloud-blue transition-colors">Back</button>
				<button onclick={confirmTeams} class="flex-1 py-3 rounded-xl bg-brand-blue text-white font-bold hover:bg-brand-blue/90 transition-colors">Confirm Teams</button>
			</div>
		</div>
	{/if}

	<!-- Step 3: Play -->
	{#if step === 3}
		<div class="text-center animate-fade-in-up">
			<h1 class="text-2xl font-black text-gray-900 mb-2 brand-heading">Match In Progress</h1>
			<p class="text-gray-500 mb-6">Good luck! Record the score when the match is over.</p>

			<div class="grid grid-cols-2 gap-4 mb-4">
				<div class="bg-[#fac400]/8 border-2 border-[#fac400]/25 rounded-xl p-4">
					<div class="w-4 h-4 rounded-full bg-brand-yellow mx-auto mb-2"></div>
					<p class="font-semibold text-sm">{yellowAttacker?.displayName}</p>
					<p class="font-semibold text-sm">{yellowDefender?.displayName}</p>
				</div>
				<div class="bg-brand-light-gray border-2 border-brand-gray rounded-xl p-4">
					<div class="w-4 h-4 rounded-full bg-brand-gray border border-gray-400 mx-auto mb-2"></div>
					<p class="font-semibold text-sm">{whiteAttacker?.displayName}</p>
					<p class="font-semibold text-sm">{whiteDefender?.displayName}</p>
				</div>
			</div>

			<!-- Win probability -->
			{#if yellowProb != null && whiteProb != null}
				<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 mb-6">
					<p class="text-xs text-brand-blue/60 mb-2 uppercase font-bold">Win Probability</p>
					<div class="flex items-center gap-3">
						<span class="text-lg font-black text-brand-blue w-16 text-right">{yellowProb.toFixed(1)}%</span>
						<div class="flex-1 h-3 bg-brand-light-gray rounded-full overflow-hidden flex">
							<div class="bg-brand-yellow h-full transition-all animate-fill-bar rounded-l-full" style="width: {yellowProb}%"></div>
							<div class="bg-brand-gray h-full transition-all animate-fill-bar rounded-r-full" style="width: {whiteProb}%"></div>
						</div>
						<span class="text-lg font-black text-gray-500 w-16">{whiteProb.toFixed(1)}%</span>
					</div>
				</div>
			{/if}

			<div class="flex gap-3">
				<button onclick={() => step = 2} class="flex-1 py-3 rounded-xl border border-brand-gray font-bold hover:bg-brand-cloud-blue transition-colors">Back</button>
				<button onclick={startScoring} class="flex-1 py-3 rounded-xl bg-green-600 text-white font-bold hover:bg-green-700 transition-colors">Enter Score</button>
			</div>
		</div>
	{/if}

	<!-- Step 4: Record Score -->
	{#if step === 4}
		<div class="animate-fade-in-up">
			<h1 class="text-2xl font-black text-gray-900 mb-4 text-center brand-heading">Record Score</h1>

			<!-- Quick score buttons -->
			<div class="flex justify-center gap-2 mb-4">
				<button onclick={() => setScore(10, 0)} class="px-3 py-1.5 rounded-lg border border-brand-gray text-xs font-bold hover:bg-brand-cloud-blue hover:border-brand-it-blue transition-all">10:0</button>
				<button onclick={() => setScore(10, 5)} class="px-3 py-1.5 rounded-lg border border-brand-gray text-xs font-bold hover:bg-brand-cloud-blue hover:border-brand-it-blue transition-all">10:5</button>
				<button onclick={() => setScore(10, 8)} class="px-3 py-1.5 rounded-lg border border-brand-gray text-xs font-bold hover:bg-brand-cloud-blue hover:border-brand-it-blue transition-all">10:8</button>
				<button onclick={() => setScore(10, 9)} class="px-3 py-1.5 rounded-lg border border-brand-gray text-xs font-bold hover:bg-brand-cloud-blue hover:border-brand-it-blue transition-all">10:9</button>
				<button onclick={() => setScore(5, 10)} class="px-3 py-1.5 rounded-lg border border-brand-gray text-xs font-bold hover:bg-brand-cloud-blue hover:border-brand-it-blue transition-all">5:10</button>
				<button onclick={() => setScore(0, 10)} class="px-3 py-1.5 rounded-lg border border-brand-gray text-xs font-bold hover:bg-brand-cloud-blue hover:border-brand-it-blue transition-all">0:10</button>
			</div>

			<div class="grid grid-cols-2 gap-6">
				<!-- Yellow score -->
				<div class="text-center">
					<div class="flex items-center gap-2 justify-center mb-1">
						<div class="w-4 h-4 rounded-full bg-brand-yellow"></div>
						<span class="font-black text-brand-blue">Yellow</span>
					</div>
					<p class="text-xs text-gray-600 truncate">{yellowAttacker?.displayName}</p>
					<p class="text-xs text-gray-600 truncate mb-2">{yellowDefender?.displayName}</p>
					<div class="flex items-center justify-center gap-3">
						<button onclick={() => changeYellow(-1)}
							class="w-12 h-12 rounded-full bg-brand-cloud-blue text-2xl font-black hover:bg-[#90d4f9] active:scale-95 transition-all flex items-center justify-center">-</button>
						<span class="text-5xl font-black tabular-nums w-16 text-center">{yellowScore}</span>
						<button onclick={() => changeYellow(1)}
							class="w-12 h-12 rounded-full bg-brand-cloud-blue text-2xl font-black hover:bg-[#90d4f9] active:scale-95 transition-all flex items-center justify-center">+</button>
					</div>
				</div>

				<!-- White score -->
				<div class="text-center">
					<div class="flex items-center gap-2 justify-center mb-1">
						<div class="w-4 h-4 rounded-full bg-brand-gray border border-gray-400"></div>
						<span class="font-black text-gray-700">White</span>
					</div>
					<p class="text-xs text-gray-600 truncate">{whiteAttacker?.displayName}</p>
					<p class="text-xs text-gray-600 truncate mb-2">{whiteDefender?.displayName}</p>
					<div class="flex items-center justify-center gap-3">
						<button onclick={() => changeWhite(-1)}
							class="w-12 h-12 rounded-full bg-brand-cloud-blue text-2xl font-black hover:bg-[#90d4f9] active:scale-95 transition-all flex items-center justify-center">-</button>
						<span class="text-5xl font-black tabular-nums w-16 text-center">{whiteScore}</span>
						<button onclick={() => changeWhite(1)}
							class="w-12 h-12 rounded-full bg-brand-cloud-blue text-2xl font-black hover:bg-[#90d4f9] active:scale-95 transition-all flex items-center justify-center">+</button>
					</div>
				</div>
			</div>

			<!-- ELO Preview -->
			{#if previewLoading}
				<div class="text-center text-gray-400 text-xs mt-4">Calculating ELO...</div>
			{:else if previewData && hasValidScore}
				{@const yAtt = getPreviewPlayer('YELLOW', 'ATTACKER')}
				{@const yDef = getPreviewPlayer('YELLOW', 'DEFENDER')}
				{@const wAtt = getPreviewPlayer('WHITE', 'ATTACKER')}
				{@const wDef = getPreviewPlayer('WHITE', 'DEFENDER')}
				<div class="mt-4 bg-brand-light-gray rounded-xl p-3 border border-brand-cloud-blue animate-fade-in-up">
					<p class="text-[10px] text-brand-blue/60 uppercase font-bold text-center mb-2">ELO Preview</p>
					<div class="grid grid-cols-2 gap-4 text-sm">
						<div class="space-y-1">
							{#each [yAtt, yDef] as p}
								{#if p}
									<div class="flex items-center justify-between">
										<span class="text-gray-700 truncate">{p.displayName}</span>
										<span class="font-mono font-black {(p.eloChange ?? 0) >= 0 ? 'text-green-600' : 'text-red-500'}">
											{(p.eloChange ?? 0) >= 0 ? '+' : ''}{p.eloChange}
										</span>
									</div>
								{/if}
							{/each}
						</div>
						<div class="space-y-1">
							{#each [wAtt, wDef] as p}
								{#if p}
									<div class="flex items-center justify-between">
										<span class="text-gray-700 truncate">{p.displayName}</span>
										<span class="font-mono font-black {(p.eloChange ?? 0) >= 0 ? 'text-green-600' : 'text-red-500'}">
											{(p.eloChange ?? 0) >= 0 ? '+' : ''}{p.eloChange}
										</span>
									</div>
								{/if}
							{/each}
						</div>
					</div>
					{#if yAtt?.winProbability != null}
						<div class="flex items-center gap-2 mt-2">
							<span class="text-[10px] font-semibold text-brand-blue/70 tabular-nums">{yAtt.winProbability.toFixed(1)}%</span>
							<div class="flex-1 h-1.5 rounded-full overflow-hidden flex bg-brand-light-gray">
								<div class="bg-brand-yellow h-full rounded-l-full" style="width: {yAtt.winProbability}%"></div>
								<div class="bg-brand-gray h-full rounded-r-full" style="width: {wAtt?.winProbability ?? 0}%"></div>
							</div>
							<span class="text-[10px] font-semibold text-gray-400 tabular-nums">{wAtt?.winProbability?.toFixed(1)}%</span>
						</div>
					{/if}
				</div>
			{/if}

			<div class="flex gap-3 mt-6">
				<button onclick={() => step = 3} class="flex-1 py-3 rounded-xl border border-brand-gray font-bold hover:bg-brand-cloud-blue transition-colors">Back</button>
				<button onclick={saveMatch} disabled={saving || !hasValidScore}
					class="flex-1 py-3 rounded-xl bg-brand-blue text-white font-bold hover:bg-brand-blue/90 disabled:bg-brand-gray transition-colors">
					{saving ? 'Saving...' : 'Save Match'}
				</button>
			</div>
		</div>
	{/if}
</div>
