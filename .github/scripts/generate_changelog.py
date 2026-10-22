"""Write CurseForge release notes using the checked-out Git history."""

import argparse
import os
import re
import subprocess
from pathlib import Path
from urllib.parse import quote


def git(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["git", *args], check=check, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        encoding="utf-8", errors="replace",
    )


def markdown_text(value: str) -> str:
    """Show commit messages as text, even if they contain Markdown or HTML."""
    return re.sub(r"([\\`*_{}\[\]<>()#+.!|~>-])", r"\\\1", value)


def generate(version: str, repository: str, notes: str = "") -> str:
    head = git("rev-parse", "HEAD").stdout.strip()
    # Exclude the tag being released when selecting the previous version.
    previous = git("describe", "--tags", "--match", "v[0-9]*", "--abbrev=0", "HEAD^", check=False)
    base = previous.stdout.strip() if previous.returncode == 0 else None
    # The first release uses its release commit instead of unrelated setup history.
    revision = f"{base}..{head}" if base else f"{head}^!"
    fields = git("log", "--format=%H%x00%B%x00", revision, "--").stdout.split("\0")
    url = f"https://github.com/{repository}"
    lines = [f"## {markdown_text(version)}", ""]
    if notes.strip():
        lines.extend([notes.strip(), ""])
    if base:
        lines.extend([f"Changes since {markdown_text(base)}.", ""])
    else:
        lines.extend(["Initial release.", ""])
    for index in range(0, len(fields) - 1, 2):
        sha, message = fields[index].strip(), fields[index + 1].strip()
        message_lines = message.splitlines() or ["(No commit message)"]
        lines.append(f"- {markdown_text(message_lines[0])} ([{sha[:7]}]({url}/commit/{sha}))")
        if len(message_lines) > 1:
            lines.append("")
            lines.extend(f"  {markdown_text(line)}" if line else "" for line in message_lines[1:])
        lines.append("")
    if base:
        lines.append(f"[Compare changes]({url}/compare/{quote(base, safe='')}...{head})")
    else:
        lines.append(f"[Source commit]({url}/commit/{head})")
    return "\n".join(lines).rstrip() + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    content = generate(os.environ["RELEASE_VERSION"], os.environ["GITHUB_REPOSITORY"],
                       os.environ.get("MANUAL_CHANGELOG", ""))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(content, encoding="utf-8")
    print(f"Release notes written to {args.output}")


if __name__ == "__main__":
    main()
