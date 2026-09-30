import logging
from typing import List, Dict, Any
from config import JobLoggerAdapter

base_logger = logging.getLogger("SemanticSegmenter")

class VideoSemanticSegment:
    def __init__(
        self,
        segment_id: str,
        start_sec: float,
        end_sec: float,
        transcript_text: str,
        scene_ids: List[str] = None,
        visual_samples: List[str] = None
    ):
        self.segment_id = segment_id
        self.start_sec = start_sec
        self.end_sec = end_sec
        self.duration_sec = max(0.0, end_sec - start_sec)
        self.transcript_text = transcript_text
        self.scene_ids = scene_ids or []
        self.visual_samples = visual_samples or []

    def to_dict(self) -> Dict[str, Any]:
        return {
            "segment_id": self.segment_id,
            "start_sec": self.start_sec,
            "end_sec": self.end_sec,
            "duration_sec": self.duration_sec,
            "transcript_text": self.transcript_text,
            "scene_ids": self.scene_ids,
            "visual_samples": self.visual_samples
        }

class SemanticSegmenter:
    @staticmethod
    def create_segments(
        transcript_chunks: List[Any],
        scenes: List[Any],
        visual_samples: List[Any],
        job_id: str = "N/A"
    ) -> List[VideoSemanticSegment]:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Fusing {len(transcript_chunks)} transcript chunks with {len(scenes)} scenes into semantic segments")

        semantic_segments = []

        for idx, chunk in enumerate(transcript_chunks):
            seg_id = f"seg_{idx + 1}"
            s_time = chunk.start_time_sec
            e_time = chunk.end_time_sec

            # Match overlapping scenes
            matched_scenes = [
                scene.scene_id for scene in scenes
                if (scene.start_sec <= e_time and scene.end_sec >= s_time)
            ]

            # Match overlapping visual samples
            matched_visuals = [
                sample.frame_path for sample in visual_samples
                if (sample.timestamp_sec >= s_time and sample.timestamp_sec <= e_time)
            ]

            semantic_segments.append(VideoSemanticSegment(
                segment_id=seg_id,
                start_sec=s_time,
                end_sec=e_time,
                transcript_text=chunk.text,
                scene_ids=matched_scenes,
                visual_samples=matched_visuals
            ))

        logger.info(f"Semantic segmentation completed: {len(semantic_segments)} segments formed")
        return semantic_segments
