import os
import sys
import subprocess
import shutil

def check_and_setup_environment():
    print("==========================================================")
    print("Flint Media Worker — Local Environment Setup Helper")
    print("==========================================================")

    # 1. Check Python Version
    print(f"[1/4] Python Version: {sys.version.split()[0]}")

    # 2. Check FFmpeg
    ffmpeg_path = shutil.which("ffmpeg")
    if ffmpeg_path:
        print(f"[2/4] FFmpeg binary found at: {ffmpeg_path}")
    else:
        print("[2/4] WARNING: FFmpeg binary not found on system PATH!")
        print("      Please install FFmpeg or add it to PATH for full video rendering capabilities.")

    # 3. Check & Install Python Dependencies
    required_packages = [
        "fastapi",
        "uvicorn",
        "pydantic",
        "scenedetect[opencv]",
        "opencv-python-headless",
        "faster-whisper",
        "torch",
        "yt-dlp",
        "Pillow",
        "requests",
        "pytest"
    ]

    print("[3/4] Installing / Verifying required Python dependencies...")
    for pkg in required_packages:
        cmd = [sys.executable, "-m", "pip", "install", "--quiet", pkg]
        try:
            subprocess.run(cmd, check=True)
            print(f"      [✓] Installed/Verified {pkg}")
        except Exception as e:
            print(f"      [!] Package {pkg} install notice: {str(e)}")

    # 4. Check PyTorch CUDA / CPU Capability
    print("[4/4] Hardware Inference Capabilities:")
    try:
        import torch
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            vram_gb = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            print(f"      CUDA GPU Detected: {gpu_name} ({vram_gb:.1f} GB VRAM)")
        else:
            print("      CUDA GPU not detected. Using CPU inference mode for Whisper and OpenCV.")
    except Exception:
        print("      CPU inference mode enabled.")

    print("==========================================================")
    print("Environment setup completed! You can now run:")
    print("  python media-worker/main.py")
    print("==========================================================")

if __name__ == "__main__":
    check_and_setup_environment()
