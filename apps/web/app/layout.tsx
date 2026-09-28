import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "FRAME X AI",
  description: "Personal, local-first AI platform"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
