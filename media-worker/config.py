import os
import logging

class Config:
    HOST = os.getenv("WORKER_HOST", "0.0.0.0")
    PORT = int(os.getenv("WORKER_PORT", "8080"))
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
    TEMP_DIR = os.getenv("TEMP_DIR", "/tmp/flint_media")
    WHISPER_MODEL = os.getenv("WHISPER_MODEL", "base")
    MAX_VIDEO_DURATION_SEC = int(os.getenv("MAX_VIDEO_DURATION_SEC", "3600"))  # 60 mins
    MAX_FILE_SIZE_MB = int(os.getenv("MAX_FILE_SIZE_MB", "2000"))

    @classmethod
    def setup_logging(cls):
        level = getattr(logging, cls.LOG_LEVEL.upper(), logging.INFO)
        logging.basicConfig(
            level=level,
            format="%(asctime)s [%(levelname)s] [job_id=%(job_id)s] %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )

class JobLoggerAdapter(logging.LoggerAdapter):
    def process(self, msg, kwargs):
        job_id = self.extra.get("job_id", "N/A") if self.extra else "N/A"
        return f"{msg}", {**kwargs, "extra": {"job_id": job_id}}
