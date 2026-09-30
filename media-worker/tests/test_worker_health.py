from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "HEALTHY"
    assert data["service"] == "Flint Media Worker"

def test_submit_job():
    payload = {
        "id": "job_test_123",
        "sourceId": "src_test_456",
        "userId": "user_789",
        "type": "VIDEO_ANALYSIS",
        "status": "QUEUED",
        "progress": 0,
        "metadata": {}
    }
    response = client.post("/jobs/process", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["jobId"] == "job_test_123"
    assert data["status"] == "QUEUED"
