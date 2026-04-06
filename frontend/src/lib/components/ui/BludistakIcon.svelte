<script lang="ts">
	let { size = 48 }: { size?: number } = $props();
</script>

<!-- Zlatý Bludišťák — golden maze trophy inspired by Czech TV series Bludiště -->
<svg width={size} height={size} viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
	<!-- Glow effect -->
	<defs>
		<linearGradient id="goldGrad" x1="0" y1="0" x2="0" y2="1">
			<stop offset="0%" stop-color="#fbbf24" />
			<stop offset="50%" stop-color="#f59e0b" />
			<stop offset="100%" stop-color="#d97706" />
		</linearGradient>
		<linearGradient id="goldShine" x1="0" y1="0" x2="1" y2="1">
			<stop offset="0%" stop-color="#fde68a" />
			<stop offset="100%" stop-color="#f59e0b" />
		</linearGradient>
		<filter id="glow">
			<feGaussianBlur stdDeviation="1.5" result="blur" />
			<feMerge>
				<feMergeNode in="blur" />
				<feMergeNode in="SourceGraphic" />
			</feMerge>
		</filter>
	</defs>

	<!-- Trophy base -->
	<rect x="22" y="54" width="20" height="4" rx="2" fill="url(#goldGrad)" />
	<rect x="26" y="48" width="12" height="7" rx="1" fill="url(#goldGrad)" />

	<!-- Trophy cup body -->
	<path d="M16 10 C16 10 14 28 22 36 L26 48 L38 48 L42 36 C50 28 48 10 48 10 Z"
		fill="url(#goldShine)" stroke="#b45309" stroke-width="1.2" />

	<!-- Trophy handles -->
	<path d="M16 14 C10 14 8 22 14 26" fill="none" stroke="url(#goldGrad)" stroke-width="2.5" stroke-linecap="round" />
	<path d="M48 14 C54 14 56 22 50 26" fill="none" stroke="url(#goldGrad)" stroke-width="2.5" stroke-linecap="round" />

	<!-- Maze pattern inside trophy (inspired by Bludiště) -->
	<g filter="url(#glow)" stroke="#92400e" stroke-width="1.2" fill="none" stroke-linecap="round">
		<!-- Outer maze walls -->
		<rect x="24" y="16" width="16" height="16" rx="1" />
		<!-- Inner maze paths -->
		<path d="M24 20 L34 20" />
		<path d="M30 20 L30 24" />
		<path d="M26 24 L36 24" />
		<path d="M36 24 L36 28" />
		<path d="M28 28 L40 28" />
		<path d="M28 28 L28 32" />
		<!-- Center dot (goal) -->
		<circle cx="32" cy="26" r="1.5" fill="#92400e" />
	</g>

	<!-- Star on top -->
	<polygon points="32,2 34,8 40,8 35,12 37,18 32,14 27,18 29,12 24,8 30,8"
		fill="#fbbf24" stroke="#b45309" stroke-width="0.8" />
</svg>
