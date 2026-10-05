import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiBaseUrl = env.VITE_API_URL?.trim() || env.VITE_API_BASE_URL?.trim();

  if (command === "build" && mode === "production") {
    let parsedUrl: URL | undefined;
    try {
      parsedUrl = apiBaseUrl ? new URL(apiBaseUrl) : undefined;
    } catch {
      // Report the same actionable production configuration error below.
    }
    const host = parsedUrl?.hostname.toLowerCase() ?? "";
    const isIpLiteral =
      /^(?:\d{1,3}\.){3}\d{1,3}$/.test(host) || host.startsWith("[");
    const isLocalHost =
      host === "localhost" || host.endsWith(".localhost") || host.endsWith(".local");
    if (
      !parsedUrl ||
      parsedUrl.protocol !== "https:" ||
      !parsedUrl.pathname.replace(/\/$/, "").endsWith("/api") ||
      parsedUrl.username ||
      parsedUrl.password ||
      parsedUrl.search ||
      parsedUrl.hash ||
      isIpLiteral ||
      isLocalHost ||
      /YOUR-|(?:^|\.)example(?:\.|$)/i.test(host)
    ) {
      throw new Error(
        "Production builds require VITE_API_URL to be the deployed HTTPS API URL ending in /api. Set it in Railway or copy admin/.env.production.example.",
      );
    }
  }

  return {
    plugins: [react()],
    server: {
      host: "0.0.0.0",
    },
  };
});
