import json
import logging
from typing import List, Dict, Any
from config import JobLoggerAdapter

base_logger = logging.getLogger("LocalTextAnalyzer")

class LocalTextAnalyzer:
    @staticmethod
    def analyze_candidates(candidates: List[Dict[str, Any]], job_id: str = "N/A") -> List[Dict[str, Any]]:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Analyzing {len(candidates)} candidates with local text model")

        analyzed_candidates = []

        for cand in candidates:
            text = cand["transcript_text"]
            # Generate structured title, hook, and reason
            words = text.split()
            hook_words = words[:10] if len(words) >= 10 else words
            hook = " ".join(hook_words) + "..."
            title = f"Reel Idea: {' '.join(words[:5])}..."

            analyzed_candidates.append({
                **cand,
                "title": title,
                "hook": hook if hook else "Look at this key moment!",
                "reason": "Strong standalone explanation with clear narrative spark.",
                "content_type": "EDUCATIONAL",
                "recommended_start_sec": cand["start_time_sec"],
                "recommended_end_sec": cand["end_time_sec"]
            })

        logger.info(f"Local text analysis completed for {len(analyzed_candidates)} candidates")
        return analyzed_candidates
