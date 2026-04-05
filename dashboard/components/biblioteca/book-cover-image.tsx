"use client";

import { useState } from "react";

interface BookCoverImageProps {
  src: string;
  alt: string;
}

/**
 * Usa <img> nativo en lugar de next/image para que el navegador siga
 * redirects de Open Library → archive.org sin restricciones de remotePatterns.
 * Muestra el nombre del libro como fallback si la imagen falla.
 */
export default function BookCoverImage({ src, alt }: BookCoverImageProps) {
  const [failed, setFailed] = useState(false);

  if (failed) {
    return (
      <div className="flex h-full items-center justify-center p-3">
        <span className="text-center text-xs text-zinc-400 leading-snug">{alt}</span>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      onError={() => setFailed(true)}
      className="absolute inset-0 h-full w-full object-cover"
    />
  );
}
