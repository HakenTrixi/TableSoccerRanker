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
</script>

<div class="bg-white rounded-xl shadow-sm p-4 {highlightPlayer ? (highlightedOnYellow || highlightedOnWhite ? 'ring-2 ring-blue-200' : '') : ''}">
	<div class="flex items-center gap-4">
		<!-- Yellow team -->
		<div class="flex-1 text-right">
			<div class="space-y-0.5">
				{#each yellowPlayers as p}
					<p class="text-sm {yellowWon ? 'font-semibold text-gray-900' : 'text-gray-600'}">
						<span class="text-[10px] text-gray-400 uppercase">{p.playerRole[0]}</span>
						<span class="{highlightPlayer === p.userId ? 'underline decoration-blue-400' : ''}">{p.displayName}</span>
						{#if p.eloChange != null}
							<span class="text-[10px] font-mono ml-0.5 {p.eloChange >= 0 ? 'text-green-600' : 'text-red-500'}">
								{p.eloChange >= 0 ? '+' : ''}{p.eloChange}
							</span>
						{/if}
					</p>
				{/each}
			</div>
		</div>

		<!-- Score -->
		<div class="flex items-center gap-2 shrink-0">
			<div class="w-3 h-3 rounded-full bg-yellow-400"></div>
			<span class="text-2xl font-bold tabular-nums {yellowWon ? 'text-gray-900' : 'text-gray-400'}">
				{match.yellowScore}
			</span>
			<span class="text-gray-300 text-lg">:</span>
			<span class="text-2xl font-bold tabular-nums {whiteWon ? 'text-gray-900' : 'text-gray-400'}">
				{match.whiteScore}
			</span>
			<div class="w-3 h-3 rounded-full bg-gray-300 border border-gray-400"></div>
		</div>

		<!-- White team -->
		<div class="flex-1">
			<div class="space-y-0.5">
				{#each whitePlayers as p}
					<p class="text-sm {whiteWon ? 'font-semibold text-gray-900' : 'text-gray-600'}">
						{#if p.eloChange != null}
							<span class="text-[10px] font-mono mr-0.5 {p.eloChange >= 0 ? 'text-green-600' : 'text-red-500'}">
								{p.eloChange >= 0 ? '+' : ''}{p.eloChange}
							</span>
						{/if}
						<span class="{highlightPlayer === p.userId ? 'underline decoration-blue-400' : ''}">{p.displayName}</span>
						<span class="text-[10px] text-gray-400 uppercase">{p.playerRole[0]}</span>
					</p>
				{/each}
			</div>
		</div>
	</div>

	<!-- ELO info row -->
	{#if hasEloData}
		<div class="flex justify-between text-[10px] text-gray-400 mt-2 px-1">
			<span>ELO {yellowPlayers[0]?.teamElo?.toFixed(0)}</span>
			<span>
				P: {yellowPlayers[0]?.winProbability?.toFixed(1)}% - {whitePlayers[0]?.winProbability?.toFixed(1)}%
			</span>
			<span>ELO {whitePlayers[0]?.teamElo?.toFixed(0)}</span>
		</div>
	{/if}

	<p class="text-[11px] text-gray-400 text-center mt-1">{date}</p>
</div>
