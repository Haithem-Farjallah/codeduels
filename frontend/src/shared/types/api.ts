export interface StandardResponse<T> {
  success: boolean;
  status: number;
  data: T;
  message: string | null;
  timestamp: string;
}