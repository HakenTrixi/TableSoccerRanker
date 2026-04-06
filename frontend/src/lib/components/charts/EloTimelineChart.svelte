<script lang="ts">
	import { api } from '$lib/api/client';
	import type { PlayerEloTimeline } from '$lib/api/types';

	const colors = [
		'#3b82f6', '#ef4444', '#22c55e', '#f59e0b', '#8b5cf6',
		'#ec4899', '#06b6d4', '#f97316', '#14b8a6', '#a855f7',
		'#e11d48', '#84cc16', '#0ea5e9', '#d946ef'
	];

	type Period = '30d' | '90d' | '365d' | 'all';
	let period: Period = $state('30d');
	let timelines: PlayerEloTimeline[] = $state([]);
	let loading = $state(false);
	let hiddenPlayers: Set<string> = $state(new Set());

	// Responsive: measure the chart area's actual rendered size
	let svgW = $state(600);
	let svgH = $state(400);

	let useWeeks = $derived(period === '365d' || period === 'all');

	const periodLabels: Record<Period, string> = {
		'30d': 'Last 30 days',
		'90d': 'Last 3 months',
		'365d': 'Last year',
		'all': 'All time'
	};

	async function loadData(p: Period) {
		period = p;
		loading = true;
		const params = new URLSearchParams();
		if (p !== 'all') {
			const days = p === '30d' ? 30 : p === '90d' ? 90 : 365;
			const from = new Date(Date.now() - days * 86400000).toISOString().split('T')[0];
			params.set('from', from);
		}
		timelines = await api.get<PlayerEloTimeline[]>(`/api/rankings/elo-timeline?${params}`);
		loading = false;
	}

	function togglePlayer(userId: string) {
		const next = new Set(hiddenPlayers);
		if (next.has(userId)) next.delete(userId);
		else next.add(userId);
		hiddenPlayers = next;
	}

	// Load on mount
	$effect(() => { loadData('30d'); });

	// --- Week helpers ---
	function getMonday(dateStr: string): string {
		const d = new Date(dateStr + 'T00:00:00');
		const day = d.getDay();
		const diff = d.getDate() - day + (day === 0 ? -6 : 1);
		d.setDate(diff);
		return d.toISOString().split('T')[0];
	}

	// --- X axis buckets: days or weeks ---
	let allBuckets = $derived(() => {
		const rawDays = [...new Set(
			timelines.flatMap(t => t.dataPoints)
				.map(p => new Date(p.playedAt).toISOString().split('T')[0])
		)].sort();
		if (!useWeeks) return rawDays;
		return [...new Set(rawDays.map(d => getMonday(d)))].sort();
	});

	// Visible timelines
	let visibleTimelines = $derived(timelines.filter(t => !hiddenPlayers.has(t.userId)));

	// For each player, get last ELO per bucket
	function playerBucketPoints(tl: PlayerEloTimeline) {
		const byBucket = new Map<string, number>();
		for (const p of tl.dataPoints) {
			const day = new Date(p.playedAt).toISOString().split('T')[0];
			const bucket = useWeeks ? getMonday(day) : day;
			byBucket.set(bucket, p.eloAfter);
		}
		return allBuckets()
			.filter(b => byBucket.has(b))
			.map(b => ({ bucket: b, elo: byBucket.get(b)! }));
	}

	// Dynamic Y based on visible players only
	let visibleElos = $derived(() => {
		return visibleTimelines.flatMap(tl => playerBucketPoints(tl).map(p => p.elo));
	});
	let minElo = $derived(visibleElos().length > 0 ? Math.min(...visibleElos()) - 30 : 980);
	let maxElo = $derived(visibleElos().length > 0 ? Math.max(...visibleElos()) + 30 : 1020);
	let eloRange = $derived(maxElo - minElo || 1);

	// Layout — derived from measured container size
	let padL = 52;
	let padR = 16;
	let padT = 16;
	let padB = 36;
	let buckets = $derived(allBuckets());
	let plotH = $derived(svgH - padT - padB);
	let plotW = $derived(svgW - padL - padR);

	function xPos(bucket: string) {
		const idx = buckets.indexOf(bucket);
		return padL + (idx / Math.max(buckets.length - 1, 1)) * plotW;
	}

	function yPos(elo: number) {
		return padT + ((maxElo - elo) / eloRange) * plotH;
	}

	// Y grid: nice round steps
	let yGridLines = $derived(() => {
		const range = maxElo - minElo;
		let step = 50;
		if (range > 800) step = 200;
		else if (range > 400) step = 100;
		else if (range > 150) step = 50;
		else if (range > 60) step = 20;
		else step = 10;
		const lines: number[] = [];
		const start = Math.ceil(minElo / step) * step;
		for (let v = start; v <= maxElo; v += step) lines.push(v);
		return lines;
	});

	// X axis labels — smart spacing
	let xLabels = $derived(() => {
		const b = buckets;
		if (b.length <= 14) return b;
		const step = Math.ceil(b.length / 12);
		return b.filter((_, i) => i % step === 0 || i === b.length - 1);
	});

	function formatXLabel(bucket: string): string {
		const d = new Date(bucket + 'T00:00:00');
		if (useWeeks || period === '90d') {
			return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
		}
		return bucket.slice(5);
	}
</script>

<!-- Period selector -->
<div class="flex gap-1 mb-4 bg-gray-100 rounded-lg p-0.5">
	{#each (['30d', '90d', '365d', 'all'] as const) as p}
		<button
			onclick={() => loadData(p)}
			class="flex-1 py-1.5 px-2 rounded-md text-xs font-medium transition-all
				{period === p ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700'}"
		>
			{periodLabels[p]}
		</button>
	{/each}
</div>

{#if loading}
	<div class="text-center py-12 text-gray-400 text-sm">Loading...</div>
{:else if timelines.length === 0}
	<p class="text-gray-400 text-sm text-center py-12">No ELO data in this period.</p>
{:else}
	<!-- Clickable Legend -->
	<div class="flex flex-wrap gap-x-4 gap-y-1.5 mb-4">
		{#each timelines as tl, i}
			{@const isHidden = hiddenPlayers.has(tl.userId)}
			{@const color = colors[i % colors.length]}
			{@const lastElo = tl.dataPoints[tl.dataPoints.length - 1]?.eloAfter}
			<button
				onclick={() => togglePlayer(tl.userId)}
				class="flex items-center gap-1.5 text-xs py-0.5 px-1 rounded transition-opacity
					{isHidden ? 'opacity-35' : 'opacity-100'} hover:bg-gray-100"
			>
				<div class="w-3 h-3 rounded-full shrink-0" style="background: {color}"></div>
				<span class="font-medium {isHidden ? 'line-through text-gray-400' : 'text-gray-700'}">{tl.displayName}</span>
				{#if lastElo != null}
					<span class="text-gray-400">({lastElo})</span>
				{/if}
			</button>
		{/each}
	</div>

	{#if visibleTimelines.length === 0}
		<p class="text-gray-400 text-sm text-center py-12">Click a player above to show their ELO.</p>
	{:else}
		<!-- Chart area: fills remaining viewport height via CSS -->
		<div
			class="chart-area"
			bind:clientWidth={svgW}
			bind:clientHeight={svgH}
		>
			<svg
				viewBox="0 0 {svgW} {svgH}"
				width="100%"
				height="100%"
				role="img"
				aria-label="ELO Timeline Chart"
			>
				<!-- Y grid lines -->
				{#each yGridLines() as elo}
					<line x1={padL} y1={yPos(elo)} x2={svgW - padR} y2={yPos(elo)} stroke="#f0f0f0" stroke-width="1" />
					<text x={padL - 6} y={yPos(elo) + 4} fill="#9ca3af" font-size="11" text-anchor="end" font-family="monospace">{elo}</text>
				{/each}

				<!-- 1000 baseline if visible -->
				{#if minElo < 1000 && maxElo > 1000}
					<line x1={padL} y1={yPos(1000)} x2={svgW - padR} y2={yPos(1000)}
						stroke="#d1d5db" stroke-width="1" stroke-dasharray="4,3" />
				{/if}

				<!-- X axis labels -->
				{#each xLabels() as bucket}
					<text x={xPos(bucket)} y={svgH - 8} fill="#9ca3af" font-size="10" text-anchor="middle">
						{formatXLabel(bucket)}
					</text>
					<line x1={xPos(bucket)} y1={padT} x2={xPos(bucket)} y2={svgH - padB} stroke="#f9fafb" stroke-width="1" />
				{/each}

				<!-- Lines per visible player -->
				{#each visibleTimelines as tl}
					{@const ti = timelines.indexOf(tl)}
					{@const color = colors[ti % colors.length]}
					{@const pts = playerBucketPoints(tl)}
					{#if pts.length > 0}
						<polyline
							fill="none"
							stroke={color}
							stroke-width="2"
							stroke-linejoin="round"
							stroke-linecap="round"
							points={pts.map(p => `${xPos(p.bucket)},${yPos(p.elo)}`).join(' ')}
						/>
						{#each pts as p}
							<circle cx={xPos(p.bucket)} cy={yPos(p.elo)} r="3.5" fill={color} stroke="white" stroke-width="1.5" class="cursor-pointer">
								<title>{tl.displayName}: {p.elo} ({useWeeks ? 'week of ' : ''}{p.bucket})</title>
							</circle>
						{/each}
					{/if}
				{/each}
			</svg>
		</div>
	{/if}
{/if}

<style>
	.chart-area {
		/* Fill remaining viewport height, min 300px */
		width: 100%;
		min-height: 300px;
		height: calc(100vh - 22rem);
		height: calc(100dvh - 22rem);
	}
</style>
