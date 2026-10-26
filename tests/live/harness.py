#!/usr/bin/env python3
"""Isolated live checks. `prepare` and `self-test` never launch a server."""

import argparse
import hashlib
import ipaddress
import json
import math
import os
from pathlib import Path
import queue
import re
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import threading
import time
import unittest
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tests/live"
STATE = ROOT / "build/live-validation"
ASSETS = STATE / "assets"
JAVA = Path("/usr/lib/jvm/java-21-openjdk/bin")
RUNTIME = ROOT / "dist/build/libs/CommandBridge-3.3.7-all.jar"
PAPER_HOME = Path("/home/consti/servers/serverback")
VELOCITY = Path("/home/consti/servers/dev/velocity/velocity-3.5.0-SNAPSHOT-601.jar")
COMMANDAPI_URL = "https://cdn.modrinth.com/data/ExxvCi0y/versions/Vo4ECzIC/CommandAPI-11.2.0-Velocity.jar"
COMMANDAPI_SHA512 = "63cb5b4768bed9acb246ee3d989e5ed786162858dfdcd191520b9d3bf18381a698b5c1c4bfdcbb4d21524efa7975ecfdb78aa6de122c51455cc60d2143da8cf7"
ANSI = re.compile(r"\x1b\[[0-?]*[ -/]*[@-~]")
URL = re.compile(r"(?:https?|wss?)://[^\s<>\"\x1b]+")


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    path.chmod(0o600)


def redact(text, hidden=()):
    text = ANSI.sub("", text)
    for value in hidden:
        if value:
            text = text.replace(value, "[REDACTED]")
    text = URL.sub("[URL REDACTED]", text)
    text = re.sub(r"sha256/[A-Za-z0-9+/=_-]+", "[PIN REDACTED]", text)
    text = re.sub(r"(?i)((?:secret|password|auth[-_ ]?hash|hmac|token)[\"']?\s*[:=]\s*)\S+", r"\1[REDACTED]", text)
    text = re.sub(r"(?i)((?:cb|cbc) (?:setup confirm|editor confirm|pair(?: accept)?) )\S+", r"\1[REDACTED]", text)
    return text


def digest(path, algorithm="sha256"):
    h = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def checked(command, *, cwd=ROOT, log=None, timeout=180):
    if log:
        with log.open("w") as output:
            result = subprocess.run(command, cwd=cwd, stdout=output, stderr=subprocess.STDOUT, timeout=timeout)
        assert result.returncode == 0, f"preparation failed; inspect {log.relative_to(ROOT)}"
    else:
        subprocess.run(command, cwd=cwd, check=True, timeout=timeout)


def compile_probe():
    assert RUNTIME.is_file(), "Build the parent-owned runtime JAR first (this harness never runs Gradle)"
    probe = ASSETS / "probe"
    classes = probe / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1"
    jars = sorted(p for p in cache.rglob("*.jar") if not p.name.endswith(("-sources.jar", "-javadoc.jar")))
    assert any(p.name.startswith("velocity-api-") for p in jars), "Velocity API not in Gradle dependency cache"
    classpath = os.pathsep.join(str(p) for p in [RUNTIME, *jars])
    args = ["-proc:none", "-implicit:none", "--release", "21", "-classpath", classpath,
            "-d", str(classes), str(SOURCE / "LiveProbe.java")]
    write(probe / "javac.args", "\n".join(json.dumps(arg) for arg in args))
    checked([str(JAVA / "javac"), "@" + str(probe / "javac.args")], log=probe / "compile.log")
    with zipfile.ZipFile(ASSETS / "live-probe.jar", "w") as archive:
        for path in classes.rglob("*.class"):
            archive.write(path, path.relative_to(classes))
        archive.writestr("velocity-plugin.json", json.dumps({
            "id": "liveprobe", "name": "LiveProbe", "version": "1", "main": "live.LiveProbe",
            "dependencies": [{"id": "commandbridge", "optional": False}],
        }))


def prepare():
    ASSETS.mkdir(parents=True, exist_ok=True)
    STATE.chmod(0o700)
    copies = [(PAPER_HOME / "paper-1.21.8.jar", ASSETS / "paper.jar"),
              (VELOCITY, ASSETS / "velocity.jar"),
              (PAPER_HOME / "plugins/CommandAPI-11.1.0-Paper.jar", ASSETS / "commandapi-paper.jar")]
    for source, destination in copies:
        assert source.is_file(), f"Missing authorized asset {source}"
        shutil.copy2(source, destination)
    eula = Path("/home/consti/servers/dev/paper/eula.txt").read_text()
    assert re.search(r"(?m)^eula=true\s*$", eula), "No existing EULA acceptance found"
    write(ASSETS / "eula.txt", "eula=true\n")
    for name in ("cache", "libraries", "versions"):
        shutil.copytree(PAPER_HOME / name, ASSETS / "paper-cache" / name, dirs_exist_ok=True)
    commandapi = ASSETS / "commandapi-velocity.jar"
    if not commandapi.exists() or digest(commandapi, "sha512") != COMMANDAPI_SHA512:
        download = commandapi.with_suffix(".download")
        try:
            with urllib.request.urlopen(COMMANDAPI_URL, timeout=60) as response, download.open("wb") as output:
                shutil.copyfileobj(response, output)
            assert digest(download, "sha512") == COMMANDAPI_SHA512, "CommandAPI checksum mismatch"
            download.replace(commandapi)
        finally:
            download.unlink(missing_ok=True)
    node = STATE / "node"
    node.mkdir(exist_ok=True)
    shutil.copy2(SOURCE / "package.json", node / "package.json")
    checked(["npm", "ci" if (node / "package-lock.json").exists() else "install", "--ignore-scripts", "--no-audit", "--no-fund", "--cache", str(STATE / "npm-cache")],
            cwd=node, log=STATE / "npm-install.log", timeout=300)
    compile_probe()
    write(STATE / "assets.json", json.dumps({p.name: digest(p) for p in ASSETS.glob("*.jar")}, indent=2) + "\n")
    print("Prepared assets and client dependencies; no servers started.")


def endpoint_config(transport, bridge_port, redis_port):
    endpoint = "REDIS" if transport == "redis" else "WEBSOCKET"
    tls = "PLAIN" if transport == "plain" else "TOFU"
    redis = f"    host: 127.0.0.1\n    port: {redis_port}\n    username: ''\n    password: ''\n"
    primary = f"""act-as-client: false
server-id: primary
endpoint-type: {endpoint}
endpoints:
  websocket:
    bind-host: 127.0.0.1
    bind-port: {bridge_port}
  redis:
{redis}security:
  tls-mode: {tls}
  keystore-path: ''
  keystore-password: ''
  keystore-type: PKCS12
timeouts:
  register-timeout: 10
  ping-timeout: 10
tasks:
  expire-after: 60
debug: false
"""
    def client(name, secret):
        return f"""client-id: {name}
endpoint-type: {endpoint}
endpoints:
  websocket:
    host: 127.0.0.1
    port: {bridge_port}
  redis:
{redis}security:
  tls-mode: {tls}
  tls-pin: ''
  secret: '{secret}'
timeouts:
  auth-timeout: 10
  reconnect-timeout: 60
  reconnect-interval: 2
debug: false
"""
    return primary, client


def velocity_config(port, paper_port):
    return f"""config-version = "2.8"
bind = "127.0.0.1:{port}"
motd = "CommandBridge isolated live validation"
show-max-players = 10
online-mode = false
force-key-authentication = false
player-info-forwarding-mode = "NONE"
forwarding-secret-file = "forwarding.secret"
ping-passthrough = "DISABLED"
enable-player-address-logging = false
[servers]
paper = "127.0.0.1:{paper_port}"
try = ["paper"]
[forced-hosts]
[advanced]
login-ratelimit = 0
connection-timeout = 10000
read-timeout = 30000
log-command-executions = false
[query]
enabled = false
"""


def script(name, register_id="primary", register_location="VELOCITY", target="paper", permission=False, replacement=False):
    location = "VELOCITY" if target == "secondary" else "BACKEND"
    command = "liveprobe mark" if target == "secondary" else "say"
    suffix = "_RELOADED" if replacement else ""
    return f"""version: 4
name: {name}
description: Isolated non-destructive live validation
enabled: true
aliases: []
permissions:
  enabled: {str(permission).lower()}
  silent: false
register:
  - id: {register_id}
    location: {register_location}
defaults:
  run-as: CONSOLE
  execute:
    - id: {target}
      location: {location}
  server:
    target-required: false
    schedule-online: false
  delay: 0s
  cooldown: 0s
args:
  - name: marker
    required: true
    type: STRING
    suggestions: []
commands:
  - command: '{command} ${{marker}}{suffix}'
"""


class Process:
    def __init__(self, name, cwd, command, hidden=()):
        self.name, self.cwd, self.hidden = name, cwd, hidden
        self.lines = []
        self.events = []
        self.output = (cwd / "console.redacted.log").open("w")
        self.proc = subprocess.Popen(command, cwd=cwd, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, text=True, bufsize=1, start_new_session=True)
        self.thread = threading.Thread(target=self.read, daemon=True)
        self.thread.start()

    def read(self):
        for line in self.proc.stdout:
            clean = ANSI.sub("", line)
            self.events.append((time.monotonic(), clean))
            self.lines.append(clean)
            self.output.write(redact(line, self.hidden))
            self.output.flush()

    def send(self, command):
        assert "\n" not in command and "\r" not in command
        assert self.proc.poll() is None, f"{self.name} exited; inspect redacted console log"
        self.proc.stdin.write(command + "\n")
        self.proc.stdin.flush()

    def wait(self, pattern, timeout=45, after=0):
        end = time.monotonic() + timeout
        while time.monotonic() < end:
            for line in self.lines[after:]:
                if re.search(pattern, line):
                    return line
            assert self.proc.poll() is None, f"{self.name} exited; inspect redacted console log"
            time.sleep(0.1)
        raise AssertionError(f"{self.name}: timed out waiting for expected output (see redacted console log)")

    def close(self):
        try:
            if self.proc.poll() is None:
                self.send("stop" if self.name == "paper" else "shutdown")
                self.proc.wait(timeout=40)
        except (OSError, AssertionError, subprocess.TimeoutExpired):
            if self.proc.poll() is None:
                os.killpg(self.proc.pid, signal.SIGTERM)
                try:
                    self.proc.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(self.proc.pid, signal.SIGKILL)
                    self.proc.wait(timeout=10)
        finally:
            self.thread.join(timeout=5)
            self.output.close()
            self.proc.stdin.close()
            self.proc.stdout.close()
            # Server log appenders bypass our stdout capture; redact their private logs after shutdown too.
            for log in self.cwd.rglob("*.log"):
                write(log, redact(log.read_text(errors="replace"), self.hidden))


class Clients:
    def __init__(self, run_dir):
        self.stderr = (run_dir / "clients.redacted.log").open("w")
        self.proc = subprocess.Popen(["node", str(SOURCE / "clients.mjs"), str(STATE / "node/package.json")],
                                     cwd=run_dir, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=self.stderr, text=True, bufsize=1, start_new_session=True)
        self.queue = queue.Queue()
        self.thread = threading.Thread(target=self.read, daemon=True)
        self.thread.start()
        self.serial = 0

    def read(self):
        for line in self.proc.stdout:
            self.queue.put(line)

    def send(self, op, **data):
        self.serial += 1
        self.proc.stdin.write(json.dumps({"id": self.serial, "op": op, **data}) + "\n")
        self.proc.stdin.flush()
        try:
            response = json.loads(self.queue.get(timeout=40))
        except queue.Empty:
            raise AssertionError("Client driver timed out") from None
        assert response.get("id") == self.serial and response.get("ok"), "Client operation failed; inspect client log"
        return response

    def close(self):
        try:
            if self.proc.poll() is None:
                self.send("quit")
                self.proc.stdin.close()
                self.proc.wait(timeout=5)
        except (OSError, AssertionError, subprocess.TimeoutExpired):
            if self.proc.poll() is None:
                os.killpg(self.proc.pid, signal.SIGKILL)
                self.proc.wait(timeout=5)
        finally:
            self.thread.join(timeout=5)
            self.stderr.close()
            self.proc.stdout.close()
            write(Path(self.stderr.name), redact(Path(self.stderr.name).read_text(errors="replace")))


def authorize():
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    assert os.environ.get("CB_LIVE_AFTER_COMMIT") == head, "Live execution requires CB_LIVE_AFTER_COMMIT=<current committed fix SHA>, set only by parent after fix20 commit"
    result = subprocess.run(["git", "diff", "--quiet", "HEAD", "--", "api", "core", "backends", "velocity", "dist"], cwd=ROOT)
    assert result.returncode == 0, "Production Java/modules changed since the authorization commit"
    return head


def assert_loopback(process):
    inodes = set()
    for descriptor in Path(f"/proc/{process.proc.pid}/fd").iterdir():
        try:
            target = descriptor.readlink().as_posix()
            if target.startswith("socket:["):
                inodes.add(target[8:-1])
        except FileNotFoundError:
            pass
    listeners = 0
    for filename, family in (("tcp", socket.AF_INET), ("tcp6", socket.AF_INET6)):
        for line in Path("/proc/net/" + filename).read_text().splitlines()[1:]:
            fields = line.split()
            if fields[3] != "0A" or fields[9] not in inodes:
                continue
            raw = bytes.fromhex(fields[1].split(":")[0])
            raw = b"".join(raw[i:i + 4][::-1] for i in range(0, len(raw), 4))
            address = ipaddress.ip_address(socket.inet_ntop(family, raw))
            if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped:
                address = address.ipv4_mapped
            assert address.is_loopback, f"{process.name} opened a non-loopback listener"
            listeners += 1
    assert listeners, f"{process.name} has no listening sockets"


def load_statistics(sent, arrivals, dispatched, elapsed):
    latencies = sorted((times[0] - sent[marker][1]) * 1000 for marker, times in arrivals.items()
                       if marker in sent and times)
    def percentile(fraction):
        return round(latencies[min(len(latencies) - 1, math.ceil(len(latencies) * fraction) - 1)], 2) if latencies else None
    return {
        "sent": len(sent), "executed": sum(len(times) for times in arrivals.values()),
        "missing": sum(not arrivals.get(marker) for marker in sent),
        "duplicates": sum(max(0, len(times) - 1) for times in arrivals.values()),
        "dispatch_failures": sum(dispatched.get(marker) != [True] for marker in sent),
        "elapsed_seconds": round(elapsed, 3),
        "completed_per_second": round(len(latencies) / elapsed, 2),
        "latency_ms": {"p50": percentile(.5), "p95": percentile(.95), "p99": percentile(.99),
                       "max": round(latencies[-1], 2) if latencies else None},
    }


def sustained_load(processes, run_dir, seconds):
    rate = 20
    prefix = "CBLIVE_" + secrets.token_hex(8) + "_LOAD_"
    token = re.escape(prefix) + r"\d+"
    received = {"paper": re.compile(r"\[Server\] (" + token + r")\s*$"),
                "secondary": re.compile(r"LIVE_MARK (" + token + r") sender=console")}
    dispatched_pattern = re.compile(r"LIVE_API_DISPATCH (" + token + r") ok=(true|false)")
    offsets = {name: len(process.events) for name, process in processes.items()}
    sent, arrivals, dispatched = {}, {}, {}
    unexpected = wrong_target = server_errors = 0
    memory = {}

    def sample_memory():
        for name, process in processes.items():
            assert process.proc.poll() is None, f"{name} exited during load"
            status = Path(f"/proc/{process.proc.pid}/status").read_text()
            rss = int(re.search(r"(?m)^VmRSS:\s+(\d+)", status).group(1))
            item = memory.setdefault(name, {"start_kib": rss, "peak_kib": rss, "end_kib": rss})
            item["peak_kib"] = max(item["peak_kib"], rss)
            item["end_kib"] = rss

    def collect():
        nonlocal unexpected, wrong_target, server_errors
        for name, process in processes.items():
            batch = process.events[offsets[name]:]
            offsets[name] += len(batch)
            for at, line in batch:
                if re.search(r"\bERROR\b|OutOfMemoryError|Exception in thread", line):
                    server_errors += 1
                match = dispatched_pattern.search(line) if name == "primary" else received[name].search(line)
                if not match:
                    continue
                marker = match.group(1)
                if marker not in sent:
                    unexpected += 1
                elif name == "primary":
                    dispatched.setdefault(marker, []).append(match.group(2) == "true")
                elif sent[marker][0] != name:
                    wrong_target += 1
                else:
                    arrivals.setdefault(marker, []).append(at)

    sample_memory()
    started = time.monotonic()
    next_sample = started + 5
    for index in range(seconds * rate):
        time.sleep(max(0, started + index / rate - time.monotonic()))
        assert time.monotonic() - started - index / rate < 5, "Load sender fell more than five seconds behind schedule"
        target = "paper" if index % 2 == 0 else "secondary"
        marker = prefix + str(index)
        sent[marker] = (target, time.monotonic())
        processes["primary"].send(f"liveprobe api {target} {marker}")
        collect()
        if time.monotonic() >= next_sample:
            sample_memory()
            next_sample = time.monotonic() + 5
    time.sleep(max(0, started + seconds - time.monotonic()))
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        collect()
        if len(arrivals) == len(sent) and len(dispatched) == len(sent):
            break
        time.sleep(.05)
    time.sleep(1)
    collect()
    sample_memory()
    stats = load_statistics(sent, arrivals, dispatched, time.monotonic() - started)
    stats.update({"duration_seconds": seconds, "target_commands_per_second": rate,
                  "wrong_target": wrong_target, "unexpected": unexpected, "server_errors": server_errors,
                  "rss": memory, "measurement": "console send to destination log receipt, including tick scheduling"})
    write(run_dir / "load.json", json.dumps(stats, indent=2) + "\n")
    print("Load result: " + json.dumps(stats), flush=True)
    assert not any(stats[key] for key in ["missing", "duplicates", "dispatch_failures", "wrong_target", "unexpected", "server_errors"]), "Sustained delivery check failed; inspect load.json"
    assert stats["executed"] == stats["sent"] == seconds * rate
    return stats


def relay_and_hold(processes, run_dir, args):
    if args.relay_smoke:
        links = {}
        for feature in ("editor", "setup"):
            start = len(processes["primary"].lines)
            processes["primary"].send("cb " + feature)
            if feature == "setup":
                # Our explicit loopback config is treated as configured, so the CLI requires its second invocation.
                processes["primary"].wait("Run /cb setup again within a minute", after=start)
                processes["primary"].send("cb setup")
            line = processes["primary"].wait(r"http://127\.0\.0\.1:8080/" + feature + r"/#", after=start)
            links[feature] = URL.search(line).group()
        write(run_dir / "session-links.private.json", json.dumps(links, indent=2) + "\n")
        print("PASS actual Java setup/editor relay sessions opened; browser/pairing checks remain parent-owned", flush=True)
    if args.hold:
        print(f"Holding {args.hold}s for parent checks; control: {run_dir.relative_to(ROOT)}/commands.jsonl", flush=True)
        control = run_dir / "commands.jsonl"
        write(control, "")
        end, offset = time.monotonic() + args.hold, 0
        while time.monotonic() < end:
            with control.open() as stream:
                stream.seek(offset)
                while True:
                    position = stream.tell()
                    line = stream.readline()
                    if not line.endswith("\n"):
                        offset = position
                        break
                    request = json.loads(line)
                    assert request["target"] in processes
                    processes[request["target"]].send(request["command"])
            assert all(p.proc.poll() is None for p in processes.values()), "Server exited during parent hold"
            time.sleep(0.2)


def run(args):
    head = authorize()
    assert (STATE / "node/node_modules/mineflayer").is_dir(), "Run prepare first"
    for name in ("paper.jar", "velocity.jar", "commandapi-paper.jar", "commandapi-velocity.jar", "live-probe.jar"):
        assert (ASSETS / name).is_file(), "Run prepare first"
    assert digest(ASSETS / "commandapi-velocity.jar", "sha512") == COMMANDAPI_SHA512
    redis_port = int(os.environ.get("CB_LIVE_REDIS_PORT", "6379"))
    assert 0 < redis_port < 65536
    if args.transport == "redis":
        assert os.environ.get("CB_LIVE_REDIS_ISOLATED") == "1", "Parent must provide a dedicated loopback Redis; shared channels cannot safely use an existing service"
        with socket.create_connection(("127.0.0.1", redis_port), timeout=3) as redis:
            redis.sendall(b"*1\r\n$4\r\nPING\r\n")
            assert redis.recv(128).startswith(b"+PONG"), "Dedicated Redis not ready"
    compile_probe()
    run_dir = STATE / (time.strftime("%Y%m%d-%H%M%S") + "-" + args.transport + "-" + secrets.token_hex(3))
    run_dir.mkdir(mode=0o700)
    secret = secrets.token_urlsafe(32)
    processes, clients, cases, sockets = {}, None, [], []
    summary = {"commit": head, "transport": args.transport, "runtime_sha256": digest(RUNTIME),
               "mode": "fresh-paper-parent-smoke" if args.fresh_paper else "automated", "cases": cases, "passed": False}

    def passed(name):
        cases.append(name)
        print("PASS " + name, flush=True)

    def marker(label):
        return "CBLIVE_" + secrets.token_hex(8) + "_" + label

    def observed(server, value, pattern=None):
        expression = pattern or (r"\[Server\] " + re.escape(value) + r"\s*$" if server == "paper"
                                 else re.escape("LIVE_MARK " + value + " sender=console"))
        processes[server].wait(expression)
        time.sleep(0.5)
        count = sum(bool(re.search(expression, line)) for line in processes[server].lines)
        assert count == 1, f"{server}: expected exactly one marker execution, found {count}"

    try:
        for _ in range(4):
            sock = socket.socket()
            sock.bind(("127.0.0.1", 0))
            sockets.append(sock)
        ports = dict(zip(("paper", "primary", "secondary", "bridge"), [s.getsockname()[1] for s in sockets]))
        write(run_dir / "run.json", json.dumps({**summary, "ports": ports}, indent=2) + "\n")
        for name in ("paper", "primary", "secondary"):
            server_dir = run_dir / name
            plugin = server_dir / "plugins/commandbridge"
            plugin.mkdir(parents=True)
            shutil.copy2(RUNTIME, server_dir / "plugins/CommandBridge.jar")
            shutil.copy2(ASSETS / ("commandapi-paper.jar" if name == "paper" else "commandapi-velocity.jar"), server_dir / "plugins/CommandAPI.jar")
            write(server_dir / "plugins/bStats/config.yml", "enabled: false\nlogFailedRequests: false\nlogSentData: false\nlogResponseStatusText: false\n")
            if name != "paper":
                shutil.copy2(ASSETS / "live-probe.jar", server_dir / "plugins/LiveProbe.jar")
                write(server_dir / "velocity.toml", velocity_config(ports[name], ports["paper"]))
                write(server_dir / "forwarding.secret", secrets.token_urlsafe(32))
                shutil.copy2(ASSETS / "velocity.jar", server_dir / "server.jar")
            else:
                shutil.copy2(ASSETS / "paper.jar", server_dir / "server.jar")
                shutil.copy2(ASSETS / "eula.txt", server_dir / "eula.txt")
                for cache in ("cache", "libraries", "versions"):
                    shutil.copytree(ASSETS / "paper-cache" / cache, server_dir / cache)
                write(server_dir / "server.properties", f"""server-ip=127.0.0.1
server-port={ports['paper']}
online-mode=false
enforce-secure-profile=false
enable-rcon=false
enable-query=false
level-name=live-world
level-type=minecraft:flat
generator-settings={{"layers":[{{"block":"minecraft:bedrock","height":1}}],"biome":"minecraft:plains"}}
generate-structures=false
spawn-protection=0
view-distance=2
simulation-distance=2
max-players=10
motd=CommandBridge isolated live validation
pause-when-empty-seconds=-1
""")
        primary_cfg, client_cfg = endpoint_config(args.transport, ports["bridge"], redis_port)
        write(run_dir / "primary/plugins/commandbridge/config.yml", primary_cfg)
        write(run_dir / "primary/plugins/commandbridge/secret.key", secret + "\n")
        write(run_dir / "secondary/plugins/commandbridge/config.yml", "act-as-client: true\n" + primary_cfg.split("\n", 1)[1].replace("server-id: primary", "server-id: secondary"))
        write(run_dir / "secondary/plugins/commandbridge/client.yml", client_cfg("secondary", secret))
        if not args.fresh_paper:
            write(run_dir / "paper/plugins/commandbridge/config.yml", client_cfg("paper", secret))
        scripts = run_dir / "primary/plugins/commandbridge/scripts"
        for name, body in {
            "liveproxy": script("liveproxy"),
            "liveremote": script("liveremote", "paper", "BACKEND", "secondary"),
            "livesecondary": script("livesecondary", "secondary"),
            "liveguard": script("liveguard", permission=True),
        }.items():
            write(scripts / (name + ".yml"), body)
        # All artifacts and config exist before the first process starts.
        for sock in sockets:
            sock.close()
        sockets.clear()
        relay = []
        if args.relay_smoke or args.hold:
            relay = ["-Dcommandbridge.relay.url=ws://127.0.0.1:8787/", "-Dcommandbridge.web.url=http://127.0.0.1:8080/"]
        for name in ("primary", "paper", "secondary"):
            command = [str(JAVA / "java"), "-Xms128M", "-Xmx" + ("768M" if name == "paper" else "384M"),
                       "-XX:ActiveProcessorCount=2", "-Dcb.ansi=false", "-Dterminal.jline=false", "-Dterminal.ansi=false", *relay,
                       "-jar", "server.jar"]
            if name == "paper":
                command.append("nogui")
            processes[name] = Process(name, run_dir / name, command, [secret])
            processes[name].wait(r"Done \(", timeout=150)
            if name != "paper":
                processes[name].wait("LIVE_PROBE_READY")
            assert_loopback(processes[name])
        passed("all owned Java listening sockets verified loopback-only via procfs")
        if args.fresh_paper:
            processes["paper"].wait("isn't paired with a proxy yet")
            passed("new isolated Paper generated default unpaired config; no preexisting worlds/configs used")
            relay_and_hold(processes, run_dir, args)
            cases.append("Java setup/editor relay sessions opened; parent checks not included in automated result")
            summary["passed"] = True
            return
        for name in ("paper", "secondary"):
            processes[name].wait("Authenticated successfully", timeout=45)
        processes["primary"].send("liveprobe status")
        processes["primary"].wait(r"LIVE_STATUS id=primary active=true servers=\[paper, secondary\]")
        passed("authenticated Paper and secondary Velocity; public API server inventory")
        if args.transport == "tls":
            assert (run_dir / "primary/plugins/commandbridge/data/keystore.p12").is_file(), "TOFU keystore absent"
            # A real TLS handshake, with verification disabled only for this generated self-signed test cert.
            import ssl
            context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
            context.check_hostname = False
            context.verify_mode = ssl.CERT_NONE
            with socket.create_connection(("127.0.0.1", ports["bridge"]), timeout=5) as connection:
                with context.wrap_socket(connection, server_hostname="localhost") as tls:
                    assert tls.version() in ("TLSv1.2", "TLSv1.3")
            passed("real TLS handshake plus authenticated Java bridge connections")
        clients = Clients(run_dir)
        clients.send("connect", name="CBAllowed", port=ports["primary"])
        clients.send("connect", name="CBDenied", port=ports["primary"])
        clients.send("connect", name="CBSecondary", port=ports["secondary"])
        passed("three real offline Minecraft clients spawned through both proxies")
        for label, user, command, target in [
            ("proxy", "CBAllowed", "liveproxy", "paper"),
            ("backend", "CBAllowed", "liveremote", "secondary"),
            ("secondary", "CBSecondary", "livesecondary", "paper"),
            ("allowed", "CBAllowed", "liveguard", "paper"),
        ]:
            value = marker(label)
            clients.send("chat", name=user, command=f"/{command} {value}")
            observed(target, value)
            passed(f"{label} player script route executed exactly once")
        value = marker("denied")
        clients.send("chat", name="CBDenied", command=f"/liveguard {value}", expect="You do not have permission")
        time.sleep(1)
        assert not any(value in line for line in processes["paper"].lines), "Denied player executed command"
        passed("permission denial returned to player and no backend execution")
        for target in ("paper", "secondary"):
            value = marker("api_" + target)
            processes["primary"].send(f"liveprobe api {target} {value}")
            processes["primary"].wait(re.escape(f"LIVE_API_DISPATCH {value} ok=true"))
            observed(target, value)
            passed(f"public CommandPayload API reached {target} exactly once")
        for target in ("paper", "secondary"):
            start = len(processes["primary"].lines)
            processes["primary"].send("cb client ping " + target)
            processes["primary"].wait(target, after=start)
            processes["primary"].wait(r"\d+ms.*(?:Excellent|Good|Poor)", after=start)
            processes["primary"].wait(r"Responded", after=start)
            assert not any("FAILED" in line for line in processes["primary"].lines[start:]), "Client ping failed"
            passed("builtin client ping " + target + " completed")
        for command, expected in (
            ("cb help", r"Shows this help menu"),
            ("cb info", r"Uptime"),
            ("cb script list", r"liveproxy"),
            ("cb task list", r"No pending scheduled tasks"),
            ("cb client list", r"secondary"),
            ("cb client players paper", r"CBAllowed"),
            ("cb config reload", r"Config reloaded"),
        ):
            start = len(processes["primary"].lines)
            processes["primary"].send(command)
            processes["primary"].wait(expected, after=start)
            passed("builtin " + command[3:] + " returned expected output")
        write(scripts / "liveproxy.yml", script("liveproxy", replacement=True))
        start = len(processes["primary"].lines)
        processes["primary"].send("cb reload")
        processes["primary"].wait(r"Config and scripts reloaded", after=start)
        value = marker("reload")
        clients.send("chat", name="CBAllowed", command=f"/liveproxy {value}")
        observed("paper", value + "_RELOADED")
        assert sum(value in line for line in processes["paper"].lines) == 1, "Reload dispatched old and new handlers"
        passed("reload replaced script behavior without duplicate execution")
        start = len(processes["paper"].lines)
        processes["paper"].send("cbc reconnect")
        processes["paper"].wait("Authenticated successfully", after=start)
        value = marker("reconnect")
        clients.send("chat", name="CBAllowed", command=f"/liveproxy {value}")
        observed("paper", value + "_RELOADED")
        passed("builtin reconnect reauthenticated and command delivery resumed")
        if args.load_seconds:
            summary["load"] = sustained_load(processes, run_dir, args.load_seconds)
            passed("sustained API command delivery without loss, duplicates or server errors")
        relay_and_hold(processes, run_dir, args)
        if args.relay_smoke:
            cases.append("Java setup/editor relay sessions opened; parent checks not included in automated result")
        summary["passed"] = True
    finally:
        for sock in sockets:
            sock.close()
        cleanup_errors = []
        for child in ([clients] if clients else []) + list(reversed(list(processes.values()))):
            try:
                child.close()
            except Exception as error:
                cleanup_errors.append(type(error).__name__)
        if cleanup_errors:
            summary["passed"] = False
            summary["cleanup_errors"] = cleanup_errors
        write(run_dir / "result.json", json.dumps(summary, indent=2) + "\n")
        print(f"Results: {run_dir.relative_to(ROOT)}/result.json", flush=True)
        assert not cleanup_errors, "A child process cleanup failed; inspect result.json and owned PIDs"


class SelfTest(unittest.TestCase):
    def test_load_statistics_detects_missing_duplicate_and_failed_dispatch(self):
        sent = {"a": ("paper", 1), "b": ("secondary", 2)}
        result = load_statistics(sent, {"a": [1.1, 1.2]}, {"a": [True], "b": [False]}, 3)
        self.assertEqual(result["missing"], 1)
        self.assertEqual(result["duplicates"], 1)
        self.assertEqual(result["dispatch_failures"], 1)
        self.assertEqual(result["latency_ms"]["p95"], 100)
        self.assertIsNone(load_statistics({}, {}, {}, 1)["latency_ms"]["p99"])

    def test_redaction(self):
        output = redact("\x1b[31mhttps://local/editor/#id:SECRET sha256/abc123 secret: hidden cb editor confirm abcd\x1b[0m", ["SECRET"])
        for value in ("SECRET", "abc123", "hidden", "abcd", "\x1b", "https://"):
            self.assertNotIn(value, output)

    def test_loopback_and_script(self):
        primary, client = endpoint_config("tls", 12345, 12346)
        self.assertIn("bind-host: 127.0.0.1", primary)
        self.assertIn("tls-mode: TOFU", client("paper", "test-secret"))
        self.assertIn("127.0.0.1:12345", velocity_config(12345, 12346))
        self.assertIn("say ${marker}_RELOADED", script("liveproxy", replacement=True))
        self.assertNotIn("0.0.0.0", primary + velocity_config(12345, 12346))

    def test_authorization_rejects_without_parent(self):
        previous = os.environ.pop("CB_LIVE_AFTER_COMMIT", None)
        try:
            with self.assertRaises(AssertionError):
                authorize()
        finally:
            if previous is not None:
                os.environ["CB_LIVE_AFTER_COMMIT"] = previous

    def test_process_redacts_and_cleans_up(self):
        folder = STATE / "self-test"
        folder.mkdir(parents=True, exist_ok=True)
        process = Process("self-test", folder, [sys.executable, "-u", "-c",
                          "import sys; print('READY https://private/#secret hidden'); sys.stdin.readline()"], ["hidden"])
        try:
            process.wait("READY", timeout=5)
        finally:
            process.close()
        self.assertIsNotNone(process.proc.poll())
        self.assertTrue(process.events)
        self.assertLessEqual(process.events[0][0], time.monotonic())
        output = (folder / "console.redacted.log").read_text()
        self.assertNotIn("hidden", output)
        self.assertNotIn("#secret", output)

    def test_client_driver_without_network(self):
        if not (STATE / "node/node_modules/mineflayer").exists():
            self.skipTest("prepare has not installed test dependencies yet")
        result = subprocess.run(["node", str(SOURCE / "clients.mjs"), str(STATE / "node/package.json")],
                                input='{"id":1,"op":"unknown"}\n{"id":2,"op":"quit"}\n',
                                text=True, capture_output=True, timeout=15, cwd=STATE)
        self.assertEqual(result.returncode, 0)
        replies = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertFalse(replies[0]["ok"])
        self.assertTrue(replies[1]["closed"])


def interrupt(signum, frame):
    raise KeyboardInterrupt()


def main():
    os.umask(0o077)
    signal.signal(signal.SIGTERM, interrupt)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("prepare", "self-test", "run"))
    parser.add_argument("--transport", choices=("tls", "plain", "redis"), default="tls")
    parser.add_argument("--relay-smoke", action="store_true", help="Open Java sessions against existing local relay8787/wiki8080")
    parser.add_argument("--hold", type=int, default=0, help="Keep isolated servers up for parent control/browser checks, maximum 1800s")
    parser.add_argument("--fresh-paper", action="store_true", help="Start unpaired Paper for parent setup/pairing checks instead of the automated suite")
    parser.add_argument("--load-seconds", type=int, default=0, help="Sustain 20 benign API commands/second after functional checks, maximum 600s")
    args = parser.parse_args()
    assert 0 <= args.hold <= 1800
    assert 0 <= args.load_seconds <= 600
    assert not (args.fresh_paper and args.load_seconds), "Load requires the configured automated lab"
    assert not args.fresh_paper or (args.relay_smoke and args.hold), "Fresh Paper mode needs --relay-smoke and a bounded --hold"
    if args.action == "prepare":
        prepare()
    elif args.action == "self-test":
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(SelfTest)
        assert unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful()
    else:
        run(args)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("Interrupted; owned child processes cleaned up.", file=sys.stderr)
        sys.exit(130)
    except (AssertionError, OSError, subprocess.SubprocessError) as error:
        print(redact(str(error)), file=sys.stderr)
        sys.exit(1)
