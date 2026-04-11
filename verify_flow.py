import requests
import time
import subprocess
import json
import os
import base64
import tempfile

BASE_URL = "http://localhost:7889"

_DUMMY_PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2Z4ZkAAAAASUVORK5CYII="
)


def create_dummy_png():
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".png")
    with open(temp_file.name, "wb") as handle:
        handle.write(_DUMMY_PNG)
    return temp_file.name


def ensure(condition, message):
    if not condition:
        raise AssertionError(message)


def response_json(response, label):
    try:
        return response.json()
    except ValueError as exc:
        raise AssertionError(f"{label} did not return valid JSON: {response.status_code} - {response.text}") from exc


def extract_items(payload):
    if isinstance(payload, list):
        return payload
    if isinstance(payload, dict):
        content = payload.get("content")
        if isinstance(content, list):
            return content
        return [payload]
    return []


def assert_contains_issue(payload, issue_id, label):
    items = extract_items(payload)
    ensure(any(str(item.get("id")) == str(issue_id) for item in items), f"{label} does not contain issue {issue_id}")


def assert_empty_collection(payload, label):
    items = extract_items(payload)
    ensure(len(items) == 0, f"{label} expected to be empty, found {len(items)} item(s)")

def get_otp(email):
    print(f"[*] Requesting OTP for {email}...")
    requests.get(f"{BASE_URL}/api/authenication/getCode?email={email}")
    time.sleep(2)
    print(f"[*] Extracting OTP from logs...")
    try:
        logs = subprocess.check_output(['docker', 'logs', 'nagarsetu', '--since', '5m', '--tail', '500']).decode('utf-8')
        # Find "Code is 123456 with mail email"
        for line in logs.split('\n'):
            if "Code is" in line and email in line:
                return line.split("Code is ")[1].split(" ")[0].strip()
    except Exception as e:
        print(f"[!] Error reading logs: {e}")
    return None

def register(fullName, email, password, role, location="Mumbai"):
    code = get_otp(email)
    if not code:
        print(f"[!] Could not get OTP for {email}")
        return None
    print(f"[*] Got OTP: {code}. Registering {role}...")
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
        print(f"[+] {role} registered successfully. Token: {data['token'][:10]}...")
        return data['token'], data['id']
    else:
        print(f"[!] Registration failed for {role}: {resp.status_code} - {resp.text}")
        return None, None

def main():
    import random
    suffix = random.randint(1000, 9999)
    email_admin = f"admin{suffix}@nagar.com"
    email_sup = f"sup{suffix}@nagar.com"
    email_worker = f"worker{suffix}@nagar.com"
    email_citizen = f"citizen{suffix}@nagar.com"
    dummy_png_path = create_dummy_png()
    run_location = f"Flow Area {suffix}"
    issue_location = run_location
    run_latitude = 19.0 + ((suffix % 500) / 1000.0)
    run_longitude = 72.0 + ((suffix % 500) / 1000.0)

    try:
        # 1. Register Admin
        admin_token, admin_id = register("Admin Boss", email_admin, "admin123", "ADMIN")
        ensure(admin_token and admin_id, "Admin registration failed")

        # 2. Register Supervisor
        sup_token, sup_id = register("Mahim Head", email_sup, "sup123456", "SUPERVISOR", location=run_location)
        ensure(sup_token and sup_id, "Supervisor registration failed")

        # 3. Register Citizen
        cit_token, cit_id = register("Concious Citizen", email_citizen, "citizen123", "CITIZEN", location=run_location)
        ensure(cit_token and cit_id, "Citizen registration failed")

        headers_admin = {"Authorization": f"Bearer {admin_token}"}

        # 4. Activate Supervisor
        print("[*] Activating Supervisor with Jurisdiction...")
        sup_payload = {
            "department": "ROAD",
            "jurisdictionName": "Mahim Area",
            "jurisdictionCenterLat": run_latitude,
            "jurisdictionCenterLon": run_longitude,
            "jurisdictionRadiusKm": 5.0
        }
        resp = requests.post(f"{BASE_URL}/api/admin/acceptSupervisor/{sup_id}", json=sup_payload, headers=headers_admin)
        print(f"[*] Accept Supervisor Status: {resp.status_code}")
        ensure(resp.status_code == 200, f"Supervisor activation failed: {resp.status_code} - {resp.text}")

        # 5. Create Issue before Worker exists
        print("[*] Reporting ROAD issue...")
        issue_dto = {
            "title": "Main Road Pothole",
            "issueType": "ROAD",
            "description": "Large pothole in the middle of Mahim highway.",
            "criticality": "HIGH",
            "location": issue_location,
            "latitude": run_latitude,
            "longitude": run_longitude,
            "submittedById": cit_id
        }

        headers_citizen = {"Authorization": f"Bearer {cit_token}"}
        headers_supervisor = {"Authorization": f"Bearer {sup_token}"}
        pre_worker_probe_id = cit_id

        with open(dummy_png_path, 'rb') as issue_image:
            files = {
                'image': ('dummy.png', issue_image, 'image/png'),
                'issueCreateDto': (None, json.dumps(issue_dto), 'application/json')
            }
            resp = requests.post(f"{BASE_URL}/api/issue/create", files=files, headers=headers_citizen)

        ensure(resp.status_code == 201, f"Issue creation failed: {resp.status_code} - {resp.text}")
        issue_id = resp.json()
        print(f"[+] Issue created successfully: {issue_id}")

        # 8. Verify Issue Details and Assignment State
        print("[*] Verifying issue details and assignment...")
        resp = requests.get(f"{BASE_URL}/api/issue/{issue_id}", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Issue fetch failed: {resp.status_code} - {resp.text}")
        issue_data = response_json(resp, "issue details")
        ensure(issue_data["title"] == issue_dto["title"], "Issue title mismatch")
        ensure(issue_data["issueType"] == issue_dto["issueType"], "Issue type mismatch")
        ensure(issue_data["location"] == issue_dto["location"], "Issue location mismatch")
        ensure(issue_data["stages"] == "PENDING", f"Unexpected issue stage before worker creation: {issue_data['stages']}")
        ensure(issue_data["submittedBy"] == "Concious Citizen", "Submitted-by name mismatch")
        ensure(abs(issue_data["latitude"] - issue_dto["latitude"]) < 1e-9, "Latitude mismatch")
        ensure(abs(issue_data["longitude"] - issue_dto["longitude"]) < 1e-9, "Longitude mismatch")
        ensure(issue_data.get("imageUrl"), "Issue image URL missing")

        resp = requests.get(f"{BASE_URL}/api/issue/{issue_id}/worker", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Issue worker lookup failed: {resp.status_code} - {resp.text}")
        worker_data = response_json(resp, "issue worker")
        ensure(worker_data.get("workerId") is None, "Worker should not exist before creation")
        ensure(worker_data.get("workerName") == "NO ONE", "Issue should not have a worker before worker creation")

        # 6. Verify map and summary views before the worker is created
        print("[*] Checking map and summary views before resolution...")
        resp = requests.get(f"{BASE_URL}/api/issue/map/supervisor?supervisorId={sup_id}", headers=headers_supervisor)
        ensure(resp.status_code == 200, f"Supervisor map failed: {resp.status_code} - {resp.text}")
        assert_contains_issue(response_json(resp, "supervisor map"), issue_id, "Supervisor map")

        resp = requests.get(f"{BASE_URL}/api/issue/map/admin", headers=headers_admin)
        ensure(resp.status_code == 200, f"Admin map failed: {resp.status_code} - {resp.text}")
        assert_contains_issue(response_json(resp, "admin map"), issue_id, "Admin map")

        resp = requests.get(f"{BASE_URL}/api/issue/recent?page=0&size=10", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Recent issues failed: {resp.status_code} - {resp.text}")
        assert_contains_issue(response_json(resp, "recent issues"), issue_id, "Recent issues page")

        resp = requests.get(f"{BASE_URL}/api/issue/stats/weekly/stages", headers=headers_admin)
        ensure(resp.status_code == 200, f"Weekly stats failed: {resp.status_code} - {resp.text}")
        stats_payload = response_json(resp, "weekly stats")
        ensure(any(item.get("stage") == "PENDING" for item in extract_items(stats_payload)), "Weekly stats missing PENDING")

        resp = requests.get(f"{BASE_URL}/api/issue/user/map?id={cit_id}", headers=headers_citizen)
        ensure(resp.status_code == 204, f"User map for a new issue should be empty, got {resp.status_code} - {resp.text}")

        # 7. Register Worker after the issue already exists
        worker_token, worker_id = register("Quick Worker", email_worker, "worker123", "WORKER", location=run_location)
        ensure(worker_token and worker_id, "Worker registration failed")

        print("[*] Accepting Worker...")
        resp = requests.post(f"{BASE_URL}/api/admin/acceptWorker/{worker_id}/{sup_id}", headers=headers_admin)
        print(f"[*] Accept Worker Status: {resp.status_code}")
        ensure(resp.status_code == 200, f"Worker activation failed: {resp.status_code} - {resp.text}")

        headers_worker = {"Authorization": f"Bearer {worker_token}"}

        resp = requests.get(f"{BASE_URL}/api/issue/{issue_id}", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Issue fetch after worker creation failed: {resp.status_code} - {resp.text}")
        issue_after_worker = response_json(resp, "issue after worker creation")
        ensure(issue_after_worker["stages"] == "TEAM_ASSIGNED", f"Unexpected issue stage after worker creation: {issue_after_worker['stages']}")

        resp = requests.get(f"{BASE_URL}/api/issue/{issue_id}/worker", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Issue worker lookup after worker creation failed: {resp.status_code} - {resp.text}")
        assigned_after_worker = response_json(resp, "issue worker after worker creation")
        ensure(assigned_after_worker.get("workerId") == worker_id, "Issue should be assigned to the newly created worker")
        ensure(assigned_after_worker.get("workerName") == "Quick Worker", "Assigned worker name mismatch after worker creation")

        resp = requests.get(f"{BASE_URL}/api/issue/map/worker?workerId={worker_id}", headers=headers_worker)
        ensure(resp.status_code == 200, f"Worker map should contain the issue after worker creation, got {resp.status_code} - {resp.text}")
        assert_contains_issue(response_json(resp, "worker map after worker creation"), issue_id, "Worker map after worker creation")

        # 8. Mark Resolved (Worker)
        print("[*] Resolving Issue (Worker)...")
        with open(dummy_png_path, 'rb') as done_image:
            files_done = {'file': ('done.png', done_image, 'image/png')}
            resp = requests.post(f"{BASE_URL}/api/issue/done?id={issue_id}", files=files_done, headers=headers_worker)
        print(f"[*] Resolve Status: {resp.status_code}")
        ensure(resp.status_code == 200, f"Issue resolution failed: {resp.status_code} - {resp.text}")

        # 11. Final verification and post-resolution checks
        resp = requests.get(f"{BASE_URL}/api/issue/{issue_id}", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Final issue fetch failed: {resp.status_code} - {resp.text}")
        final_data = response_json(resp, "final issue details")
        ensure(final_data["stages"] == "RESOLVED", f"Unexpected final stage: {final_data['stages']}")
        print(f"[COMPLETE] Final Issue Status: {final_data['stages']}")

        resp = requests.get(f"{BASE_URL}/api/issue/{issue_id}/worker", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Post-resolution worker lookup failed: {resp.status_code} - {resp.text}")
        worker_after_done = response_json(resp, "post-resolution worker")
        ensure(worker_after_done.get("workerId") == worker_id, "Resolved issue should remain linked to the created worker")
        ensure(worker_after_done.get("workerName") == "Quick Worker", "Resolved issue worker name mismatch")

        resp = requests.get(f"{BASE_URL}/api/issue/map/worker?workerId={worker_id}", headers=headers_worker)
        ensure(resp.status_code in (200, 204), f"Worker map after resolution returned unexpected status: {resp.status_code} - {resp.text}")
        if resp.status_code == 200:
            assert_empty_collection(response_json(resp, "worker map after resolution"), "Worker map after resolution")

        resp = requests.get(f"{BASE_URL}/api/issue/map/admin", headers=headers_admin)
        ensure(resp.status_code == 200, f"Admin map after resolution failed: {resp.status_code} - {resp.text}")
        assert_contains_issue(response_json(resp, "admin map after resolution"), issue_id, "Admin map after resolution")

        resp = requests.get(f"{BASE_URL}/api/issue/recent?page=0&size=10", headers=headers_citizen)
        ensure(resp.status_code == 200, f"Recent issues after resolution failed: {resp.status_code} - {resp.text}")
        assert_contains_issue(response_json(resp, "recent issues after resolution"), issue_id, "Recent issues after resolution")

    finally:
        try:
            os.unlink(dummy_png_path)
        except OSError:
            pass

if __name__ == "__main__":
    main()
