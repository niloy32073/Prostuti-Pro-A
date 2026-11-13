---
name: "work-rules"
description: "MANDATORY user rules for this project, applied to every task that starts, uses or tests on an Android emulator (adb, instrumented/device tests, screenshots, running the app) or that edits any code. Rule 1: only ONE emulator may run at a time — finish all testing on the first, shut it down, then start the second; never run two in parallel. Rule 2: edit only the code the task needs — ask permission before any other edit, and report any dead code found."
---

# Work rules (set by the user — follow literally, no exceptions)

These two rules come straight from the user. They apply on top of `android-master` and every other skill.
If a rule blocks the task, stop and ask; never work around it.

## Rule 1 — Only one emulator at a time

The user's words: always run a single emulator. If two are needed (to check scaling on another device size, etc.),
finish every test on the first emulator, shut it down, and only then start the second. Never run two in parallel.

How to follow it (this machine: `adb` = `~/Library/Android/sdk/platform-tools/adb`,
`emulator` = `~/Library/Android/sdk/emulator/emulator`; neither is on PATH):

1. **Before starting any emulator**, check what is running: `adb devices`.
   - One already running and it is the device you need → use it. Do not start another.
   - One already running but you need a different device → do NOT start the new one yet (see step 3).
   - None running → start exactly one.
2. **Finish everything on the current emulator first**: all tests, screenshots and manual checks planned for it.
   Pull screenshots/results before shutting it down.
3. **Switch devices only in this order**:
   1. Stop the current emulator: `adb -s <serial> emu kill`.
   2. Wait until `adb devices` no longer lists it (poll briefly; never assume it is gone).
   3. Only then start the second emulator and wait for `adb shell getprop sys.boot_completed` to return `1`.
4. Never pass two serials to parallel jobs, never launch an emulator in the background while another is up,
   and never start a second one "to save time".
5. If you find two emulators already running (e.g. the user started them), do not add a third and do not kill
   either one on your own — tell the user and ask which one to use or stop.
6. In the final report, say which emulator(s) were used, in what order, and that the first was stopped before
   the second started.

## Rule 2 — Edit only what the task needs; report dead code

The user's words: before editing any code that is not needed, ask permission. If you find dead code anywhere, report it.

### What counts as "needed"
An edit is needed only if the task cannot be done without it:
- the code the user asked to add or change;
- direct fallout of that change: call sites, imports your change made unused, tests for the changed behaviour,
  strings/resources the new code uses, a DI line for a new class.

### Ask first (do not just do it)
Anything else needs the user's OK **before** you touch it, for example:
- refactors, renames, moving files, reformatting, reordering imports in untouched code;
- fixing lint warnings, typos or style in code the task doesn't touch;
- "while I'm here" bug fixes or improvements you noticed in passing;
- deleting or rewriting existing code, comments or resources that the task doesn't require;
- changing build files, dependencies, CLAUDE.md or other config not required by the task.

How to ask: list each proposed extra edit as `file:line — what — why`, then wait for a clear yes per item.
Without a yes, leave it unchanged and mention it in the final report as "not done, needs your OK".

### Dead code — always report, never silently delete
While reading or editing, watch for dead code: unused functions/classes/files, unused parameters or properties,
unused imports in files you didn't change, unused string/drawable resources, unreachable branches, commented-out code,
feature flags that are always on/off, routes or DI definitions nothing uses.

- Report every finding to the user as `file:line — what is dead — how you know` (e.g. "no references found with grep").
- Do not delete it unless the user says so (deleting it is an unneeded edit under this rule).
- Dead code that your own change created (e.g. an import your edit made unused) is not a finding — just remove it.

### Final report
Every task's closing message includes:
- edits made beyond the request, each with the user's approval (or "none");
- extra edits proposed but not done;
- dead code found (or "none found").
