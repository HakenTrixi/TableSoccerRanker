<script lang="ts">
	import type { PlayerRanking } from '$lib/api/types';

	let { rankings, scoreLabel = 'Score' }: { rankings: PlayerRanking[]; scoreLabel?: string } = $props();
</script>

<div class="bg-white rounded-2xl border border-brand-cloud-blue overflow-hidden">
	<!-- Desktop table -->
	<table class="w-full hidden md:table">
		<thead>
			<tr class="border-b border-brand-cloud-blue">
				<th class="px-4 py-3 text-left text-xs font-semibold text-brand-blue/60 uppercase">#</th>
				<th class="px-4 py-3 text-left text-xs font-semibold text-brand-blue/60 uppercase">Player</th>
				<th class="px-4 py-3 text-right text-xs font-semibold text-brand-blue/60 uppercase">{scoreLabel}</th>
			</tr>
		</thead>
		<tbody>
			{#each rankings as player, i}
				<tr
					class="border-b border-brand-cloud-blue/50 hover:bg-brand-cloud-blue transition-colors duration-150 animate-fade-in-up"
					style="--delay: {i * 40}ms"
				>
					<td class="px-4 py-3">
						<span class="text-sm font-black {i === 0 ? 'text-brand-yellow' : i === 1 ? 'text-gray-400' : i === 2 ? 'text-amber-600' : 'text-gray-500'}">
							{player.rank}
						</span>
					</td>
					<td class="px-4 py-3">
						<a href="/players/{player.userId}" class="flex items-center gap-3 hover:underline decoration-brand-yellow">
							{#if player.avatarUrl}
								<img src={player.avatarUrl} alt={player.displayName} class="w-8 h-8 rounded-full" />
							{:else}
								<div class="w-8 h-8 rounded-full bg-brand-cloud-blue flex items-center justify-center text-xs font-bold text-brand-blue">
									{player.displayName?.[0] ?? '?'}
								</div>
							{/if}
							<span class="font-semibold text-gray-900">{player.displayName}</span>
						</a>
					</td>
					<td class="px-4 py-3 text-right">
						<span class="font-mono font-bold text-gray-900">{Math.round(player.score)}</span>
					</td>
				</tr>
			{:else}
				<tr>
					<td colspan="3" class="px-4 py-8 text-center text-gray-400">No rankings yet</td>
				</tr>
			{/each}
		</tbody>
	</table>

	<!-- Mobile cards -->
	<div class="md:hidden divide-y divide-brand-cloud-blue/50">
		{#each rankings as player, i}
			<a
				href="/players/{player.userId}"
				class="flex items-center gap-3 px-4 py-3 hover:bg-brand-cloud-blue transition-colors duration-150 animate-fade-in-up"
				style="--delay: {i * 40}ms"
			>
				<span class="w-8 text-center text-sm font-black {i === 0 ? 'text-brand-yellow' : i === 1 ? 'text-gray-400' : i === 2 ? 'text-amber-600' : 'text-gray-500'}">
					{player.rank}
				</span>
				{#if player.avatarUrl}
					<img src={player.avatarUrl} alt={player.displayName} class="w-10 h-10 rounded-full" />
				{:else}
					<div class="w-10 h-10 rounded-full bg-brand-cloud-blue flex items-center justify-center text-sm font-bold text-brand-blue">
						{player.displayName?.[0] ?? '?'}
					</div>
				{/if}
				<div class="flex-1 min-w-0">
					<p class="font-semibold text-gray-900 truncate">{player.displayName}</p>
				</div>
				<span class="font-mono font-bold text-gray-900">{Math.round(player.score)}</span>
			</a>
		{:else}
			<div class="px-4 py-8 text-center text-gray-400">No rankings yet</div>
		{/each}
	</div>
</div>
