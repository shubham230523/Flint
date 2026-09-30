import os
from render.caption_renderer import CaptionRenderer
from render.overlay_renderer import OverlayRenderer
from pipeline.speech_transcriber import TranscriptSegment

def test_srt_generation():
    segments = [
        TranscriptSegment("Welcome to Flint!", 10000, 15000), # 10s to 15s
        TranscriptSegment("Create viral Instagram Reels.", 16000, 25000) # 16s to 25s
    ]
    srt_file = "/tmp/test_reel.srt"
    cap_segs = CaptionRenderer.generate_srt_subtitles(segments, 10.0, 30.0, srt_file, logger=CaptionRenderer)
    assert len(cap_segs) == 2
    assert cap_segs[0].rel_start_sec == 0.0
    assert os.path.exists(srt_file)

    if os.path.exists(srt_file):
        os.remove(srt_file)

def test_overlay_renderer():
    src_path = "/tmp/test_ovr_src.mp4"
    out_path = "/tmp/test_ovr_out.mp4"
    os.makedirs("/tmp", exist_ok=True)
    with open(src_path, "wb") as f:
        f.write(b"MOCK_OVERLAY_VIDEO_DATA")

    OverlayRenderer.render_flint_overlays(
        src_path,
        hook_text="The Core Spark Strategy",
        cta_text="Save & Share This Reel!",
        output_overlay_path=out_path,
        job_id="test_ovr"
    )
    assert os.path.exists(out_path)
    assert os.path.getsize(out_path) > 0

    if os.path.exists(src_path):
        os.remove(src_path)
    if os.path.exists(out_path):
        os.remove(out_path)
