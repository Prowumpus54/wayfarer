import { createRemoteJWKSet, jwtVerify } from 'jose';

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'wayfarer-rpg';
const JEV_URL = 'https://ai-gateway.vercel.sh/v1/evaluate';
const MODEL = 'typesafe-ai/jev';
const JWKS = createRemoteJWKSet(new URL(
  'https://www.googleapis.com/service_accounts/v1/jwk/' +
  'securetoken@system.gserviceaccount.com'
));

const questions = {
  actionType: {
    type: 'choice',
    instructions: 'Classify the player primary combat intent.',
    criteria: {
      strike: 'weapon or unarmed attack against a creature',
      move: 'movement, repositioning, approach, retreat, or chase',
      cast_spell: 'cast, sustain, dismiss, or otherwise use a spell',
      interact: 'draw, open, pick up, manipulate, or use an object',
      skill_action: 'attempt a skill-based combat action',
      defend: 'raise defenses, take cover, guard, or protect oneself',
      aid: 'help another creature perform an action',
      ready: 'prepare an action to trigger later',
      other: 'anything not represented by the bounded choices',
    },
  },
  targetType: {
    type: 'choice',
    instructions: 'Classify the intended target category.',
    criteria: {
      enemy: 'a hostile or opposed creature',
      ally: 'a friendly creature other than the actor',
      self: 'the acting character',
      environment: 'an object, location, door, terrain, or hazard',
      none: 'no specific target',
    },
  },
  includesMovement: {
    type: 'boolean',
    instructions: 'Does the declared action include movement or repositioning?',
  },
  needsGm: {
    type: 'boolean',
    instructions:
      'Does this action require creative or ambiguous GM adjudication ' +
      'instead of ordinary deterministic combat rules?',
    criteria: {
      true: 'ambiguous, improvised, social, cinematic, or environment-dependent',
      false: 'ordinary bounded combat action the rules engine can validate',
    },
  },
};

async function verifyFirebase(req) {
  const header = req.headers.authorization || '';
  if (!header.startsWith('Bearer ')) {
    throw new Error('missing_auth');
  }
  const token = header.slice(7);
  const { payload } = await jwtVerify(token, JWKS, {
    audience: PROJECT_ID,
    issuer: 'https://securetoken.google.com/' + PROJECT_ID,
  });
  return payload;
}

function topProbability(answer) {
  const values = Object.values(answer?.probabilities || {});
  return values.length ? Math.max(...values.map(Number)) : 0;
}

function sanitizeState(body) {
  const action = String(body?.action || '').trim().slice(0, 1000);
  if (!action) throw new Error('missing_action');
  return {
    mode: 'combat',
    playerText: action,
    actor: body?.actor || {},
    target: body?.target || {},
    scene: String(body?.scene || '').slice(0, 6000),
    rulesAuthority:
      'Android validates all mechanics, action economy, legality, and dice.',
  };
}

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'method_not_allowed' });
    return;
  }

  try {
    await verifyFirebase(req);
  } catch {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }

  if (!process.env.AI_GATEWAY_API_KEY) {
    res.status(503).json({ error: 'jev_not_configured' });
    return;
  }

  let state;
  try {
    state = sanitizeState(req.body);
  } catch {
    res.status(400).json({ error: 'invalid_action' });
    return;
  }

  const started = Date.now();
  const gateway = await fetch(JEV_URL, {
    method: 'POST',
    headers: {
      Authorization: 'Bearer ' + process.env.AI_GATEWAY_API_KEY,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: MODEL,
      state,
      questions,
      providerOptions: {
        gateway: {
          only: ['typesafe-ai'],
        },
      },
    }),
  });

  const payload = await gateway.json().catch(() => ({}));
  if (!gateway.ok) {
    res.status(gateway.status).json({
      error: 'jev_gateway_error',
      type: payload?.error?.type || 'unknown',
    });
    return;
  }

  const answers = payload.answers || {};
  const action = answers.actionType || {};
  const target = answers.targetType || {};

  res.status(200).json({
    model: payload.model || MODEL,
    actionType: action.choice || 'other',
    actionConfidence: topProbability(action),
    targetType: target.choice || 'none',
    targetConfidence: topProbability(target),
    movementProbability: Number(
      answers.includesMovement?.probability || 0
    ),
    needsGmProbability: Number(
      answers.needsGm?.probability || 0
    ),
    latencyMs: Date.now() - started,
  });
}
