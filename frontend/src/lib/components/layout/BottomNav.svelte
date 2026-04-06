<script lang="ts">
	import { page } from '$app/stores';

	let currentPath = $derived($page.url.pathname);

	const navItems = [
		{ href: '/', label: 'Rankings', icon: '&#127942;' },
		{ href: '/next-match', label: 'Play', icon: '&#9917;' },
		{ href: '/matches', label: 'Matches', icon: '&#128203;' },
		{ href: '/stats', label: 'Stats', icon: '&#128200;' }
	];
</script>

<nav class="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 md:hidden z-50">
	<div class="flex justify-around items-center h-16">
		{#each navItems as item}
			<a
				href={item.href}
				class="flex flex-col items-center justify-center gap-0.5 min-w-[64px] py-1 transition-colors
					{currentPath === item.href ? 'text-blue-600' : 'text-gray-500'}"
			>
				<span class="text-xl">{@html item.icon}</span>
				<span class="text-[10px] font-medium">{item.label}</span>
			</a>
		{/each}
	</div>
</nav>
