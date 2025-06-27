import { useUserStore } from "@/lib/store/user-store";

describe("User Store", () => {
  const mockUser = {
    id: "123",
    email: "test@example.com",
    name: "Test User",
    username: "testuser",
    role: "user",
    avatar: "avatar.jpg",
    isMerchant: false,
    status: "active",
    kycLevel: 2,
    phoneVerified: true,
    documentVerified: true,
  };

  beforeEach(() => {
    // Clear the store before each test
    useUserStore.setState({ user: null });
    // Clear localStorage
    localStorage.clear();
  });

  describe("setUser", () => {
    it("should set user data", () => {
      const { setUser } = useUserStore.getState();
      setUser(mockUser);

      const state = useUserStore.getState();
      expect(state.user).toEqual(mockUser);
    });

    it("should set user to null", () => {
      const { setUser } = useUserStore.getState();
      setUser(null);

      const state = useUserStore.getState();
      expect(state.user).toBeNull();
    });
  });

  describe("updateUsername", () => {
    it("should update username when user exists", () => {
      const { setUser, updateUsername } = useUserStore.getState();
      setUser(mockUser);

      updateUsername("newusername");

      const state = useUserStore.getState();
      expect(state.user?.username).toBe("newusername");
      // Verify other user properties remain unchanged
      expect(state.user?.id).toBe(mockUser.id);
      expect(state.user?.email).toBe(mockUser.email);
    });

    it("should handle updateUsername when user is null", () => {
      const { updateUsername } = useUserStore.getState();
      updateUsername("newusername");

      const state = useUserStore.getState();
      expect(state.user).toBeNull();
    });
  });

  describe("logout", () => {
    beforeEach(() => {
      // Set up localStorage with a token
      localStorage.setItem("token", "test-token");
    });

    it("should clear user data and remove token from localStorage", () => {
      const { setUser, logout } = useUserStore.getState();
      setUser(mockUser);

      logout();

      const state = useUserStore.getState();
      expect(state.user).toBeNull();
      expect(localStorage.getItem("token")).toBeFalsy();
    });

    it("should handle logout when user is already null", () => {
      const { logout } = useUserStore.getState();
      logout();

      const state = useUserStore.getState();
      expect(state.user).toBeNull();
      expect(localStorage.getItem("token")).toBeFalsy();
    });
  });

  describe("Store Initialization", () => {
    it("should initialize with null user", () => {
      const state = useUserStore.getState();
      expect(state.user).toBeNull();
    });

    it("should have all required methods", () => {
      const store = useUserStore.getState();
      expect(store.setUser).toBeDefined();
      expect(store.updateUsername).toBeDefined();
      expect(store.logout).toBeDefined();
    });
  });

  describe("Store Updates", () => {
    it("should maintain user data integrity when updating", () => {
      const { setUser, updateUsername } = useUserStore.getState();

      // Set initial user
      setUser(mockUser);

      // Update username
      updateUsername("newusername");

      const state = useUserStore.getState();
      expect(state.user).toEqual({
        ...mockUser,
        username: "newusername",
      });
    });

    it("should handle multiple updates in sequence", () => {
      const { setUser, updateUsername } = useUserStore.getState();

      setUser(mockUser);
      updateUsername("username1");
      updateUsername("username2");

      const state = useUserStore.getState();
      expect(state.user?.username).toBe("username2");
    });
  });

  describe("Edge Cases", () => {
    it("should handle setting user with missing optional fields", () => {
      const { setUser } = useUserStore.getState();
      const minimalUser = {
        id: "123",
        email: "test@example.com",
        name: "Test User",
        role: "user",
      };

      setUser(minimalUser);

      const state = useUserStore.getState();
      expect(state.user).toEqual(minimalUser);
    });

    it("should handle updating username with empty string", () => {
      const { setUser, updateUsername } = useUserStore.getState();
      setUser(mockUser);

      updateUsername("");

      const state = useUserStore.getState();
      expect(state.user?.username).toBe("");
    });

    it("should handle rapid state changes", () => {
      const { setUser, updateUsername, logout } = useUserStore.getState();

      setUser(mockUser);
      updateUsername("newname");
      logout();
      setUser(mockUser);

      const state = useUserStore.getState();
      expect(state.user).toEqual(mockUser);
    });
  });
});
