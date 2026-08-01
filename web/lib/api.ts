const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "";

export type AuthResponse = {
  token: string;
  userId: number;
  name: string;
  email: string;
};

export type WalletBalance = {
  userId: number;
  name: string;
  email: string;
  balance: number | string;
};

export type TransferResponse = {
  txnId: string;
  status: string;
  idempotencyKey: string;
};

export type TransactionStatus = {
  txnId: string;
  status: string;
  reason?: string;
  fromUserId: number;
  toUserId: number;
  amount: number | string;
  remark?: string;
};

export type TopUpResponse = {
  sessionId: string;
  provider: string;
  checkoutUrl: string;
  amount: number | string;
  status: string;
};

function authHeader(token?: string | null): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function parse<T>(res: Response): Promise<T> {
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const message = data?.message || data?.error || res.statusText;
    throw new Error(message);
  }
  return data as T;
}

export async function register(body: {
  name: string;
  email: string;
  password: string;
  phone: string;
  kycId: string;
  address?: string;
}) {
  const res = await fetch(`${API_BASE}/user-service/auth/register`, {
    method: "POST",
    headers: authHeader(),
    body: JSON.stringify(body),
  });
  return parse<AuthResponse>(res);
}

export async function login(body: { email: string; password: string }) {
  const res = await fetch(`${API_BASE}/user-service/auth/login`, {
    method: "POST",
    headers: authHeader(),
    body: JSON.stringify(body),
  });
  return parse<AuthResponse>(res);
}

export async function getBalance(token: string) {
  const res = await fetch(`${API_BASE}/wallet-service/balance`, {
    headers: authHeader(token),
  });
  return parse<WalletBalance>(res);
}

export async function transfer(
  token: string,
  body: { toUserId: number; amount: number; remark?: string },
  idempotencyKey: string
) {
  const res = await fetch(`${API_BASE}/transaction-service/transfers`, {
    method: "POST",
    headers: {
      ...authHeader(token),
      "Idempotency-Key": idempotencyKey,
    },
    body: JSON.stringify(body),
  });
  return parse<TransferResponse>(res);
}

export async function getHistory(token: string) {
  const res = await fetch(`${API_BASE}/transaction-service/history`, {
    headers: authHeader(token),
  });
  return parse<TransactionStatus[]>(res);
}

export async function getStatus(token: string, txnId: string) {
  const res = await fetch(`${API_BASE}/transaction-service/status/${txnId}`, {
    headers: authHeader(token),
  });
  return parse<TransactionStatus>(res);
}

export async function createTopUp(token: string, amount: number, idempotencyKey: string) {
  const res = await fetch(`${API_BASE}/wallet-service/topups`, {
    method: "POST",
    headers: {
      ...authHeader(token),
      "Idempotency-Key": idempotencyKey,
    },
    body: JSON.stringify({ amount }),
  });
  return parse<TopUpResponse>(res);
}

export function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `key_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}
