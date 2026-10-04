import os
import logging
from typing import List, Dict, Any, Callable, Optional
from render.clip_extractor import ClipExtractor
from render.subject_tracker import SubjectTracker
from render.caption_renderer import CaptionRenderer
from render.overlay_renderer import OverlayRenderer
from pipeline.reel_validator import ReelValidator, ReelValidationResult
from config import JobLoggerAdapter

base_logger = logging.getLogger("ReelRenderPipeline")

class ReelRenderPipeline:
    @staticmethod
    def execute_pipeline(
        source_video_path: str,
        start_sec: float,
        end_sec: float,
        transcript_segments: List[Any],
        hook_text: str,
        cta_text: str,
        output_reel_path: str,
        job_id: str = "N/A",
        on_progress: Optional[Callable[[int, str], None]] = None
    ) -> ReelValidationResult:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Starting complete Reel render pipeline for job: {job_id}")

        job_dir = os.path.dirname(output_reel_path) or os.path.dirname(source_video_path)

        # Stage 1: Trim Segment (25%)
        if on_progress: on_progress(25, "Extracting segment clip...")
        raw_clip = os.path.join(job_dir, f"{job_id}_stage1_clip.mp4")
        ClipExtractor.extract_clip(source_video_path, start_sec, end_sec, raw_clip, job_id=job_id)

        # Stage 2: 9:16 Reframe & Speaker Tracking (45%)
        if on_progress: on_progress(45, "Tracking speaker & reframing to 9:16...")
        reframed_clip = os.path.join(job_dir, f"{job_id}_stage2_916.mp4")
        SubjectTracker.reframe_to_vertical_9_16(raw_clip, reframed_clip, job_id=job_id)

        # Stage 3: Burned-in Subtitle Captions (65%)
        if on_progress: on_progress(65, "Rendering burned-in subtitles...")
        captioned_clip = os.path.join(job_dir, f"{job_id}_stage3_captions.mp4")
        CaptionRenderer.render_burned_captions(
            reframed_clip, transcript_segments, start_sec, end_sec, captioned_clip, job_id=job_id
        )

        # Stage 4: Flint Hook & CTA Visual Overlays (85%)
        if on_progress: on_progress(85, "Applying Flint visual overlays...")
        OverlayRenderer.render_flint_overlays(
            captioned_clip, hook_text, cta_text, output_reel_path, job_id=job_id
        )

        # Stage 5: Output Validation (95%)
        if on_progress: on_progress(95, "Validating final Reel MP4...")
        validation_res = ReelValidator.validate_reel_output(output_reel_path, target_duration_sec=(end_sec - start_sec), job_id=job_id)

        if on_progress and validation_res.is_valid:
            on_progress(100, "Reel render completed successfully!")

        return validation_res
