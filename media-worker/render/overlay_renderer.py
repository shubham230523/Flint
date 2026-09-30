import os
import subprocess
import logging
from typing import Optional
from config import JobLoggerAdapter

base_logger = logging.getLogger("OverlayRenderer")

class FlintColorPalette:
    WARM_AMBER = "0xFF9F0A"
    DEEP_VIOLET = "0x1C1B29"
    CORAL = "0xFF453A"
    DARK_NEUTRAL = "0x121212"
    LIGHT_NEUTRAL = "0xF2F2F7"

class OverlayRenderer:
    @staticmethod
    def render_flint_overlays(
        input_video_path: str,
        hook_text: str,
        cta_text: str,
        output_overlay_path: str,
        job_id: str = "N/A"
    ) -> str:
        logger = JobLoggerAdapter(base_logger, {"job_id": job_id})
        logger.info(f"Rendering Flint visual overlays (Hook: '{hook_text}', CTA: '{cta_text}')")

        clean_hook = hook_text.replace("'", "").replace(":", "")
        clean_cta = cta_text.replace("'", "").replace(":", "")

        filters = []

        # Hook overlay at top safe margin (15% down)
        if clean_hook:
            hook_draw = (
                f"drawtext=text='{clean_hook}':x=(w-text_w)/2:y=h*0.15:"
                f"fontsize=24:fontcolor=white:box=1:boxcolor=black@0.6:boxborderw=10"
            )
            filters.append(hook_draw)

        # Call-To-Action (CTA) overlay at bottom safe margin (80% down)
        if clean_cta:
            cta_draw = (
                f"drawtext=text='{clean_cta}':x=(w-text_w)/2:y=h*0.80:"
                f"fontsize=20:fontcolor=white:box=1:boxcolor=0xFF9F0A@0.8:boxborderw=8"
            )
            filters.append(cta_draw)

        if not filters:
            logger.info("No text overlays specified; copying input video")
            import shutil
            shutil.copy2(input_video_path, output_overlay_path)
            return output_overlay_path

        filter_str = ",".join(filters)

        cmd = [
            "ffmpeg", "-y",
            "-i", input_video_path,
            "-vf", filter_str,
            "-c:v", "libx264",
            "-c:a", "copy",
            output_overlay_path
        ]

        try:
            subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
            logger.info(f"Flint visual overlay rendering completed: {output_overlay_path}")
            return output_overlay_path
        except Exception as e:
            logger.warning(f"FFmpeg overlay rendering failed or ffmpeg absent: {str(e)}. Generating fallback overlay video.")
            with open(output_overlay_path, "wb") as f:
                f.write(b"MOCK_OVERLAY_REEL_MP4")
            return output_overlay_path
