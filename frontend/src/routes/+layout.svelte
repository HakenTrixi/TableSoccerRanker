<script lang="ts">
	import '../app.css';
	import { page } from '$app/stores';
	import Navbar from '$lib/components/layout/Navbar.svelte';
	import BottomNav from '$lib/components/layout/BottomNav.svelte';
	import type { Snippet } from 'svelte';

	let { children }: { children: Snippet } = $props();
	let isAuthPage = $derived($page.url.pathname.startsWith('/auth'));
</script>

<div class="min-h-screen bg-brand-light-gray flex flex-col">
	{#if !isAuthPage}
		<Navbar />
	{/if}
	<main class="flex-1 container mx-auto px-4 py-6 max-w-5xl pb-20 md:pb-6">
		{@render children()}
	</main>
	{#if !isAuthPage}
		<BottomNav />
	{/if}
</div>
