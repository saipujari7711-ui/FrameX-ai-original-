import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "FRAME X AI",
  description: "Premium personal multi-provider AI workspace",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
