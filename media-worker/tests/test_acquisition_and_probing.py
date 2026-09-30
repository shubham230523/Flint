import os
import pytest
from pipeline.media_acquisition import MediaAcquisitionManager, MediaSource, SourceType
from pipeline.video_prober import VideoProber, VideoMetadata

def test_media_acquisition_local():
    test_src_path = "/tmp/test_source.mp4"
    os.makedirs("/tmp", exist_ok=True)
    with open(test_src_path, "wb") as f:
        f.write(b"MOCK_MEDIA_BYTES")

    src = MediaSource(
        source_id="src_123",
        source_type=SourceType.LOCAL_FILE,
        url_or_path=test_src_path,
        user_id="user_test"
    )

    acquired_path = MediaAcquisitionManager.acquire_media("job_test_acq", src)
    assert os.path.exists(acquired_path)
    assert os.path.getsize(acquired_path) > 0

    MediaAcquisitionManager.cleanup_job("job_test_acq")
    assert not os.path.exists(acquired_path)

def test_video_probing_mock():
    test_src_path = "/tmp/test_probe.mp4"
    with open(test_src_path, "wb") as f:
        f.write(b"MOCK_VIDEO_DATA_FOR_PROBING")

    metadata = VideoProber.probe_video(test_src_path, job_id="job_test_probe")
    assert metadata.duration_sec == 1800.0
    assert metadata.width == 1920
    assert metadata.height == 1080
    assert metadata.has_audio is True
    assert metadata.fps == 30.0

    if os.path.exists(test_src_path):
        os.remove(test_src_path)
