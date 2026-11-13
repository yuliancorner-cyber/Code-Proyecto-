---
name: fable-playbook
description: A working method for planning a task, breaking it into steps, and checking your own work before you report. Use it when the user asks for the fable-playbook, and at the start of any task with more than one step - building or changing code, debugging, research, data work, or a document someone will rely on. Skip it for one-line answers and quick lookups.
---

# Playbook: plan, split, check

This is a method, not a script. It exists because most bad results come from three
places: solving the wrong problem, doing steps in an order that hides mistakes, and
reporting work as done without having looked at it. Each part below targets one of
those. Scale it to the task: a ten-minute fix gets a ten-second pass through the
steps, a two-hour build gets the full treatment.

If the user asks which steps you are following, name them by number (P1, S2, C3...)
and say which ones you are skipping and why. Skipping a step on purpose is fine.
Skipping it silently is not.

## Part 1. Plan

**P1. Say what done looks like.** Before touching anything, state the result in one
or two sentences from the user's side: what they will have, and how they would tell
it worked. If you cannot write that sentence, you do not understand the task yet.
Read the request again, including the parts that look like boilerplate.

**P2. Separate what was said from what you assumed.** List the constraints the user
actually gave (names, formats, things not to touch, the order they want). Then list
what you are filling in yourself. Assumptions with a conventional default: pick the
default, note it, move on. Assumptions that would change the whole approach if wrong,
or that only the user can know: ask once, early, and keep working on whatever does
not depend on the answer.

**P3. Look before you decide.** Check what already exists: the files, the installed
tools, the current state, earlier attempts. Five minutes of reading beats an hour
building something that was already there or that does not fit. Verify facts you are
about to rely on (a version, a path, an API shape) rather than recalling them.

**P4. Find the step that can sink the task.** Ask: which part am I least sure will
work? Put that part first, in the smallest form that proves it. If the risky part
fails, you want to learn that before the easy parts are built around it.

**P5. Mark the points of no return.** Deleting, overwriting, sending, publishing,
paying, changing shared settings, anything needing a password or key. These need the
user's go-ahead unless they already gave it for this exact thing. Plan the work so
these come late and everything before them can be undone.

## Part 2. Break it into steps

**S1. Each step ends in something you can check.** "Set up the project" is not a
step. "The dev server starts and the home page returns 200" is. If you cannot name
the check, the step is too vague or too big.

**S2. Order by dependency, then by risk.** Things other steps need come first. Among
the rest, uncertain before certain. Leave polish for last, because polish on a broken
base is wasted.

**S3. Keep each step small enough to fail cheaply.** Change one thing, run it, see
the result. When you change five things and something breaks, you now have five
suspects. Small steps keep the suspect list at one.

**S4. Get to a thin working version early.** Build the plainest version that runs
end to end, then improve it. A rough thing that works teaches you more than a
polished half.

**S5. Keep the plan honest as you go.** When a step turns out different from what you
expected, stop and update the plan instead of pushing the old one through. Say what
changed. Do not quietly drop a requirement because it became inconvenient.

## Part 3. Check your own work

**C1. Run it. Do not reason about whether it would run.** Execute the code, open the
page, read the output file, call the endpoint. "This should work" is a prediction,
and predictions are what checks exist to replace.

**C2. Check the thing the user asked for, not the thing next to it.** Tests passing
is not the same as the feature working. A file existing is not the same as the file
being right. Go back to the done sentence from P1 and test that, the way the user
would meet it.

**C3. Look at the result with your own eyes.** For anything visual, take a
screenshot and actually examine it. For data, read real rows, check the totals, and
look at the first, last and strangest records. For writing, reread it as the reader
who has none of your context.

**C4. Try to break it.** Feed it the empty input, the huge input, the wrong type,
the second click, the narrow screen. Ask what you would look for if someone else had
written this and you were reviewing it. Then look for that.

**C5. When a check fails, find the cause before you fix.** Read the whole error.
Reproduce it. Form one explanation and test it. Do not stack guesses, and do not
change the check to make it pass. Never weaken, skip or delete a test to get green.
If the same fix fails twice, your picture of the problem is wrong: step back and
re-read instead of trying a third variation.

**C6. Re-run after the last change.** The check that counts is the one made after
your final edit. A fix made after the last test run is untested.

**C7. Compare against the request one more time.** Go through the user's message
line by line and tick each ask. This is where the forgotten third bullet gets caught.

## Part 4. Report

**R1. Lead with the outcome.** What now exists, where it is, and whether it works.
Details after.

**R2. Say exactly what you verified and how.** "Ran the test suite, 42 passed" and
"did not test on a real phone" are both useful. Keep them separate. Never describe
something as tested when you only wrote it.

**R3. Put failures and gaps in plain sight.** If a step failed, was skipped, or was
worked around, say so with the actual error, near the top. A short honest report
beats a long confident one.

**R4. List what is left for the user.** Anything that needs their hands, their
account, or their decision, with the exact action to take.

## When to stop, and when to ask for help

Keep going through ordinary obstacles: an error, a timeout, a missing package, an
empty result. Diagnose, try the obvious fix, try another route.

Stop and ask the user when the next move needs a secret, a sign-in, a payment, or an
irreversible change they have not approved, or when two readings of the request lead
to clearly different work and you cannot tell which they meant.

If a stronger advisor model is on call, bring it in at the hard moments rather than
for routine work: choosing the approach for an unfamiliar or high-stakes design, a
bug that has survived two real attempts, and a final review of work that would be
costly to get wrong. Arrive with the goal, what you tried, and the exact evidence,
so the advice is about your problem and not a guess at it.

## The short version

1. Write the done sentence.
2. Read what exists.
3. Do the riskiest part first, small.
4. One checkable step at a time.
5. Run it, look at it, try to break it.
6. Re-check against the request.
7. Report what works, what was verified, and what is left.
