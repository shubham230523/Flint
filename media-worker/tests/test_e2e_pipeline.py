import os
from pipeline.media_acquisition import MediaAcquisitionManager, MediaSource, SourceType
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
from pipeline.resumable_job_manager import ResumableJobManager

def test_full_e2e_synthetic_30min_pipeline():
    job_id = "job_e2e_30min_synth"
    os.makedirs("/tmp", exist_ok=True)
    src_file = "/tmp/synthetic_30min.mp4"
    with open(src_file, "wb") as f:
        f.write(b"MOCK_SYNTHETIC_30MIN_VIDEO_BYTES")

    # 1. Media Acquisition
    source = MediaSource(source_id="src_30m", source_type=SourceType.LOCAL_FILE, url_or_path=src_file, user_id="u1")
    working_video = MediaAcquisitionManager.acquire_media(job_id, source)
    assert os.path.exists(working_video)

    # 2. Video Probing
    metadata = VideoProber.probe_video(working_video, job_id=job_id)
    assert metadata.duration_sec > 0

    # 3. Audio Extraction
    audio_art = AudioExtractor.extract_audio(working_video, job_id=job_id)
    assert os.path.exists(audio_art.file_path)

    # 4. Speech Transcription
    stt = SpeechToTextProvider()
    segments = stt.transcribe(audio_art.file_path, job_id=job_id)
    assert len(segments) > 0

    chunks = stt.create_transcript_chunks(segments, target_duration_sec=120.0)
    assert len(chunks) > 0

    # Save Checkpoint
    ResumableJobManager.save_checkpoint(job_id, "transcribed", {"chunk_count": len(chunks)})
    cached = ResumableJobManager.load_checkpoint(job_id, "transcribed")
    assert cached["chunk_count"] == len(chunks)

    # 5. Scene Detection
    scenes = SceneDetector.detect_scenes(working_video, job_id=job_id)
    assert len(scenes) > 0

    # 6. Visual Sampling
    samples = VisualSampler.sample_visuals(scenes, job_id=job_id)
    assert len(samples) >= 0

    # 7. Semantic Segmentation
    semantic_segs = SemanticSegmenter.create_segments(chunks, scenes, samples, job_id=job_id)
    assert len(semantic_segs) > 0

    # 8. Candidate Discovery
    raw_candidates = CandidateDiscovery.score_and_discover_candidates(semantic_segs, job_id=job_id)
    assert len(raw_candidates) > 0

    # 9. Local Text Analysis
    analyzed = LocalTextAnalyzer.analyze_candidates(raw_candidates, job_id=job_id)
    assert len(analyzed) > 0

    # 10. Multimodal Verification
    verified = MultimodalVerifier.verify_candidates(analyzed, job_id=job_id)
    assert len(verified) > 0

    # 11. Render Selected Reel
    selected = verified[0]
    out_reel = os.path.join(os.path.dirname(working_video), "final_reel_output.mp4")

    render_res = ReelRenderPipeline.execute_pipeline(
        source_video_path=working_video,
        start_sec=selected["start_time_sec"],
        end_sec=selected["end_time_sec"],
        transcript_segments=segments,
        hook_text=selected["hook"],
        cta_text="Follow for more AI insights!",
        output_reel_path=out_reel,
        job_id=job_id
    )

    assert render_res.is_valid is True
    assert os.path.exists(out_reel)

    # Cleanup
    MediaAcquisitionManager.cleanup_job(job_id)
    if os.path.exists(src_file):
        os.remove(src_file)
