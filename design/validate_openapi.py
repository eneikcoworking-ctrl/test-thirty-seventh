#!/usr/bin/env python3
import sys
import os
import re

try:
    import yaml
except ImportError:
    print("Error: PyYAML is not installed. Run 'pip install pyyaml'")
    sys.exit(1)

def validate_openapi():
    yaml_path = "design/openapi.yaml"
    if not os.path.exists(yaml_path):
        print(f"Error: {yaml_path} does not exist!")
        return False

    print(f"Loading and parsing {yaml_path}...")
    try:
        with open(yaml_path, "r", encoding="utf-8") as f:
            spec = yaml.safe_load(f)
    except Exception as e:
        print(f"YAML Parsing Error: {e}")
        return False

    errors = []

    # 1. Check openapi version
    version = spec.get("openapi")
    if not version:
        errors.append("Missing 'openapi' version field.")
    elif not str(version).startswith("3."):
        errors.append(f"Unsupported OpenAPI version: {version}. Expected 3.x.x")

    # 2. Check info block
    info = spec.get("info")
    if not info:
        errors.append("Missing 'info' object.")
    else:
        if "title" not in info:
            errors.append("Missing 'info.title'.")
        if "version" not in info:
            errors.append("Missing 'info.version'.")

    # 3. Check paths
    paths = spec.get("paths")
    if not paths:
        errors.append("Missing 'paths' object.")
    else:
        required_paths = [
            "/api/accounts",
            "/api/accounts/{id}",
            "/api/accounts/onboard/request-otp",
            "/api/accounts/onboard/verify-otp",
            "/api/accounts/onboard/upload-session",
            "/api/accounts/{id}/proxy",
            "/api/accounts/{id}/settings",
            "/api/accounts/{id}/check-health",
            "/api/proxies",
            "/api/proxies/{id}"
        ]
        for rp in required_paths:
            if rp not in paths:
                errors.append(f"Missing required path: '{rp}'")

    # 4. Check schemas
    components = spec.get("components", {})
    schemas = components.get("schemas", {})
    required_schemas = [
        "TgAccount",
        "Proxy",
        "SessionStatus",
        "AccountSettings",
        "ProxyCreateRequest",
        "OnboardRequestOtpRequest",
        "OnboardRequestOtpResponse",
        "OnboardVerifyOtpRequest",
        "OnboardUploadSessionRequest",
        "BindProxyRequest"
    ]
    for rs in required_schemas:
        if rs not in schemas:
            errors.append(f"Missing required schema definition: '{rs}'")

    # 5. Check resolve of references
    # Traverse through the spec and collect all $ref values, then verify they exist
    refs = []
    def find_refs(obj):
        if isinstance(obj, dict):
            for k, v in obj.items():
                if k == "$ref" and isinstance(v, str):
                    refs.append(v)
                else:
                    find_refs(v)
        elif isinstance(obj, list):
            for item in obj:
                find_refs(item)

    find_refs(spec)

    for r in refs:
        if not r.startswith("#/"):
            errors.append(f"External or non-standard reference format found: '{r}'")
            continue
        parts = r.lstrip("#/").split("/")
        # resolve within spec
        curr = spec
        resolved = True
        for part in parts:
            if isinstance(curr, dict) and part in curr:
                curr = curr[part]
            else:
                resolved = False
                break
        if not resolved:
            errors.append(f"Unresolved reference: '{r}'")

    if errors:
        print("\nValidation failed with the following errors:")
        for err in errors:
            print(f" - {err}")
        return False

    print("\nAPI contract validation successful! All required endpoints, models, and references are valid.")
    return True

if __name__ == "__main__":
    success = validate_openapi()
    sys.exit(0 if success else 1)
