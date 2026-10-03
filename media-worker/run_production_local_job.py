import os
import sys
import logging
from typing import Dict, Any, Optional
from config import Config, JobLoggerAdapter

from pipeline.youtube_downloader import YouTubeDownloader
from pipeline.video_prober import VideoProber
from pipeline.audio_extractor import AudioExtractor
from pipeline.speech_transcriber import SpeechToTextProvider
from pipeline.scene_detector import SceneDetector
from pipeline.visual_sampler import VisualSampler
from pipeline.semantic_segmenter import SemanticSegmenter
from ai.candidate_discovery import CandidateDiscovery
from ai.local_text_analyzer import LocalTextAnalyzer
from ai.multimodal_verifier import MultimodalVerifier
from render.reel_render_pipeline import ReelRenderPipeline
from pipeline.reel_validator import ReelValidator
from pipeline.resumable_job_manager import ResumableJobManager

base_logger = logging.getLogger("ProductionLocalJob")

def run_production_job(
    job_id: str = "job_prod_local_1",
    youtube_url: str = "https://www.youtube.com/watch?v=45K3zHckCnQ",
    start_sec: float = 0.0,
    end_sec: float = 35.0,
    hook_text: str = "",
    cta_text: str = "Save & Share this Reel!",
    output_path: Optional[str] = None
) -> Dict[str, Any]:
    logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
    logger.info(f"==========================================================")
    logger.info(f"Starting Production Local Media Job: URL={youtube_url}")

    reels_dir = os.path.join(Config.TEMP_DIR, "reels")
    os.makedirs(reels_dir, exist_ok=True)

    if not output_path:
        output_path = os.path.join(reels_dir, f"{job_id}_final_reel.mp4")

    # 1. Real YouTube Video Download (yt-dlp)
    logger.info("STAGE 1: Downloading real YouTube video via yt-dlp...")
    dl_result = YouTubeDownloader.download_video(youtube_url, job_id=job_id)
    video_path = dl_result.video_path

    # 2. Video Probing (ffprobe)
    logger.info("STAGE 2: Probing video streams and format...")
    metadata = VideoProber.probe_video(video_path, job_id=job_id)

    # 3. Audio Extraction (ffmpeg 16kHz PCM WAV)
    logger.info("STAGE 3: Extracting 16kHz mono audio stream...")
    audio_art = AudioExtractor.extract_audio(video_path, job_id=job_id)

    # 4. Open Speech Transcription (Faster-Whisper)
    logger.info("STAGE 4: Transcribing audio stream with open Whisper model...")
    stt = SpeechToTextProvider()
    transcript_segments = stt.transcribe(audio_art.file_path, job_id=job_id)
    transcript_chunks = stt.create_transcript_chunks(transcript_segments, target_duration_sec=60.0)

    # Save Checkpoint
    ResumableJobManager.save_checkpoint(job_id, "transcribed", {"chunks": len(transcript_chunks)})

    # 5. Scene Detection & Keyframe Extraction (PySceneDetect)
    logger.info("STAGE 5: Running PySceneDetect content detection...")
    scenes = SceneDetector.detect_scenes(video_path, job_id=job_id)

    # 6. Visual Sampling (OpenCV perceptual hashing)
    logger.info("STAGE 6: Sampling visual keyframes...")
    samples = VisualSampler.sample_visuals(scenes, job_id=job_id)

    # 7. Semantic Segmentation
    logger.info("STAGE 7: Fusing transcript, scenes, and visuals into semantic segments...")
    semantic_segs = SemanticSegmenter.create_segments(transcript_chunks, scenes, samples, job_id=job_id)

    # 8. Candidate Discovery & Reasoning
    logger.info("STAGE 8: Discovering and scoring candidates...")
    candidates = CandidateDiscovery.score_and_discover_candidates(semantic_segs, job_id=job_id)
    analyzed_candidates = LocalTextAnalyzer.analyze_candidates(candidates, job_id=job_id)
    verified_candidates = MultimodalVerifier.verify_candidates(analyzed_candidates, job_id=job_id)

    # 9. Execute Deterministic Render Pipeline
    logger.info("STAGE 9: Executing deterministic FFmpeg/OpenCV Reel render pipeline...")
    def on_prog(pct, msg):
        logger.info(f"Render Progress [{pct}%]: {msg}")

    val_res = ReelRenderPipeline.execute_pipeline(
        source_video_path=video_path,
        start_sec=start_sec,
        end_sec=end_sec,
        transcript_segments=transcript_segments,
        hook_text=hook_text,
        cta_text=cta_text,
        output_reel_path=output_path,
        job_id=job_id,
        on_progress=on_prog
    )

    logger.info(f"==========================================================")
    logger.info(f"Production Local Job Completed! Output Reel MP4: {output_path}")

    return {
        "job_id": job_id,
        "output_reel_path": output_path,
        "duration_sec": end_sec - start_sec,
        "is_valid": val_res.is_valid,
        "candidates_found": len(verified_candidates)
    }

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Run Production Local Reel Job")
    parser.add_argument("--job-id", type=str, default="job_prod_local_1", help="Job ID")
    parser.add_argument("--url", type=str, default="https://www.youtube.com/watch?v=45K3zHckCnQ", help="YouTube Video URL")
    parser.add_argument("--start", type=float, default=0.0, help="Clip start timestamp (seconds)")
    parser.add_argument("--end", type=float, default=35.0, help="Clip end timestamp (seconds)")
    parser.add_argument("--hook", type=str, default="", help="Hook text overlay")
    parser.add_argument("--cta", type=str, default="Save & Share this Reel!", help="CTA text overlay")
    parser.add_argument("--output", type=str, default=None, help="Output MP4 file path")
    args = parser.parse_args()

    run_production_job(
        job_id=args.job_id,
        youtube_url=args.url,
        start_sec=args.start,
        end_sec=args.end,
        hook_text=args.hook,
        cta_text=args.cta,
        output_path=args.output
    )
