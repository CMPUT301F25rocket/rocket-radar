#!/usr/bin/env python3
import subprocess
import json
import sys
import argparse
import sqlite3
from datetime import datetime

REPO="CMPUT301F25rocket/rocket-radar"

conn = sqlite3.connect(":memory:")
cursor = conn.cursor()

# PERF: This is a slow intuitive implementation. 
def levenshtein(a: str, b: str):
    if len(a) == 0:
        return len(b)
    if len(b) == 0:
        return len(a)
    if a[0] == b[0]:
        return levenshtein(a[1:], b[1:])
    return 1 + min(levenshtein(a[1:], b), levenshtein(a, b[1:]), levenshtein(a[1:], b[1:]))

# Given a list of issues and number of assignees to each figure out
# how many ours are required for a person participating in all.
# Difficulties:
# Considering day as 8hour work day.
# - Super Easy: Less than hour = 1h
# - Easy: Half day = 4h
# - Average: Full day = 8h
# - Hard: 2-3 days = 24h
# - Super Hard: More than a week = 40h
def compute_work(diffs: list[tuple[str, int]]) -> float:
    table = {
        "D-Super Easy": 1,
        "D-Easy": 4,
        "D-Average": 8,
        "D-Hard": 24,
        "D-Super Hard": 40,
    }
    total = 0
    for (difficulty, num_assigned) in diffs:
        if num_assigned == 0:
            continue
        factor = table.get(difficulty)
        # Failed to get difficulty something may have changed try next best.
        if factor is None:
            distance = None
            best = ""
            for key in table.keys():
                if distance is None:
                    best = key
                    distance = levenshtein(key, difficulty)
                    continue
                current_distance = levenshtein(key, difficulty)
                if current_distance < distance:
                    best = key
                    distance = current_distance
            # If this fails lets just call the factor 0.
            factor = table.get(best, 0)
        total += (1.0/num_assigned) * float(factor)
    return total

# TODO: Query issues is cool. Basically just github issue search via SQL.
def query(args):
    cursor.execute("CREATE TABLE assignees ()")
    for issue in data:
        difficulty = None
        for label in issue["labels"]:
            if label["name"].startswith("D-"):
                difficulty = label["name"]
                break
        cursor.execute("CREATE TABLE issues (id TEXT, name TEXT, difficulty TEXT, )")

# Assign issues to people balancing by difficulty and priority.
# Specifically it will balance by spreading work as evenly as possible with regards to 
# future issues. It doesn't care about maintaining overall balance, so wont assign more
# work to people if they did less in the past. -- By design.
# This will always try to make sure things get done by the milestone attached to them.
def assign(args, issues):
    # Sorted
    contributors = [
        "bitokn",
        "bwoodsy",
        "lauriesama",
        "ArtDynasty13",
        "GenericConfluent",
    ]

    # We care about issues that are OPEN. Find them.
    todo = list()
    for issue in issues:
        if issue["state"] == "OPEN":
            todo.append(issue)

    # Ok now we need to balance work across these. 
    # We only ever care about getting work done for 
    # the next milestone. 

    # The milestone with #1 is the same as and issue having "F-Checkpoint"
    milestone = None
    best = None
    for issue in todo:
        if best == None:
            if issue.get("milestone") is not None:
                current = issue["milestone"]["number"]
                current_due = datetime.fromisoformat(issue["milestone"]["dueOn"])
                best = current_due
                milestone = current
            elif "F-Checkpoint" in issue["labels"]:
                best = datetime.fromisoformat("2025-10-21T00:00:00Z")
                milestone = 1
        else:
            if issue.get("milestone") is not None:
                current = issue["milestone"]["number"]
                current_due = datetime.fromisoformat(issue["milestone"]["dueOn"])
                best = current_due
                milestone = current
            elif "F-Checkpoint" in issue["labels"]:
                best = datetime.fromisoformat("2025-10-21T00:00:00Z")
                milestone = 1
    # FIXME: We should now have the current milestone. Filter the issues.
    # And assign trying to keep work even.
    
    
if __name__ == "__main__":
    # Load issues.
    try:
        result = subprocess.run(
            ["gha", "issue", "-R", REPO, "list", "--limit", "1000", "--json", "id,title,labels,assignees,milestone,state"],
            capture_output=True,
            text=True,
            check=True
        )
        # Probably not signed in. Just forward the message.
        if result.returncode != 0:
            print(result.stdout)
            print(result.stderr)
            exit(1)

    except FileNotFoundError:
        print("Error: This script requires the github cli executable to be installed")
        exit(1)

    data = json.loads(result.stdout)

    # Then get the subcommand to figure out what we want to do.
    parser = argparse.ArgumentParser(prog="issue-mgmnt.py")
    command = parser.add_subparsers(dest="command")

    # Run an sql query on issues
    command.add_parser("query")
    # Randomly assign an even workload for up to the next milestone.
    command.add_parser("assign")

    args = parser.parse_args()

    # Load an object with the name of the parser from the current module.
    # Hopefully its a function.
    try:
        self_module = sys.modules[__name__]
        vars(self_module)[args.command](args, data)
    except KeyError:
        print(f"Error: The subcommand {args.command} is not implemented")

