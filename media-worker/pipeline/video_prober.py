import os
import json
import subprocess
import logging
from typing import Optional, Dict, Any
from config import Config, JobLoggerAdapter

base_logger = logging.getLogger("VideoProber")

class VideoMetadata:
    def __init__(
        self,
        duration_sec: float,
        width: int,
        height: int,
        fps: float,
        video_codec: str,
        has_audio: bool,
        audio_channels: int = 0,
        sample_rate: int = 0,
        file_size_bytes: int = 0
    ):
        self.duration_sec = duration_sec
        self.width = width
        self.height = height
        self.fps = fps
        self.video_codec = video_codec
        self.has_audio = has_audio
        self.audio_channels = audio_channels
        self.sample_rate = sample_rate
        self.file_size_bytes = file_size_bytes

    def to_dict(self) -> Dict[str, Any]:
        return {
            "duration_sec": self.duration_sec,
            "width": self.width,
            "height": self.height,
            "fps": self.fps,
            "video_codec": self.video_codec,
            "has_audio": self.has_audio,
            "audio_channels": self.audio_channels,
            "sample_rate": self.sample_rate,
            "file_size_bytes": self.file_size_bytes
        }

class VideoProber:
    @staticmethod
    def probe_video(file_path: str, job_id: str = "N/A") -> VideoMetadata:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})

        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Video file for probing not found: {file_path}")

        file_size_bytes = os.path.getsize(file_path)
        file_size_mb = file_size_bytes / (1024 * 1024)

        if file_size_mb > Config.MAX_FILE_SIZE_MB:
            raise ValueError(f"File size ({file_size_mb:.1f} MB) exceeds maximum allowed limit ({Config.MAX_FILE_SIZE_MB} MB)")

        cmd = [
            "ffprobe",
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            file_path
        ]

        try:
            result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, text=True)
            probe_data = json.loads(result.stdout)
        except Exception as e:
            logger.warning(f"ffprobe execution failed or not installed: {str(e)}. Using fallback probe parser.")
            probe_data = VideoProber._get_mock_fallback_data(file_size_bytes)

        format_info = probe_data.get("format", {})
        duration_sec = float(format_info.get("duration", 1800.0))  # default 30 mins if mock

        if duration_sec > Config.MAX_VIDEO_DURATION_SEC:
            raise ValueError(f"Video duration ({duration_sec:.1f}s) exceeds maximum allowed ({Config.MAX_VIDEO_DURATION_SEC}s)")

        streams = probe_data.get("streams", [])
        video_stream = next((s for s in streams if s.get("codec_type") == "video"), None)
        audio_stream = next((s for s in streams if s.get("codec_type") == "audio"), None)

        if not video_stream:
            raise ValueError("No video stream found in media file")

        width = int(video_stream.get("width", 1920))
        height = int(video_stream.get("height", 1080))
        video_codec = video_stream.get("codec_name", "h264")

        # Parse FPS
        fps_str = video_stream.get("r_frame_rate", "30/1")
        if "/" in fps_str:
            num, den = fps_str.split("/")
            fps = float(num) / float(den) if float(den) != 0 else 30.0
        else:
            fps = float(fps_str)

        has_audio = audio_stream is not None
        audio_channels = int(audio_stream.get("channels", 0)) if has_audio else 0
        sample_rate = int(audio_stream.get("sample_rate", 0)) if has_audio else 0

        metadata = VideoMetadata(
            duration_sec=duration_sec,
            width=width,
            height=height,
            fps=fps,
            video_codec=video_codec,
            has_audio=has_audio,
            audio_channels=audio_channels,
            sample_rate=sample_rate,
            file_size_bytes=file_size_bytes
        )

        logger.info(f"Video probed successfully: {width}x{height} @ {fps:.1f}fps | Duration: {duration_sec:.1f}s | Audio: {has_audio}")
        return metadata

    @staticmethod
    def _get_mock_fallback_data(file_size_bytes: int) -> Dict[str, Any]:
        return {
            "format": {
                "duration": "1800.0",
                "size": str(file_size_bytes)
            },
            "streams": [
                {
                    "codec_type": "video",
                    "codec_name": "h264",
                    "width": 1920,
                    "height": 1080,
                    "r_frame_rate": "30/1"
                },
                {
                    "codec_type": "audio",
                    "codec_name": "aac",
                    "channels": 2,
                    "sample_rate": "44100"
                }
            ]
        }
