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
                        "你是一个资深 Java 微服务单元测试设计 Agent。"
                        "请严格基于项目上下文、单元测试规则和 Git diff 生成测试建议。"
                        "不要输出 Code Review 报告，不要编造 diff 中不存在的业务。"
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


def build_prompt(project_context, test_prompt, diff, truncated):
    diff_note = ""
    if truncated:
        diff_note = "\n注意：本次 diff 过长，以下只截取前 60000 个字符，请优先基于已提供内容生成测试建议。\n"

    return f"""
# 项目上下文

{project_context}

# AI 单元测试生成规则

{test_prompt}

# 本次 Pull Request Diff
{diff_note}

```diff
{diff}
```

请基于以上内容生成一份完整的 AI 单元测试建议报告。

必须输出以下结构：

## AI 单元测试建议

### 测试目标
说明本次变更最需要验证的核心行为。

### 建议新增或修改的测试类
按模块列出建议测试类路径。

### 重点测试用例
每个用例请包含：
- 测试方法名
- 测试场景
- 输入数据
- Mock 依赖
- 预期结果
- 覆盖的风险点

### 边界场景
列出空值、异常、并发、重复提交、非法参数等测试场景。

### 外部依赖 Mock 建议
说明 Redis、MySQL、RocketMQ、MinIO、ES、WebSocket 等依赖如何 mock。

### 覆盖率关注点
说明哪些分支必须覆盖，哪些模块是高优先级。
"""


def main():
    base_sha = os.getenv("BASE_SHA")
    head_sha = os.getenv("HEAD_SHA")

    if not base_sha or not head_sha:
        raise RuntimeError("BASE_SHA or HEAD_SHA is missing.")

    project_context = read_file("AI_CONTEXT.md")
    test_prompt = read_file(".agent/prompts/unit-test.md")

    if not project_context.strip():
        raise RuntimeError("AI_CONTEXT.md is missing or empty.")

    if not test_prompt.strip():
        raise RuntimeError(".agent/prompts/unit-test.md is missing or empty.")

    diff = run_git_diff(base_sha, head_sha)

    if not diff.strip():
        post_pr_comment("## AI 单元测试建议\n\n本次 PR 没有检测到代码 diff。")
        return

    truncated = False
    if len(diff) > MAX_DIFF_CHARS:
        diff = diff[:MAX_DIFF_CHARS]
        truncated = True

    prompt = build_prompt(project_context, test_prompt, diff, truncated)
    test_result = call_llm(prompt)

    comment = f"""## AI 单元测试建议

{test_result}

---

本评论由 AI Unit Test Suggestion workflow 自动生成。
"""

    post_pr_comment(comment)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        error_message = f"""## AI 单元测试建议执行失败

```text
{str(error)}
```

请检查 GitHub Actions 日志、OPENAI_API_KEY、模型配置、unit-test.md 和 workflow 环境变量。
"""
        print(error_message)
        try:
            post_pr_comment(error_message)
        except Exception:
            pass
        sys.exit(1)
