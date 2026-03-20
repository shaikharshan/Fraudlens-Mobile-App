import { NativeModules, Platform } from "react-native";

type FraudLensNativeType = {
  initialize(config: object): Promise<void>;
  clear(): Promise<void>;
  audioHealth(): Promise<string | null>;
  audioDetectBase64(payload: object): Promise<string | null>;
  imageHealth(): Promise<string | null>;
  imageDetectBase64(payload: object): Promise<string | null>;
  videoHealth(): Promise<string | null>;
  videoDetectBase64(payload: object): Promise<string | null>;
  checkIpReputation(ip: string): Promise<object | null>;
  checkModelHealth(): Promise<object | null>;
  predictFraud(input: object): Promise<object | null>;
  analyzeScam(combinedText: string): Promise<object | null>;
};

const Native = NativeModules.FraudLens as FraudLensNativeType | undefined;

function requireNative(): FraudLensNativeType {
  if (!Native) {
    throw new Error(
      "[react-native-fraudlens] Native module FraudLens not linked. Run pod install / rebuild Android."
    );
  }
  return Native;
}

export type FraudLensInitConfig = {
  audioBaseUrl?: string;
  audioApiKey?: string | null;
  imageBaseUrl?: string;
  imageApiKey?: string | null;
  videoBaseUrl?: string;
  videoApiKey?: string | null;
  abuseIpDbBaseUrl?: string;
  abuseIpDbApiKey?: string | null;
  fraudModelBaseUrl?: string;
  geminiApiKey?: string | null;
  enableHttpLogging?: boolean;
};

export type MediaPayloadBase64 = {
  /** Base64-encoded file bytes (no data: URL prefix). */
  base64: string;
  filename: string;
  contentType: string;
  partName?: string;
};

/**
 * Initialize native FraudLens SDK (Android). Call once near app startup.
 * On iOS, resolves after rejecting unless you extend the native module.
 */
export async function initialize(config: FraudLensInitConfig): Promise<void> {
  if (Platform.OS !== "android") {
    console.warn(
      "[react-native-fraudlens] initialize: native Android SDK not used on this platform."
    );
    return;
  }
  await requireNative().initialize(config);
}

export async function clear(): Promise<void> {
  if (Platform.OS !== "android") return;
  await requireNative().clear();
}

export async function audioHealth(): Promise<string | null> {
  if (Platform.OS !== "android") return null;
  return requireNative().audioHealth();
}

export async function audioDetect(payload: MediaPayloadBase64): Promise<string | null> {
  if (Platform.OS !== "android") return null;
  return requireNative().audioDetectBase64({
    base64: payload.base64,
    filename: payload.filename,
    contentType: payload.contentType,
    partName: payload.partName ?? "file",
  });
}

export async function imageHealth(): Promise<string | null> {
  if (Platform.OS !== "android") return null;
  return requireNative().imageHealth();
}

export async function imageDetect(payload: MediaPayloadBase64): Promise<string | null> {
  if (Platform.OS !== "android") return null;
  return requireNative().imageDetectBase64({
    base64: payload.base64,
    filename: payload.filename,
    contentType: payload.contentType,
    partName: payload.partName ?? "file",
  });
}

export async function videoHealth(): Promise<string | null> {
  if (Platform.OS !== "android") return null;
  return requireNative().videoHealth();
}

export async function videoDetect(payload: MediaPayloadBase64): Promise<string | null> {
  if (Platform.OS !== "android") return null;
  return requireNative().videoDetectBase64({
    base64: payload.base64,
    filename: payload.filename,
    contentType: payload.contentType,
    partName: payload.partName ?? "file",
  });
}

export async function checkIpReputation(ip: string): Promise<Record<string, unknown> | null> {
  if (Platform.OS !== "android") return null;
  const r = await requireNative().checkIpReputation(ip);
  return r as Record<string, unknown> | null;
}

export async function checkModelHealth(): Promise<Record<string, unknown> | null> {
  if (Platform.OS !== "android") return null;
  const r = await requireNative().checkModelHealth();
  return r as Record<string, unknown> | null;
}

export async function predictFraud(
  input: Record<string, unknown>
): Promise<Record<string, unknown> | null> {
  if (Platform.OS !== "android") return null;
  const r = await requireNative().predictFraud(input);
  return r as Record<string, unknown> | null;
}

export async function analyzeScam(
  combinedText: string
): Promise<Record<string, unknown> | null> {
  if (Platform.OS !== "android") return null;
  const r = await requireNative().analyzeScam(combinedText);
  return r as Record<string, unknown> | null;
}
