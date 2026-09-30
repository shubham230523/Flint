import os
import json
import logging
from typing import Dict, Any, Optional
from config import Config, JobLoggerAdapter

base_logger = logging.getLogger("ResumableJobManager")

class ResumableJobManager:
    @staticmethod
    def get_checkpoint_path(job_id: str, stage_name: str) -> str:
        job_dir = os.path.join(Config.TEMP_DIR, "jobs", job_id)
        os.makedirs(job_dir, exist_ok=True)
        return os.path.join(job_dir, f"checkpoint_{stage_name.lower()}.json")

    @staticmethod
    def save_checkpoint(job_id: str, stage_name: str, data: Dict[str, Any]):
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        path = ResumableJobManager.get_checkpoint_path(job_id, stage_name)
        try:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2)
            logger.info(f"Saved job stage checkpoint: {stage_name} -> {path}")
        except Exception as e:
            logger.warning(f"Failed to save checkpoint {stage_name}: {str(e)}")

    @staticmethod
    def load_checkpoint(job_id: str, stage_name: str) -> Optional[Dict[str, Any]]:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        path = ResumableJobManager.get_checkpoint_path(job_id, stage_name)
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                logger.info(f"Resuming job from cached stage checkpoint: {stage_name}")
                return data
            except Exception as e:
                logger.warning(f"Failed to read checkpoint {stage_name}: {str(e)}")
        return None
