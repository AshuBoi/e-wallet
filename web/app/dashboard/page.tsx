"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  AuthResponse,
  createTopUp,
  getBalance,
  getHistory,
  getStatus,
  newIdempotencyKey,
  TopUpResponse,
  TransactionStatus,
  transfer,
  TransferResponse,
  WalletBalance,
} from "../../lib/api";
import { clearAuth, loadAuth } from "../../lib/auth";

export default function DashboardPage() {
  const router = useRouter();
  const [auth, setAuth] = useState<AuthResponse | null>(null);
  const [balance, setBalance] = useState<WalletBalance | null>(null);
  const [history, setHistory] = useState<TransactionStatus[]>([]);
  const [toUserId, setToUserId] = useState("");
  const [amount, setAmount] = useState("10.00");
  const [remark, setRemark] = useState("Portfolio demo transfer");
  const [topUpAmount, setTopUpAmount] = useState("50.00");
  const [transferKey, setTransferKey] = useState("");
  const [topUpKey, setTopUpKey] = useState("");
  const [lastTransfer, setLastTransfer] = useState<TransferResponse | null>(null);
  const [lastTopUp, setLastTopUp] = useState<TopUpResponse | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async (token: string) => {
    const [b, h] = await Promise.all([getBalance(token), getHistory(token)]);
    setBalance(b);
    setHistory(h);
  }, []);

  useEffect(() => {
    const stored = loadAuth();
    if (!stored) {
      router.replace("/login");
      return;
    }
    setAuth(stored);
    setTransferKey(newIdempotencyKey());
    setTopUpKey(newIdempotencyKey());
    refresh(stored.token).catch((err) =>
      setError(err instanceof Error ? err.message : "Failed to load wallet")
    );
  }, [router, refresh]);

  async function onTransfer(e: FormEvent) {
    e.preventDefault();
    if (!auth) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      const result = await transfer(
        auth.token,
        {
          toUserId: Number(toUserId),
          amount: Number(amount),
          remark,
        },
        transferKey
      );
      setLastTransfer(result);
      setMessage(`Transfer accepted. txnId=${result.txnId}. Polling status…`);
      for (let i = 0; i < 8; i++) {
        await new Promise((r) => setTimeout(r, 700));
        const status = await getStatus(auth.token, result.txnId);
        setMessage(`Transfer ${status.status}${status.reason ? ` — ${status.reason}` : ""}`);
        if (status.status !== "PENDING") break;
      }
      await refresh(auth.token);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Transfer failed");
    } finally {
      setBusy(false);
    }
  }

  async function onReplayTransfer() {
    if (!auth || !lastTransfer) return;
    setBusy(true);
    setError("");
    try {
      const replayed = await transfer(
        auth.token,
        {
          toUserId: Number(toUserId),
          amount: Number(amount),
          remark,
        },
        transferKey
      );
      setMessage(
        `Replay returned same txnId=${replayed.txnId}. No double debit (idempotency).`
      );
      await refresh(auth.token);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Replay failed");
    } finally {
      setBusy(false);
    }
  }

  async function onTopUp(e: FormEvent) {
    e.preventDefault();
    if (!auth) return;
    setBusy(true);
    setError("");
    try {
      const result = await createTopUp(auth.token, Number(topUpAmount), topUpKey);
      setLastTopUp(result);
      setMessage(`Checkout created via ${result.provider}. Complete payment in the mock PG.`);
      window.open(result.checkoutUrl, "_blank", "noopener,noreferrer");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Top-up failed");
    } finally {
      setBusy(false);
    }
  }

  async function onReplayTopUp() {
    if (!auth) return;
    setBusy(true);
    setError("");
    try {
      const replayed = await createTopUp(auth.token, Number(topUpAmount), topUpKey);
      setLastTopUp(replayed);
      setMessage(
        `Replay returned same sessionId=${replayed.sessionId}. Checkout creation is idempotent.`
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Top-up replay failed");
    } finally {
      setBusy(false);
    }
  }

  if (!auth) {
    return (
      <main>
        <p className="pulse">Loading session…</p>
      </main>
    );
  }

  return (
    <main className="fade-in">
      <header className="nav">
        <div>
          <Link href="/">AshuBoi Wallet</Link>
          <h1 className="brand" style={{ fontSize: "2.2rem" }}>
            {auth.name}
          </h1>
          <p style={{ color: "var(--muted)", margin: 0 }}>
            userId <span className="mono">{auth.userId}</span>
          </p>
        </div>
        <div className="nav-links">
          <button className="btn ghost" onClick={() => refresh(auth.token)} disabled={busy}>
            Refresh
          </button>
          <button
            className="btn ghost"
            onClick={() => {
              clearAuth();
              router.push("/");
            }}
          >
            Sign out
          </button>
        </div>
      </header>

      <section className="grid two">
        <div className="panel">
          <p style={{ margin: 0, color: "var(--muted)" }}>Available balance</p>
          <p className="balance">{balance ? Number(balance.balance).toFixed(2) : "—"}</p>
          <div className="callout">
            Money uses BigDecimal on the backend. Wallet updates are versioned to avoid lost updates.
          </div>
        </div>
        <div className="panel">
          <h2 style={{ fontFamily: "var(--display)", marginTop: 0 }}>Provider-agnostic top-up</h2>
          <form onSubmit={onTopUp}>
            <label>
              Amount
              <input
                value={topUpAmount}
                onChange={(e) => setTopUpAmount(e.target.value)}
                type="number"
                min="1"
                step="0.01"
                required
              />
            </label>
            <p className="mono">Idempotency-Key: {topUpKey}</p>
            <div className="nav-links">
              <button className="btn" disabled={busy} type="submit">
                Create checkout
              </button>
              <button className="btn ghost" type="button" disabled={busy || !lastTopUp} onClick={onReplayTopUp}>
                Replay request
              </button>
              <button
                className="btn ghost"
                type="button"
                onClick={() => setTopUpKey(newIdempotencyKey())}
              >
                New key
              </button>
            </div>
          </form>
          {lastTopUp && (
            <p className="mono" style={{ marginTop: "1rem" }}>
              session={lastTopUp.sessionId} provider={lastTopUp.provider}
            </p>
          )}
        </div>
      </section>

      <section className="panel" style={{ marginTop: "1rem" }}>
        <h2 style={{ fontFamily: "var(--display)", marginTop: 0 }}>P2P transfer</h2>
        <form onSubmit={onTransfer} className="grid two">
          <div>
            <label>
              Recipient userId
              <input
                value={toUserId}
                onChange={(e) => setToUserId(e.target.value)}
                type="number"
                required
              />
            </label>
            <label>
              Amount
              <input
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                type="number"
                min="0.01"
                step="0.01"
                required
              />
            </label>
            <label>
              Remark
              <input value={remark} onChange={(e) => setRemark(e.target.value)} />
            </label>
          </div>
          <div>
            <div className="callout">
              Send the same Idempotency-Key twice — the API returns the original txnId and does not
              start a second Kafka settlement.
            </div>
            <p className="mono">Idempotency-Key: {transferKey}</p>
            <div className="nav-links">
              <button className="btn" disabled={busy} type="submit">
                Transfer
              </button>
              <button
                className="btn ghost"
                type="button"
                disabled={busy || !lastTransfer}
                onClick={onReplayTransfer}
              >
                Replay request
              </button>
              <button
                className="btn ghost"
                type="button"
                onClick={() => setTransferKey(newIdempotencyKey())}
              >
                New key
              </button>
            </div>
          </div>
        </form>
      </section>

      {(message || error) && (
        <section className="panel" style={{ marginTop: "1rem" }}>
          {message && <p style={{ margin: 0 }}>{message}</p>}
          {error && <p className="error">{error}</p>}
        </section>
      )}

      <section className="panel" style={{ marginTop: "1rem" }}>
        <h2 style={{ fontFamily: "var(--display)", marginTop: 0 }}>Transaction history</h2>
        {history.length === 0 ? (
          <p style={{ color: "var(--muted)" }}>No transfers yet.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Amount</th>
                <th>From → To</th>
                <th>Txn</th>
              </tr>
            </thead>
            <tbody>
              {history.map((txn) => (
                <tr key={txn.txnId}>
                  <td>{txn.status}</td>
                  <td>{Number(txn.amount).toFixed(2)}</td>
                  <td>
                    {txn.fromUserId} → {txn.toUserId}
                  </td>
                  <td className="mono">{txn.txnId.slice(0, 8)}…</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}
