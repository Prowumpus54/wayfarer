import { getVercelOidcToken } from "@vercel/oidc";

export default async function handler(req, res) {
  let oidcAvailable = false;
  try {
    oidcAvailable = Boolean(await getVercelOidcToken());
  } catch {
    oidcAvailable = false;
  }

  return res.status(200).json({
    ok: true,
    oidcAvailable,
    gatewayKeyAvailable: Boolean(
      process.env.AI_GATEWAY_API_KEY || process.env.JEV_API_KEY
    )
  });
}
