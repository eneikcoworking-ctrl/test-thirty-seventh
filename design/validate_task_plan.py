import sys
import json
import re

def validate_task_plan(filepath):
    print(f"Validating task plan: {filepath}")
    with open(filepath, 'r') as f:
        data = json.load(f)

    errors = []

    def check_six_sigma_metric(val, path):
        if not val:
            errors.append(f"{path}: sixSigmaMetric is empty.")
            return

        l_val = val.lower()
        if "from" not in l_val or "to" not in l_val:
            errors.append(f"{path}: sixSigmaMetric '{val}' must specify a measurable delta (containing 'from A to B') rather than an absolute target.")

    def check_acceptance_criteria(criteria, path):
        if not criteria:
            errors.append(f"{path}: acceptanceCriteria is empty.")
            return

        scenarios = re.findall(r'(Given\s+.*?When\s+.*?Then\s+.*?)(?=Given|\Z)', criteria, re.DOTALL | re.IGNORECASE)
        if len(scenarios) < 4:
            errors.append(f"{path}: acceptanceCriteria must contain at least 4 distinct Given/When/Then scenarios. Found {len(scenarios)}.")
            return

        annotated_scenarios = [s.lower() for s in scenarios]

        pos_detected = any("positive" in s or "success" in s for s in annotated_scenarios)
        neg_detected_count = sum(1 for s in annotated_scenarios if "negative" in s or "fail" in s or "invalid" in s or "incorrect" in s or "block" in s or "timeout" in s or "error" in s)
        edge_detected = any("edge" in s or "reset" in s or "expire" in s or "restart" in s or "shutdown" in s or "window" in s or "reopen" in s for s in annotated_scenarios)

        if not pos_detected:
            errors.append(f"{path}: acceptanceCriteria must include at least 1 positive scenario (explicitly annotated with 'Positive').")
        if neg_detected_count < 2:
            errors.append(f"{path}: acceptanceCriteria must include at least 2 negative scenarios (explicitly annotated with 'Negative'). Detected: {neg_detected_count}.")
        if not edge_detected:
            errors.append(f"{path}: acceptanceCriteria must include at least 1 edge case scenario (explicitly annotated with 'Edge Case').")

    epics = data.get("epics", [])
    if not epics:
        errors.append("Task plan does not contain any epics.")

    for i, epic in enumerate(epics):
        epic_path = f"epics[{i}]"
        if "sixSigmaMetric" in epic and epic["sixSigmaMetric"]:
            check_six_sigma_metric(epic["sixSigmaMetric"], f"{epic_path}.sixSigmaMetric")

        slices = epic.get("slices", [])
        for j, slice_obj in enumerate(slices):
            slice_path = f"{epic_path}.slices[{j}]"
            if "sixSigmaMetric" in slice_obj:
                check_six_sigma_metric(slice_obj["sixSigmaMetric"], f"{slice_path}.sixSigmaMetric")

            if "acceptanceCriteria" in slice_obj:
                check_acceptance_criteria(slice_obj["acceptanceCriteria"], f"{slice_path}.acceptanceCriteria")

    if errors:
        print("Validation FAILED with the following errors:")
        for err in errors:
            print(f" - {err}")
        sys.exit(1)

    print("Validation SUCCESSFUL! Task plan strictly complies with all quality constraints.")
    sys.exit(0)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 validate_task_plan.py <path_to_json>")
        sys.exit(1)
    validate_task_plan(sys.argv[1])
