export type SortKey = "type" | "amount" | "status" | "date";
export type SortDirection = "asc" | "desc";

export interface SortState {
  key: SortKey;
  direction: SortDirection;
}
