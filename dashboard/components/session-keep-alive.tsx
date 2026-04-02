"use client";

import { useEffect } from "react";

const INTERVAL_MS = 4 * 60 * 1000; // cada 4 minutos

export default function SessionKeepAlive() {
  useEffect(() => {
    const id = setInterval(() => {
      fetch("/api/auth/session").catch(() => {});
    }, INTERVAL_MS);
    return () => clearInterval(id);
  }, []);

  return null;
}
