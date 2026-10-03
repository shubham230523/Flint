import os
import cv2
import subprocess
import logging
import numpy as np
from typing import Tuple, List
from config import JobLoggerAdapter

base_logger = logging.getLogger("SubjectTracker")

class SubjectTracker:
    @staticmethod
    def calculate_smoothed_crop_offset(video_path: str, target_width: int, target_height: int, logger) -> float:
        """Track face center X over video and calculate normalized smoothed crop offset [0.0..1.0]."""
        try:
            face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
            cap = cv2.VideoCapture(video_path)

            centers_x = []
            frame_count = 0

            while cap.isOpened() and frame_count < 300:  # Sample up to first 300 frames
                ret, frame = cap.read()
                if not ret or frame is None:
                    break

                frame_count += 1
                if frame_count % 5 != 0:  # Sample every 5th frame
                    continue

                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60))

                if len(faces) > 0:
                    (x, y, w, h) = faces[0]
                    center_x = (x + w / 2.0) / frame.shape[1]
                    centers_x.append(center_x)

            cap.release()

            if centers_x:
                avg_x = float(np.mean(centers_x))
                logger.info(f"Speaker face tracked across {len(centers_x)} frames | Smoothed center X: {avg_x:.3f}")
                return avg_x

        except Exception as e:
            logger.warning(f"Face tracking exception: {str(e)}. Defaulting to center crop (0.50).")

        return 0.5  # Default center crop

    @staticmethod
    def reframe_to_vertical_9_16(
        input_clip_path: str,
        output_reframe_path: str,
        job_id: str = "N/A"
    ) -> str:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Reframing video clip to 9:16 vertical format (1080x1920): {input_clip_path}")

        offset_x = SubjectTracker.calculate_smoothed_crop_offset(input_clip_path, 1080, 1920, logger)

        # FFmpeg crop filter for 9:16 vertical output (out_w = trunc(ih*9/32)*2 to ensure even dimensions)
        crop_filter = f"crop=trunc(ih*9/32)*2:ih:(iw-trunc(ih*9/32)*2)*{offset_x:.3f}:0,scale=1080:1920"

        cmd = [
            "ffmpeg", "-y",
            "-i", input_clip_path,
            "-vf", crop_filter,
            "-c:v", "libx264",
            "-preset", "fast",
            "-c:a", "aac",
            "-b:a", "128k",
            output_reframe_path
        ]

        try:
            subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
            logger.info(f"Reframing 9:16 completed successfully: {output_reframe_path}")
            return output_reframe_path
        except Exception as e:
            logger.warning(f"FFmpeg 9:16 reframing with audio failed ({str(e)}). Retrying without audio re-encode...")
            cmd_no_a = [
                "ffmpeg", "-y",
                "-i", input_clip_path,
                "-vf", crop_filter,
                "-c:v", "libx264",
                "-an",
                output_reframe_path
            ]
            try:
                subprocess.run(cmd_no_a, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
                logger.info(f"Reframing 9:16 (video only) completed successfully: {output_reframe_path}")
                return output_reframe_path
            except Exception as e2:
                logger.error(f"FFmpeg 9:16 reframing failed completely: {str(e2)}")
                import shutil
                if os.path.exists(input_clip_path) and os.path.getsize(input_clip_path) > 100:
                    shutil.copy2(input_clip_path, output_reframe_path)
                return output_reframe_path
