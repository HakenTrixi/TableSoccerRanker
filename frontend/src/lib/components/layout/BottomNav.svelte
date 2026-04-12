<script lang="ts">
	import { page } from '$app/stores';

	let currentPath = $derived($page.url.pathname);

	const navItems = [
		{ href: '/', label: 'Rankings' },
		{ href: '/next-match', label: 'Play' },
		{ href: '/matches', label: 'Matches' },
		{ href: '/stats', label: 'Stats' }
	];
</script>

<nav class="fixed bottom-0 left-0 right-0 bg-white border-t border-brand-gray md:hidden z-50">
	<div class="flex justify-around items-center h-16">
		{#each navItems as item}
			{@const active = currentPath === item.href}
			<a
				href={item.href}
				class="flex flex-col items-center justify-center gap-0.5 min-w-[64px] py-1 transition-all duration-150 active:scale-95
					{active ? 'text-brand-blue' : 'text-gray-400'}"
			>
				<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
					{#if item.href === '/'}
						<!-- Trophy -->
						{#if active}
							<path stroke-linecap="round" stroke-linejoin="round" d="M16.5 18.75h-9m9 0a3 3 0 013 3h-15a3 3 0 013-3m9 0v-4.5A3.375 3.375 0 0019.875 11h-1.5a.375.375 0 01-.375-.375V7.5a.375.375 0 01.375-.375h1.5A2.625 2.625 0 0022.5 4.5v-.75a.375.375 0 00-.375-.375H1.875A.375.375 0 001.5 3.75v.75A2.625 2.625 0 004.125 7.125h1.5A.375.375 0 016 7.5v3.125a.375.375 0 01-.375.375h-1.5A3.375 3.375 0 007.5 14.25v4.5" fill="currentColor" stroke="none" />
						{:else}
							<path stroke-linecap="round" stroke-linejoin="round" d="M16.5 18.75h-9m9 0a3 3 0 013 3h-15a3 3 0 013-3m9 0v-3.375c0-.621-.504-1.125-1.125-1.125h-.871M7.5 18.75v-3.375c0-.621.504-1.125 1.125-1.125h.872m5.007 0H9.497m5.007 0a7.454 7.454 0 01-.982-3.172M9.497 14.25a7.454 7.454 0 00.981-3.172M5.25 4.236c-.982.143-1.954.317-2.916.52A6.003 6.003 0 007.73 9.728M5.25 4.236V4.5c0 2.108.966 3.99 2.48 5.228M5.25 4.236V2.721C7.456 2.41 9.71 2.25 12 2.25c2.291 0 4.545.16 6.75.47v1.516M18.75 4.236c.982.143 1.954.317 2.916.52A6.003 6.003 0 0016.27 9.728M18.75 4.236V4.5c0 2.108-.966 3.99-2.48 5.228m0 0a17.106 17.106 0 01-5.27.846m0 0a17.106 17.106 0 01-5.27-.846" />
						{/if}
					{:else if item.href === '/next-match'}
						<!-- Play / Whistle circle -->
						{#if active}
							<circle cx="12" cy="12" r="10" fill="currentColor" stroke="none" />
							<polygon points="10,8 16,12 10,16" fill="white" stroke="none" />
						{:else}
							<circle cx="12" cy="12" r="9" fill="none" />
							<polygon points="10,8 16,12 10,16" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
						{/if}
					{:else if item.href === '/matches'}
						<!-- Scoreboard / clipboard -->
						{#if active}
							<path d="M9 2a1 1 0 00-1 1v1H6a2 2 0 00-2 2v14a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-2V3a1 1 0 00-1-1H9z" fill="currentColor" stroke="none" />
							<path d="M8 11h3m-3 4h3m2-4h3m-3 4h3" stroke="white" stroke-width="1.5" stroke-linecap="round" />
						{:else}
							<path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H10.5A2.25 2.25 0 008.15 4.028m0 0a48.298 48.298 0 00-1.123.08C5.845 4.01 5 4.973 5 6.108V19.5a2.25 2.25 0 002.25 2.25h.75m0 0h4.5" />
						{/if}
					{:else}
						<!-- Stats / Chart -->
						{#if active}
							<rect x="3" y="3" width="18" height="18" rx="2" fill="currentColor" stroke="none" />
							<path d="M7 17V13m4 4V10m4 7V7" stroke="white" stroke-width="2" stroke-linecap="round" />
						{:else}
							<path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
						{/if}
					{/if}
				</svg>
				<span class="text-[10px] font-semibold">{item.label}</span>
				{#if active}
					<span class="w-1 h-1 rounded-full bg-brand-yellow -mt-0.5"></span>
				{/if}
			</a>
		{/each}
	</div>
</nav>
