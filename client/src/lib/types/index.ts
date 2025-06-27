export type Network = "ERC20" | "TRC20" | "BEP20";
export type FiatCurrency = "VND" | "PHP" | "NGN";
export type OfferType = "BUY" | "SELL";
export type TradeStatus =
  | "PENDING"
  | "REJECTED"
  | "USDT_LOCKED"
  | "USDT_SENT"
  | "FIAT_SENT"
  | "COMPLETED"
  | "CANCELLED"
  | "PAYMENT_SENT"
  | "FIAT_RECEIVED"
  | "FIAT_LOCKED"
  | "PAID"
  | "DISPUTED";

export interface User {
  id: string;
  email: string;
  role: "MERCHANT" | "CUSTOMER";
  name: string;
  createdAt: Date;
  walletAddress?: string;
}

export interface Offer {
  id: string;
  type: OfferType;
  fiatCurrency: FiatCurrency;
  amount: number;
  minAmount: number;
  maxAmount: number;
  isActive: boolean;
}

export interface Trade {
  id: string;
  offerId: string;
  type: OfferType;
  merchantId: string;
  customerId: string;
  amount: number;
  status: TradeStatus;
  network: Network;
  fiatCurrency: FiatCurrency;
  rate: number;
  escrowAddress?: string;
  paymentProof?: string;
  createdAt: Date;
  updatedAt: Date;
  expiresAt: Date;
}

export interface PaymentMethod {
  id: string;
  userId: string;
  type: "BANK" | "E_WALLET";
  accountNumber: string;
  accountName: string;
  bankName?: string;
  isActive: boolean;
}

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type:
    | "TRADE_REQUEST"
    | "PAYMENT_SENT"
    | "PAYMENT_CONFIRMED"
    | "TRADE_COMPLETED"
    | "TRADE_CANCELLED";
  isRead: boolean;
  createdAt: Date;
}
