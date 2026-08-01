"use client";

import Link from "next/link";
import { ArchitectureBoard } from "../../components/ArchitectureBoard";

export default function ArchitecturePage() {
  return (
    <main className="fade-in">
      <header className="nav">
        <div>
          <p className="pulse" style={{ margin: 0, color: "var(--accent-soft)", letterSpacing: "0.08em" }}>
            SYSTEM DESIGN
          </p>
          <h1 className="brand" style={{ fontSize: "clamp(2rem, 5vw, 3.2rem)" }}>
            Architecture story
          </h1>
        </div>
        <div className="nav-links">
          <Link className="btn ghost" href="/">
            Home
          </Link>
          <Link className="btn" href="/dashboard">
            Dashboard
          </Link>
        </div>
      </header>

      <p className="lede">
        Watch registration publish <strong style={{ color: "var(--ink)" }}>USER-CREATED</strong> over
        Kafka, initialize a wallet, then follow a transfer through TXN-INIT → TXN-COMPLETED and
        notification.
      </p>

      <div style={{ marginTop: "1.5rem" }}>
        <ArchitectureBoard />
      </div>
    </main>
  );
}
