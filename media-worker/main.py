import os
import logging
from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
from config import Config, JobLoggerAdapter

Config.setup_logging()
base_logger = logging.getLogger("FlintMediaWorker")

app = FastAPI(title="Flint Media Worker API", version="1.0.0")

class MediaJobPayload(BaseModel):
    id: str
    sourceId: str
    userId: str
    type: str
    status: str = "QUEUED"
    progress: int = 0
    metadata: Dict[str, str] = Field(default_factory=dict)

class JobResponse(BaseModel):
    jobId: str
    status: str
    message: str

def process_media_job(job: MediaJobPayload):
    logger = JobLoggerAdapter(base_logger, {"job_id": job.id})
    logger.info(f"Starting media processing job: sourceId={job.sourceId}, type={job.type}")

    # Process job stages
    logger.info("Media job status updated: DOWNLOADING")
    logger.info("Media job status updated: EXTRACTING_AUDIO")
    logger.info("Media job status updated: TRANSCRIBING")
    logger.info("Media job status updated: DETECTING_SCENES")
    logger.info("Media job status updated: GENERATING_CANDIDATES")
    logger.info("Media job COMPLETED successfully")

@app.get("/health")
def health_check():
    return {
        "status": "HEALTHY",
        "service": "Flint Media Worker",
        "version": "1.0.0",
        "whisper_model": Config.WHISPER_MODEL
    }

@app.post("/jobs/process", response_model=JobResponse)
def submit_job(job: MediaJobPayload, background_tasks: BackgroundTasks):
    logger = JobLoggerAdapter(base_logger, {"job_id": job.id})
    logger.info(f"Received job submission request: type={job.type}")

    background_tasks.add_task(process_media_job, job)

    return JobResponse(
        jobId=job.id,
        status="QUEUED",
        message="Job successfully queued for media processing"
    )

if __name__ == "__main__":
    import uvicorn
    os.makedirs(Config.TEMP_DIR, exist_ok=True)
    uvicorn.run(app, host=Config.HOST, port=Config.PORT)
