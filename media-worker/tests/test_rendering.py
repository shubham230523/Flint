import os
from render.clip_extractor import ClipExtractor
from render.subject_tracker import SubjectTracker

def test_clip_extractor_fallback():
    src_path = "/tmp/test_clip_src.mp4"
    out_clip = "/tmp/test_clip_out.mp4"
    os.makedirs("/tmp", exist_ok=True)
    with open(src_path, "wb") as f:
        f.write(b"MOCK_SRC_VIDEO_DATA")

    ClipExtractor.extract_clip(src_path, 10.0, 30.0, out_clip, job_id="test_clip")
    assert os.path.exists(out_clip)
    assert os.path.getsize(out_clip) > 0

    if os.path.exists(src_path):
        os.remove(src_path)
    if os.path.exists(out_clip):
        os.remove(out_clip)

def test_subject_tracker_reframe():
    clip_path = "/tmp/test_reframe_src.mp4"
    out_path = "/tmp/test_reframe_out.mp4"
    with open(clip_path, "wb") as f:
        f.write(b"MOCK_CLIP_DATA")

    SubjectTracker.reframe_to_vertical_9_16(clip_path, out_path, job_id="test_reframe")
    assert os.path.exists(out_path)
    assert os.path.getsize(out_path) > 0

    if os.path.exists(clip_path):
        os.remove(clip_path)
    if os.path.exists(out_path):
        os.remove(out_path)
