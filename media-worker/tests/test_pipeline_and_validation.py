import os
from render.reel_render_pipeline import ReelRenderPipeline
from pipeline.reel_validator import ReelValidator

def test_full_render_pipeline_mock():
    src_video = "/tmp/test_pipe_src.mp4"
    out_reel = "/tmp/test_pipe_out.mp4"
    os.makedirs("/tmp", exist_ok=True)
    with open(src_video, "wb") as f:
        f.write(b"MOCK_SOURCE_VIDEO_BYTES")

    progress_events = []
    def on_prog(pct, msg):
        progress_events.append((pct, msg))

    res = ReelRenderPipeline.execute_pipeline(
        source_video_path=src_video,
        start_sec=10.0,
        end_sec=40.0,
        transcript_segments=[],
        hook_text="The Spark Strategy",
        cta_text="Save this Reel!",
        output_reel_path=out_reel,
        job_id="test_pipe",
        on_progress=on_prog
    )

    assert res.is_valid is True
    assert os.path.exists(out_reel)
    assert len(progress_events) >= 5
    assert progress_events[-1][0] == 100

    if os.path.exists(src_video):
        os.remove(src_video)
    if os.path.exists(out_reel):
        os.remove(out_reel)
