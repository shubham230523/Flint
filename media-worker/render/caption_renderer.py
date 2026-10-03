import os
import subprocess
import logging
from typing import List, Dict, Any
from config import JobLoggerAdapter

base_logger = logging.getLogger("CaptionRenderer")

class ReelCaptionSegment:
    def __init__(self, text: str, rel_start_sec: float, rel_end_sec: float):
        self.text = text
        self.rel_start_sec = max(0.0, rel_start_sec)
        self.rel_end_sec = max(0.1, rel_end_sec)

class CaptionRenderer:
    @staticmethod
    def generate_srt_subtitles(
        transcript_segments: List[Any],
        reel_start_sec: float,
        reel_end_sec: float,
        output_srt_path: str,
        logger
    ) -> List[ReelCaptionSegment]:
        caption_segments = []
        srt_lines = []
        counter = 1

        for seg in transcript_segments:
            seg_start = getattr(seg, "start_ms", 0) / 1000.0
            seg_end = getattr(seg, "end_ms", 0) / 1000.0
            text = getattr(seg, "text", "").strip()

            if seg_end >= reel_start_sec and seg_start <= reel_end_sec:
                rel_start = max(0.0, seg_start - reel_start_sec)
                rel_end = min(reel_end_sec - reel_start_sec, seg_end - reel_start_sec)

                if rel_end > rel_start:
                    cap_seg = ReelCaptionSegment(text=text, rel_start_sec=rel_start, rel_end_sec=rel_end)
                    caption_segments.append(cap_seg)

                    start_srt = CaptionRenderer._format_srt_timestamp(rel_start)
                    end_srt = CaptionRenderer._format_srt_timestamp(rel_end)

                    srt_lines.append(f"{counter}\n{start_srt} --> {end_srt}\n{text}\n")
                    counter += 1

        with open(output_srt_path, "w", encoding="utf-8") as f:
            f.writelines("\n".join(srt_lines))

        logger.info(f"Generated SRT subtitles file ({len(caption_segments)} entries) at: {output_srt_path}")
        return caption_segments

    @staticmethod
    def render_burned_captions(
        input_video_path: str,
        transcript_segments: List[Any],
        reel_start_sec: float,
        reel_end_sec: float,
        output_captioned_path: str,
        job_id: str = "N/A"
    ) -> str:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Rendering burned-in subtitles for Reel video: {input_video_path}")

        job_dir = os.path.dirname(input_video_path)
        srt_path = os.path.join(job_dir, "reel_captions.srt")

        CaptionRenderer.generate_srt_subtitles(
            transcript_segments, reel_start_sec, reel_end_sec, srt_path, logger
        )

        # Escape backslashes and colons for FFmpeg filter path
        escaped_srt = srt_path.replace("\\", "/").replace(":", "\\:")
        sub_filter = f"subtitles='{escaped_srt}':force_style='Fontname=Arial,Fontsize=12,PrimaryColour=&H00FFFFFF,BackColour=&H80000000,BorderStyle=3,Outline=1,Shadow=0,Alignment=2,MarginL=40,MarginR=40,MarginV=70'"

        cmd = [
            "ffmpeg", "-y",
            "-i", input_video_path,
            "-vf", sub_filter,
            "-c:v", "libx264",
            "-preset", "fast",
            "-c:a", "aac",
            "-b:a", "128k",
            output_captioned_path
        ]

        try:
            subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
            logger.info(f"Burned-in subtitle rendering completed: {output_captioned_path}")
            return output_captioned_path
        except Exception as e:
            logger.warning(f"FFmpeg caption rendering with audio failed ({str(e)}). Retrying without audio re-encode...")
            cmd_no_a = [
                "ffmpeg", "-y",
                "-i", input_video_path,
                "-vf", sub_filter,
                "-c:v", "libx264",
                "-an",
                output_captioned_path
            ]
            try:
                subprocess.run(cmd_no_a, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
                logger.info(f"Burned-in subtitle rendering (video only) completed: {output_captioned_path}")
                return output_captioned_path
            except Exception as e2:
                logger.error(f"FFmpeg caption rendering failed completely: {str(e2)}")
                import shutil
                if os.path.exists(input_video_path) and os.path.getsize(input_video_path) > 100:
                    shutil.copy2(input_video_path, output_captioned_path)
                return output_captioned_path

    @staticmethod
    def _format_srt_timestamp(seconds: float) -> str:
        hrs = int(seconds // 3600)
        mins = int((seconds % 3600) // 60)
        secs = int(seconds % 60)
        millis = int((seconds - int(seconds)) * 1000)
        return f"{hrs:02d}:{mins:02d}:{secs:02d},{millis:03d}"
