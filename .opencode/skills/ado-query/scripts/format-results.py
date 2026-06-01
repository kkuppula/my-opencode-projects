#!/usr/bin/env python3
"""Format ADO work item JSON into Markdown reports.

Usage:
    cat items.json | python3 format-results.py --format table [--sort-by field] [--title "Title"]
    cat items.json | python3 format-results.py --format summary --group-by System.State [--title "Title"]
    cat items.json | python3 format-results.py --format velocity [--title "Title"]
    cat items.json | python3 format-results.py --format regressions [--title "Title"]
    cat items.json | python3 format-results.py --format bugs [--title "Title"]
    cat items.json | python3 format-results.py --format timeline [--title "Title"]
"""

import json
import sys
import argparse
from collections import Counter, defaultdict
from datetime import datetime


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--format", required=True, choices=["table", "summary", "velocity", "regressions", "bugs", "timeline"])
    p.add_argument("--group-by", dest="group_by")
    p.add_argument("--sort-by", dest="sort_by")
    p.add_argument("--title")
    return p.parse_args()


def get_field(item, field):
    for key in [field, field.replace(".", ".")]:
        if key in item:
            val = item[key]
            if isinstance(val, dict):
                return val.get("displayName", val.get("name", str(val)))
            return val
    return ""


def short_field(field_name):
    return field_name.split(".")[-1] if "." in field_name else field_name


def ado_url(item_id):
    return f"https://dev.azure.com/Blackboard-01/Learn/_workitems/edit/{item_id}"


def format_table(items, args):
    if not items:
        print("*No items found.*")
        return

    title = args.title or "Work Items"
    print(f"## {title}\n")
    print(f"**{len(items)} items**\n")

    fields = ["System.Id", "System.Title", "System.State", "System.WorkItemType"]
    extra = set()
    for item in items[:1]:
        for k in item:
            if k not in ("id", "url") and k not in fields:
                extra.add(k)
    fields.extend(sorted(extra))

    headers = ["ID", "Title", "State", "Type"] + [short_field(f) for f in sorted(extra)]
    print("| " + " | ".join(headers) + " |")
    print("| " + " | ".join(["---"] * len(headers)) + " |")

    if args.sort_by:
        items.sort(key=lambda x: str(get_field(x, args.sort_by)))

    for item in items:
        item_id = item.get("id", get_field(item, "System.Id"))
        title_val = str(get_field(item, "System.Title"))[:80]
        state = get_field(item, "System.State")
        wtype = get_field(item, "System.WorkItemType")
        row = [f"[{item_id}]({ado_url(item_id)})", title_val, str(state), str(wtype)]
        for f in sorted(extra):
            row.append(str(get_field(item, f))[:40])
        print("| " + " | ".join(row) + " |")


def format_summary(items, args):
    if not items:
        print("*No items found.*")
        return

    title = args.title or "Summary"
    group_field = args.group_by or "System.State"
    print(f"## {title}\n")
    print(f"**{len(items)} total items**, grouped by `{short_field(group_field)}`\n")

    groups = Counter()
    for item in items:
        val = get_field(item, group_field) or "(empty)"
        if isinstance(val, str) and "\\" in val:
            val = val.split("\\")[-1]
        groups[str(val)] += 1

    print(f"| {short_field(group_field)} | Count | % |")
    print("| --- | ---: | ---: |")
    for name, count in groups.most_common():
        pct = round(100 * count / len(items))
        print(f"| {name} | {count} | {pct}% |")


def format_velocity(items, args):
    if not items:
        print("*No items found.*")
        return

    title = args.title or "Velocity Report"
    print(f"## {title}\n")

    sprints = defaultdict(lambda: {"points": 0, "count": 0, "items": []})
    for item in items:
        iteration = get_field(item, "System.IterationPath") or "(no iteration)"
        if "\\" in str(iteration):
            iteration = str(iteration).split("\\")[-1]
        points = get_field(item, "Microsoft.VSTS.Scheduling.StoryPoints") or 0
        try:
            points = float(points)
        except (ValueError, TypeError):
            points = 0
        sprints[iteration]["points"] += points
        sprints[iteration]["count"] += 1
        sprints[iteration]["items"].append(item)

    print("| Sprint | Story Points | Items | Avg Points/Item |")
    print("| --- | ---: | ---: | ---: |")
    total_pts = 0
    total_items = 0
    for sprint_name in sorted(sprints.keys()):
        data = sprints[sprint_name]
        avg = round(data["points"] / data["count"], 1) if data["count"] else 0
        print(f"| {sprint_name} | {data['points']:.0f} | {data['count']} | {avg} |")
        total_pts += data["points"]
        total_items += data["count"]

    print(f"\n**Totals:** {total_pts:.0f} points across {total_items} items in {len(sprints)} sprints")
    if len(sprints) > 1:
        avg_velocity = round(total_pts / len(sprints), 1)
        print(f"**Average velocity:** {avg_velocity} points/sprint")


def format_regressions(items, args):
    if not items:
        print("*No regressions found.*")
        return

    title = args.title or "Regression Analysis"
    print(f"## {title}\n")
    print(f"**{len(items)} regressions**\n")

    by_severity = Counter()
    by_area = Counter()
    by_state = Counter()
    for item in items:
        sev = get_field(item, "Microsoft.VSTS.Common.Severity") or "(unset)"
        area = get_field(item, "System.AreaPath") or "(unset)"
        if "\\" in str(area):
            area = str(area).split("\\")[-1]
        state = get_field(item, "System.State") or "(unset)"
        by_severity[str(sev)] += 1
        by_area[str(area)] += 1
        by_state[str(state)] += 1

    print("### By Severity\n")
    print("| Severity | Count |")
    print("| --- | ---: |")
    for sev, count in sorted(by_severity.items()):
        print(f"| {sev} | {count} |")

    print("\n### By Area\n")
    print("| Area | Count |")
    print("| --- | ---: |")
    for area, count in by_area.most_common():
        print(f"| {area} | {count} |")

    print("\n### By State\n")
    print("| State | Count |")
    print("| --- | ---: |")
    for state, count in by_state.most_common():
        print(f"| {state} | {count} |")

    print("\n### Details\n")
    print("| ID | Title | Severity | State | Area |")
    print("| --- | --- | --- | --- | --- |")
    for item in items:
        item_id = item.get("id", get_field(item, "System.Id"))
        t = str(get_field(item, "System.Title"))[:60]
        sev = get_field(item, "Microsoft.VSTS.Common.Severity") or "-"
        state = get_field(item, "System.State")
        area = get_field(item, "System.AreaPath") or "-"
        if "\\" in str(area):
            area = str(area).split("\\")[-1]
        print(f"| [{item_id}]({ado_url(item_id)}) | {t} | {sev} | {state} | {area} |")


def format_bugs(items, args):
    if not items:
        print("*No bugs found.*")
        return

    title = args.title or "Bug Analysis"
    print(f"## {title}\n")
    print(f"**{len(items)} bugs**\n")

    by_severity = Counter()
    by_priority = Counter()
    by_area = Counter()
    for item in items:
        sev = get_field(item, "Microsoft.VSTS.Common.Severity") or "(unset)"
        pri = get_field(item, "Microsoft.VSTS.Common.Priority") or "(unset)"
        area = get_field(item, "System.AreaPath") or "(unset)"
        if "\\" in str(area):
            area = str(area).split("\\")[-1]
        by_severity[str(sev)] += 1
        by_priority[str(pri)] += 1
        by_area[str(area)] += 1

    print("### By Severity\n")
    print("| Severity | Count | % |")
    print("| --- | ---: | ---: |")
    for sev, count in sorted(by_severity.items()):
        pct = round(100 * count / len(items))
        print(f"| {sev} | {count} | {pct}% |")

    print("\n### By Priority\n")
    print("| Priority | Count | % |")
    print("| --- | ---: | ---: |")
    for pri, count in sorted(by_priority.items()):
        pct = round(100 * count / len(items))
        print(f"| P{pri} | {count} | {pct}% |")

    print("\n### By Area (Top 10)\n")
    print("| Area | Count |")
    print("| --- | ---: |")
    for area, count in by_area.most_common(10):
        print(f"| {area} | {count} |")

    print("\n### Details\n")
    print("| ID | Title | Severity | Priority | State |")
    print("| --- | --- | --- | --- | --- |")
    for item in items:
        item_id = item.get("id", get_field(item, "System.Id"))
        t = str(get_field(item, "System.Title"))[:60]
        sev = get_field(item, "Microsoft.VSTS.Common.Severity") or "-"
        pri = get_field(item, "Microsoft.VSTS.Common.Priority") or "-"
        state = get_field(item, "System.State")
        print(f"| [{item_id}]({ado_url(item_id)}) | {t} | {sev} | P{pri} | {state} |")


def format_timeline(items, args):
    if not items:
        print("*No items found.*")
        return

    title = args.title or "Timeline"
    print(f"## {title}\n")

    by_date = defaultdict(int)
    for item in items:
        created = get_field(item, "System.CreatedDate") or ""
        if created:
            try:
                dt = datetime.fromisoformat(str(created).replace("Z", "+00:00"))
                by_date[dt.strftime("%Y-%m-%d")] += 1
            except (ValueError, TypeError):
                pass

    if not by_date:
        print("*No date data available.*")
        return

    print(f"**{len(items)} items** from {min(by_date.keys())} to {max(by_date.keys())}\n")

    by_week = defaultdict(int)
    for date_str, count in sorted(by_date.items()):
        dt = datetime.strptime(date_str, "%Y-%m-%d")
        week_start = dt.strftime("%Y-W%V")
        by_week[week_start] += count

    print("| Week | Created | Cumulative |")
    print("| --- | ---: | ---: |")
    cumulative = 0
    for week, count in sorted(by_week.items()):
        cumulative += count
        bar = "+" * min(count, 30)
        print(f"| {week} | {count} {bar} | {cumulative} |")


FORMATTERS = {
    "table": format_table,
    "summary": format_summary,
    "velocity": format_velocity,
    "regressions": format_regressions,
    "bugs": format_bugs,
    "timeline": format_timeline,
}


def main():
    args = parse_args()
    items = json.load(sys.stdin)
    if not isinstance(items, list):
        print(f"Error: expected JSON array, got {type(items).__name__}", file=sys.stderr)
        sys.exit(1)
    FORMATTERS[args.format](items, args)


if __name__ == "__main__":
    main()
