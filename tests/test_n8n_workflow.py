import json
from pathlib import Path


def test_n8n_pipeline_workflow_runs_complete_reviewed_flow() -> None:
    path = Path(__file__).parents[1] / "n8n" / "workflows" / "02_local_album_pipeline.json"
    payload = json.loads(path.read_text(encoding="utf-8"))
    command = next(
        node["parameters"]["command"]
        for node in payload["nodes"]
        if node["type"] == "n8n-nodes-base.executeCommand"
    )

    assert "--build" in command
    assert "--render-video" in command
    assert "--upload-youtube" in command
    assert "--report" in command
    assert "--preflight" in command
    assert "--non-interactive" in command
    assert "python3 -m budget.suno_generate" in command
    assert "--queue /workspace/config/suno_generation_queue.json" in command
    assert "--state /workspace/.state/suno-generation.json" in command
    assert "--report /workspace/outputs/albums/automation-report.json" in command
    assert payload["active"] is False
