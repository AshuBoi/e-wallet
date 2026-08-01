"use client";

import { useCallback, useEffect, useState } from "react";

type ServiceId = "user" | "wallet" | "transaction" | "notification" | "kafka";

type Popup = {
  id: number;
  title: string;
  body: string;
};

const services: {
  id: Exclude<ServiceId, "kafka">;
  name: string;
  blurb: string;
  endpoints: string[];
}[] = [
  {
    id: "user",
    name: "User Service",
    blurb: "Register / login, JWT issuance, publishes USER-CREATED.",
    endpoints: ["POST /user-service/users", "POST /user-service/login"],
  },
  {
    id: "wallet",
    name: "Wallet Service",
    blurb: "Ledger, top-ups, settles TXN-INIT, emits WALLET-UPDATED.",
    endpoints: ["POST /wallet-service/topups", "GET /wallet-service/wallets/{id}"],
  },
  {
    id: "transaction",
    name: "Transaction Service",
    blurb: "P2P orchestration, Idempotency-Key, status history.",
    endpoints: ["POST /transaction-service/transfers", "GET .../transactions/{id}"],
  },
  {
    id: "notification",
    name: "Notification Service",
    blurb: "Consumes Kafka events and sends demo email via Mailhog.",
    endpoints: ["Kafka → welcome / balance emails"],
  },
];

const tour: { service: ServiceId; title: string; body: string; delay: number }[] = [
  {
    service: "user",
    title: "User registered",
    body: "User Service persists the account and publishes USER-CREATED to Kafka.",
    delay: 0,
  },
  {
    service: "kafka",
    title: "Topic: USER-CREATED",
    body: "Event fans out to Wallet Service and Notification Service consumers.",
    delay: 1400,
  },
  {
    service: "wallet",
    title: "Wallet initialized",
    body: "Wallet Service creates a ledger row for the new userId.",
    delay: 2800,
  },
  {
    service: "notification",
    title: "Welcome email queued",
    body: "Notification Service consumes USER-CREATED and sends a welcome message.",
    delay: 4000,
  },
  {
    service: "transaction",
    title: "Create transfer",
    body: "Transaction Service accepts POST /transfers + Idempotency-Key → PENDING.",
    delay: 5400,
  },
  {
    service: "kafka",
    title: "Topic: TXN-INIT",
    body: "Wallet debits/credits with BigDecimal + @Version, then emits TXN-COMPLETED.",
    delay: 6800,
  },
  {
    service: "transaction",
    title: "Receive balance outcome",
    body: "Transaction Service marks SUCCESS/FAILED; UI polls status / history.",
    delay: 8200,
  },
  {
    service: "wallet",
    title: "Topic: WALLET-UPDATED",
    body: "Balance change published; Notification Service emails the updated balance.",
    delay: 9600,
  },
];

export function ArchitectureBoard() {
  const [active, setActive] = useState<ServiceId | null>(null);
  const [popups, setPopups] = useState<Popup[]>([]);
  const [playing, setPlaying] = useState(false);

  const pushPopup = useCallback((title: string, body: string, service: ServiceId) => {
    const id = Date.now() + Math.random();
    setActive(service);
    setPopups((p) => [...p.slice(-2), { id, title, body }]);
    window.setTimeout(() => setPopups((p) => p.filter((x) => x.id !== id)), 3200);
  }, []);

  useEffect(() => {
    if (!playing) return;
    const timers = tour.map((beat) =>
      window.setTimeout(() => {
        pushPopup(beat.title, beat.body, beat.service);
        if (beat === tour[tour.length - 1]) {
          window.setTimeout(() => setPlaying(false), 3200);
        }
      }, beat.delay),
    );
    return () => timers.forEach((t) => window.clearTimeout(t));
  }, [playing, pushPopup]);

  const selected = services.find((s) => s.id === active);

  return (
    <section className="arch-board fade-in">
      <div className="arch-toolbar">
        <div>
          <p className="pulse" style={{ margin: 0, color: "var(--accent-soft)", letterSpacing: "0.08em" }}>
            INTERACTIVE SYSTEM DESIGN
          </p>
          <h2 style={{ fontFamily: "var(--display)", margin: "0.4rem 0 0" }}>Event flow board</h2>
        </div>
        <div className="nav-links">
          <button type="button" className="btn" disabled={playing} onClick={() => setPlaying(true)}>
            {playing ? "Playing…" : "Play guided tour"}
          </button>
          <button
            type="button"
            className="btn ghost"
            onClick={() => {
              setPlaying(false);
              setPopups([]);
              setActive(null);
            }}
          >
            Reset
          </button>
        </div>
      </div>

      <div className="arch-stage">
        <div className="arch-iso">
          <div className="arch-floor" />
          <div className={`arch-kafka${active === "kafka" ? " lit" : ""}`}>
            <span>Kafka bus</span>
            <small>USER-CREATED · TXN-INIT · TXN-COMPLETED · WALLET-UPDATED</small>
          </div>
          {services.map((svc) => (
            <button
              key={svc.id}
              type="button"
              className={`arch-node arch-${svc.id}${active === svc.id ? " lit" : ""}`}
              onClick={() => pushPopup(svc.name, svc.blurb, svc.id)}
            >
              <span className="mono">SERVICE</span>
              <strong>{svc.name}</strong>
            </button>
          ))}
        </div>
        <div className="arch-popups">
          {popups.map((p) => (
            <div key={p.id} className="arch-popup panel">
              <p className="mono" style={{ color: "var(--accent-soft)", margin: 0 }}>
                EVENT
              </p>
              <strong>{p.title}</strong>
              <p style={{ color: "var(--muted)", margin: "0.35rem 0 0", fontSize: "0.9rem" }}>{p.body}</p>
            </div>
          ))}
        </div>
      </div>

      {selected ? (
        <div className="panel" style={{ marginTop: "1rem" }}>
          <h3 style={{ fontFamily: "var(--display)", marginTop: 0 }}>{selected.name}</h3>
          <p style={{ color: "var(--muted)" }}>{selected.blurb}</p>
          <ul className="mono" style={{ color: "var(--accent-soft)" }}>
            {selected.endpoints.map((ep) => (
              <li key={ep}>{ep}</li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}
