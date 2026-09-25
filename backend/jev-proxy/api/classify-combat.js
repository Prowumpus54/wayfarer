import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getVercelOidcToken } from "@vercel/oidc";

if (!getApps().length) {
  initializeApp({ projectId: "wayfarer-rpg" });
}

const MODEL = "typesafe-ai/jev";
const GATEWAY_URL = "https://ai-gateway.vercel.sh/v1/evaluate";

const questions = {
  rulesPath: {
    type: "choice",
    instructions:
      "Decide whether this combat declaration is a normal bounded action the Android rules engine can validate, or needs creative GM adjudication.",
    criteria: {
      rules:
        "ordinary Strike, Stride/Step, spell use, draw/use item, Raise Shield, Take Cover, Aid, Ready, Demoralize, Feint, Shove, Trip, Grapple, Tumble Through, Recall Knowledge, or similarly defined rules action",
      gm:
        "improvised stunt, negotiation, unusual environmental manipulation, cinematic multi-part stunt, or action whose outcome depends on fiction",
    },
  },
  actionType: {
    type: "choice",
    instructions:
      "Classify the primary rules action. Prefer a rules action even when movement is also included. Use other only for genuinely improvised actions.",
    criteria: {
      strike: "weapon or unarmed attack against a creature",
      move: "Stride, Step, reposition, approach, retreat, or chase",
      cast_spell: "cast, sustain, dismiss, or otherwise use a spell",
      interact: "draw, open, pick up, manipulate, or use an ordinary object",
      skill_action:
        "defined skill action such as Demoralize, Feint, Shove, Trip, Grapple, Tumble Through, Recall Knowledge, or similar",
      defend: "Raise Shield, Take Cover, guard, or improve defenses",
      aid: "Aid another character's action",
      ready: "Ready an action for a later trigger",
      other: "improvised or unbounded action not covered above",
    },
  },
  targetType: {
    type: "choice",
    instructions:
      "Classify the object of the main action. For pure movement away from a foe, use none unless the destination itself is the object of the action.",
    criteria: {
      enemy: "a hostile creature is the object of the main action",
      ally: "a friendly creature is the object of the main action",
      self: "the actor is explicitly the object of the main action",
      environment: "an object, terrain feature, door, lever, or destination",
      none: "no distinct target; pure movement can use none",
    },
  },
  actorMoves: {
    type: "boolean",
    instructions:
      "Does the acting character physically change position as part of the declaration? Do not count moving the target, drawing an item, or merely moving a limb.",
  },
  needsGm: {
    type: "boolean",
    instructions:
      "Is creative fictional adjudication required? Return false for normal defined rules actions, including opening an ordinary door, pulling a normal lever, Demoralize, Feint, Shove, Tumble Through, Aid, Ready, Raise Shield and Take Cover.",
  },
};

function clampText(value, max) {
  return String(value ?? "").trim().slice(0, max);
}
async function verifyFirebaseUser(req) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    throw Object.assign(new Error("Missing Firebase ID token."), { status: 401 });
  }
  const token = authHeader.slice(7).trim();
  try {
    return await getAuth().verifyIdToken(token);
  } catch {
    throw Object.assign(new Error("Invalid Firebase ID token."), { status: 401 });
  }
}

function normalizedAnswer(answer, type) {
  if (!answer) return null;
  if (type === "choice") {
    const probabilities = answer.probabilities || {};
    const choice = answer.choice || null;
    return {
      choice,
      confidence: Number(probabilities[choice] || 0),
      probabilities,
    };
  }
  const probability = Number(answer.probability || 0);
  return { value: probability >= 0.5, probability };
}

export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  try {
    const user = await verifyFirebaseUser(req);
    const playerText = clampText(req.body?.playerText, 600);
    if (!playerText) {
      return res.status(400).json({ error: "playerText is required" });
    }

    const state = {
      mode: "combat",
      playerText,
      actor: {
        name: clampText(req.body?.actor?.name, 80),
        weapon: clampText(req.body?.actor?.weapon, 100),
        position: clampText(req.body?.actor?.position, 120),
      },
      target: {
        name: clampText(req.body?.target?.name, 80),
        relation: clampText(req.body?.target?.relation, 40),
        distanceFt: Number(req.body?.target?.distanceFt || 0),
      },
      scene: clampText(req.body?.scene, 400),
      rulesAuthority: "Android validates all mechanics and rolls all dice.",
    };

    const gatewayToken =
      process.env.AI_GATEWAY_API_KEY ||
      process.env.JEV_API_KEY ||
      await getVercelOidcToken();
    if (!gatewayToken) {
      return res.status(500).json({
        error: "AI Gateway authentication is not available."
      });
    }

    const started = Date.now();
    const gateway = await fetch(GATEWAY_URL, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + gatewayToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: MODEL,
        state,
        questions,
        providerOptions: { gateway: { only: ["typesafe-ai"] } },
      }),
    });

    const raw = await gateway.text();
    let data;
    try {
      data = JSON.parse(raw);
    } catch {
      data = { raw };
    }

    if (!gateway.ok) {
      return res.status(gateway.status).json({
        error: data?.error?.message || "Jev request failed",
        type: data?.error?.type || "gateway_error",
      });
    }

    const answers = data.answers || {};
    const route = normalizedAnswer(answers.rulesPath, "choice");
    const action = normalizedAnswer(answers.actionType, "choice");
    const target = normalizedAnswer(answers.targetType, "choice");
    const movement = normalizedAnswer(answers.actorMoves, "boolean");
    const needsGm = normalizedAnswer(answers.needsGm, "boolean");

    return res.status(200).json({
      userId: user.uid,
      model: data.model || MODEL,
      latencyMs: Date.now() - started,
      route,
      action,
      target,
      movement,
      needsGm,
      usage: data.usage || null,
    });
  } catch (error) {
    const status = Number(error.status || 500);
    return res.status(status).json({
      error: error.message || "Unexpected server error",
    });
  }
}
