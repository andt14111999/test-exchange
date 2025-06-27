import { create } from "zustand";

interface User {
  id: string;
  email: string;
  name: string;
  username?: string | null;
  role: string;
  avatar?: string;
  isMerchant?: boolean;
  status?: string;
  kycLevel?: number;
  phoneVerified?: boolean;
  documentVerified?: boolean;
}

interface UserState {
  user: User | null;
  setUser: (user: User | null) => void;
  updateUsername: (username: string) => void;
  logout: () => void;
}

export const useUserStore = create<UserState>()((set) => ({
  user: null,
  setUser: (user) => {
    set({ user });
  },
  updateUsername: (username) => {
    set((state) => {
      const updatedUser = state.user ? { ...state.user, username } : null;
      return { user: updatedUser };
    });
  },
  logout: () => {
    localStorage.removeItem("token");
    set({ user: null });
  },
}));
