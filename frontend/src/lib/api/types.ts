export interface User {
	id: string;
	displayName: string;
	email: string;
	avatarUrl: string | null;
	role: 'ADMIN' | 'PLAYER';
	eloRating: number;
	attackerElo: number;
	defenderElo: number;
	active: boolean;
}

export interface AdminUser extends User {
	hasGoogleLinked: boolean;
	hasMatches: boolean;
}

export interface MatchPlayer {
	userId: string;
	displayName: string;
	avatarUrl: string | null;
	teamColor: 'YELLOW' | 'WHITE';
	playerRole: 'ATTACKER' | 'DEFENDER';
	eloBefore: number | null;
	eloAfter: number | null;
	eloChange: number | null;
	teamElo: number | null;
	winProbability: number | null;
}

export interface Match {
	id: string;
	yellowScore: number;
	whiteScore: number;
	playedAt: string;
	players: MatchPlayer[];
}

export interface Page<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	number: number;
	size: number;
}

export interface PlayerRanking {
	rank: number;
	userId: string;
	displayName: string;
	avatarUrl: string | null;
	score: number;
}

export interface TeamSuggestion {
	yellowAttacker: User;
	yellowDefender: User;
	whiteAttacker: User;
	whiteDefender: User;
}

export interface EloSnapshot {
	date: string;
	eloRating: number;
}

export interface EloDataPoint {
	matchId: string;
	playedAt: string;
	eloAfter: number;
}

export interface PlayerEloTimeline {
	userId: string;
	displayName: string;
	avatarUrl: string | null;
	dataPoints: EloDataPoint[];
}

export interface CompanyStats {
	totalMatches: number;
	totalGoals: number;
	mostActivePlayer: { userId: string; displayName: string; value: number } | null;
	topScorer: { userId: string; displayName: string; value: number } | null;
	biggestWin: { matchId: string; goalDiff: number; description: string } | null;
	longestWinStreak: { userId: string; displayName: string; streak: number } | null;
	longestLoseStreak: { userId: string; displayName: string; streak: number } | null;
	mostCommonPairing: { player1Id: string; player1Name: string; player2Id: string; player2Name: string; count: number } | null;
	currentStreaks: { userId: string; displayName: string; type: 'WIN' | 'LOSE'; count: number }[];
	currentBludistak: { userId: string; displayName: string; wins: number; month: string } | null;
	mostBludistakWins: { userId: string; displayName: string; wins: number; month: string } | null;
	colorStats: { yellowWins: number; whiteWins: number; yellowAvgGoals: number; whiteAvgGoals: number };
	monthlyActivity: { month: string; matchCount: number }[];
}

export interface PlayerStats {
	userId: string;
	displayName: string;
	totalMatches: number;
	wins: number;
	losses: number;
	draws: number;
	winRate: number;
	totalGoalsScored: number;
	totalGoalsConceded: number;
	avgGoalsScoredPerMatch: number;
	avgGoalsConcededPerMatch: number;
	currentElo: number;
	attackerElo: number;
	defenderElo: number;
	highestEloEver: number | null;
	lowestEloEver: number | null;
	biggestEloGain: number | null;
	biggestEloLoss: number | null;
	averageEloChange: number;
	longestWinStreak: number;
	longestLoseStreak: number;
	biggestWin: { goalDiff: number; description: string } | null;
	currentStreak: { type: 'WIN' | 'LOSE' | 'NONE'; count: number };
	bludistakWins: number;
	bestPartner: { userId: string; displayName: string; matches: number; wins: number; winRate: number } | null;
	worstPartner: { userId: string; displayName: string; matches: number; wins: number; winRate: number } | null;
	nemesis: { userId: string; displayName: string; matches: number; losses: number; lossRate: number } | null;
	favoriteOpponent: { userId: string; displayName: string; matches: number; losses: number; lossRate: number } | null;
	attackerStats: { matches: number; wins: number; winRate: number; avgGoalDiff: number };
	defenderStats: { matches: number; wins: number; winRate: number; avgGoalDiff: number };
	yellowStats: { matches: number; wins: number; winRate: number; goalsScored: number; goalsConceded: number };
	whiteStats: { matches: number; wins: number; winRate: number; goalsScored: number; goalsConceded: number };
	recentForm: { matchId: string; won: boolean; goalDiff: number }[];
}

export interface MatchCreateRequest {
	yellowAttacker: string;
	yellowDefender: string;
	whiteAttacker: string;
	whiteDefender: string;
	yellowScore: number;
	whiteScore: number;
	playedAt?: string;
}
