import requests
import time
import subprocess
import json
import os
import base64
import tempfile
import random
import sys
import io

# Force UTF-8 encoding for stdout
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE_URL = "http://localhost:7889"

_DUMMY_PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2Z4ZkAAAAASUVORK5CYII="
)

class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'

def create_dummy_png():
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".png")
    with open(temp_file.name, "wb") as handle:
        handle.write(_DUMMY_PNG)
    return temp_file.name

def ensure(condition, message):
    if not condition:
        print(f"{Colors.RED}[X] {message}{Colors.END}")
        raise AssertionError(message)

def success(message):
    print(f"{Colors.GREEN}[OK] {message}{Colors.END}")

def info(message):
    print(f"{Colors.BLUE}[*] {message}{Colors.END}")

def warn(message):
    print(f"{Colors.YELLOW}[!] {message}{Colors.END}")

def response_json(response, label):
    try:
        return response.json()
    except ValueError as exc:
        raise AssertionError(f"{label} did not return valid JSON: {response.status_code} - {response.text}") from exc

def get_otp(email):
    info(f"Requesting OTP for {email}...")
    requests.get(f"{BASE_URL}/api/authenication/getCode?email={email}")
    time.sleep(2)
    info(f"Extracting OTP from logs...")
    try:
        logs = subprocess.check_output(['docker', 'logs', 'nagarsetu', '--since', '5m', '--tail', '500']).decode('utf-8')
        for line in logs.split('\n'):
            if "Code is" in line and email in line:
                return line.split("Code is ")[1].split(" ")[0].strip()
    except Exception as e:
        warn(f"Error reading logs: {e}")
    return None

def register(fullName, email, password, role, location="Mumbai"):
    code = get_otp(email)
    if not code:
        warn(f"Could not get OTP for {email} - using default code 000000")
        code = "000000"
    info(f"Got OTP: {code}. Registering {role}...")
    payload = {
        "fullName": fullName,
        "email": email,
        "password": password,
        "role": role,
        "code": code,
        "phoneNumber": "1234567890",
        "age": 25,
        "gender": "MALE",
        "location": location
    }
    resp = requests.post(f"{BASE_URL}/api/authenication/registration", json=payload)
    if resp.status_code == 201:
        data = resp.json()
        success(f"{role} registered with ID: {data['id']}")
        return data['token'], data['id']
    else:
        warn(f"Registration failed for {role}: {resp.status_code} - {resp.text}")
        return None, None

def test_ward_creation(headers_admin, suffix):
    """Test 1: Ward Creation with Region"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 1: WARD CREATION WITH REGION")
    print(f"{'='*60}{Colors.END}")

    info("Uploading Ward GeoJSON with Region...")
    geojson = {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "properties": {
                    "Ward": f"WARD_{suffix}",
                    "Region": "North Mumbai"
                },
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [[[72.0, 19.0], [73.0, 19.0], [73.0, 20.0], [72.0, 20.0], [72.0, 19.0]]]
                }
            }
        ]
    }
    res = requests.post(f"{BASE_URL}/api/wards/upload", json=geojson, headers=headers_admin)
    ensure(res.status_code == 200, f"Ward upload failed: {res.text}")
    success("Ward uploaded successfully with Region")

    # Fetch and verify ward
    res = requests.get(f"{BASE_URL}/api/admin/wards", headers=headers_admin)
    ensure(res.status_code == 200, "Get wards failed")
    wards = res.json()
    target_ward = next((w for w in wards if w["wardName"] == f"WARD_{suffix}"), None)
    ensure(target_ward, "Uploaded ward not found")
    ensure(target_ward.get("region") == "North Mumbai", f"Region mismatch: {target_ward.get('region')}")
    success(f"Ward verified with Region: {target_ward.get('region')}")

    return target_ward["wardId"]

def test_supervisor_creation(headers_admin, sup_id, suffix):
    """Test 2: Supervisor Creation and Acceptance"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 2: SUPERVISOR CREATION AND ACCEPTANCE")
    print(f"{'='*60}{Colors.END}")

    info("Admin accepting Supervisor...")
    sup_payload = {
        "department": "ROAD",
        "jurisdictionName": f"WARD_{suffix}",
        "jurisdictionCenterLat": 19.5,
        "jurisdictionCenterLon": 72.5,
        "jurisdictionRadiusKm": 10.0
    }
    res = requests.post(f"{BASE_URL}/api/admin/acceptSupervisor/{sup_id}", json=sup_payload, headers=headers_admin)
    ensure(res.status_code == 200, f"Accept supervisor failed: {res.text}")
    success("Supervisor accepted successfully")

    # Verify supervisor
    res = requests.get(f"{BASE_URL}/api/admin/supervisors", headers=headers_admin)
    ensure(res.status_code == 200, "Get supervisors failed")
    sups = res.json()
    mapped_sup = next((s for s in sups if s["id"] == sup_id), None)
    ensure(mapped_sup, "Supervisor not found in list")
    success(f"Supervisor verified: {mapped_sup['id']}")

def test_ward_assignment(headers_admin, ward_id, sup_id, suffix):
    """Test 3: Ward Assignment to Supervisor"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 3: WARD ASSIGNMENT TO SUPERVISOR")
    print(f"{'='*60}{Colors.END}")

    info("Allocating Ward to Supervisor...")
    res = requests.put(f"{BASE_URL}/api/admin/wards/{ward_id}/supervisor/{sup_id}", headers=headers_admin)
    ensure(res.status_code == 200, f"Allocate ward failed: {res.text}")
    success("Ward allocated to Supervisor")

    # Verify allocation
    res = requests.get(f"{BASE_URL}/api/admin/supervisors", headers=headers_admin)
    sups = res.json()
    mapped_sup = next((s for s in sups if s["id"] == sup_id), None)
    ensure(mapped_sup.get("wardName") == f"WARD_{suffix}", "Supervisor wardName mismatch")
    success(f"Ward assignment verified: {mapped_sup['wardName']}")

def test_worker_creation(headers_admin, sup_id, suffix):
    """Test 4: Worker Creation and Assignment"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 4: WORKER CREATION AND ASSIGNMENT")
    print(f"{'='*60}{Colors.END}")

    # Register worker
    email_worker = f"worker{suffix}@nagar.com"
    info("Registering Worker...")
    worker_token, worker_id = register("Fast Worker", email_worker, "wrk123", "WORKER")
    ensure(worker_token and worker_id, "Worker registration failed")
    success(f"Worker registered with ID: {worker_id}")

    # Accept worker
    headers_admin_local = headers_admin
    info("Admin accepting Worker...")
    res = requests.post(f"{BASE_URL}/api/admin/acceptWorker/{worker_id}/{sup_id}", headers=headers_admin_local)
    ensure(res.status_code == 200, f"Accept worker failed: {res.text}")
    success("Worker accepted successfully")

    # Verify worker mapping
    res = requests.get(f"{BASE_URL}/api/admin/workers", headers=headers_admin_local)
    workers = res.json()
    mapped_wrk = next((w for w in workers if w["id"] == worker_id), None)
    ensure(mapped_wrk, "Worker not found in list")
    ensure(mapped_wrk.get("supervisorId") == sup_id, "Worker supervisorId mismatch")
    ensure(mapped_wrk.get("wardName") == f"WARD_{suffix}", "Worker wardName mismatch")
    success(f"Worker assignment verified: Supervisor={sup_id}, Ward={mapped_wrk['wardName']}")

    return worker_token, worker_id

def test_issue_creation(headers_cit, cit_id, suffix, run_location, dummy_png_path, headers_admin):
    """Test 5: Issue Creation and Auto-Assignment"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 5: ISSUE CREATION AND AUTO-ASSIGNMENT")
    print(f"{'='*60}{Colors.END}")

    info("Citizen reporting Issue...")
    issue_dto = {
        "title": "Road Crack",
        "issueType": "ROAD",
        "description": "Crack inside the ward polygon test",
        "criticality": "HIGH",
        "location": run_location,
        "latitude": 19.5,
        "longitude": 72.5,
        "submittedById": cit_id
    }
    with open(dummy_png_path, 'rb') as img:
        files = {
            'image': ('dummy.png', img, 'image/png'),
            'issueCreateDto': (None, json.dumps(issue_dto), 'application/json')
        }
        res = requests.post(f"{BASE_URL}/api/issue/create", files=files, headers=headers_cit)
    ensure(res.status_code == 201, f"Issue create failed: {res.text}")
    issue_id = res.json()
    success(f"Issue created with ID: {issue_id}")

    # Verify auto-assignment
    res = requests.get(f"{BASE_URL}/api/issue/{issue_id}", headers=headers_cit)
    issue_data = res.json()
    ensure(issue_data["stages"] == "TEAM_ASSIGNED", f"Issue not auto-assigned: {issue_data['stages']}")
    success(f"Issue auto-assigned to team with stage: {issue_data['stages']}")

    return issue_id

def test_issue_resolution(headers_worker, issue_id, dummy_png_path):
    """Test 6: Issue Resolution"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 6: ISSUE RESOLUTION")
    print(f"{'='*60}{Colors.END}")

    info("Worker resolving Issue...")
    with open(dummy_png_path, 'rb') as img:
        res = requests.post(f"{BASE_URL}/api/issue/done?id={issue_id}", files={'file': ('done.png', img, 'image/png')}, headers=headers_worker)
    ensure(res.status_code == 200, f"Issue resolve failed: {res.status_code}")
    success("Issue resolved successfully")

def test_supervisor_deletion(headers_admin, sup_id, worker_id, ward_id, suffix):
    """Test 7: Supervisor Deletion and Cascading"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"TEST 7: SUPERVISOR DELETION AND CASCADING")
    print(f"{'='*60}{Colors.END}")

    info("Admin Deleting Supervisor...")
    res = requests.delete(f"{BASE_URL}/api/admin/supervisors/{sup_id}", headers=headers_admin)
    ensure(res.status_code == 200, f"Delete supervisor failed: {res.text}")
    success("Supervisor deleted successfully")

    # Verify cleanup - workers should have no supervisor
    info("Verifying cleanup...")
    res = requests.get(f"{BASE_URL}/api/admin/workers", headers=headers_admin)
    workers = res.json()
    mapped_wrk = next((w for w in workers if w["id"] == worker_id), None)
    ensure(mapped_wrk.get("supervisorId") is None, "Worker should have no supervisor linked")
    success("Worker cleanup verified: supervisorId = None")

    # Verify ward has no supervisor
    res = requests.get(f"{BASE_URL}/api/admin/wards", headers=headers_admin)
    wards = res.json()
    target_ward = next((w for w in wards if w["wardName"] == f"WARD_{suffix}"), None)
    ensure(target_ward.get("supervisorId") is None, "Ward should have no supervisor linked")
    success("Ward cleanup verified: supervisorId = None")

def main():
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"NAGAR SETU - COMPLETE FLOW VERIFICATION")
    print(f"{'='*60}{Colors.END}\n")

    suffix = random.randint(1000, 9999)
    email_admin = f"admin{suffix}@nagar.com"
    email_sup = f"sup{suffix}@nagar.com"
    email_citizen = f"citizen{suffix}@nagar.com"
    dummy_png_path = create_dummy_png()
    run_location = f"Flow Area {suffix}"

    try:
        # SETUP: Registration
        print(f"\n{Colors.YELLOW}SETUP: User Registration{Colors.END}")
        print(f"{Colors.YELLOW}{'-'*60}{Colors.END}")
        admin_token, admin_id = register("Admin Boss", email_admin, "admin123", "ADMIN")
        ensure(admin_token and admin_id, "Admin registration failed")

        sup_token, sup_id = register("Mahim Head", email_sup, "sup123456", "SUPERVISOR", location=run_location)
        ensure(sup_token and sup_id, "Supervisor registration failed")

        cit_token, cit_id = register("Conscious Citizen", email_citizen, "cit123", "CITIZEN", location=run_location)
        ensure(cit_token and cit_id, "Citizen registration failed")

        headers_admin = {"Authorization": f"Bearer {admin_token}"}
        headers_sup = {"Authorization": f"Bearer {sup_token}"}
        headers_cit = {"Authorization": f"Bearer {cit_token}"}

        # Test 1: Ward Creation
        ward_id = test_ward_creation(headers_admin, suffix)

        # Test 2: Supervisor Creation
        test_supervisor_creation(headers_admin, sup_id, suffix)

        # Test 3: Ward Assignment
        test_ward_assignment(headers_admin, ward_id, sup_id, suffix)

        # Test 4: Worker Creation and Assignment
        worker_token, worker_id = test_worker_creation(headers_admin, sup_id, suffix)
        headers_worker = {"Authorization": f"Bearer {worker_token}"}

        # Test 5: Issue Creation
        issue_id = test_issue_creation(headers_cit, cit_id, suffix, run_location, dummy_png_path, headers_admin)

        # Test 6: Issue Resolution
        test_issue_resolution(headers_worker, issue_id, dummy_png_path)

        # Test 7: Supervisor Deletion
        test_supervisor_deletion(headers_admin, sup_id, worker_id, ward_id, suffix)

        # Final Summary
        print(f"\n{Colors.BLUE}{'='*60}")
        print(f"ALL TESTS PASSED SUCCESSFULLY! [OK]")
        print(f"{'='*60}{Colors.END}\n")

    except AssertionError as e:
        print(f"\n{Colors.RED}{'='*60}")
        print(f"TEST FAILED")
        print(f"{'='*60}{Colors.END}")
        sys.exit(1)
    except Exception as e:
        print(f"{Colors.RED}[X] Unexpected error: {str(e)}{Colors.END}")
        sys.exit(1)
    finally:
        try:
            os.unlink(dummy_png_path)
        except OSError:
            pass

if __name__ == "__main__":
    main()
