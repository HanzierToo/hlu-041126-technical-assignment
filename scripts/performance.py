#!/usr/bin/env python3
"""Measure the search in an explicitly disposable, pre-migrated MySQL schema."""

import argparse
import re
import subprocess
import time


def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql", required=True, help="Path to mysql client")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--database", default="hlu_performance", help="Disposable hlu_performance schema")
    parser.add_argument("--login-path", help="Optional mysql_config_editor login path")
    parser.add_argument("--confirm-disposable", action="store_true")

    args = parser.parse_args()

    if not re.fullmatch(r"hlu_performance(?:_[A-Za-z0-9_]+)?", args.database):
        parser.error("Database must be named hlu_performance or start with hlu_performance_")

    if not args.confirm_disposable:
        parser.error("Use a disposable pre-migrated hlu_performance schema and pass --confirm-disposable")

    return args


def mysql_command(args):
    command = [args.mysql]

    if args.login_path:
        command.append("--login-path=" + args.login_path)

    command.extend([
        "--host=" + args.host,
        "--port=" + str(args.port),
        "--user=" + args.user,
        "--batch",
        "--raw",
        "--skip-column-names",
        args.database,
    ])
    return command


def run_sql(command, statement):
    result = subprocess.run(command, input=statement, capture_output=True, text=True, check=True)
    return result.stdout.strip()


def add_branches(command):
    branches = ",".join(
        f"({100 + i},'Performance Branch {i}',{1 + i % 2},1,0)"
        for i in range(100)
    )
    run_sql(command, "INSERT INTO branch(id,name,province_id,active,deleted) VALUES " + branches)


def add_stores(command, start):
    for offset in range(start, start + 20000, 1000):
        rows = ",".join(
            f"({10000 + i},'Performance Store {i}',{100 + i % 100},1,0)"
            for i in range(offset, offset + 1000)
        )
        run_sql(command, "INSERT INTO store(id,name,branch_id,active,deleted) VALUES " + rows)


def add_whitelist_entries(command):
    entries = ",".join(
        f"({10000 + i},UTC_TIMESTAMP(6))"
        for i in range(0, 20000, 201)
    )
    run_sql(command, "INSERT INTO whitelist_store(store_id,created_at) VALUES " + entries)


def measure_queries(command):
    where = """ FROM store s JOIN branch b ON b.id=s.branch_id JOIN province p ON p.id=b.province_id
        WHERE s.active=1 AND s.deleted=0 AND b.active=1 AND b.deleted=0
          AND p.active=1 AND p.deleted=0
          AND (p.id IN (1) OR EXISTS (SELECT 1 FROM whitelist_store w WHERE w.store_id=s.id))"""

    store_count = run_sql(command, "SELECT COUNT(*) FROM store")
    branch_count = run_sql(command, "SELECT COUNT(*) FROM branch")
    whitelist_count = run_sql(command, "SELECT COUNT(*) FROM whitelist_store")
    print(f"\\nDATASET stores={store_count} branches={branch_count} whitelist={whitelist_count}")

    queries = (
        ("page", "SELECT s.id,s.name,b.id,b.name,p.id,p.name" + where + " ORDER BY s.id LIMIT 25"),
        ("count", "SELECT COUNT(s.id)" + where),
    )

    for label, query in queries:
        for run in range(1, 4):
            started = time.perf_counter()
            plan = run_sql(command, "EXPLAIN ANALYZE " + query)
            elapsed = (time.perf_counter() - started) * 1000
            print(f"\\n{label} run {run}: client elapsed {elapsed:.1f} ms")
            print(plan)


def main():
    args = parse_arguments()
    command = mysql_command(args)

    has_original_data = (
        run_sql(command, "SELECT COUNT(*) FROM store") == "7"
        and run_sql(command, "SELECT COUNT(*) FROM branch") == "5"
    )
    if not has_original_data:
        raise SystemExit("Expected untouched fictional seed data; refusing to modify this schema.")

    print("MySQL:", run_sql(command, "SELECT VERSION()"))
    add_branches(command)

    for start in (0, 20000):
        add_stores(command, start)

        if start == 0:
            add_whitelist_entries(command)

        run_sql(command, "ANALYZE TABLE province,branch,store,whitelist_store")
        measure_queries(command)


if __name__ == "__main__":
    main()
