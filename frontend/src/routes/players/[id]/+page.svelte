<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import type { PlayerStats, User, PlayerEloTimeline, EloDataPoint } from '$lib/api/types';
	import BludistakIcon from '$lib/components/ui/BludistakIcon.svelte';

	let playerId = $derived($page.params.id);
	let player: User | null = $state(null);
	let stats: PlayerStats | null = $state(null);
	let allDataPoints: EloDataPoint[] = $state([]);
	let loadError: string | null = $state(null);

	// Chart state
	type Period = '30d' | '90d' | '365d' | 'all';
	let period: Period = $state('all');
	let chartW = $state(600);
	let chartH = $state(300);
	const periodLabels: Record<Period, string> = { '30d': '30d', '90d': '90d', '365d': '1y', 'all': 'All' };

	let useWeeks = $derived(period === '365d' || period === 'all');

	// Filter data by period
	let filteredPoints = $derived(() => {
		if (period === 'all') return allDataPoints;
		const days = period === '30d' ? 30 : period === '90d' ? 90 : 365;
		const cutoff = new Date(Date.now() - days * 86400000).toISOString();
		return allDataPoints.filter(p => p.playedAt >= cutoff);
	});

	// Week helper
	function getMonday(dateStr: string): string {
		const d = new Date(dateStr + 'T00:00:00');
		const day = d.getDay();
		d.setDate(d.getDate() - day + (day === 0 ? -6 : 1));
		return d.toISOString().split('T')[0];
	}

	// Bucket points by day or week
	let bucketedPoints = $derived(() => {
		const pts = filteredPoints();
		const byBucket = new Map<string, number>();
		for (const p of pts) {
			const day = new Date(p.playedAt).toISOString().split('T')[0];
			const bucket = useWeeks ? getMonday(day) : day;
			byBucket.set(bucket, p.eloAfter);
		}
		return [...byBucket.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([bucket, elo]) => ({ bucket, elo }));
	});

	let chartPoints = $derived(bucketedPoints());
	let minElo = $derived(chartPoints.length > 0 ? Math.min(...chartPoints.map(p => p.elo)) - 25 : 990);
	let maxElo = $derived(chartPoints.length > 0 ? Math.max(...chartPoints.map(p => p.elo)) + 25 : 1010);
	let eloRange = $derived(maxElo - minElo || 1);

	// Layout
	const padL = 50, padR = 12, padT = 12, padB = 32;
	let plotW = $derived(chartW - padL - padR);
	let plotH = $derived(chartH - padT - padB);

	function xPos(i: number) { return padL + (i / Math.max(chartPoints.length - 1, 1)) * plotW; }
	function yPos(elo: number) { return padT + ((maxElo - elo) / eloRange) * plotH; }

	let yGridLines = $derived(() => {
		const range = maxElo - minElo;
		let step = range > 600 ? 200 : range > 300 ? 100 : range > 120 ? 50 : range > 50 ? 20 : 10;
		const lines: number[] = [];
		for (let v = Math.ceil(minElo / step) * step; v <= maxElo; v += step) lines.push(v);
		return lines;
	});

	let xLabels = $derived(() => {
		if (chartPoints.length <= 10) return chartPoints.map((p, i) => ({ i, label: p.bucket }));
		const step = Math.ceil(chartPoints.length / 8);
		return chartPoints
			.filter((_, i) => i % step === 0 || i === chartPoints.length - 1)
			.map(p => ({ i: chartPoints.indexOf(p), label: p.bucket }));
	});

	function formatLabel(bucket: string): string {
		const d = new Date(bucket + 'T00:00:00');
		if (useWeeks || period === '90d') return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
		return bucket.slice(5);
	}

	// Gradient fill path
	let areaPath = $derived(() => {
		if (chartPoints.length < 2) return '';
		const bottom = padT + plotH;
		let d = `M${xPos(0)},${bottom}`;
		chartPoints.forEach((p, i) => { d += ` L${xPos(i)},${yPos(p.elo)}`; });
		d += ` L${xPos(chartPoints.length - 1)},${bottom} Z`;
		return d;
	});

	let linePath = $derived(() => {
		if (chartPoints.length < 2) return '';
		return chartPoints.map((p, i) => `${i === 0 ? 'M' : 'L'}${xPos(i)},${yPos(p.elo)}`).join(' ');
	});

	// Tooltip
	let hoverIdx: number | null = $state(null);

	// Performance ring data
	const RING_RADIUS = 38;
	const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;
	function ringOffset(pct: number) { return RING_CIRCUMFERENCE * (1 - pct / 100); }

	let performanceRings = $derived(stats ? [
		{ label: 'Attacker ELO', winRate: stats.attackerStats.winRate, matches: stats.attackerStats.matches, color: '#4595da', showMatches: true },
		{ label: 'Defender ELO', winRate: stats.defenderStats.winRate, matches: stats.defenderStats.matches, color: '#285694', showMatches: true },
		{ label: 'Yellow', winRate: stats.yellowStats.winRate, matches: stats.yellowStats.matches, color: '#fac400', showMatches: false },
		{ label: 'White', winRate: stats.whiteStats.winRate, matches: stats.whiteStats.matches, color: '#9ca3af', showMatches: false },
	] : []);

	onMount(async () => {
		try {
			const [p, s, timelines] = await Promise.all([
				api.get<User>(`/api/users/${playerId}`),
				api.get<PlayerStats>(`/api/stats/player/${playerId}`),
				api.get<PlayerEloTimeline[]>('/api/rankings/elo-timeline')
			]);
			player = p;
			stats = s;
			const tl = timelines.find(t => t.userId === playerId);
			allDataPoints = tl?.dataPoints ?? [];
		} catch (e) {
			loadError = 'Failed to load player profile. Please try again.';
		}
	});
</script>

{#if loadError}
	<div class="bg-red-50 text-red-700 rounded-lg p-4 text-sm">{loadError}</div>
{:else if !stats || !player}
	<div class="text-center py-12 text-gray-400">Loading...</div>
{:else}
	<div class="space-y-4">
		<!-- 1. Header Card -->
		<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 sm:p-6 animate-fade-in-up" style="--delay: 0ms">
			<div class="flex items-center gap-4">
				{#if player.avatarUrl}
					<img src={player.avatarUrl} alt={player.displayName} class="w-20 h-20 rounded-full ring-4 ring-brand-blue/20 shrink-0" />
				{:else}
					<div class="w-20 h-20 rounded-full ring-4 ring-brand-blue/20 bg-brand-cloud-blue flex items-center justify-center text-2xl font-bold text-brand-blue shrink-0">
						{player.displayName?.[0] ?? '?'}
					</div>
				{/if}
				<div class="flex-1 min-w-0">
					<div class="flex items-center gap-2">
						<h1 class="text-xl sm:text-2xl font-bold text-gray-900 truncate">{player.displayName}</h1>
						{#if stats.bludistakWins > 0}
							<div class="flex items-center gap-1 shrink-0">
								<BludistakIcon size={28} />
								<span class="text-sm font-bold text-amber-700">{stats.bludistakWins}x</span>
							</div>
						{/if}
					</div>
				</div>
				<div class="hidden sm:flex items-center gap-4 shrink-0">
					<div class="text-center">
						<p class="text-xs text-gray-400 uppercase tracking-wide">ELO</p>
						<p class="text-2xl font-bold text-green-600">{player.eloRating}</p>
					</div>
					<div class="w-px h-10 bg-gray-200"></div>
					<div class="text-center">
						<p class="text-xs text-gray-400 uppercase tracking-wide">ATT</p>
						<p class="text-2xl font-bold text-gray-700">{stats.attackerElo}</p>
					</div>
					<div class="w-px h-10 bg-gray-200"></div>
					<div class="text-center">
						<p class="text-xs text-gray-400 uppercase tracking-wide">DEF</p>
						<p class="text-2xl font-bold text-gray-700">{stats.defenderElo}</p>
					</div>
				</div>
			</div>
			<!-- Mobile ELO row -->
			<div class="flex sm:hidden items-center justify-around mt-4 pt-4 border-t border-gray-100">
				<div class="text-center">
					<p class="text-xs text-gray-400 uppercase tracking-wide">ELO</p>
					<p class="text-xl font-bold text-green-600">{player.eloRating}</p>
				</div>
				<div class="w-px h-8 bg-gray-200"></div>
				<div class="text-center">
					<p class="text-xs text-gray-400 uppercase tracking-wide">ATT</p>
					<p class="text-xl font-bold text-gray-700">{stats.attackerElo}</p>
				</div>
				<div class="w-px h-8 bg-gray-200"></div>
				<div class="text-center">
					<p class="text-xs text-gray-400 uppercase tracking-wide">DEF</p>
					<p class="text-xl font-bold text-gray-700">{stats.defenderElo}</p>
				</div>
			</div>
		</div>

		<!-- 2. Overview Card -->
		<div class="bg-white rounded-2xl border border-brand-cloud-blue p-5 sm:p-6 animate-fade-in-up" style="--delay: 60ms">
			<h2 class="font-bold text-gray-900 mb-4">Overview</h2>
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-0 md:divide-x md:divide-gray-200">
				<div class="text-center px-4">
					<p class="text-2xl sm:text-3xl font-bold text-gray-900">{stats.totalMatches}</p>
					<p class="text-xs text-gray-500 mt-1">Matches</p>
				</div>
				<div class="text-center px-4">
					<p class="text-2xl sm:text-3xl font-bold text-gray-900">{stats.winRate}%</p>
					<p class="text-xs text-gray-500 mt-1">Win Rate</p>
				</div>
				<div class="text-center px-4">
					<p class="text-2xl sm:text-3xl font-bold">
						<span class="text-green-600">{stats.wins}W</span>
						{' '}
						<span class="text-red-500">{stats.losses}L</span>
					</p>
					<p class="text-xs text-gray-500 mt-1">Record</p>
				</div>
				<div class="text-center px-4">
					<p class="text-2xl sm:text-3xl font-bold text-gray-900">{stats.avgGoalsScoredPerMatch}</p>
					<p class="text-xs text-gray-500 mt-1">Avg Goals/Match</p>
				</div>
			</div>
		</div>

		<!-- 3. Performance Card (Donut Rings) -->
		<div class="bg-white rounded-2xl border border-brand-cloud-blue p-5 sm:p-6 animate-fade-in-up" style="--delay: 120ms">
			<h2 class="font-bold text-gray-900 mb-6">Performance</h2>
			<div class="grid grid-cols-2 md:grid-cols-4 gap-6">
				{#each performanceRings as ring}
					{@const offset = ringOffset(ring.winRate)}
					<div class="flex flex-col items-center text-center">
						<svg width="100" height="100" viewBox="0 0 100 100">
							<circle cx="50" cy="50" r={RING_RADIUS} fill="none" stroke="#e4f5fd" stroke-width="7" />
							<circle cx="50" cy="50" r={RING_RADIUS} fill="none" stroke={ring.color} stroke-width="7"
								stroke-dasharray={RING_CIRCUMFERENCE} stroke-dashoffset={offset}
								stroke-linecap="round" transform="rotate(-90 50 50)"
								class="animate-ring-fill" style="--ring-offset: {offset}; --ring-circumference: {RING_CIRCUMFERENCE}" />
							<text x="50" y="50" text-anchor="middle" dominant-baseline="central"
								font-size="18" font-weight="bold" fill="#1f2937">{ring.winRate.toFixed(0)}%</text>
						</svg>
						<p class="text-sm font-semibold text-gray-700 mt-2">{ring.label}</p>
						<p class="text-xs text-gray-500">
							{ring.winRate.toFixed(0)}% win{#if ring.showMatches}, {ring.matches}{/if}
						</p>
					</div>
				{/each}
			</div>
		</div>

		<!-- 4. Streaks Card -->
		<div class="bg-white rounded-2xl border border-brand-cloud-blue p-5 sm:p-6 animate-fade-in-up" style="--delay: 180ms">
			<h2 class="font-bold text-gray-900 mb-4">Streaks</h2>
			<div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
				<div class="flex items-center justify-between p-3 bg-brand-light-gray rounded-xl">
					<span class="text-sm text-gray-600">Longest Win Streak:</span>
					<span class="text-lg font-bold text-green-600">{stats.longestWinStreak}</span>
				</div>
				<div class="flex items-center justify-between p-3 bg-brand-light-gray rounded-xl">
					<span class="text-sm text-gray-600">Longest Lose Streak:</span>
					<span class="text-lg font-bold text-red-500">{stats.longestLoseStreak}</span>
				</div>
				{#if stats.biggestWin}
					<div class="flex items-center justify-between p-3 bg-brand-light-gray rounded-xl">
						<span class="text-sm text-gray-600">Biggest Win (+{stats.biggestWin.goalDiff}):</span>
						<span class="text-lg font-bold text-gray-900">{stats.biggestWin.description}</span>
					</div>
				{/if}
				<div class="flex items-center justify-between p-3 bg-brand-light-gray rounded-xl">
					<span class="text-sm text-gray-600">Current Streak:</span>
					{#if stats.currentStreak.type === 'WIN'}
						<span class="text-lg font-bold text-green-600">{stats.currentStreak.count}W</span>
					{:else if stats.currentStreak.type === 'LOSE'}
						<span class="text-lg font-bold text-red-500">{stats.currentStreak.count}L</span>
					{:else}
						<span class="text-lg font-bold text-gray-400">-</span>
					{/if}
				</div>
			</div>
		</div>

		<!-- 5. ELO Progression Chart -->
		{#if allDataPoints.length > 1}
			<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 sm:p-6 animate-fade-in-up" style="--delay: 240ms">
				<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
					<h2 class="font-bold text-gray-900 shrink-0">ELO Progression</h2>
					<div class="flex items-center gap-3">
						<div class="flex gap-0.5 bg-brand-cloud-blue rounded-lg p-0.5">
							{#each (['30d', '90d', '365d', 'all'] as const) as p}
								<button
									onclick={() => period = p}
									class="py-1 px-2 sm:px-2.5 rounded-md text-xs font-medium transition-all
										{period === p ? 'bg-brand-blue text-white shadow-sm' : 'text-brand-blue/60 hover:text-brand-blue'}"
								>
									{periodLabels[p]}
								</button>
							{/each}
						</div>
					</div>
				</div>

				{#if chartPoints.length < 2}
					<p class="text-gray-400 text-sm text-center py-8">Not enough data for this period.</p>
				{:else}
					{@const currentElo = chartPoints[chartPoints.length - 1].elo}
					{@const eloDiff = chartPoints[chartPoints.length - 1].elo - chartPoints[0].elo}
					<div class="flex items-baseline justify-between mb-3">
						<div></div>
						<div class="text-right">
							<span class="text-3xl font-bold text-gray-900">{currentElo}</span>
							<p class="text-sm font-medium {eloDiff >= 0 ? 'text-green-600' : 'text-red-500'}">
								{eloDiff >= 0 ? '+' : ''}{eloDiff} in this period
							</p>
						</div>
					</div>

					<div
						class="player-chart-area"
						bind:clientWidth={chartW}
						bind:clientHeight={chartH}
						role="img"
						aria-label="ELO Progression Chart"
					>
						<svg viewBox="0 0 {chartW} {chartH}" width="100%" height="100%"
							onmouseleave={() => hoverIdx = null}
						>
							<defs>
								<linearGradient id="eloFill" x1="0" y1="0" x2="0" y2="1">
									<stop offset="0%" stop-color="#285694" stop-opacity="0.15" />
									<stop offset="100%" stop-color="#285694" stop-opacity="0.02" />
								</linearGradient>
							</defs>

							<!-- Y grid -->
							{#each yGridLines() as elo}
								<line x1={padL} y1={yPos(elo)} x2={chartW - padR} y2={yPos(elo)} stroke="#f3f4f6" stroke-width="1" />
								<text x={padL - 6} y={yPos(elo) + 4} fill="#9ca3af" font-size="10" text-anchor="end" font-family="monospace">{elo}</text>
							{/each}

							<!-- X labels -->
							{#each xLabels() as { i, label }}
								<text x={xPos(i)} y={chartH - 6} fill="#9ca3af" font-size="9" text-anchor="middle">{formatLabel(label)}</text>
							{/each}

							<!-- Gradient fill area -->
							<path d={areaPath()} fill="url(#eloFill)" />

							<!-- Line -->
							<path d={linePath()} fill="none" stroke="#285694" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />

							<!-- Hover hit areas -->
							{#each chartPoints as p, i}
								{@const x = xPos(i)}
								{@const halfGap = plotW / Math.max(chartPoints.length - 1, 1) / 2}
								<rect
									x={x - halfGap} y={padT} width={halfGap * 2} height={plotH}
									fill="transparent"
									onmouseenter={() => hoverIdx = i}
								/>
							{/each}

							<!-- Hover crosshair + tooltip -->
							{#if hoverIdx !== null && chartPoints[hoverIdx]}
								{@const hp = chartPoints[hoverIdx]}
								{@const hx = xPos(hoverIdx)}
								{@const hy = yPos(hp.elo)}
								<line x1={hx} y1={padT} x2={hx} y2={padT + plotH} stroke="#285694" stroke-width="1" stroke-dasharray="3,3" opacity="0.4" />
								<circle cx={hx} cy={hy} r="5" fill="#285694" stroke="white" stroke-width="2" />
								{@const tx = Math.min(Math.max(hx, padL + 40), chartW - padR - 40)}
								<rect x={tx - 38} y={hy - 30} width="76" height="22" rx="4" fill="#1f2937" opacity="0.9" />
								<text x={tx} y={hy - 15} fill="white" font-size="10" text-anchor="middle" font-weight="600">{hp.elo}</text>
								<rect x={tx - 32} y={hy - 52} width="64" height="18" rx="4" fill="#1f2937" opacity="0.7" />
								<text x={tx} y={hy - 39} fill="#d1d5db" font-size="9" text-anchor="middle">{formatLabel(hp.bucket)}</text>
							{/if}

							<!-- Static dots -->
							{#each chartPoints as p, i}
								{#if hoverIdx !== i}
									<circle cx={xPos(i)} cy={yPos(p.elo)} r="2.5" fill="#285694" opacity="0.6" />
								{/if}
							{/each}
						</svg>
					</div>
				{/if}
			</div>
		{/if}

		<!-- 6. Best Partner & Nemesis -->
		{#if stats.bestPartner || stats.nemesis}
			<div class="grid md:grid-cols-2 gap-4 animate-fade-in-up" style="--delay: 300ms">
				{#if stats.bestPartner}
					<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 sm:p-5">
						<h2 class="font-bold text-gray-900 mb-3">Best Partner</h2>
						<div class="flex items-center gap-3">
							<div class="w-12 h-12 rounded-full bg-brand-cloud-blue flex items-center justify-center text-lg font-bold text-brand-blue shrink-0">
								{stats.bestPartner.displayName?.[0] ?? '?'}
							</div>
							<div>
								<p class="text-lg font-semibold text-gray-900">{stats.bestPartner.displayName}</p>
								<p class="text-sm text-gray-500">{stats.bestPartner.winRate}% win rate ({stats.bestPartner.matches} matches)</p>
							</div>
						</div>
					</div>
				{/if}
				{#if stats.nemesis}
					<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 sm:p-5">
						<h2 class="font-bold text-gray-900 mb-3">Nemesis</h2>
						<div class="flex items-center gap-3">
							<div class="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center text-lg font-bold text-red-500 shrink-0">
								{stats.nemesis.displayName?.[0] ?? '?'}
							</div>
							<div>
								<p class="text-lg font-semibold text-gray-900">{stats.nemesis.displayName}</p>
								<p class="text-sm text-gray-500">{stats.nemesis.lossRate}% loss rate ({stats.nemesis.matches} matches)</p>
							</div>
						</div>
					</div>
				{/if}
			</div>
		{/if}

		<!-- 7. Recent Form -->
		<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4 sm:p-5 animate-fade-in-up" style="--delay: 360ms">
			<h2 class="font-bold text-gray-900 mb-3">Recent Form</h2>
			<div class="flex gap-1.5">
				{#each stats.recentForm as entry}
					<div class="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold
						{entry.won ? 'bg-green-500 text-white' : 'bg-red-500 text-white'}">
						{entry.won ? 'W' : 'L'}
					</div>
				{:else}
					<p class="text-gray-400 text-sm">No matches yet</p>
				{/each}
			</div>
		</div>
	</div>
{/if}

<style>
	.player-chart-area {
		width: 100%;
		height: 280px;
		min-height: 200px;
	}
	@media (min-height: 700px) {
		.player-chart-area { height: 320px; }
	}
	@media (min-height: 900px) {
		.player-chart-area { height: 380px; }
	}
</style>
