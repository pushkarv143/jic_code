import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import accessApi from '@/api/accessApi';
import { useAppSelector } from '@/store/hooks';
import type { Homeroom, MyAccess, Permission, Role } from '@/types';

export const MANAGEMENT_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
export const TEACHING_ROLES: Role[] = ['TEACHER', 'CLASS_TEACHER'];
export const SELF_SCOPED_ROLES: Role[] = ['STUDENT', 'PARENT'];

/**
 * Live authorization state — the single answer to "may this user do this?".
 *
 * <p><b>Where it comes from.</b> `GET /api/v1/me/access`, re-read on every event
 * that can change what the user is allowed to do:
 *
 * <ul>
 *   <li><b>login</b> — the login response carries a permission list, but it is a
 *       snapshot written to `localStorage`, and it is not what the API enforces;</li>
 *   <li><b>token refresh</b> — the axios interceptor silently swaps tokens, and the
 *       new token's authorities were rebuilt from `role_permissions` server-side, so
 *       this is precisely when the UI's copy can have gone stale;</li>
 *   <li><b>page load</b> — the provider mounts and fetches once;</li>
 *   <li><b>logout</b> — grants are dropped rather than left for the next user;</li>
 *   <li><b>{@link AccessContextValue.refresh}</b> — after an administrator edits
 *       roles or modules on the settings screen.</li>
 * </ul>
 *
 * Login, refresh and logout are all observed through `auth.sessionEpoch`, which
 * those three reducers increment. Watching `isAuthenticated` would miss the token
 * refresh entirely, since it stays `true` across one.
 *
 * <p><b>It fails closed.</b> Until the fetch resolves, and if it fails, `can()`
 * answers `false` for everything. An earlier draft fell back to the login-cached
 * grants and treated an empty set as "grants unknown, allow and let the API
 * refuse" — convenient, but it means a user is briefly offered actions their role
 * does not have, which is the behaviour this whole change exists to remove. The
 * shell waits on {@link AccessContextValue.settled} instead, so nothing is rendered
 * from a guess, and a failed fetch surfaces as an error the user can retry rather
 * than as a silently over- or under-powered UI.
 *
 * <p>None of this is a security boundary. Every endpoint re-checks the same grant
 * on every request — `CustomUserDetailsService` rebuilds the caller's authorities
 * from `role_permissions` per request, filtered by the enabled modules. This only
 * decides what the interface *offers*.
 */
export interface AccessContextValue {
  access: MyAccess | null;
  loading: boolean;
  /** True once a fetch has completed, successfully or not. Gate the shell on this. */
  settled: boolean;
  /** Set when the last fetch failed. `can()` denies everything while this is set. */
  error: boolean;
  role: Role | undefined;
  permissions: Set<Permission>;
  enabledModules: Set<string>;
  /** The section this user is class teacher of, or null. */
  homeroom: Homeroom | null;
  /** Holds a homeroom AND the MY_CLASS module is on. Decides whether My Class exists. */
  isClassTeacherOfOwnSection: boolean;
  /** True when the user's role grants every one of `names`. */
  can: (...names: Permission[]) => boolean;
  /** True when the user's role grants at least one of `names`. */
  canAny: (...names: Permission[]) => boolean;
  /** True when the user holds one of `roles`. */
  is: (...roles: Role[]) => boolean;
  /** True for the roles that see the whole school rather than a slice of it. */
  isManagement: boolean;
  /** True for TEACHER/CLASS_TEACHER, whose data is scoped to what they teach. */
  isTeaching: boolean;
  /** True for STUDENT/PARENT, scoped to themselves or their children. */
  isSelfScoped: boolean;
  /** True when the named org module is switched on. Unknown keys read as on. */
  moduleEnabled: (moduleKey: string) => boolean;
  /** Re-reads /me/access. Call after editing roles or modules. */
  refresh: () => Promise<void>;
}

const AccessContext = createContext<AccessContextValue | undefined>(undefined);

export function AccessProvider({ children }: { children: ReactNode }) {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);
  const sessionEpoch = useAppSelector((state) => state.auth.sessionEpoch);
  const cachedRole = useAppSelector((state) => state.auth.user?.role);

  const [access, setAccess] = useState<MyAccess | null>(null);
  const [loading, setLoading] = useState(false);
  const [settled, setSettled] = useState(false);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    if (!isAuthenticated) {
      setAccess(null);
      setError(false);
      setSettled(true);
      return;
    }
    setLoading(true);
    setError(false);
    try {
      setAccess(await accessApi.getMyAccess());
    } catch {
      // Flag the failure but keep whatever answer we already had.
      //
      // On the first fetch there is nothing to keep, so `access` stays null and
      // everything is denied — DashboardLayout turns that into a retry prompt
      // rather than a stripped-looking app. On a later re-read, holding the
      // previous grants for a few more seconds is better than blanking a working
      // screen over one timed-out background request, and it cannot grant
      // anything: the API re-checks every request against role_permissions.
      //
      // The 401 path does not reach here — the axios interceptor refreshes the
      // token and retries, and if that fails it dispatches logout and redirects.
      setError(true);
    } finally {
      setLoading(false);
      setSettled(true);
    }
  }, [isAuthenticated]);

  // Re-runs on login, on token refresh and on logout — see sessionEpoch.
  useEffect(() => {
    void load();
  }, [load, sessionEpoch]);

  const value = useMemo<AccessContextValue>(() => {
    // Role may come from the cached user before the fetch resolves; it only shapes
    // coarse menu structure and is re-asserted by the server below.
    const role = (access?.role ?? cachedRole) as Role | undefined;

    const permissions = new Set<Permission>((access?.permissions ?? []) as Permission[]);
    const enabledModules = new Set<string>(access?.enabledModules ?? []);

    // SUPER_ADMIN is never gated by a permission grant, mirroring
    // AppConstants.ADMIN_OVERRIDE on the backend — but only once the server has
    // confirmed the role. Trusting the cached role here would let a tampered
    // localStorage entry unlock every button in the UI.
    const isAdmin = access?.role === 'SUPER_ADMIN';

    // Strict: no grants means no actions. See the class comment for why the old
    // "empty set means unknown, so allow" fallback was removed.
    const can = (...names: Permission[]) =>
      isAdmin || (names.length > 0 && names.every((name) => permissions.has(name)));
    const canAny = (...names: Permission[]) =>
      isAdmin || names.some((name) => permissions.has(name));

    const is = (...roles: Role[]) => Boolean(role && roles.includes(role));

    /**
     * Unknown modules read as enabled, not disabled.
     *
     * <p>Mirrors `OrgModuleRepository.findDisabledModuleKeys` on the backend: a
     * module with no registry row must behave as on, so a migration that adds a
     * module and forgets to register it does not black out that whole area. The
     * empty-set case covers the pre-fetch window, where the shell is showing a
     * loader anyway.
     */
    const moduleEnabled = (moduleKey: string) =>
      enabledModules.size === 0 || enabledModules.has(moduleKey);

    return {
      access,
      loading,
      settled,
      error,
      role,
      permissions,
      enabledModules,
      homeroom: access?.homeroom ?? null,
      // Strictly from the server. Never inferred from the CLASS_TEACHER role,
      // which 27 of its 44 holders cannot back with an actual assignment.
      isClassTeacherOfOwnSection: Boolean(access?.classTeacherOfOwnSection),
      can,
      canAny,
      is,
      isManagement: is(...MANAGEMENT_ROLES),
      isTeaching: is(...TEACHING_ROLES),
      isSelfScoped: is(...SELF_SCOPED_ROLES),
      moduleEnabled,
      refresh: load,
    };
  }, [access, cachedRole, loading, settled, error, load]);

  return <AccessContext.Provider value={value}>{children}</AccessContext.Provider>;
}

export function useAccess(): AccessContextValue {
  const ctx = useContext(AccessContext);
  if (!ctx) throw new Error('useAccess must be used within an AccessProvider');
  return ctx;
}

export default AccessProvider;
