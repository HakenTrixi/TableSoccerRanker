<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import type { CompanyStats } from '$lib/api/types';
	import BludistakIcon from '$lib/components/ui/BludistakIcon.svelte';

	let stats: CompanyStats | null = $state(null);
	let totalColorMatches = $derived(stats ? stats.colorStats.yellowWins + stats.colorStats.whiteWins : 0);

	onMount(async () => {
		stats = await api.get<CompanyStats>('/api/stats/company');
	});
</script>

{#if !stats}
	<div class="text-center py-12 text-gray-400">Loading...</div>
{:else}
	<div class="space-y-6">
		<h1 class="text-2xl font-bold text-gray-900">Company Stats</h1>

		<!-- Zlatý Bludišťák -->
		{#if stats.currentBludistak}
			<div class="bg-gradient-to-r from-amber-50 via-yellow-50 to-amber-50 border-2 border-amber-200 rounded-xl p-5 shadow-sm">
				<div class="flex items-center gap-4">
					<BludistakIcon size={64} />
					<div class="flex-1">
						<p class="text-xs font-semibold text-amber-700 uppercase tracking-wide">Zlatý Bludišťák - Current Champion</p>
						<p class="text-2xl font-bold text-gray-900">{stats.currentBludistak.displayName}</p>
						<p class="text-sm text-amber-600">
							Won {stats.currentBludistak.wins}x total
						</p>
					</div>
					{#if stats.mostBludistakWins}
						<div class="text-center hidden sm:block border-l border-amber-200 pl-4">
							<p class="text-[10px] font-semibold text-amber-600 uppercase">Most Titles</p>
							<p class="text-lg font-bold text-gray-900">{stats.mostBludistakWins.displayName}</p>
							<p class="text-sm text-amber-600">{stats.mostBludistakWins.wins}x winner</p>
						</div>
					{/if}
				</div>
			</div>
		{/if}

		<!-- Overview cards -->
		<div class="grid grid-cols-2 md:grid-cols-4 gap-3">
			<div class="bg-white rounded-xl shadow-sm p-4 text-center">
				<p class="text-3xl font-bold text-gray-900">{stats.totalMatches}</p>
				<p class="text-xs text-gray-500 mt-1">Total Matches</p>
			</div>
			<div class="bg-white rounded-xl shadow-sm p-4 text-center">
				<p class="text-3xl font-bold text-gray-900">{stats.totalGoals}</p>
				<p class="text-xs text-gray-500 mt-1">Total Goals</p>
			</div>
			{#if stats.mostActivePlayer}
				<div class="bg-white rounded-xl shadow-sm p-4 text-center">
					<p class="text-lg font-bold text-gray-900">{stats.mostActivePlayer.displayName}</p>
					<p class="text-xs text-gray-500 mt-1">Most Active ({stats.mostActivePlayer.value} matches)</p>
				</div>
			{/if}
			{#if stats.topScorer}
				<div class="bg-white rounded-xl shadow-sm p-4 text-center">
					<p class="text-lg font-bold text-gray-900">{stats.topScorer.displayName}</p>
					<p class="text-xs text-gray-500 mt-1">Top Scorer ({stats.topScorer.value} goals)</p>
				</div>
			{/if}
		</div>

		<!-- Curiosities -->
		<div class="grid md:grid-cols-2 gap-4">
			{#if stats.biggestWin}
				<div class="bg-white rounded-xl shadow-sm p-4">
					<h2 class="font-semibold text-gray-900 mb-1">Biggest Win</h2>
					<p class="text-2xl font-bold">{stats.biggestWin.description}</p>
					<p class="text-sm text-gray-500">{stats.biggestWin.goalDiff} goal difference</p>
				</div>
			{/if}

			{#if stats.longestWinStreak}
				<div class="bg-white rounded-xl shadow-sm p-4">
					<h2 class="font-semibold text-gray-900 mb-1">Longest Win Streak</h2>
					<p class="text-lg font-bold text-green-600">{stats.longestWinStreak.displayName}</p>
					<p class="text-sm text-gray-500">{stats.longestWinStreak.streak} consecutive wins</p>
				</div>
			{/if}

			{#if stats.longestLoseStreak}
				<div class="bg-white rounded-xl shadow-sm p-4">
					<h2 class="font-semibold text-gray-900 mb-1">Longest Losing Streak</h2>
					<p class="text-lg font-bold text-red-600">{stats.longestLoseStreak.displayName}</p>
					<p class="text-sm text-gray-500">{stats.longestLoseStreak.streak} consecutive losses</p>
				</div>
			{/if}

			{#if stats.mostCommonPairing}
				<div class="bg-white rounded-xl shadow-sm p-4">
					<h2 class="font-semibold text-gray-900 mb-1">Iconic Duo</h2>
					<p class="text-lg font-bold">{stats.mostCommonPairing.player1Name} & {stats.mostCommonPairing.player2Name}</p>
					<p class="text-sm text-gray-500">{stats.mostCommonPairing.count} matches together</p>
				</div>
			{/if}
		</div>

		<!-- Current Streaks -->
		{#if stats.currentStreaks.length > 0}
			<div class="bg-white rounded-xl shadow-sm p-4">
				<h2 class="font-semibold text-gray-900 mb-3">Current Streaks</h2>
				<div class="grid grid-cols-2 md:grid-cols-3 gap-2">
					{#each stats.currentStreaks as streak}
						<div class="flex items-center gap-2 p-2 rounded-lg {streak.type === 'WIN' ? 'bg-green-50' : 'bg-red-50'}">
							<span class="text-lg font-bold {streak.type === 'WIN' ? 'text-green-600' : 'text-red-500'}">
								{streak.count}{streak.type === 'WIN' ? 'W' : 'L'}
							</span>
							<span class="text-sm text-gray-700 truncate">{streak.displayName}</span>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<!-- Color stats -->
		<div class="bg-white rounded-xl shadow-sm p-4">
			<h2 class="font-semibold text-gray-900 mb-4">Yellow vs White</h2>
			<div class="grid grid-cols-2 gap-6">
				<div class="text-center">
					<div class="w-6 h-6 rounded-full bg-yellow-400 mx-auto mb-2"></div>
					<p class="text-3xl font-bold text-gray-900">{stats.colorStats.yellowWins}</p>
					<p class="text-xs text-gray-500">Wins</p>
					<p class="text-sm text-gray-600 mt-1">{stats.colorStats.yellowAvgGoals.toFixed(1)} avg goals</p>
				</div>
				<div class="text-center">
					<div class="w-6 h-6 rounded-full bg-gray-300 border border-gray-400 mx-auto mb-2"></div>
					<p class="text-3xl font-bold text-gray-900">{stats.colorStats.whiteWins}</p>
					<p class="text-xs text-gray-500">Wins</p>
					<p class="text-sm text-gray-600 mt-1">{stats.colorStats.whiteAvgGoals.toFixed(1)} avg goals</p>
				</div>
			</div>
			{#if totalColorMatches > 0}
				<div class="mt-4 h-3 bg-gray-100 rounded-full overflow-hidden flex">
					<div class="bg-yellow-400 h-full" style="width: {(stats.colorStats.yellowWins / totalColorMatches) * 100}%"></div>
					<div class="bg-gray-400 h-full" style="width: {(stats.colorStats.whiteWins / totalColorMatches) * 100}%"></div>
				</div>
			{/if}
		</div>

		<!-- Monthly activity -->
		{#if stats.monthlyActivity.length > 0}
			<div class="bg-white rounded-xl shadow-sm p-4">
				<h2 class="font-semibold text-gray-900 mb-4">Monthly Activity</h2>
				<div class="flex items-end gap-1 h-32">
					{#each stats.monthlyActivity as month}
						{@const max = Math.max(...stats.monthlyActivity.map(m => m.matchCount))}
						{@const height = max > 0 ? (month.matchCount / max) * 100 : 0}
						<div class="flex-1 group relative">
							<div class="bg-blue-200 rounded-t hover:bg-blue-400 transition-colors"
								 style="height: {Math.max(height, 4)}%">
							</div>
							<div class="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-[10px] px-1.5 py-0.5 rounded opacity-0 group-hover:opacity-100 whitespace-nowrap z-10">
								{month.month}: {month.matchCount}
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}
	</div>
{/if}
