import os
import cv2
import logging
import numpy as np
from typing import List, Dict, Any
from config import JobLoggerAdapter

base_logger = logging.getLogger("VisualSampler")

class VisualSample:
    def __init__(self, timestamp_sec: float, frame_path: str, scene_id: str, perceptual_hash: str = ""):
        self.timestamp_sec = timestamp_sec
        self.frame_path = frame_path
        self.scene_id = scene_id
        self.perceptual_hash = perceptual_hash

class VisualSampler:
    @staticmethod
    def sample_visuals(scenes: List[Any], job_id: str = "N/A") -> List[VisualSample]:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Filtering and sampling visual frames from {len(scenes)} scenes")

        samples = []
        hashes_seen = set()

        for scene in scenes:
            for frame_path in scene.frame_paths:
                if not os.path.exists(frame_path):
                    continue

                phash = VisualSampler.compute_perceptual_hash(frame_path)

                # Filter out near-identical duplicate frames
                if phash not in hashes_seen:
                    hashes_seen.add(phash)
                    samples.append(VisualSample(
                        timestamp_sec=scene.start_sec,
                        frame_path=frame_path,
                        scene_id=scene.scene_id,
                        perceptual_hash=phash
                    ))

        logger.info(f"Visual sampling completed: {len(samples)} distinct visual samples retained")
        return samples

    @staticmethod
    def compute_perceptual_hash(image_path: str) -> str:
        try:
            img = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
            if img is None:
                return "0" * 16
            resized = cv2.resize(img, (8, 8), interpolation=cv2.INTER_AREA)
            avg = resized.mean()
            diff = resized > avg
            phash = "".join("1" if b else "0" for b in diff.flatten())
            return phash
        except Exception:
            return "0" * 16
