# PR 工作流规范

> 本项目的所有开发必须严格遵循基于 Pull Request 的协作流程。
> **禁止直接向 `dev` 和 `master` 分支 push 代码。**

---

## 1. 分支策略

```
master            ← 生产分支，只允许从 dev 通过 PR 合并
  └─ dev          ← 集成分支，日常开发目标分支
       ├─ feat/xxx    ← 新功能开发
       ├─ fix/xxx     ← Bug 修复
       ├─ perf/xxx    ← 性能优化
       └─ refactor/xxx ← 重构
```

### 分支命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 功能开发 | `feat/<简短描述>` | `feat/seckill-optimization` |
| Bug 修复 | `fix/<简短描述>` | `fix/cache-penetration-npe` |
| 性能优化 | `perf/<简短描述>` | `perf/second-level-cache` |
| 重构 | `refactor/<简短描述>` | `refactor/order-service` |
| 文档 | `docs/<简短描述>` | `docs/pr-workflow` |

---

## 2. 开发流程

### 2.1 开始新任务

```bash
# 确保本地 dev 是最新的
git checkout dev
git pull origin dev

# 新建功能分支（从 dev 拉出）
git checkout -b feat/xxx dev
```

### 2.2 开发中提交

- **小粒度提交**：每次提交只做一件事，方便 review 和回滚
- **提交信息格式**：使用约定式提交（Conventional Commits）

```
<type>: <简短描述>

<可选的详细说明>
```

### 2.3 提交类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 实现基于Redis的分布式ID生成器` |
| `fix` | Bug 修复 | `fix: 修复缓存穿透时空指针异常` |
| `perf` | 性能优化 | `perf: 将秒杀接口响应时间从500ms降至50ms` |
| `refactor` | 重构 | `refactor: 抽取缓存策略为独立模块` |
| `docs` | 文档 | `docs: 添加PR工作流规范` |
| `test` | 测试 | `test: 添加秒杀资格预检单元测试` |
| `chore` | 构建/工具 | `chore: 配置CI自动构建` |

### 2.4 保持分支同步

开发过程中定期将 dev 的更新合并到自己的分支：

```bash
git fetch origin
git rebase origin/dev
# 或 git merge origin/dev
```

> **推荐使用 rebase**，保持提交历史线性整洁。如果已 push 过，rebase 后需要 `git push --force-with-lease`。

---

## 3. 发起 Pull Request

### 3.1 PR 前置检查

发起 PR 前，自行检查以下内容：

- [ ] 代码编译通过，无报错
- [ ] 相关单元测试/集成测试通过
- [ ] 自测核心业务流程正常
- [ ] 已同步最新的 dev 分支代码
- [ ] 提交信息清晰，无 WIP 提交
- [ ] 无调试日志、TODO 等残留代码

### 3.2 PR 模板

```markdown
## 变更内容
<!-- 清晰描述本次 PR 改了什么 -->

## 关联 Issue
<!-- 如有关联 Issue，请链接 -->

## 测试情况
<!-- 描述测试覆盖情况和自测结果 -->

## 性能影响
<!-- 如果是性能优化，附上优化前后的对比数据 -->

## 需要关注的潜在风险
<!-- 可能会影响到哪些模块，需要注意什么 -->
```

### 3.3 PR 规范

- **PR 标题**：用 `[type] 描述` 格式，如 `[perf] 秒杀接口优化`
- **PR 范围**：一个 PR 只做一件事。如果一个功能涉及多个独立模块，拆分为多个 PR
- **PR 大小**：尽量控制在 **200-400 行变更**内。过大的 PR 难以 review
- **目标分支**：默认目标为 `dev`

---

## 4. Code Review

### 4.1 Review 原则

| 原则 | 说明 |
|------|------|
| **先理解再 review** | 先看 PR 描述和关联 Issue，理解上下文 |
| **关注逻辑，不拘泥格式** | 格式问题用自动化工具解决，review 聚焦正确性 |
| **提问而非指责** | 用 "这里是不是可能的并发问题？" 替代 "这代码是错的" |
| **及时回复** | Review 反馈应在 24 小时内响应 |

### 4.2 Review 检查清单

- [ ] 功能正确性：代码是否实现了预期功能？
- [ ] 边界条件：空值、异常输入、并发冲突是否有处理？
- [ ] 安全：SQL 注入、超卖、权限校验等是否有漏洞？
- [ ] 性能：是否有不必要的循环、N+1 查询、锁竞争？
- [ ] 幂等性：消息处理和异步操作是否考虑了重复执行？
- [ ] 可读性：命名是否清晰？是否需要加注释？
- [ ] 测试覆盖：是否缺少关键测试用例？

### 4.3 Review 流程

```
作者发起 PR
    ↓
Reviewer 收到通知
    ↓  ┌──────────────────────┐
    ├──│ Review 通过 (Approve) │ → 进入合并流程
    ↓  └──────────────────────┘
    ↓  ┌──────────────────────┐
    └──│ 请求修改 (Request)   │ → 作者修改后重新 review
       └──────────────────────┘
```

---

## 5. 合并策略

### 5.1 合并方式

本项目使用 **Squash Merge**：

```
feat/seckill-optimization 分支的 N 次提交
    ↓ squash
1 次提交合并到 dev，提交信息为 PR 标题
```

**为什么用 Squash Merge？**
- 保持 `dev` 提交历史线性、整洁
- 每个 PR 对应一个提交，回滚时只需 revert 一次
- 开发分支上的小步提交细节不污染主线历史

### 5.2 合并前置条件

- [ ] 至少获得 1 个 Approve
- [ ] 所有 CI 检查通过
- [ ] 无未解决的对话（Resolved conversations）
- [ ] 分支已同步最新的 dev

### 5.3 合并后

- 删除功能分支（可在 GitHub PR 页面点击 Delete branch）
- 本地清理：`git branch -D feat/xxx`

---

## 6. 分支保护规则

### `master` 分支
- ❌ 禁止直接 push
- ❌ 禁止删除
- 🔒 仅通过 `dev → master` 的 PR 合并
- ✅ 需要 CI 通过

### `dev` 分支
- ❌ 禁止直接 push
- ❌ 禁止删除
- 🔒 仅通过功能分支 PR 合并
- ✅ 至少 1 人 Approve
- ✅ 需要 CI 通过

---

## 7. 紧急修复流程（Hotfix）

生产环境出现紧急 Bug 时：

```bash
# 从 master 拉出 hotfix 分支
git checkout -b fix/urgent-issue master

# 修复并提交
git commit -m "fix: 紧急修复xxx问题"

# 发起 PR，目标分支为 master
# master 合并后，再同步到 dev
git checkout dev
git merge master
```

> Hotfix 可放宽 review 要求，但事后必须在 dev 补充测试用例。

---

## 8. 速查命令

```bash
# 创建功能分支
git checkout -b feat/xxx dev

# 同步 dev 最新代码
git fetch origin
git rebase origin/dev

# 发起 PR 前最后一次自检
git log --oneline origin/dev..HEAD   # 查看当前分支的提交
git diff origin/dev...HEAD           # 查看与 dev 的差异

# 撤回 PR（合并前）
git checkout dev
git branch -D feat/xxx
git push origin --delete feat/xxx

# 回滚已合并的 PR（找到那个 squash commit）
git revert <commit-hash>
```
