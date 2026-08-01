import type { AuthResponse } from "./api";

const KEY = "ewallet_auth";

export function saveAuth(auth: AuthResponse) {
  if (typeof window === "undefined") return;
  localStorage.setItem(KEY, JSON.stringify(auth));
}

export function loadAuth(): AuthResponse | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export function clearAuth() {
  if (typeof window === "undefined") return;
  localStorage.removeItem(KEY);
}
