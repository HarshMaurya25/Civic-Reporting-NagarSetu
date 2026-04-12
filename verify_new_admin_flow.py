import requests
import time
import subprocess
import json
import os
import base64
import tempfile
import random

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

def get_otp(email):
    print(f"[*] Requesting OTP for {email}...")
    requests.get(f"{BASE_URL}/api/authenication/getCode?email={email}")
    time.sleep(2)
    print(f"[*] Extracting OTP from logs...")
    try:
        logs = subprocess.check_output(['docker', 'logs', 'nagarsetu', '--since', '5m', '--tail', '500']).decode('utf-8')
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
        return data['token'], data['id']
    else:
        print(f"[!] Registration failed for {role}: {resp.status_code} - {resp.text}")
        return None, None

def main():
    suffix = random.randint(1000, 9999)
    email_admin = f"admin{suffix}@nagar.com"
    email_sup = f"sup{suffix}@nagar.com"
    email_worker = f"worker{suffix}@nagar.com"
    email_citizen = f"citizen{suffix}@nagar.com"
    dummy_png_path = create_dummy_png()
    run_location = f"Flow Area {suffix}"
    
    try:
        # 1. Registration
        admin_token, admin_id = register("Admin Boss", email_admin, "admin123", "ADMIN")
        ensure(admin_token and admin_id, "Admin registration failed")
        sup_token, sup_id = register("Mahim Head", email_sup, "sup123456", "SUPERVISOR", location=run_location)
        ensure(sup_token and sup_id, "Supervisor registration failed")
        cit_token, cit_id = register("Concious Citizen", email_citizen, "cit123", "CITIZEN", location=run_location)
        ensure(cit_token and cit_id, "Citizen registration failed")

        headers_admin = {"Authorization": f"Bearer {admin_token}"}
        headers_sup = {"Authorization": f"Bearer {sup_token}"}
        headers_cit = {"Authorization": f"Bearer {cit_token}"}

        # 2. Upload Dummy Ward (Admin)
        print("[*] Uploading Ward GeoJSON...")
        geojson = {
            "type": "FeatureCollection",
            "features": [
                {
                    "type": "Feature",
                    "properties": {"name": f"WARD_{suffix}"},
                    "geometry": {
                        "type": "Polygon",
                        "coordinates": [[[72.0, 19.0], [73.0, 19.0], [73.0, 20.0], [72.0, 20.0], [72.0, 19.0]]]
                    }
                }
            ]
        }
        res = requests.post(f"{BASE_URL}/api/wards/upload", json=geojson, headers=headers_admin)
        ensure(res.status_code == 200, f"Ward upload failed: {res.text}")

        # 3. Accept Supervisor
        print("[*] Admin Accepting Supervisor...")
        sup_payload = {
            "department": "ROAD",
            "jurisdictionName": f"WARD_{suffix}",
            "jurisdictionCenterLat": 19.5,
            "jurisdictionCenterLon": 72.5,
            "jurisdictionRadiusKm": 10.0
        }
        res = requests.post(f"{BASE_URL}/api/admin/acceptSupervisor/{sup_id}", json=sup_payload, headers=headers_admin)
        ensure(res.status_code == 200, f"Accept supervisor failed: {res.text}")

        # 4. Fetch Ward
        res = requests.get(f"{BASE_URL}/api/admin/wards", headers=headers_admin)
        ensure(res.status_code == 200, "Get wards failed")
        wards = res.json()
        target_ward = next((w for w in wards if w["wardName"] == f"WARD_{suffix}"), None)
        ensure(target_ward, "Uploaded ward not found")
        ward_id = target_ward["wardId"]

        # 5. Allocate Ward to Supervisor
        print("[*] Allocating Ward to Supervisor...")
        res = requests.put(f"{BASE_URL}/api/admin/wards/{ward_id}/supervisor/{sup_id}", headers=headers_admin)
        ensure(res.status_code == 200, f"Allocate ward failed: {res.text}")

        # Verify allocation
        res = requests.get(f"{BASE_URL}/api/admin/supervisors", headers=headers_admin)
        sups = res.json()
        mapped_sup = next((s for s in sups if s["id"] == sup_id), None)
        ensure(mapped_sup, "Supervisor not found in list")
        ensure(mapped_sup.get("wardName") == f"WARD_{suffix}", "Supervisor wardName mismatch")

        # 6. Worker Registration and Acceptance
        worker_token, worker_id = register("Fast Worker", email_worker, "wrk123", "WORKER")
        ensure(worker_token and worker_id, "Worker registration failed")
        headers_worker = {"Authorization": f"Bearer {worker_token}"}
        
        print("[*] Admin Accepting Worker...")
        res = requests.post(f"{BASE_URL}/api/admin/acceptWorker/{worker_id}/{sup_id}", headers=headers_admin)
        ensure(res.status_code == 200, f"Accept worker failed: {res.text}")

        # Verify Worker mapping
        res = requests.get(f"{BASE_URL}/api/admin/workers", headers=headers_admin)
        workers = res.json()
        mapped_wrk = next((w for w in workers if w["id"] == worker_id), None)
        ensure(mapped_wrk, "Worker not found in list")
        ensure(mapped_wrk.get("supervisorId") == sup_id, "Worker supervisorId mismatch")
        ensure(mapped_wrk.get("wardName") == f"WARD_{suffix}", "Worker wardName mismatch")

        # 7. Citizen creates issue inside the polygon
        print("[*] Citizen reporting Issue...")
        issue_dto = {
            "title": "Ward Crack",
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
        print(f"[+] Issue created: {issue_id}")

        # Ensure assigned to team
        res = requests.get(f"{BASE_URL}/api/issue/{issue_id}", headers=headers_cit)
        issue_data = res.json()
        ensure(issue_data["stages"] == "TEAM_ASSIGNED", f"Issue not auto-assigned: {issue_data['stages']}")

        # 8. Resolve Issue
        print("[*] Worker Resolving Issue...")
        with open(dummy_png_path, 'rb') as img:
            res = requests.post(f"{BASE_URL}/api/issue/done?id={issue_id}", files={'file': ('done.png', img, 'image/png')}, headers=headers_worker)
        ensure(res.status_code == 200, f"Issue resolve failed: {res.status_code}")

        # 9. Delete Supervisor
        print("[*] Admin Deleting Supervisor...")
        res = requests.delete(f"{BASE_URL}/api/admin/supervisors/{sup_id}", headers=headers_admin)
        ensure(res.status_code == 200, f"Delete supervisor failed: {res.text}")

        # Verify cascades
        print("[*] Verifying Cleanup...")
        # Get workers, should have no supervisior
        res = requests.get(f"{BASE_URL}/api/admin/workers", headers=headers_admin)
        workers = res.json()
        mapped_wrk = next((w for w in workers if w["id"] == worker_id), None)
        ensure(mapped_wrk.get("supervisorId") is None, "Worker should have no supervisor linked")

        # Get wards, should have no supervisor
        res = requests.get(f"{BASE_URL}/api/admin/wards", headers=headers_admin)
        wards = res.json()
        target_ward = next((w for w in wards if w["wardName"] == f"WARD_{suffix}"), None)
        ensure(target_ward.get("supervisorId") is None, "Ward should have no supervisor linked")

        print("[+] ALL TESTS PASSED SUCCESSFULLY! ✅")

    finally:
        try:
            os.unlink(dummy_png_path)
        except OSError:
            pass

if __name__ == "__main__":
    main()
