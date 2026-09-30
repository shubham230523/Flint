from ai.candidate_discovery import CandidateDiscovery
from ai.local_text_analyzer import LocalTextAnalyzer
from ai.multimodal_verifier import MultimodalVerifier, ModelCapabilityService
from pipeline.semantic_segmenter import VideoSemanticSegment

def test_candidate_scoring_and_verification():
    segments = [
        VideoSemanticSegment("seg_1", 0.0, 45.0, "Welcome to Flint! This is a complete standalone video segment about AI.", [], ["/tmp/frame1.jpg"])
    ]

    candidates = CandidateDiscovery.score_and_discover_candidates(segments, job_id="test_cand")
    assert len(candidates) == 1
    assert candidates[0]["deterministic_score"] >= 0.5

    analyzed = LocalTextAnalyzer.analyze_candidates(candidates, job_id="test_cand")
    assert "hook" in analyzed[0]
    assert "title" in analyzed[0]

    verified = MultimodalVerifier.verify_candidates(analyzed, job_id="test_cand")
    assert len(verified) == 1
    assert verified[0]["verified_visually"] is True
    assert verified[0]["final_score"] > 0
