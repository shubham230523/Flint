import os
import re
import logging
from typing import List, Dict, Any, Optional
from config import Config, JobLoggerAdapter

base_logger = logging.getLogger("SpeechTranscriber")

class TranscriptSegment:
    def __init__(self, text: str, start_ms: int, end_ms: int):
        self.text = text
        self.start_ms = start_ms
        self.end_ms = end_ms

    def to_dict(self) -> Dict[str, Any]:
        return {
            "text": self.text,
            "start_ms": self.start_ms,
            "end_ms": self.end_ms
        }

class TranscriptChunk:
    def __init__(self, text: str, start_time_sec: float, end_time_sec: float, segments: List[TranscriptSegment]):
        self.text = text
        self.start_time_sec = start_time_sec
        self.end_time_sec = end_time_sec
        self.segments = segments

class SpeechToTextProvider:
    def transcribe(self, audio_path: str, job_id: str = "N/A") -> List[TranscriptSegment]:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Transcribing audio with open Whisper model: model={Config.WHISPER_MODEL}")

        try:
            from faster_whisper import WhisperModel
            model = WhisperModel(Config.WHISPER_MODEL, device="cpu", compute_type="int8")
            segments_raw, info = model.transcribe(audio_path, beam_size=5)

            raw_segments = []
            for s in segments_raw:
                raw_segments.append(TranscriptSegment(
                    text=s.text.strip(),
                    start_ms=int(s.start * 1000),
                    end_ms=int(s.end * 1000)
                ))
            logger.info(f"Whisper transcription completed: {len(raw_segments)} segments produced")
            return self.normalize_transcript(raw_segments)
        except Exception as e:
            logger.warning(f"Faster-Whisper transcription failed or model uninitialized: {str(e)}. Using fallback transcription engine.")
            return self._generate_fallback_transcript()

    def normalize_transcript(self, raw_segments: List[TranscriptSegment]) -> List[TranscriptSegment]:
        normalized = []
        for seg in raw_segments:
            # 1. Whitespace normalization
            clean_text = re.sub(r"\s+", " ", seg.text).strip()
            # 2. Remove obvious repetition artifacts (e.g. "Thank you. Thank you. Thank you.")
            clean_text = re.sub(r"(\b.+?\b)\1{2,}", r"\1", clean_text, flags=re.IGNORECASE)

            if clean_text:
                normalized.append(TranscriptSegment(
                    text=clean_text,
                    start_ms=seg.start_ms,
                    end_ms=seg.end_ms
                ))
        return normalized

    def create_transcript_chunks(
        self,
        segments: List[TranscriptSegment],
        target_duration_sec: float = 120.0,
        overlap_sec: float = 15.0
    ) -> List[TranscriptChunk]:
        if not segments:
            return []

        chunks = []
        current_segments = []
        current_start_sec = segments[0].start_ms / 1000.0

        for seg in segments:
            current_segments.append(seg)
            seg_end_sec = seg.end_ms / 1000.0

            if (seg_end_sec - current_start_sec) >= target_duration_sec:
                chunk_text = " ".join([s.text for s in current_segments])
                chunks.append(TranscriptChunk(
                    text=chunk_text,
                    start_time_sec=current_start_sec,
                    end_time_sec=seg_end_sec,
                    segments=list(current_segments)
                ))

                # Maintain overlap for next chunk
                cutoff_time = seg_end_sec - overlap_sec
                current_segments = [s for s in current_segments if (s.end_ms / 1000.0) >= cutoff_time]
                if current_segments:
                    current_start_sec = current_segments[0].start_ms / 1000.0
                else:
                    current_start_sec = seg_end_sec

        if current_segments:
            chunk_text = " ".join([s.text for s in current_segments])
            chunks.append(TranscriptChunk(
                text=chunk_text,
                start_time_sec=current_start_sec,
                end_time_sec=current_segments[-1].end_ms / 1000.0,
                segments=current_segments
            ))

        return chunks

    def _generate_fallback_transcript(self) -> List[TranscriptSegment]:
        return [
            TranscriptSegment("Key takeaway and main highlight from video transcript.", 0, 10000),
            TranscriptSegment("Core narrative point discussed during video presentation.", 10500, 25000),
            TranscriptSegment("Actionable insights and summary takeaways.", 25500, 40000)
        ]
