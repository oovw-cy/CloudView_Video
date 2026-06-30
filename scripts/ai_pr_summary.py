import os
import subprocess
import sys

import requests


MAX_DIFF_CHARS = 60000


def read_file(path):
    if not os.path.exists(path):
        return ""
    with open(path, "r", encoding="utf-8") as file:
        return file.read()


def run_git_diff(base_sha, head_sha):
    try:
        result = subprocess.run(
            ["git", "diff", f"{base_sha}...{head_sha}"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            check=True,
        )
        return result.stdout
    except subprocess.CalledProcessError:
        result = subprocess.run(
            ["git", "diff", base_sha, head_sha],
            capture_output=True,
            text=True,
            encoding="utf-8",
            check=True,
        )
        return result.stdout


def call_llm(prompt):
    api_key = os.getenv("OPENAI_API_KEY")
    base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is missing. Please add it in GitHub Actions Secrets.")

    response = requests.post(
        f"{base_url}/chat/completions",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        json={
            "model": model,
            "temperature": 0.2,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是一个资深 Java 微服务 PR 摘要 Agent。"
                        "请严格基于项目上下文、PR 摘要规则和 Git diff 生成评审摘要。"
                        "不要输出 Code Review 报告，不要输出单元测试建议，不要编造 diff 中不存在的业务。"
                    ),
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
        },
        timeout=120,
    )

    if response.status_code >= 400:
        raise RuntimeError(f"LLM request failed: {response.status_code}\n{response.text}")

    data = response.json()
    return data["choices"][0]["message"]["content"]


def post_pr_comment(body):
    github_token = os.getenv("GITHUB_TOKEN")
    github_repository = os.getenv("GITHUB_REPOSITORY")
    pr_number = os.getenv("PR_NUMBER")

    if not github_token or not github_repository or not pr_number:
        print(body)
        return

    response = requests.post(
        f"https://api.github.com/repos/{github_repository}/issues/{pr_number}/comments",
        headers={
            "Authorization": f"Bearer {github_token}",
            "Accept": "application/vnd.github+json",
        },
        json={"body": body},
        timeout=60,
    )

    if response.status_code >= 400:
        raise RuntimeError(f"GitHub comment failed: {response.status_code}\n{response.text}")


def build_prompt(project_context, summary_prompt, diff, truncated):
    diff_note = ""
    if truncated:
        diff_note = "\n注意：本次 diff 过长，以下只截取前 60000 个字符，请优先基于已提供内容生成 PR 摘要。\n"

    return f"""
# 项目上下文

{project_context}

# AI PR 摘要规则

{summary_prompt}

# 本次 Pull Request Diff
{diff_note}

```diff
{diff}
