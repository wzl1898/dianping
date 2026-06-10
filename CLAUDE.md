# 项目规范

## 开发流程

本项目所有代码变更必须严格遵循 PR 工作流，详见 [PR-WORKFLOW.md](./PR-WORKFLOW.md)。

### 强制规则

1. **禁止直接向 `master` 或 `dev` 分支提交代码**
2. 所有开发必须在功能分支上进行，命名规范：
   - `feat/xxx` — 新功能
   - `fix/xxx` — Bug 修复
   - `perf/xxx` — 性能优化
   - `refactor/xxx` — 重构
   - `docs/xxx` — 文档
3. 功能分支从 `dev` 拉出，完成后发起 PR 到 `dev`
4. PR 需经过 Code Review 并获得 Approve 后才能合并
5. 确保分支保护规则在远程仓库已启用

### 操作流程

```bash
# 开始开发
git checkout dev
git pull origin dev
git checkout -b feat/xxx dev

# 开发、提交、推送
git add .
git commit -m "feat: xxx"
git push origin feat/xxx

# 然后通过 GitHub 发起 PR
```

### 环境信息

- 远程仓库：`git@github.com:wzl1898/dianping.git`
- 生产分支：`master`
- 开发分支：`dev`
