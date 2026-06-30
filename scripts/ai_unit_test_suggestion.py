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
```

请基于以上内容生成一份完整的 AI PR 摘要。

必须输出以下结构：

## AI PR 摘要

### 变更目的
说明本次 PR 想解决什么问题。

### 涉及模块
列出涉及的模块、包、类和关键文件。

### 核心修改点
用条目列出主要改动，说明每个改动的作用。

### 影响范围
说明影响哪些接口、服务、业务流程、数据结构或外部依赖。

### 潜在风险
说明本次变更可能带来的风险，例如兼容性、空指针、事务、并发、性能、配置、数据一致性等。

### 建议评审重点
说明人工 Review 时应该重点检查哪些文件、方法或逻辑。

### 建议验证方式
说明建议如何验证本次 PR，例如接口测试、单元测试、集成测试、手工验证步骤等。
"""


def main():
    base_sha = os.getenv("BASE_SHA")
    head_sha = os.getenv("HEAD_SHA")

    if not base_sha or not head_sha:
        raise RuntimeError("BASE_SHA or HEAD_SHA is missing.")

    project_context = read_file("AI_CONTEXT.md")
    summary_prompt = read_file(".agent/prompts/pr-summary.md")

    if not project_context.strip():
        raise RuntimeError("AI_CONTEXT.md is missing or empty.")

    if not summary_prompt.strip():
        raise RuntimeError(".agent/prompts/pr-summary.md is missing or empty.")

    diff = run_git_diff(base_sha, head_sha)

    if not diff.strip():
        post_pr_comment("## AI PR 摘要\n\n本次 PR 没有检测到代码 diff。")
        return

    truncated = False
    if len(diff) > MAX_DIFF_CHARS:
        diff = diff[:MAX_DIFF_CHARS]
        truncated = True

    prompt = build_prompt(project_context, summary_prompt, diff, truncated)
    summary_result = call_llm(prompt)

    comment = f"""## AI PR 摘要

{summary_result}

---

本评论由 AI PR Summary workflow 自动生成。
"""

    post_pr_comment(comment)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        error_message = f"""## AI PR 摘要执行失败

```text
{str(error)}
```

请检查 GitHub Actions 日志、OPENAI_API_KEY、模型配置、pr-summary.md 和 workflow 环境变量。
"""
        print(error_message)
        try:
            post_pr_comment(error_message)
        except Exception:
            pass
        sys.exit(1)
