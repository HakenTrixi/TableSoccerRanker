<script lang="ts">
	import type { Match } from '$lib/api/types';

	let { match, highlightPlayer }: { match: Match; highlightPlayer?: string } = $props();

	let yellowPlayers = $derived(match.players.filter(p => p.teamColor === 'YELLOW'));
	let whitePlayers = $derived(match.players.filter(p => p.teamColor === 'WHITE'));
	let yellowWon = $derived(match.yellowScore > match.whiteScore);
	let whiteWon = $derived(match.whiteScore > match.yellowScore);
	let date = $derived(new Date(match.playedAt).toLocaleDateString('cs-CZ', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }));
	let hasEloData = $derived(yellowPlayers[0]?.eloChange != null);
	let highlightedOnYellow = $derived(highlightPlayer ? yellowPlayers.some(p => p.userId === highlightPlayer) : false);
	let highlightedOnWhite = $derived(highlightPlayer ? whitePlayers.some(p => p.userId === highlightPlayer) : false);

	let yellowProb = $derived(yellowPlayers[0]?.winProbability);
	let whiteProb = $derived(whitePlayers[0]?.winProbability);
	let hasProb = $derived(yellowProb != null && whiteProb != null);
</script>

<div
	class="match-card bg-white rounded-2xl border overflow-hidden hover-lift
		{highlightPlayer ? (highlightedOnYellow || highlightedOnWhite ? 'ring-2 ring-brand-it-blue border-brand-it-blue' : 'border-brand-cloud-blue') : 'border-brand-cloud-blue'}"
>
	<!-- Date - top right -->
	<div class="flex justify-end px-4 pt-2.5">
		<span class="text-[11px] text-gray-400 font-medium">{date}</span>
	</div>

	<!-- Main content: teams + score -->
	<div class="flex items-center gap-3 px-4 pb-1">
		<!-- Yellow team -->
		<div class="flex-1 text-right">
			<div class="space-y-1">
				{#each yellowPlayers as p}
					<div class="flex items-center justify-end gap-1.5">
						{#if p.eloChange != null}
							<span class="inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-bold
								{p.eloChange >= 0 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'}">
								{p.eloChange >= 0 ? '+' : ''}{p.eloChange}
							</span>
						{/if}
						<span class="text-sm {yellowWon ? 'font-bold text-gray-900' : 'text-gray-500'}
							{highlightPlayer === p.userId ? 'underline decoration-brand-yellow decoration-2' : ''}">
							{p.displayName}
						</span>
						<span class="w-5 h-5 rounded-full bg-brand-cloud-blue text-brand-blue flex items-center justify-center text-[9px] font-bold shrink-0">
							{p.playerRole[0]}
						</span>
					</div>
				{/each}
			</div>
		</div>

		<!-- Central score badge -->
		<div class="flex items-center gap-1.5 shrink-0">
			<div class="w-1.5 h-7 rounded-full bg-brand-yellow"></div>
			<div class="bg-white shadow-md rounded-xl px-3 py-1.5 flex items-center gap-2">
				<span class="text-3xl font-black tabular-nums {yellowWon ? 'text-brand-blue' : 'text-gray-300'}">
					{match.yellowScore}
				</span>
				<span class="text-gray-300 text-lg font-light">:</span>
				<span class="text-3xl font-black tabular-nums {whiteWon ? 'text-brand-blue' : 'text-gray-300'}">
					{match.whiteScore}
				</span>
			</div>
			<div class="w-1.5 h-7 rounded-full bg-brand-gray"></div>
		</div>

		<!-- White team -->
		<div class="flex-1">
			<div class="space-y-1">
				{#each whitePlayers as p}
					<div class="flex items-center gap-1.5">
						<span class="w-5 h-5 rounded-full bg-brand-cloud-blue text-brand-blue flex items-center justify-center text-[9px] font-bold shrink-0">
							{p.playerRole[0]}
						</span>
						<span class="text-sm {whiteWon ? 'font-bold text-gray-900' : 'text-gray-500'}
							{highlightPlayer === p.userId ? 'underline decoration-brand-yellow decoration-2' : ''}">
							{p.displayName}
						</span>
						{#if p.eloChange != null}
							<span class="inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-bold
								{p.eloChange >= 0 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'}">
								{p.eloChange >= 0 ? '+' : ''}{p.eloChange}
							</span>
						{/if}
					</div>
				{/each}
			</div>
		</div>
	</div>

	<!-- Probability bar + ELO info -->
	{#if hasEloData}
		<div class="px-4 pb-3 pt-1">
			{#if hasProb}
				<!-- Probability progress bar -->
				<div class="flex items-center gap-2 mb-1">
					<span class="text-[10px] font-semibold text-brand-blue/70 w-10 text-right tabular-nums">{yellowProb?.toFixed(1)}%</span>
					<div class="flex-1 h-1.5 rounded-full overflow-hidden flex bg-brand-light-gray">
						<div class="bg-brand-yellow h-full animate-fill-bar rounded-l-full" style="width: {yellowProb}%"></div>
						<div class="bg-brand-gray h-full animate-fill-bar rounded-r-full" style="width: {whiteProb}%"></div>
					</div>
					<span class="text-[10px] font-semibold text-gray-400 w-10 tabular-nums">{whiteProb?.toFixed(1)}%</span>
				</div>
			{/if}
			<!-- Team ELOs -->
			<div class="flex justify-between text-[10px] text-gray-400 px-0.5">
				<span>ELO {yellowPlayers[0]?.teamElo?.toFixed(0)}</span>
				<span>ELO {whitePlayers[0]?.teamElo?.toFixed(0)}</span>
			</div>
		</div>
	{/if}
</div>

<style>
	.match-card {
		background:
			linear-gradient(135deg, rgba(250, 196, 0, 0.05) 0%, rgba(250, 196, 0, 0.05) 42%, transparent 42%, transparent 100%),
			white;
	}
</style>
