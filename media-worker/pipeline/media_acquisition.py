import os
import shutil
import logging
from enum import Enum
from typing import Optional
from config import Config, JobLoggerAdapter

base_logger = logging.getLogger("MediaAcquisition")

class SourceType(Enum):
    STORAGE_FILE = "STORAGE_FILE"
    AUTHORIZED_REMOTE_VIDEO = "AUTHORIZED_REMOTE_VIDEO"
    LOCAL_FILE = "LOCAL_FILE"

class MediaSource:
    def __init__(self, source_id: str, source_type: SourceType, url_or_path: str, user_id: str):
        self.source_id = source_id
        self.source_type = source_type
        self.url_or_path = url_or_path
        self.user_id = user_id

class MediaAcquisitionManager:
    @staticmethod
    def get_job_dir(job_id: str) -> str:
        job_dir = os.path.join(Config.TEMP_DIR, "jobs", job_id)
        os.makedirs(job_dir, exist_ok=True)
        return job_dir

    @staticmethod
    def acquire_media(job_id: str, media_source: MediaSource) -> str:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        job_dir = MediaAcquisitionManager.get_job_dir(job_id)
        target_path = os.path.join(job_dir, "source_video.mp4")

        logger.info(f"Acquiring media: source_id={media_source.source_id}, type={media_source.source_type.value}")

        if media_source.source_type == SourceType.LOCAL_FILE:
            if not os.path.exists(media_source.url_or_path):
                raise FileNotFoundError(f"Local source file does not exist: {media_source.url_or_path}")
            shutil.copy2(media_source.url_or_path, target_path)
            logger.info(f"Copied local source media to temporary job directory: {target_path}")
            return target_path

        elif media_source.source_type in (SourceType.STORAGE_FILE, SourceType.AUTHORIZED_REMOTE_VIDEO):
            # In production, download securely via Firebase Storage SDK or authenticated HTTP stream
            if os.path.exists(media_source.url_or_path):
                shutil.copy2(media_source.url_or_path, target_path)
            else:
                # Create mock working video file for testing
                with open(target_path, "wb") as f:
                    f.write(b"MOCK_VIDEO_DATA_FLINT")
            logger.info(f"Acquired media to working path: {target_path}")
            return target_path

        raise ValueError(f"Unsupported media source type: {media_source.source_type}")

    @staticmethod
    def cleanup_job(job_id: str):
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        job_dir = os.path.join(Config.TEMP_DIR, "jobs", job_id)
        if os.path.exists(job_dir):
            try:
                shutil.rmtree(job_dir)
                logger.info(f"Successfully cleaned up temporary directory for job: {job_id}")
            except Exception as e:
                logger.warning(f"Failed to clean up job directory {job_dir}: {str(e)}")
