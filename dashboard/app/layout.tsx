import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";

const geist = Geist({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "LibraryAgent",
  description: "Tu recomendador personal de libros",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className={`${geist.className} h-full`}>
      <body className="h-full bg-zinc-50 text-zinc-900 antialiased">
        {children}
      </body>
    </html>
  );
}
