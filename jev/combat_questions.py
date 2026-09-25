COMBAT_QUESTIONS = {
    "rulesPath": {
        "type": "choice",
        "instructions": (
            "Decide whether this combat declaration is a normal bounded action "
            "the Android rules engine can validate, or needs creative GM adjudication."
        ),
        "criteria": {
            "rules": (
                "ordinary Strike, Stride/Step, spell use, draw/use item, Raise Shield, "
                "Take Cover, Aid, Ready, Demoralize, Feint, Shove, Trip, Grapple, "
                "Tumble Through, Recall Knowledge, or similarly defined rules action"
            ),
            "gm": (
                "improvised stunt, negotiation, unusual environmental manipulation, "
                "cinematic multi-part stunt, or action whose outcome depends on fiction"
            ),
        },
    },
    "actionType": {
        "type": "choice",
        "instructions": (
            "Classify the primary rules action. Prefer a rules action even when "
            "movement is also included. Use other only for genuinely improvised actions."
        ),
        "criteria": {
            "strike": "weapon or unarmed attack against a creature",
            "move": "Stride, Step, reposition, approach, retreat, or chase",
            "cast_spell": "cast, sustain, dismiss, or otherwise use a spell",
            "interact": "draw, open, pick up, manipulate, or use an ordinary object",
            "skill_action": (
                "defined skill action such as Demoralize, Feint, Shove, Trip, "
                "Grapple, Tumble Through, Recall Knowledge, or similar"
            ),
            "defend": "Raise Shield, Take Cover, guard, or improve defenses",
            "aid": "Aid another character's action",
            "ready": "Ready an action for a later trigger",
            "other": "improvised or unbounded action not covered above",
        },
    },
    "targetType": {
        "type": "choice",
        "instructions": (
            "Classify the object of the main action. For pure movement away from a foe, "
            "use none unless the destination itself is the object of the action."
        ),
        "criteria": {
            "enemy": "a hostile creature is the object of the main action",
            "ally": "a friendly creature is the object of the main action",
            "self": "the actor is explicitly the object of the main action",
            "environment": "an object, terrain feature, door, lever, or destination",
            "none": "no distinct target; pure movement can use none",
        },
    },
    "actorMoves": {
        "type": "boolean",
        "instructions": (
            "Does the acting character physically change position as part of the declaration? "
            "Do not count moving the target, drawing an item, or merely moving a limb."
        ),
    },
    "needsGm": {
        "type": "boolean",
        "instructions": (
            "Is creative fictional adjudication required? Return false for normal defined "
            "rules actions, including opening an ordinary door, pulling a normal lever, "
            "Demoralize, Feint, Shove, Tumble Through, Aid, Ready, Raise Shield and Take Cover."
        ),
    },
}
