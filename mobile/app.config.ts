import type { ConfigContext, ExpoConfig } from "expo/config";

export default ({ config }: ConfigContext): ExpoConfig => {
  const environment = process.env.APP_ENV ?? "development";
  const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL?.trim() ?? "";

  if (environment === "production") {
    if (!apiBaseUrl.startsWith("https://")) {
      throw new Error(
        "Production builds require a real HTTPS EXPO_PUBLIC_API_BASE_URL.",
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
