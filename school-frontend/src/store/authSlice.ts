import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { AuthResponse, User } from '@/types';

const ACCESS_TOKEN_KEY = 'sms-access-token';
const REFRESH_TOKEN_KEY = 'sms-refresh-token';
const USER_KEY = 'sms-user';

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  /**
   * Bumped every time the session's credentials change — a fresh login, or a
   * silent token refresh by the axios interceptor.
   *
   * <p>Exists so AccessProvider can re-read `GET /api/v1/me/access` on both
   * events. Watching `isAuthenticated` alone is not enough: a token refresh
   * leaves it `true` throughout, so nothing would tell the UI to re-check what
   * the user is allowed to do, and a permission an administrator changed
   * mid-session would keep its stale answer until the tab was reloaded.
   *
   * <p>A counter rather than a timestamp so it stays deterministic and cannot go
   * backwards if the client clock moves.
   */
  sessionEpoch: number;
}

function loadUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}

const initialState: AuthState = {
  user: loadUser(),
  accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
  refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
  isAuthenticated: Boolean(localStorage.getItem(ACCESS_TOKEN_KEY)),
  // Deliberately not persisted: a page reload mounts AccessProvider fresh, which
  // fetches once on its own. Restoring a previous epoch would only risk it
  // matching the value the provider already acted on.
  sessionEpoch: 0,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<AuthResponse>) => {
      const { accessToken, refreshToken, user } = action.payload;
      state.user = user;
      state.accessToken = accessToken;
      state.refreshToken = refreshToken;
      state.isAuthenticated = true;
      // Login: make AccessProvider fetch the caller's real grants rather than
      // trusting the permission list embedded in this response.
      state.sessionEpoch += 1;
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    },
    updateUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      localStorage.setItem(USER_KEY, JSON.stringify(action.payload));
    },
    setTokens: (
      state,
      action: PayloadAction<{ accessToken: string; refreshToken: string }>,
    ) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      // Token refresh. The new token's PERM_* authorities were rebuilt from
      // role_permissions server-side, so this is exactly the moment the UI's
      // notion of what the user may do can have drifted from the API's.
      state.sessionEpoch += 1;
      localStorage.setItem(ACCESS_TOKEN_KEY, action.payload.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, action.payload.refreshToken);
    },
    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      // Bumped on the way out too, so AccessProvider drops the previous user's
      // grants instead of leaving them readable to whoever logs in next.
      state.sessionEpoch += 1;
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    },
  },
});

export const { setCredentials, updateUser, setTokens, logout } = authSlice.actions;
export default authSlice.reducer;
