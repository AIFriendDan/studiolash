---
name: linear-dude
description: Manage everything in Linear — log and triage issues, and keep Linear project notes (status updates, descriptions, documents) current. Use when the user asks to log a bug/issue/task in Linear, create or update a Linear issue, check issue status, post a Linear project update, or update project notes/docs in Linear.
---

# Linear Dude

Owns all Linear work: creating/updating issues and keeping each project's notes current. Uses the `mcp__claude_ai_Linear__*` tools (connected via the Productivity plugin's Linear connector, workspace "AiFriendDan | HCiHY Tech").

## Loading tools

These tools are deferred. Before first use in a session, load them in one call:

`ToolSearch({query: "select:mcp__claude_ai_Linear__list_teams,mcp__claude_ai_Linear__list_issues,mcp__claude_ai_Linear__get_issue,mcp__claude_ai_Linear__save_issue,mcp__claude_ai_Linear__list_projects,mcp__claude_ai_Linear__get_project,mcp__claude_ai_Linear__save_project,mcp__claude_ai_Linear__list_documents,mcp__claude_ai_Linear__get_document,mcp__claude_ai_Linear__save_document,mcp__claude_ai_Linear__get_status_updates,mcp__claude_ai_Linear__save_status_update,mcp__claude_ai_Linear__list_issue_labels,mcp__claude_ai_Linear__list_issue_statuses,mcp__claude_ai_Linear__list_comments,mcp__claude_ai_Linear__save_comment"})`

Pull in `list_cycles`, `list_milestones`, `list_users`, `get_team` etc. only if the task needs them.

When passing text to any Linear tool, use real newlines in markdown — not literal `\n` escape sequences.

## Logging issues

1. **Find the team.** Call `list_teams` (cache for the session) and match by name/context. If more than one team could plausibly own the issue and it's not obvious, ask which team before creating anything.
2. **Check for duplicates.** Run `list_issues` filtered to that team with a keyword search before creating — don't file a second issue for something already tracked. If a close match exists, prefer updating/commenting on it (`save_issue` / `save_comment`) over creating a new one, unless the user clearly wants a separate issue.
3. **Create with `save_issue`** (omit the id to create new): concise title, a description that captures the actual context (what's broken, repro steps, where it came from — pull from the conversation, don't invent details), and label/priority only if you have real signal for them via `list_issue_labels` / `list_issue_statuses`.
4. **Confirm back** with the issue identifier and a link so the user can jump straight to it.

## Keeping project notes up to date

Linear gives three places "project notes" can live — pick based on what the user means, and ask once per project if it's not established:

- **Status updates** (`get_status_updates` / `save_status_update`) — the right home for an ongoing narrative log ("what's the state of this project this week"). Default here for recurring "keep notes up to date" asks. Post a new update rather than editing old ones — it's a timeline, not a doc.
- **Project description** (`get_project` / `save_project`) — the project's own summary/scope field. Update this when the project's goal, scope, or target date actually changes, not for routine status chatter.
- **Documents** (`list_documents` / `get_document` / `save_document`) — freeform long-form notes attached to a project (specs, decision logs). Use for persistent reference material the team will reread, and edit in place, appending a dated section rather than overwriting prior content.

Before writing, use `list_projects` to find the right project by name — confirm with the user if the name is ambiguous or no close match exists rather than guessing or creating a new project.

## Hard rules

- Never fabricate issue details, statuses, or "wins" for a project note — pull only from what's actually in the conversation or already in Linear. If there's nothing concrete to report, say so plainly.
- Don't create a new team or project to hold something — only file into teams/projects that already exist unless the user explicitly asks you to create one.
- Search before creating (issues, and check `list_documents`/status update history before adding a note) to avoid duplicates.
- Project notes are additive by default (new status update, appended doc section) — don't silently overwrite prior notes/history unless the user asks for a rewrite.
