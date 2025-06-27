import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getTrades,
  getTrade,
  createTrade,
  markTradePaid,
  releaseTrade,
  disputeTrade,
  cancelTrade,
  TradeListParams,
  CreateTradeParams,
  DisputeTradeParams,
  CancelTradeParams,
  ApiTrade,
} from "../trades";

export interface PaymentDetails {
  bank_id?: string;
  bank_name?: string;
  bank_account_name?: string;
  bank_account_number?: string;
  bank_branch?: string;
  [key: string]: string | undefined;
}

// Hook for fetching trades list with optional filters
export function useTrades(params?: TradeListParams & { id?: string }) {
  return useQuery({
    queryKey: ["trades", params],
    queryFn: () => getTrades(params),
  });
}

// Hook for fetching a single trade by ID
export function useTrade(id: string) {
  return useQuery<ApiTrade>({
    queryKey: ["trade", id],
    queryFn: () => getTrade(id),
    enabled: !!id,
  });
}

// Hook for creating a new trade
export function useCreateTrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (params: CreateTradeParams) => createTrade(params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["trades"] });
    },
  });
}

// Hook for marking a trade as paid
export function useMarkTradePaid() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (params: {
      id: string;
      payment_receipt_details: {
        file: File;
        description: string;
      };
    }) => markTradePaid(params.id, params.payment_receipt_details),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["trades"] });
      queryClient.invalidateQueries({ queryKey: ["trade", data.id] });
    },
  });
}

// Hook for disputing a trade
export function useDisputeTrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, params }: { id: string; params: DisputeTradeParams }) =>
      disputeTrade(id, params),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["trades"] });
      queryClient.invalidateQueries({ queryKey: ["trade", data.id] });
    },
  });
}

// Hook for releasing funds for a trade
export function useReleaseTrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => releaseTrade(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["trades"] });
      queryClient.invalidateQueries({ queryKey: ["trade", data.id] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
    },
  });
}

// Hook for canceling a trade
export function useCancelTrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, params }: { id: string; params: CancelTradeParams }) =>
      cancelTrade(id, params),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["trades"] });
      queryClient.invalidateQueries({ queryKey: ["trade", data.id] });
    },
  });
}
