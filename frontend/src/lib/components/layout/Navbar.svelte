<script lang="ts">
	import { page } from '$app/stores';
	import { invalidateAll } from '$app/navigation';

	let currentPath = $derived($page.url.pathname);
	let user = $derived($page.data.user);
	let menuOpen = $state(false);

	const navLinks = [
		{ href: '/', label: 'Rankings' },
		{ href: '/next-match', label: 'Next Match' },
		{ href: '/matches', label: 'Matches' },
		{ href: '/stats', label: 'Stats' }
	];

	async function logout() {
		await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
		await invalidateAll();
		window.location.href = '/auth/login';
	}
</script>

<svelte:window onclick={() => menuOpen = false} />

<nav class="bg-brand-blue hidden md:block">
	<div class="container mx-auto max-w-5xl px-4">
		<div class="flex items-center justify-between h-16">
			<a href="/" class="flex items-center gap-1 text-xl font-black tracking-tight">
				<span class="text-brand-yellow text-2xl">{"{"}</span>
				<span class="text-white">TS</span>
				<span class="text-brand-yellow text-2xl">{"}"}</span>
				<span class="text-white/60 font-semibold text-sm ml-1.5 hidden lg:inline">Table Soccer</span>
			</a>

			<div class="flex items-center gap-1">
				{#each navLinks as link}
					<a
						href={link.href}
						class="px-3 py-2 rounded-lg text-sm font-semibold transition-all duration-150
							{currentPath === link.href
								? 'bg-white/15 text-white'
								: 'text-white/70 hover:text-white hover:bg-white/10'}"
					>
						{link.label}
					</a>
				{/each}

				{#if user?.role === 'ADMIN'}
					<a
						href="/admin"
						class="px-3 py-2 rounded-lg text-sm font-semibold transition-all duration-150
							{currentPath === '/admin'
								? 'bg-white/15 text-white'
								: 'text-white/70 hover:text-white hover:bg-white/10'}"
					>
						Admin
					</a>
				{/if}
			</div>

			<div class="flex items-center gap-3">
				{#if user}
					<!-- Profile dropdown -->
					<div class="relative">
						<button
							onclick={(e) => { e.stopPropagation(); menuOpen = !menuOpen; }}
							class="flex items-center gap-2 text-sm text-white/80 hover:text-white cursor-pointer transition-colors"
						>
							{#if user.avatarUrl}
								<img src={user.avatarUrl} alt={user.displayName} class="w-8 h-8 rounded-full ring-2 ring-white/20" />
							{:else}
								<div class="w-8 h-8 rounded-full bg-brand-cloud-blue text-brand-blue flex items-center justify-center text-xs font-bold">
									{user.displayName?.[0] ?? '?'}
								</div>
							{/if}
							<span class="hidden lg:inline font-medium">{user.displayName}</span>
							<svg class="w-4 h-4 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
							</svg>
						</button>

						{#if menuOpen}
							<div class="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-lg border border-brand-cloud-blue py-1 z-50 animate-fade-in-up">
								<a href="/players/{user.id}" class="block px-4 py-2 text-sm text-gray-700 hover:bg-brand-cloud-blue transition-colors">
									My Profile
								</a>
								<a href="/settings" class="block px-4 py-2 text-sm text-gray-700 hover:bg-brand-cloud-blue transition-colors">
									Settings
								</a>
								<hr class="my-1 border-brand-cloud-blue" />
								<button
									onclick={logout}
									class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
								>
									Sign Out
								</button>
							</div>
						{/if}
					</div>
				{:else}
					<a href="/auth/login" class="px-4 py-2 bg-brand-yellow text-brand-blue rounded-lg text-sm font-bold hover:brightness-110 transition-all">
						Sign In
					</a>
				{/if}
			</div>
		</div>
	</div>
</nav>
