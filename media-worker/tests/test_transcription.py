from pipeline.speech_transcriber import SpeechToTextProvider, TranscriptSegment

def test_transcript_normalization():
    provider = SpeechToTextProvider()
    raw = [
        TranscriptSegment("   Welcome   to   Flint!  ", 0, 3000),
        TranscriptSegment("Thank you Thank you Thank you", 3500, 6000)
    ]
    norm = provider.normalize_transcript(raw)
    assert len(norm) == 2
    assert norm[0].text == "Welcome to Flint!"

def test_transcript_chunking():
    provider = SpeechToTextProvider()
    segments = [
        TranscriptSegment(f"Sentence number {i} in the transcript.", i * 10000, (i + 1) * 10000)
        for i in range(20)  # 0 to 200 seconds
    ]
    chunks = provider.create_transcript_chunks(segments, target_duration_sec=60.0, overlap_sec=10.0)
    assert len(chunks) >= 3
    assert chunks[0].start_time_sec == 0.0
    assert chunks[0].end_time_sec >= 60.0
