import statistics
import sys
import time

from benchmark_cases import CASES, structured_case
from combat_questions import COMBAT_QUESTIONS
from jev_client import JevError, evaluate


def top_choice(answer):
    probs = answer.get("probabilities", {})
    choice = answer.get("choice")
    return choice, float(probs.get(choice, 0.0))


def as_bool(answer):
    probability = float(answer.get("probability", 0))
    return probability >= 0.5, probability


def main():
    exact_results = []
    route_results = []
    latencies = []
    for index, raw_case in enumerate(CASES, 1):
        case = structured_case(raw_case)
        state = {
            "mode": "combat",
            "playerText": case["text"],
            "rulesAuthority": "Android validates all mechanics and rolls dice.",
        }
        try:
            response = evaluate(state, COMBAT_QUESTIONS)
        except JevError as error:
            print("JEV REQUEST BLOCKED")
            print("status:", error.status)
            print("type:", error.error_type)
            print("message:", str(error))
            return 2

        answers = response["answers"]
        route, route_conf = top_choice(answers["rulesPath"])
        action, action_conf = top_choice(answers["actionType"])
        target, target_conf = top_choice(answers["targetType"])
        movement, _ = as_bool(answers["actorMoves"])
        needs_gm, gm_prob = as_bool(answers["needsGm"])
        latency = response.get("_elapsedMs", 0)
        attempts = response.get("_attempts", 1)
        latencies.append(latency)

        expected = case["expected"]
        expected_route = "gm" if expected["needsGm"] else "rules"
        route_ok = route == expected_route
        exact_ok = (
            action == expected["actionType"]
            and movement == expected["includesMovement"]
            and needs_gm == expected["needsGm"]
        )
        exact_results.append(exact_ok)
        route_results.append(route_ok)
        print(
            f"{index:02d} {'ROUTE' if route_ok else 'MISS '} "
            f"{latency:4d}ms x{attempts} "
            f"path={route}({route_conf:.2f}) "
            f"action={action}({action_conf:.2f}) "
            f"target={target}({target_conf:.2f}) "
            f"move={movement} gm={needs_gm}({gm_prob:.2f})"
        )

        time.sleep(0.08)

    route_accuracy = sum(route_results) / len(route_results)
    exact_accuracy = sum(exact_results) / len(exact_results)
    ordered = sorted(latencies)
    p95_index = max(0, int(len(ordered) * 0.95) - 1)
    print()
    print(f"cases: {len(route_results)}")
    print(f"routing accuracy: {route_accuracy:.1%}")
    print(f"action/move/gm exact: {exact_accuracy:.1%}")
    print(f"median latency: {statistics.median(latencies):.0f}ms")
    print(f"p95 latency: {ordered[p95_index]}ms")
    return 0 if route_accuracy >= 0.90 else 1


if __name__ == "__main__":
    sys.exit(main())
