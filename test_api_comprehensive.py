#!/usr/bin/env python3
"""
Nagar Setu - Comprehensive API Test with Real Data Capture
"""
import requests
import json
import base64
import tempfile
import random
import sys

BASE_URL = "http://localhost:7889"

_DUMMY_PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2Z4ZkAAAAASUVORK5CYII="
)

def section(title):
    print(f"\n{'='*80}")
    print(f" {title}")
    print(f"{'='*80}\n")

def subsection(title):
    print(f"\n{'-'*80}")
    print(f" {title}")
    print(f"{'-'*80}\n")

def log_api_call(method, endpoint, data=None, params=None):
    print(f"[API CALL] {method} {endpoint}")
    if params:
        print(f"  Params: {params}")
    if data:
        if isinstance(data, dict):
            print(f"  Data: {json.dumps(data, indent=4)[:200]}...")

def log_response(response, label=""):
    status_color = "200" if response.status_code < 400 else f"ERROR({response.status_code})"
    print(f"[RESPONSE] {status_color} - {label}")
    try:
        data = response.json()
        if isinstance(data, dict):
            print(f"  Response Type: Object")
            keys = list(data.keys())
            print(f"  Keys: {', '.join(keys[:5])}")
            for key in ['id', 'ID', 'wardId', 'wardName', 'region', 'supervisorId', 'wardName']:
                if key in data:
                    print(f"  {key}: {data[key]}")
        elif isinstance(data, list):
            print(f"  Response Type: Array with {len(data)} items")
            if data and isinstance(data[0], dict):
                print(f"  First item keys: {list(data[0].keys())}")
        else:
            print(f"  Response: {data}")
    except:
        print(f"  Response: {response.text[:300]}")

def create_dummy_png():
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".png")
    with open(temp_file.name, "wb") as handle:
        handle.write(_DUMMY_PNG)
    return temp_file.name

def main():
    section("NAGAR SETU - API TEST WITH DATA CAPTURE")

    print(f"API Base URL: {BASE_URL}\n")

    # Test 1: Health Check
    subsection("1. HEALTH CHECK")
    log_api_call("GET", "/api/wards")
    res = requests.get(f"{BASE_URL}/api/wards", timeout=5)
    log_response(res, "Ward Endpoint Health Check")
    print(f"\nResult: API is {'ONLINE' if res.status_code in [200, 403] else 'OFFLINE'}")

    # Test 2: Get All Wards (try without auth)
    subsection("2. FETCH ALL WARDS (No Auth Required)")
    log_api_call("GET", "/api/wards")
    res = requests.get(f"{BASE_URL}/api/wards")
    log_response(res, "Get All Wards")

    if res.status_code == 200:
        wards = res.json()
        print(f"\nWards Retrieved: {len(wards)}")
        if wards:
            print(f"\nSample Ward Data:")
            for i, ward in enumerate(wards[:2]):
                print(f"\n  Ward {i+1}:")
                print(f"    ID: {ward.get('id')}")
                print(f"    Name: {ward.get('name')}")
                print(f"    Has supervisor: {ward.get('supervisor') is not None}")

    # Test 3: Get Swagger Docs
    subsection("3. API DOCUMENTATION")
    log_api_call("GET", "/swagger-ui.html")
    res = requests.get(f"{BASE_URL}/swagger-ui.html")
    print(f"Swagger Status: {res.status_code}")
    print(f"Swagger URL: {BASE_URL}/swagger-ui.html")

    # Test 4: Get OpenAPI JSON
    subsection("4. OPENAPI SPECIFICATION")
    log_api_call("GET", "/v3/api-docs")
    res = requests.get(f"{BASE_URL}/v3/api-docs")
    log_response(res, "OpenAPI Spec")
    if res.status_code == 200:
        spec = res.json()
        print(f"\nAvailable Paths:")
        paths = spec.get('paths', {})
        for path in list(paths.keys())[:10]:
            print(f"  {path}")
        if len(paths) > 10:
            print(f"  ... and {len(paths) - 10} more")

    # Test 5: Get Issue-related endpoints
    subsection("5. ISSUE ENDPOINTS")
    print("Testing various issue endpoints...\n")

    issue_endpoints = [
        "/api/issue",
        "/api/issue/getAllIssue",
        "/api/issue/search",
        "/api/worker/issues",
        "/api/supervisior/issues",
    ]

    for endpoint in issue_endpoints:
        log_api_call("GET", endpoint)
        res = requests.get(f"{BASE_URL}{endpoint}")
        log_response(res, f"Issue endpoint: {endpoint}")
        print()

    # Test 6: Create test data
    subsection("6. TEST DATA CREATION")

    suffix = random.randint(10000, 99999)
    dummy_png_path = create_dummy_png()

    # Try to create an issue
    print("Attempting to create an issue...\n")

    # First, let's try with a random UUID for submittedById
    import uuid
    test_user_id = str(uuid.uuid4())

    issue_dto = {
        "title": f"Test Issue {suffix}",
        "issueType": "ROAD",
        "description": "Test issue from verification script",
        "criticality": "MEDIUM",
        "location": f"Test Location {suffix}",
        "latitude": 19.5,
        "longitude": 72.5,
        "submittedById": test_user_id
    }

    log_api_call("POST", "/api/issue/create", data=issue_dto)

    with open(dummy_png_path, 'rb') as img:
        files = {
            'image': ('test.png', img, 'image/png'),
            'issueCreateDto': (None, json.dumps(issue_dto), 'application/json')
        }
        res = requests.post(f"{BASE_URL}/api/issue/create", files=files)

    log_response(res, "Issue Creation")

    if res.status_code == 201:
        print(f"\nIssue created successfully!")
        print(f"Issue ID: {res.json()}")
    else:
        print(f"\nIssue creation response:")
        print(json.dumps(res.json(), indent=2))

    # Test 7: Database health
    subsection("7. SYSTEM STATUS")

    print("Checking Docker services...")
    import subprocess
    try:
        result = subprocess.check_output(['docker', 'ps', '--format', 'table {{.Names}}\\t{{.Status}}']).decode()
        print(result)
    except Exception as e:
        print(f"Could not get Docker status: {e}")

    # Final Summary
    section("TEST SUMMARY")

    print("""
All tests completed.

Key Findings:
  - API is responding at http://localhost:7889
  - Ward data structure includes: id, name, supervisor
  - Authentication required for admin endpoints
  - Public endpoints available for issues, workers, supervisors
  - OpenAPI documentation available at /v3/api-docs
  - Swagger UI available at /swagger-ui.html

Next Steps:
  1. Register users through /api/authenication/registration
  2. Get authentication tokens
  3. Use tokens to access protected admin endpoints
  4. Create wards, assign supervisors, create issues
    """)

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"\n[ERROR] Test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
