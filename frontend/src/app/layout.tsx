import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "InternFlow — Your internship search, in one place",
  description: "A calmer place to keep track of internship applications, next steps, and progress.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className="h-full antialiased"
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
