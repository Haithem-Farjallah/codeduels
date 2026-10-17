export type DuelStatus =
  | 'WAITING_FOR_OPPONENT'
  | 'SCHEDULED'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'CANCELED'
  | 'EXPIRED';

export interface DuelProblem {
  problemId: string;
  url: string | null;
  name: string;
}

export interface ProblemStatus {
  solved: boolean;
  attempts: number;
  penalty: number | null;
  solvedAtEpoch: number | null;
}

export interface PlayerScore {
  userId: string;
  handle: string;
  username: string | null;
  solvedCount: number;
  totalPenalty: number;
  problems: Record<string, ProblemStatus>;
}

export interface DuelSubmission {
  submissionId: number;
  handle: string;
  problemId: string;
  verdict: string;
  submittedAtEpoch: number;
  penalised: boolean;
}

export interface DuelState {
  duelId: string;
  roomCode: string | null;
  status: DuelStatus;
  problems: DuelProblem[];
  playerOne: PlayerScore | null;
  playerTwo: PlayerScore | null;
  scheduledAtEpoch: number | null;
  startedAtEpoch: number | null;
  endsAtEpoch: number | null;
  durationMinutes: number;
  startDelayMinutes: number;
  secondsRemaining: number;
  winnerHandle: string | null;
  /** newest first */
  submissions: DuelSubmission[];
}

export interface CreateDuelRequest {
  problemUrls: string[];
  handle: string;
  durationMinutes: number;
  startDelayMinutes: number;
}

export interface CreateDuelResponse {
  duelId: string;
  roomCode: string;
  shareableLink: string;
}

export interface JoinDuelRequest {
  handle: string;
}

export interface JoinDuelResponse {
  duelId: string;
  scheduledAtEpoch: number;
  durationMinutes: number;
}

export type DuelResult = 'WIN' | 'LOSS' | 'DRAW';

export interface DuelHistoryItem {
  duelId: string;
  result: DuelResult;
  yourHandle: string;
  opponentHandle: string | null;
  opponentUsername: string | null;
  problemCount: number;
  yourSolved: number;
  yourPenalty: number;
  opponentSolved: number;
  opponentPenalty: number;
  startedAt: string | null;
  endedAt: string | null;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

/** Messages on /topic/duel/{duelId} */
export type DuelEvent =
  | { eventType: 'PLAYER_JOINED'; state: DuelState }
  | { eventType: 'DUEL_START'; state: DuelState }
  | { eventType: 'SCOREBOARD_UPDATE'; player: PlayerScore; submission: DuelSubmission }
  | { eventType: 'DUEL_END'; state: DuelState }
  | { eventType: 'DUEL_CANCELED'; reason: string };
