/**
 * The parameters the ProjectForge app passes to `/appLogin` (AppLogin.login of the app).
 *
 * The app sends them twice: packed into one parameter `login` (base64url of a json object, first in the query)
 * and one by one. An SSO proxy in front of the server may keep only the first parameter when it redirects a
 * browser without a proxy session to its login and back (an unencoded `rd=…?a=1&b=2` loses everything after the
 * first `&`); the packed one survives that. Some proxies bring the browser back without any query: against a server
 * that can (`appLoginPath` of the system status and the token), the app also puts the packed one into the path
 * (`/appLogin/<login>`), which wins. An app of an older version sends the single ones only.
 */
export interface AppLoginParams {
  challenge: string | null;
  state: string | null;
  deviceName: string | null;
  platform: string | null;
  deviceId: number | null;
}

/** The fields of the packed parameter, as the app writes them. */
interface PackedLogin {
  challenge?: unknown;
  state?: unknown;
  device?: unknown;
  platform?: unknown;
  deviceId?: unknown;
}

export function readAppLoginParams(
  params: Pick<URLSearchParams, "get">,
  pathLogin: string | null = null
): AppLoginParams {
  const packed = unpack(pathLogin) ?? unpack(params.get("login"));
  const text = (key: keyof PackedLogin, single: string) => {
    const value = packed?.[key];
    return typeof value === "string" && value
      ? value
      : params.get(single) || null;
  };
  const packedDeviceId = Number(packed?.deviceId);
  return {
    challenge: text("challenge", "challenge"),
    state: text("state", "state"),
    deviceName: text("device", "device"),
    platform: text("platform", "platform"),
    deviceId:
      Number.isInteger(packedDeviceId) && packedDeviceId > 0
        ? packedDeviceId
        : Number(params.get("deviceId")) || null,
  };
}

/** The json object of the packed parameter, null if there is none or it isn't readable. */
function unpack(value: string | null): PackedLogin | null {
  if (!value) return null;
  try {
    const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
    const binary = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "="));
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
    const json: unknown = JSON.parse(new TextDecoder().decode(bytes));
    return json && typeof json === "object" ? (json as PackedLogin) : null;
  } catch {
    return null;
  }
}
