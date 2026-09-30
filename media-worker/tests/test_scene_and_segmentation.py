import os
import cv2
import numpy as np
from pipeline.scene_detector import SceneDetector
from pipeline.visual_sampler import VisualSampler
from pipeline.semantic_segmenter import SemanticSegmenter
from pipeline.speech_transcriber import TranscriptChunk

def test_scene_detection_fallback():
    test_video = "/tmp/test_scene.mp4"
    os.makedirs("/tmp", exist_ok=True)
    with open(test_video, "wb") as f:
        f.write(b"MOCK_DUMMY_VIDEO_DATA")

    scenes = SceneDetector.detect_scenes(test_video, job_id="job_test_scene")
    assert len(scenes) >= 1
    assert scenes[0].duration_sec > 0

    if os.path.exists(test_video):
        os.remove(test_video)

def test_perceptual_hashing():
    img_path = "/tmp/hash_test.jpg"
    dummy = np.zeros((360, 640, 3), dtype=np.uint8)
    cv2.imwrite(img_path, dummy)

    phash = VisualSampler.compute_perceptual_hash(img_path)
    assert len(phash) == 64
    assert phash == "0" * 64

    if os.path.exists(img_path):
        os.remove(img_path)

def test_semantic_segmentation():
    chunks = [
        TranscriptChunk("First 30 seconds of transcript text.", 0.0, 30.0, []),
        TranscriptChunk("Next 30 seconds of transcript text.", 30.0, 60.0, [])
    ]
    scenes = []
    visuals = []

    segments = SemanticSegmenter.create_segments(chunks, scenes, visuals, job_id="job_test_sem")
    assert len(segments) == 2
    assert segments[0].start_sec == 0.0
    assert segments[0].end_sec == 30.0
    assert segments[1].transcript_text == "Next 30 seconds of transcript text."
