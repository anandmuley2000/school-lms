import type { ConfigContext, ExpoConfig } from "expo/config";

const DEVELOPMENT_API_URL = "http://localhost:8080/api";

export default ({ config }: ConfigContext): ExpoConfig => {
  const environment = process.env.APP_ENV ?? "development";
  const apiBaseUrl =
    process.env.EXPO_PUBLIC_API_BASE_URL ??
    (environment === "development" ? DEVELOPMENT_API_URL : "");

  if (environment === "production") {
    if (!apiBaseUrl.startsWith("https://")) {
      throw new Error(
        "Production builds require EXPO_PUBLIC_API_BASE_URL to be an HTTPS URL.",
      );
    }
    if (apiBaseUrl.includes("YOUR-ONLINE-API-DOMAIN")) {
      throw new Error(
        "Replace the production API placeholder with your deployed API domain before building.",
      );
    }
  }

  return {
    ...config,
    name: config.name ?? "Northstar School",
    slug: config.slug ?? "northstar-school",
    extra: {
      ...config.extra,
      apiBaseUrl,
      environment,
    },
  };
};
