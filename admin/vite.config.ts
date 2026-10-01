import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiBaseUrl = env.VITE_API_BASE_URL?.trim();

  if (command === "build" && mode === "production") {
    if (
      !apiBaseUrl ||
      !apiBaseUrl.startsWith("https://") ||
      apiBaseUrl.includes("YOUR-ONLINE-API-DOMAIN")
    ) {
      throw new Error(
        "Production builds require VITE_API_BASE_URL to be your deployed HTTPS API URL.",
      );
    }
  }

  return {
    plugins: [react()],
    server: { port: 5173 },
  };
});
