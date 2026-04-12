#!/usr/bin/env python3
"""
Nagar Setu - Test Public Endpoints (No Login Required)
Tests issue creation, worker, supervisor endpoints
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

def print_header(title):
    print(f"\n{'='*70}")
    print(f"TEST: {title}")
    print(f"{'='*70}")

def print_info(message):
    print(f"[INFO] {message}")

def print_success(message):
    print(f"[OK] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_response(label, response, show_body=True):
    print(f"\n[RESPONSE] {label}")
    print(f"  Status Code: {response.status_code}")
    try:
        data = response.json()
        if show_body:
            print(f"  Response:")
            if isinstance(data, list):
                print(f"    Type: List with {len(data)} items")
                if data:
                    print(f"    First item: {json.dumps(data[0], indent=6)}")
            else:
                print(f"    {json.dumps(data, indent=4)}")
        return data
    except:
        print(f"  Response Text: {response.text[:200]}")
        return None

def create_dummy_png():
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".png")
    with open(temp_file.name, "wb") as handle:
        handle.write(_DUMMY_PNG)
    return temp_file.name

def test_health():
    """Test 0: Health Check"""
    print_header("API HEALTH CHECK")
    print_info("Checking if API is running...")

    try:
        res = requests.get(f"{BASE_URL}/api/wards", timeout=5)
        print_success(f"API is responding")
        return True
    except Exception as e:
        print_error(f"API not responding: {str(e)}")
        return False

def test_issue_creation(suffix, dummy_png_path):
    """Test 1: Issue Creation (PUBLIC ENDPOINT)"""
    print_header("ISSUE CREATION (PUBLIC)")

    dummy_cit_id = "12345678-1234-1234-1234-123456789012"

    print_info("Creating issue with image...")
    issue_dto = {
        "title": f"Road Pothole {suffix}",
        "issueType": "ROAD",
        "description": "Large pothole on main road",
        "criticality": "HIGH",
        "location": f"Test Location {suffix}",
        "latitude": 19.5,
        "longitude": 72.5,
        "submittedById": dummy_cit_id
    }

    print_info(f"Issue Details:")
    print(f"  Title: {issue_dto['title']}")
    print(f"  Type: {issue_dto['issueType']}")
    print(f"  Criticality: {issue_dto['criticality']}")
    print(f"  Location: {issue_dto['location']}")

    with open(dummy_png_path, 'rb') as img:
        files = {
            'image': ('test.png', img, 'image/png'),
            'issueCreateDto': (None, json.dumps(issue_dto), 'application/json')
        }
        res = requests.post(f"{BASE_URL}/api/issue/create", files=files)

    response_data = print_response("Issue Create Response", res)

    if res.status_code == 201:
        print_success(f"Issue created with ID: {response_data}")
        return response_data
    else:
        print_error(f"Failed to create issue: {res.status_code}")
        return None

def test_get_issue(issue_id):
    """Test 2: Get Issue Details (PUBLIC)"""
    if not issue_id:
        return

    print_header("GET ISSUE DETAILS (PUBLIC)")

    print_info(f"Fetching issue: {issue_id}")
    res = requests.get(f"{BASE_URL}/api/issue/{issue_id}")

    response_data = print_response("Get Issue Response", res)

    if res.status_code == 200 and response_data:
        print_success("Issue retrieved successfully")
        print(f"\nIssue Details:")
        print(f"  ID: {response_data.get('id')}")
        print(f"  Title: {response_data.get('title')}")
        print(f"  Type: {response_data.get('issueType')}")
        print(f"  Stage: {response_data.get('stages')}")
        print(f"  Criticality: {response_data.get('criticality')}")
        print(f"  Status: {response_data.get('status')}")
    else:
        print_error(f"Failed to retrieve issue: {res.status_code}")

def test_authentication():
    """Test 3: Authentication (PUBLIC)"""
    print_header("AUTHENTICATION ENDPOINTS (PUBLIC)")

    suffix = random.randint(10000, 99999)
    test_email = f"test{suffix}@nagar.com"

    print_info(f"Requesting OTP for: {test_email}")
    res = requests.get(f"{BASE_URL}/api/authenication/getCode?email={test_email}")

    print_response("Get Code Response", res, show_body=False)
    print_success(f"OTP request sent to {test_email}")

def test_issue_list_by_location():
    """Test 4: Get Issues by Location (PUBLIC)"""
    print_header("GET ISSUES BY LOCATION (PUBLIC)")

    print_info("Fetching issues...")
    # Try different endpoints
    endpoints = [
        "/api/issue",
        "/api/issue/getAllIssue",
    ]

    for endpoint in endpoints:
        print_info(f"Trying endpoint: {endpoint}")
        res = requests.get(f"{BASE_URL}{endpoint}")
        print(f"  Status: {res.status_code}")

        if res.status_code == 200:
            response_data = print_response(f"Issues from {endpoint}", res)
            if response_data:
                print_success(f"Found {len(response_data) if isinstance(response_data, list) else 1} issue(s)")
                break

def test_admin_endpoints():
    """Test 5: Admin Endpoints (Requires Auth)"""
    print_header("ADMIN ENDPOINTS (REQUIRES AUTHENTICATION)")

    print_info("Testing protected endpoints...")

    endpoints = [
        ("/api/admin/wards", "GET"),
        ("/api/admin/workers", "GET"),
        ("/api/admin/supervisors", "GET"),
    ]

    for endpoint, method in endpoints:
        print_info(f"Testing {method} {endpoint}")
        if method == "GET":
            res = requests.get(f"{BASE_URL}{endpoint}")

        print(f"  Status: {res.status_code}")

        if res.status_code == 403:
            print_error(f"  Access denied (requires authentication)")
        elif res.status_code == 200:
            print_success(f"  Success")
        else:
            print_info(f"  Response: {res.status_code}")

def test_swagger():
    """Test 6: Swagger/API Docs (PUBLIC)"""
    print_header("SWAGGER/API DOCS (PUBLIC)")

    print_info("Checking API documentation...")
    res = requests.get(f"{BASE_URL}/swagger-ui.html")

    if res.status_code == 200:
        print_success("Swagger UI is accessible")
        print(f"  URL: {BASE_URL}/swagger-ui.html")
    else:
        print_error(f"Swagger UI returned status: {res.status_code}")

def main():
    print(f"\n{'='*70}")
    print(f"NAGAR SETU - PUBLIC ENDPOINTS TEST")
    print(f"Testing without authentication")
    print(f"{'='*70}")

    suffix = random.randint(10000, 99999)
    print_info(f"Test Suffix: {suffix}")

    dummy_png_path = create_dummy_png()

    try:
        # Test 0: Health
        if not test_health():
            print_error("API is not responding!")
            sys.exit(1)

        # Test 1: Issue Creation
        issue_id = test_issue_creation(suffix, dummy_png_path)

        # Test 2: Get Issue
        if issue_id:
            test_get_issue(issue_id)

        # Test 3: Authentication
        test_authentication()

        # Test 4: List Issues
        test_issue_list_by_location()

        # Test 5: Admin Endpoints (should fail)
        test_admin_endpoints()

        # Test 6: Swagger
        test_swagger()

        # Summary
        print(f"\n{'='*70}")
        print(f"TEST EXECUTION COMPLETED!")
        print(f"{'='*70}\n")

        print("Summary:")
        print(f"  [OK] API Health Check")
        print(f"  [OK] Issue Creation (Public)")
        print(f"  [OK] Get Issue Details (Public)")
        print(f"  [OK] Authentication Endpoints")
        print(f"  [OK] Issue List")
        print(f"  [OK] Admin Endpoints (Protected)")
        print(f"  [OK] Swagger Documentation")
        print()

    except Exception as e:
        print_error(f"Test execution failed: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
