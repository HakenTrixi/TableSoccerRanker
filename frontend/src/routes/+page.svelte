<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import type { PlayerRanking, Match, Page, CompanyStats } from '$lib/api/types';
	import Leaderboard from '$lib/components/ranking/Leaderboard.svelte';
	import MatchCard from '$lib/components/match/MatchCard.svelte';
	import EloTimelineChart from '$lib/components/charts/EloTimelineChart.svelte';
	import BludistakIcon from '$lib/components/ui/BludistakIcon.svelte';

	let longTermRankings: PlayerRanking[] = $state([]);
	let monthlyRankings: PlayerRanking[] = $state([]);
	let recentMatches: Match[] = $state([]);
	let companyStats: CompanyStats | null = $state(null);
	let activeTab: 'longterm' | 'monthly' | 'timeline' = $state('monthly');
	let loading = $state(true);
	let error: string | null = $state(null);

	// Monthly picker
	const now = new Date();
	let selectedMonth = $state(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`);
	let loadingMonthly = $state(false);

	let selectedMonthLabel = $derived(() => {
		const [y, m] = selectedMonth.split('-');
		return new Date(parseInt(y), parseInt(m) - 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
	});

	let isCurrentMonth = $derived(
		selectedMonth === `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
	);

	async function loadMonthlyRankings(month: string) {
		loadingMonthly = true;
		try {
			monthlyRankings = await api.get<PlayerRanking[]>(`/api/rankings/monthly?month=${month}`);
		} finally {
			loadingMonthly = false;
		}
	}

	function changeMonth(delta: number) {
		const [y, m] = selectedMonth.split('-').map(Number);
		const d = new Date(y, m - 1 + delta);
		selectedMonth = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
		loadMonthlyRankings(selectedMonth);
	}

	onMount(async () => {
		try {
			const [lt, monthly, matches, stats] = await Promise.all([
				api.get<PlayerRanking[]>('/api/rankings/long-term'),
				api.get<PlayerRanking[]>('/api/rankings/monthly'),
				api.get<Page<Match>>('/api/matches?size=5'),
				api.get<CompanyStats>('/api/stats/company')
			]);
			longTermRankings = lt;
			monthlyRankings = monthly;
			recentMatches = matches.content;
			companyStats = stats;
		} catch (e) {
			error = 'Failed to load rankings. Please try again.';
		} finally {
			loading = false;
		}
	});
</script>

<div class="space-y-6">
	<h1 class="text-2xl font-black text-gray-900 brand-heading">Rankings</h1>

	{#if loading}
		<div class="text-center py-12 text-gray-400">Loading...</div>
	{:else if error}
		<div class="bg-red-50 text-red-700 rounded-xl p-4 text-sm border border-red-100">{error}</div>
	{:else}

	<!-- Zlatý Bludišťák Banner -->
	{#if companyStats?.currentBludistak}
		<div class="bg-gradient-to-r from-[#fac400]/10 via-[#fac400]/5 to-[#fac400]/10 border-2 border-[#fac400]/30 rounded-2xl p-4 animate-fade-in-up">
			<div class="flex items-center gap-4">
				<BludistakIcon size={56} />
				<div class="flex-1 min-w-0">
					<p class="text-xs font-bold text-brand-blue uppercase tracking-wide">Zlatý Bludišťák</p>
					<p class="text-xl font-black text-gray-900 truncate">{companyStats.currentBludistak.displayName}</p>
					<p class="text-sm text-brand-blue/70">
						Champion since {new Date(companyStats.currentBludistak.month + '-01').toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
					</p>
				</div>
				{#if companyStats.mostBludistakWins && companyStats.mostBludistakWins.userId !== companyStats.currentBludistak.userId}
					<div class="text-right hidden sm:block">
						<p class="text-[10px] font-bold text-brand-blue/60 uppercase">Most Titles</p>
						<p class="text-sm font-black text-gray-900">{companyStats.mostBludistakWins.displayName}</p>
						<p class="text-xs text-brand-blue/70">{companyStats.mostBludistakWins.wins}x winner</p>
					</div>
				{:else if companyStats.currentBludistak}
					<div class="text-right hidden sm:block">
						<p class="text-[10px] font-bold text-brand-blue/60 uppercase">Total Titles</p>
						<p class="text-lg font-black text-brand-yellow">{companyStats.currentBludistak.wins}x</p>
					</div>
				{/if}
			</div>
		</div>
	{/if}

	<!-- Tab switcher -->
	<div class="flex gap-1 bg-brand-cloud-blue rounded-xl p-1">
		<button
			onclick={() => activeTab = 'longterm'}
			class="flex-1 py-2 px-3 rounded-lg text-sm font-bold transition-all duration-200
				{activeTab === 'longterm' ? 'bg-brand-blue shadow-sm text-white' : 'text-brand-blue/60 hover:text-brand-blue'}"
		>
			Long-Term (ELO)
		</button>
		<button
			onclick={() => activeTab = 'monthly'}
			class="flex-1 py-2 px-3 rounded-lg text-sm font-bold transition-all duration-200
				{activeTab === 'monthly' ? 'bg-brand-blue shadow-sm text-white' : 'text-brand-blue/60 hover:text-brand-blue'}"
		>
			Monthly
		</button>
		<button
			onclick={() => activeTab = 'timeline'}
			class="flex-1 py-2 px-3 rounded-lg text-sm font-bold transition-all duration-200
				{activeTab === 'timeline' ? 'bg-brand-blue shadow-sm text-white' : 'text-brand-blue/60 hover:text-brand-blue'}"
		>
			ELO Timeline
		</button>
	</div>

	{#if activeTab === 'longterm'}
		<Leaderboard rankings={longTermRankings} scoreLabel="ELO" />
	{:else if activeTab === 'monthly'}
		<!-- Month picker -->
		<div class="flex items-center justify-center gap-3">
			<button
				onclick={() => changeMonth(-1)}
				class="p-2 rounded-lg hover:bg-brand-cloud-blue text-brand-blue/60 hover:text-brand-blue transition-colors"
				aria-label="Previous month"
			>
				<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
				</svg>
			</button>
			<span class="text-lg font-bold text-gray-900 min-w-[160px] text-center">
				{selectedMonthLabel()}
			</span>
			<button
				onclick={() => changeMonth(1)}
				disabled={isCurrentMonth}
				class="p-2 rounded-lg hover:bg-brand-cloud-blue text-brand-blue/60 hover:text-brand-blue transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
				aria-label="Next month"
			>
				<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
				</svg>
			</button>
		</div>
		{#if loadingMonthly}
			<div class="text-center py-4 text-gray-400">Loading...</div>
		{:else}
			<Leaderboard rankings={monthlyRankings} scoreLabel="ELO" />
		{/if}
	{:else}
		<div class="bg-white rounded-2xl border border-brand-cloud-blue p-4">
			<h2 class="font-bold text-gray-900 mb-4">ELO Rating Over Time</h2>
			<EloTimelineChart />
		</div>
	{/if}

	<!-- Recent matches -->
	{#if activeTab !== 'timeline'}
		<div>
			<div class="flex items-center justify-between mb-3">
				<h2 class="text-lg font-bold text-gray-900">Recent Matches</h2>
				<a href="/matches" class="text-sm text-brand-blue hover:text-brand-it-blue font-semibold transition-colors">View all</a>
			</div>
			<div class="space-y-3">
				{#each recentMatches as match, i}
					<div class="animate-fade-in-up" style="--delay: {i * 80}ms">
						<MatchCard {match} />
					</div>
				{:else}
					<p class="text-gray-400 text-sm">No matches yet. Start playing!</p>
				{/each}
			</div>
		</div>
	{/if}
	{/if}
</div>
