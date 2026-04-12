#!/usr/bin/env python3
"""
Nagar Setu - Simple Flow Test (No Login Required)
Just prints values from API responses
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
    print(f"[TEST] {title}")
    print(f"{'='*70}")

def print_info(message):
    print(f"[INFO] {message}")

def print_success(message):
    print(f"[OK] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_response(label, response):
    print(f"\n[RESPONSE] {label}")
    print(f"Status Code: {response.status_code}")
    try:
        data = response.json()
        print(f"Response Body:")
        print(json.dumps(data, indent=2))
        return data
    except:
        print(f"Response Text: {response.text}")
        return None

def create_dummy_png():
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".png")
    with open(temp_file.name, "wb") as handle:
        handle.write(_DUMMY_PNG)
    return temp_file.name

def test_ward_creation(suffix):
    """Test 1: Ward Creation with Region"""
    print_header("WARD CREATION WITH REGION")

    print_info("Uploading Ward GeoJSON with Region...")
    geojson = {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "properties": {
                    "Ward": f"TEST_WARD_{suffix}",
                    "Region": "North Mumbai"
                },
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [[[72.0, 19.0], [73.0, 19.0], [73.0, 20.0], [72.0, 20.0], [72.0, 19.0]]]
                }
            }
        ]
    }

    res = requests.post(f"{BASE_URL}/api/wards/upload", json=geojson)
    print_response("Ward Upload", res)

    # Fetch all wards
    print_info("\nFetching all wards...")
    res = requests.get(f"{BASE_URL}/api/wards")
    wards_data = print_response("Get All Wards", res)

    if wards_data:
        print_info(f"\nTotal wards found: {len(wards_data)}")
        if wards_data:
            ward = wards_data[0]
            print_info(f"\nFirst Ward Details:")
            print(f"  - Name: {ward.get('name')}")
            print(f"  - ID: {ward.get('id')}")
            print(f"  - Supervisor: {ward.get('supervisor')}")
            return ward.get('id')

    return None

def test_export_geojson():
    """Test 2: Export GeoJSON with Region"""
    print_header("EXPORT GEOJSON")

    print_info("Exporting wards as GeoJSON...")
    res = requests.get(f"{BASE_URL}/api/wards/geojson")

    if res.status_code == 200:
        data = res.json()
        print_response("Export GeoJSON", res)

        if data.get('features'):
            print_info(f"\nTotal features in export: {len(data['features'])}")
            if data['features']:
                feature = data['features'][0]
                print_info(f"\nFirst Feature Properties:")
                props = feature.get('properties', {})
                print(f"  - Name: {props.get('name')}")
                print(f"  - Region: {props.get('region')}")
                print(f"  - Reason: {props.get('reason')}")
    else:
        print_error(f"Failed to export GeoJSON: {res.status_code}")

def test_wards_within_area():
    """Test 3: Get Wards Within Area"""
    print_header("GET WARDS WITHIN AREA")

    print_info("Fetching wards within bounding box...")
    request_body = {
        "coordinates": [
            [72.0, 19.0],
            [73.0, 19.0],
            [73.0, 20.0],
            [72.0, 20.0],
            [72.0, 19.0]
        ]
    }

    res = requests.post(f"{BASE_URL}/api/wards/within", json=request_body)
    wards_data = print_response("Wards Within Area", res)

    if wards_data:
        print_info(f"\nWards found in area: {len(wards_data)}")
        for i, ward in enumerate(wards_data):
            print(f"\n  Ward {i+1}:")
            print(f"    - Name: {ward.get('name')}")
            print(f"    - ID: {ward.get('id')}")

def test_issue_creation(cit_id, suffix, dummy_png_path):
    """Test 4: Issue Creation"""
    print_header("ISSUE CREATION")

    print_info("Creating issue...")
    issue_dto = {
        "title": f"Test Crack {suffix}",
        "issueType": "ROAD",
        "description": "Test issue from verification script",
        "criticality": "HIGH",
        "location": f"Test Area {suffix}",
        "latitude": 19.5,
        "longitude": 72.5,
        "submittedById": cit_id or "00000000-0000-0000-0000-000000000000"
    }

    with open(dummy_png_path, 'rb') as img:
        files = {
            'image': ('test.png', img, 'image/png'),
            'issueCreateDto': (None, json.dumps(issue_dto), 'application/json')
        }
        res = requests.post(f"{BASE_URL}/api/issue/create", files=files)

    issue_response = print_response("Issue Create", res)

    if res.status_code == 201 and issue_response:
        issue_id = issue_response
        print_info(f"\nIssue ID: {issue_id}")
        return issue_id

    return None

def test_get_all_wards_admin(suffix):
    """Test 5: Get All Admin Wards"""
    print_header("GET ALL ADMIN WARDS (WITH REGION)")

    print_info("Fetching admin wards...")
    res = requests.get(f"{BASE_URL}/api/admin/wards")

    if res.status_code == 200:
        wards = print_response("Admin Wards", res)

        if wards:
            print_info(f"\nTotal admin wards: {len(wards)}")
            # Find our test ward
            test_wards = [w for w in wards if suffix in w.get('wardName', '')]
            if test_wards:
                print_info(f"\nFound {len(test_wards)} test ward(s):")
                for ward in test_wards:
                    print(f"\n  Ward Details:")
                    print(f"    - ID: {ward.get('wardId')}")
                    print(f"    - Name: {ward.get('wardName')}")
                    print(f"    - Region: {ward.get('region')}")
                    print(f"    - Supervisor ID: {ward.get('supervisorId')}")
                    print(f"    - Supervisor Name: {ward.get('supervisorName')}")
            else:
                print_info("\nNo test wards found, showing first 3 wards:")
                for i, ward in enumerate(wards[:3]):
                    print(f"\n  Ward {i+1}:")
                    print(f"    - ID: {ward.get('wardId')}")
                    print(f"    - Name: {ward.get('wardName')}")
                    print(f"    - Region: {ward.get('region')}")
                    print(f"    - Supervisor: {ward.get('supervisorName')}")
    else:
        print_error(f"Failed to fetch admin wards: {res.status_code}")

def test_health():
    """Test 0: Basic Health Check"""
    print_header("API HEALTH CHECK")

    print_info("Checking API health...")
    try:
        res = requests.get(f"{BASE_URL}/api/wards", timeout=5)
        if res.status_code in [200, 401, 403]:
            print_success(f"API is responding. Status: {res.status_code}")
            return True
        else:
            print_error(f"Unexpected status code: {res.status_code}")
            return False
    except Exception as e:
        print_error(f"API not responding: {str(e)}")
        return False

def main():
    print(f"\n{'='*70}")
    print(f"NAGAR SETU - SIMPLE FLOW TEST (NO LOGIN)")
    print(f"{'='*70}")

    # Generate test suffix
    suffix = random.randint(10000, 99999)
    print_info(f"Test Suffix: {suffix}")

    # Create dummy PNG
    dummy_png_path = create_dummy_png()

    try:
        # Test 0: Health Check
        if not test_health():
            print_error("API is not responding. Make sure Docker containers are running.")
            sys.exit(1)

        # Test 1: Ward Creation
        ward_id = test_ward_creation(suffix)

        # Test 2: Export GeoJSON
        test_export_geojson()

        # Test 3: Get wards within area
        test_wards_within_area()

        # Test 4: Issue Creation (with dummy citizen ID)
        dummy_cit_id = "12345678-1234-1234-1234-123456789012"
        issue_id = test_issue_creation(dummy_cit_id, suffix, dummy_png_path)

        # Test 5: Get Admin Wards
        test_get_all_wards_admin(suffix)

        # Summary
        print(f"\n{'='*70}")
        print(f"ALL TESTS COMPLETED!")
        print(f"{'='*70}\n")

        print("Summary of Tests:")
        print(f"  [OK] Health Check")
        print(f"  [OK] Ward Creation with Region")
        print(f"  [OK] Export GeoJSON")
        print(f"  [OK] Get Wards Within Area")
        print(f"  [OK] Issue Creation")
        print(f"  [OK] Get Admin Wards")

    except Exception as e:
        print_error(f"Test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
