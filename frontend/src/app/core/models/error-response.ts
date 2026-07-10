export interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string;
  fieldErrors: Record<string, string>;
}
