"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { register } from "../../lib/api";
import { saveAuth } from "../../lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    phone: "",
    kycId: "",
    address: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(key: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const auth = await register(form);
      saveAuth(auth);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="fade-in" style={{ maxWidth: 560 }}>
      <Link href="/">← AshuBoi Wallet</Link>
      <h1 className="brand" style={{ fontSize: "2.4rem", marginTop: "1rem" }}>
        Create account
      </h1>
      <p className="lede">Registration publishes USER-CREATED on Kafka and provisions a wallet asynchronously.</p>
      <form className="panel" onSubmit={onSubmit} style={{ marginTop: "1.5rem" }}>
        <label>
          Name
          <input value={form.name} onChange={(e) => update("name", e.target.value)} required />
        </label>
        <label>
          Email
          <input
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
            type="email"
            required
          />
        </label>
        <label>
          Password
          <input
            value={form.password}
            onChange={(e) => update("password", e.target.value)}
            type="password"
            required
            minLength={6}
          />
        </label>
        <label>
          Phone
          <input value={form.phone} onChange={(e) => update("phone", e.target.value)} required />
        </label>
        <label>
          KYC ID
          <input value={form.kycId} onChange={(e) => update("kycId", e.target.value)} required />
        </label>
        <label>
          Address
          <input value={form.address} onChange={(e) => update("address", e.target.value)} />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="btn" disabled={loading} type="submit">
          {loading ? "Creating…" : "Create account"}
        </button>
      </form>
    </main>
  );
}
