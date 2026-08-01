"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { loadAuth, clearAuth } from "../lib/auth";
import type { AuthResponse } from "../lib/api";

export default function HomePage() {
  const [auth, setAuth] = useState<AuthResponse | null>(null);

  useEffect(() => {
    setAuth(loadAuth());
  }, []);

  return (
    <main className="fade-in">
      <header className="nav">
        <div>
          <p className="pulse" style={{ margin: 0, color: "var(--accent-soft)", letterSpacing: "0.08em" }}>
            EVENT-DRIVEN LEDGER
          </p>
          <h1 className="brand">AshuBoi Wallet</h1>
        </div>
        <div className="nav-links">
          <Link className="btn ghost" href="/architecture">
            Architecture
          </Link>
          {auth ? (
            <>
              <Link className="btn" href="/dashboard">
                Open dashboard
              </Link>
              <button
                className="btn ghost"
                onClick={() => {
                  clearAuth();
                  setAuth(null);
                }}
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link className="btn" href="/login">
                Sign in
              </Link>
              <Link className="btn ghost" href="/register">
                Create account
              </Link>
            </>
          )}
        </div>
      </header>

      <p className="lede">
        A portfolio e-wallet that plugs into any payment infrastructure through a provider port,
        with strict <strong style={{ color: "var(--ink)" }}>Idempotency-Key</strong> protection on
        transfers and top-up credits.
      </p>

      <section className="grid two" style={{ marginTop: "2rem" }}>
        <div className="panel">
          <h2 style={{ fontFamily: "var(--display)", marginTop: 0 }}>What recruiters can verify</h2>
          <ul style={{ color: "var(--muted)", lineHeight: 1.7 }}>
            <li>Kafka-driven P2P settlement across microservices</li>
            <li>PaymentGatewayPort + Mock PG (Stripe/Razorpay stubs)</li>
            <li>Replay-safe transfers and webhook credits</li>
            <li>JWT-authenticated money movement</li>
          </ul>
        </div>
        <div className="panel">
          <h2 style={{ fontFamily: "var(--display)", marginTop: 0 }}>Try the demo flow</h2>
          <ol style={{ color: "var(--muted)", lineHeight: 1.7 }}>
            <li>Register two users</li>
            <li>Top up via mock payment gateway</li>
            <li>Transfer with an Idempotency-Key</li>
            <li>Hit Replay — balance stays correct</li>
          </ol>
        </div>
      </section>
    </main>
  );
}
