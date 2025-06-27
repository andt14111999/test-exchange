export interface UserData {
  id: number;
  email: string;
  role: string;
  created_at: string;
  updated_at: string;
  display_name?: string;
  avatar_url?: string;
  username?: string;
  status?: string;
  kyc_level?: number;
  phone_verified?: boolean;
  document_verified?: boolean;
}
