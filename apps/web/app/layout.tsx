import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "FRAME X AI",
  description: "Premium personal multi-provider AI workspace",
  icons: { icon: "/brand/favicon-32.png", apple: "/brand/framex-icon-192.png" }
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
