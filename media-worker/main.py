import os
import logging
from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
from config import Config, JobLoggerAdapter
from run_production_local_job import run_production_job

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
    outputVideoUrl: Optional[str] = None

def process_media_job(job: MediaJobPayload):
    logger = JobLoggerAdapter(base_logger, {"job_id": job.id})
    logger.info(f"Starting media processing job: sourceId={job.sourceId}, type={job.type}")

    youtube_url = job.metadata.get("youtubeUrl", "https://www.youtube.com/watch?v=45K3zHckCnQ")
    start_sec = float(job.metadata.get("startSec", "0.0"))
    end_sec = float(job.metadata.get("endSec", "35.0"))
    hook_text = job.metadata.get("hookText", "")
    cta_text = job.metadata.get("ctaText", "Save & Share this Reel!")

    try:
        res = run_production_job(
            job_id=job.id,
            youtube_url=youtube_url,
            start_sec=start_sec,
            end_sec=end_sec,
            hook_text=hook_text,
            cta_text=cta_text
        )
        logger.info(f"Job {job.id} COMPLETED! Output video: {res['output_reel_path']}")
    except Exception as e:
        logger.error(f"Job {job.id} FAILED: {str(e)}")

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

    reels_dir = os.path.join(Config.TEMP_DIR, "reels")
    output_path = os.path.join(reels_dir, f"{job.id}_final_reel.mp4")

    return JobResponse(
        jobId=job.id,
        status="QUEUED",
        message="Job successfully queued for local media processing",
        outputVideoUrl=output_path
    )

if __name__ == "__main__":
    import uvicorn
    os.makedirs(Config.TEMP_DIR, exist_ok=True)
    uvicorn.run(app, host=Config.HOST, port=Config.PORT)
