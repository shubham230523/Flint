import logging
from typing import List, Dict, Any
from config import JobLoggerAdapter

base_logger = logging.getLogger("CandidateDiscovery")

class CandidateDiscovery:
    @staticmethod
    def score_and_discover_candidates(segments: List[Any], job_id: str = "N/A") -> List[Dict[str, Any]]:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Running deterministic candidate discovery on {len(segments)} semantic segments")

        candidates = []

        for idx, seg in enumerate(segments):
            dur = seg.duration_sec
            text = seg.transcript_text.strip()
            text_len = len(text)

            # Deterministic scoring factors
            duration_score = 1.0 if (20.0 <= dur <= 90.0) else (0.5 if (15.0 <= dur <= 120.0) else 0.2)
            word_count = len(text.split())
            density = word_count / dur if dur > 0 else 0
            density_score = 1.0 if (1.5 <= density <= 3.5) else 0.6
            completeness_score = 1.0 if text.endswith((".", "!", "?")) else 0.7

            total_score = (duration_score * 0.4) + (density_score * 0.3) + (completeness_score * 0.3)

            if total_score >= 0.5:
                candidates.append({
                    "id": f"cand_{idx + 1}",
                    "start_time_sec": seg.start_sec,
                    "end_time_sec": seg.end_sec,
                    "duration_sec": dur,
                    "transcript_text": text,
                    "deterministic_score": round(total_score, 2),
                    "visual_samples": seg.visual_samples
                })

        logger.info(f"Deterministic discovery shortlisted {len(candidates)} candidates")
        return candidates
