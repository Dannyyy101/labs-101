"""Generates the Grafana dashboard of the labs-101 backend (python3 generate.py backend.json)."""
import json
import sys

DS = {"type": "prometheus", "uid": "${datasource}"}
F = 'application="backend", namespace="$namespace", pod=~"$pod"'
K8S = 'namespace="$namespace", pod=~"labs-101-.*"'
SYNC = 'uri=~"/api/users/me/health/(samples|samples/delete|sync/complete)"'

panels = []
_id = 0
_y = 0


def nid():
    global _id
    _id += 1
    return _id


def row(title):
    global _y
    panels.append({"type": "row", "id": nid(), "title": title, "collapsed": False,
                   "gridPos": {"h": 1, "w": 24, "x": 0, "y": _y}, "panels": []})
    _y += 1


def targets(*exprs):
    return [{"datasource": DS, "expr": e, "legendFormat": l, "refId": chr(65 + i),
             **({"format": "table", "instant": True} if l == "__table" else {})}
            for i, (e, l) in enumerate(exprs)]


def stat(title, desc, x, w, expr, unit, thresholds, decimals=None):
    p = {"type": "stat", "id": nid(), "title": title, "description": desc, "datasource": DS,
         "gridPos": {"h": 4, "w": w, "x": x, "y": _y},
         "targets": targets((expr, "")),
         "options": {"reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
                     "colorMode": "background", "graphMode": "area", "textMode": "value"},
         "fieldConfig": {"defaults": {"unit": unit, "thresholds": {"mode": "absolute", "steps": thresholds}},
                         "overrides": []}}
    if decimals is not None:
        p["fieldConfig"]["defaults"]["decimals"] = decimals
    panels.append(p)


def ts(title, desc, x, w, exprs, unit, h=8, stack=False, overrides=None, minv=0):
    panels.append({
        "type": "timeseries", "id": nid(), "title": title, "description": desc, "datasource": DS,
        "gridPos": {"h": h, "w": w, "x": x, "y": _y},
        "targets": targets(*exprs),
        "options": {"legend": {"displayMode": "table", "placement": "bottom", "calcs": ["mean", "max", "lastNotNull"]},
                    "tooltip": {"mode": "multi", "sort": "desc"}},
        "fieldConfig": {"defaults": {"unit": unit, "min": minv,
                                     "custom": {"fillOpacity": 15 if stack else 5, "lineWidth": 1, "showPoints": "never",
                                                "stacking": {"mode": "normal" if stack else "none", "group": "A"}}},
                        "overrides": overrides or []}})


def table(title, desc, x, w, exprs, h=8):
    panels.append({
        "type": "table", "id": nid(), "title": title, "description": desc, "datasource": DS,
        "gridPos": {"h": h, "w": w, "x": x, "y": _y},
        "targets": [{"datasource": DS, "expr": e, "refId": chr(65 + i), "format": "table", "instant": True}
                    for i, (e, _) in enumerate(exprs)],
        "transformations": [
            {"id": "merge", "options": {}},
            {"id": "organize", "options": {
                "excludeByName": {"Time": True},
                "renameByName": {"Value #A": "req/s", "Value #B": "p95", "Value #C": "Fehler %"}}},
            {"id": "sortBy", "options": {"sort": [{"field": "req/s", "desc": True}]}}],
        "fieldConfig": {"defaults": {}, "overrides": [
            {"matcher": {"id": "byName", "options": "req/s"}, "properties": [{"id": "unit", "value": "reqps"}, {"id": "decimals", "value": 3}]},
            {"matcher": {"id": "byName", "options": "p95"}, "properties": [
                {"id": "unit", "value": "s"},
                {"id": "custom.cellOptions", "value": {"type": "color-background", "mode": "basic"}},
                {"id": "thresholds", "value": {"mode": "absolute", "steps": [
                    {"color": "green", "value": None}, {"color": "orange", "value": 0.5}, {"color": "red", "value": 2}]}}]},
            {"matcher": {"id": "byName", "options": "Fehler %"}, "properties": [
                {"id": "unit", "value": "percentunit"},
                {"id": "custom.cellOptions", "value": {"type": "color-text"}},
                {"id": "thresholds", "value": {"mode": "absolute", "steps": [
                    {"color": "green", "value": None}, {"color": "red", "value": 0.01}]}}]}]},
        "options": {"showHeader": True, "cellHeight": "sm"}})


def advance(h):
    global _y
    _y += h


GREEN = {"color": "green", "value": None}

# ---------------------------------------------------------------- overview
row("Überblick")
stat("Requests/s", "Alle Anfragen an das Backend, Durchschnitt der letzten 5 Minuten.", 0, 4,
     f'sum(rate(http_server_requests_seconds_count{{{F}}}[5m]))', "reqps", [GREEN], 2)
stat("Fehlerquote 5xx", "Anteil der Antworten mit Status 5xx. Ab 1 % orange, ab 5 % rot.", 4, 4,
     f'sum(rate(http_server_requests_seconds_count{{{F}, status=~"5.."}}[5m])) / sum(rate(http_server_requests_seconds_count{{{F}}}[5m])) or vector(0)',
     "percentunit", [GREEN, {"color": "orange", "value": 0.01}, {"color": "red", "value": 0.05}], 2)
stat("p95 Antwortzeit", "95 % aller Anfragen sind schneller als dieser Wert (ohne abgewiesene Tokens).", 8, 4,
     f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{{F}, uri!="UNKNOWN"}}[5m])))',
     "s", [GREEN, {"color": "orange", "value": 0.5}, {"color": "red", "value": 2}], 2)
stat("Heap", "Belegter Heap im Verhältnis zum Maximum.", 12, 4,
     f'sum(jvm_memory_used_bytes{{{F}, area="heap"}}) / sum(jvm_memory_max_bytes{{{F}, area="heap"}} > 0)',
     "percentunit", [GREEN, {"color": "orange", "value": 0.75}, {"color": "red", "value": 0.9}], 0)
stat("Pod-Neustarts (24h)", "Neustarts der labs-101-Container in den letzten 24 Stunden, z. B. wegen OOM oder fehlgeschlagener Probes.", 16, 4,
     f'sum(increase(kube_pod_container_status_restarts_total{{{K8S}}}[24h])) or vector(0)',
     "short", [GREEN, {"color": "red", "value": 1}], 0)
stat("Uptime", "Laufzeit des Backends seit dem letzten Start.", 20, 4,
     f'min(process_uptime_seconds{{{F}}})', "s", [{"color": "orange", "value": None}, {"color": "green", "value": 600}], 0)
advance(4)

# ---------------------------------------------------------------- http
row("HTTP")
ts("Requests/s nach Statusklasse", "2xx erfolgreich, 4xx Fehler des Clients (z. B. 401 ohne gültigen Token), 5xx Fehler im Backend.", 0, 12,
   [(f'sum by (status_class) (label_replace(rate(http_server_requests_seconds_count{{{F}}}[$__rate_interval]), "status_class", "${{1}}xx", "status", "(.).."))', "{{status_class}}")],
   "reqps", stack=True, overrides=[
       {"matcher": {"id": "byName", "options": n}, "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": c}}]}
       for n, c in [("2xx", "green"), ("3xx", "blue"), ("4xx", "orange"), ("5xx", "red")]])
ts("Antwortzeit p50 / p95 / p99", "Quantile über alle Endpunkte, abgewiesene Tokens ausgenommen.", 12, 12,
   [(f'histogram_quantile({q}, sum by (le) (rate(http_server_requests_seconds_bucket{{{F}, uri!="UNKNOWN"}}[$__rate_interval])))', f"p{int(q * 100)}")
    for q in (0.5, 0.95, 0.99)], "s")
advance(8)
table("Endpunkte", "Pro Endpunkt: Anfragen pro Sekunde, p95-Antwortzeit und Anteil der 5xx-Antworten, jeweils über den gewählten Zeitraum.", 0, 24, [
    (f'sum by (method, uri) (rate(http_server_requests_seconds_count{{{F}, uri!="UNKNOWN"}}[$__range]))', ""),
    (f'histogram_quantile(0.95, sum by (le, method, uri) (rate(http_server_requests_seconds_bucket{{{F}, uri!="UNKNOWN"}}[$__range])))', ""),
    (f'sum by (method, uri) (rate(http_server_requests_seconds_count{{{F}, uri!="UNKNOWN", status=~"5.."}}[$__range])) / sum by (method, uri) (rate(http_server_requests_seconds_count{{{F}, uri!="UNKNOWN"}}[$__range]))', ""),
], h=10)
advance(10)
ts("Abgewiesene Anfragen (401/403)", "Fehlende oder abgelaufene Tokens. Ein Anstieg nach einem Deploy deutet auf ein Problem mit Zitadel oder dem Issuer hin. Die Gründe stehen im Log des Backends.", 0, 12,
   [(f'sum by (status) (rate(http_server_requests_seconds_count{{{F}, status=~"401|403"}}[$__rate_interval]))', "{{status}}")], "reqps")
ts("Langsamste Endpunkte (p95)", "Die fünf Endpunkte mit der höchsten p95-Antwortzeit.", 12, 12,
   [(f'topk(5, histogram_quantile(0.95, sum by (le, method, uri) (rate(http_server_requests_seconds_bucket{{{F}, uri!="UNKNOWN"}}[$__rate_interval]))))', "{{method}} {{uri}}")], "s")
advance(8)

# ---------------------------------------------------------------- health sync
row("Health-Sync (iOS)")
ts("Sync-Anfragen/s", "Uploads, Löschungen und abgeschlossene Full-Syncs der iOS-App. Ein Full-Sync zeigt sich als Spitze bei samples.", 0, 8,
   [(f'sum by (uri) (rate(http_server_requests_seconds_count{{{F}, {SYNC}}}[$__rate_interval]))', "{{uri}}")], "reqps")
ts("Sync-Antwortzeit p95", "Wie lange ein Upload-Batch braucht. Der Kafka-Umbau soll diesen Wert vom Datenbank-Upsert entkoppeln.", 8, 8,
   [(f'histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{{{F}, {SYNC}}}[$__rate_interval])))', "{{uri}}")], "s")
ts("Sync-Fehler", "Fehlgeschlagene Sync-Anfragen nach Status. Bei 5xx lädt die App den Batch beim nächsten Sync erneut hoch.", 16, 8,
   [(f'sum by (status) (rate(http_server_requests_seconds_count{{{F}, {SYNC}, status!~"2.."}}[$__rate_interval]))', "{{status}}")], "reqps")
advance(8)

# ---------------------------------------------------------------- jvm
row("JVM")
ts("Heap", "Belegt, reserviert (committed) und Maximum. Steigt belegt nach jeder GC weiter an, gibt es ein Leck.", 0, 8,
   [(f'sum(jvm_memory_used_bytes{{{F}, area="heap"}})', "belegt"),
    (f'sum(jvm_memory_committed_bytes{{{F}, area="heap"}})', "committed"),
    (f'sum(jvm_memory_max_bytes{{{F}, area="heap"}} > 0)', "max")], "bytes")
ts("GC-Pausen", "Zeit pro Sekunde, in der die Anwendung wegen Garbage Collection stand.", 8, 8,
   [(f'sum by (gc) (rate(jvm_gc_pause_seconds_sum{{{F}}}[$__rate_interval]))', "{{gc}}")], "s")
ts("CPU und Threads", "CPU-Anteil des Prozesses (links) und lebende Threads (rechts).", 16, 8,
   [(f'sum(process_cpu_usage{{{F}}})', "CPU"),
    (f'sum(jvm_threads_live_threads{{{F}}})', "Threads")], "percentunit", overrides=[
       {"matcher": {"id": "byName", "options": "Threads"},
        "properties": [{"id": "unit", "value": "short"}, {"id": "custom.axisPlacement", "value": "right"}]}])
advance(8)

# ---------------------------------------------------------------- database
row("Datenbank")
ts("Connection-Pool", "Hikari-Verbindungen zur Postgres. Wartende (pending) heißt: Der Pool ist zu klein oder Abfragen blockieren ihn.", 0, 8,
   [(f'sum(hikaricp_connections_active{{{F}}})', "aktiv"),
    (f'sum(hikaricp_connections_idle{{{F}}})', "frei"),
    (f'sum(hikaricp_connections_pending{{{F}}})', "wartend"),
    (f'sum(hikaricp_connections_max{{{F}}})', "max")], "short", overrides=[
       {"matcher": {"id": "byName", "options": "wartend"}, "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": "red"}}]},
       {"matcher": {"id": "byName", "options": "max"}, "properties": [{"id": "custom.lineStyle", "value": {"fill": "dash", "dash": [10, 10]}}]}])
ts("Wartezeit auf eine Verbindung", "Durchschnittliche Zeit, bis eine Anfrage eine freie Verbindung bekommt.", 8, 8,
   [(f'sum(rate(hikaricp_connections_acquire_seconds_sum{{{F}}}[$__rate_interval])) / sum(rate(hikaricp_connections_acquire_seconds_count{{{F}}}[$__rate_interval]))', "Ø")], "s")
ts("Langsamste Repository-Methoden", "Spring-Data-Repositories mit der höchsten durchschnittlichen Dauer (JdbcTemplate-Repositories wie HealthSampleRepository erscheinen hier nicht).", 16, 8,
   [(f'topk(5, sum by (repository, method) (rate(spring_data_repository_invocations_seconds_sum{{{F}}}[$__rate_interval])) / sum by (repository, method) (rate(spring_data_repository_invocations_seconds_count{{{F}}}[$__rate_interval])))', "{{repository}}.{{method}}")], "s")
advance(8)

# ---------------------------------------------------------------- logs and kubernetes
row("Logs und Kubernetes")
ts("Warnungen und Fehler im Log", "Log-Einträge pro Sekunde nach Level. Ein Anstieg bei error lohnt einen Blick ins Log: kubectl -n labs-101 logs deploy/labs-101-backend", 0, 8,
   [(f'sum by (level) (rate(logback_events_total{{{F}, level=~"warn|error"}}[$__rate_interval]))', "{{level}}")], "short", overrides=[
       {"matcher": {"id": "byName", "options": "error"}, "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": "red"}}]},
       {"matcher": {"id": "byName", "options": "warn"}, "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": "orange"}}]}])
ts("Speicher der Container", "Working Set je Container (das zählt für das Limit) und das Limit als gestrichelte Linie.", 8, 8,
   [(f'sum by (container) (container_memory_working_set_bytes{{{K8S}, container!="", container!="POD"}})', "{{container}}"),
    (f'sum by (container) (kube_pod_container_resource_limits{{{K8S}, resource="memory"}})', "{{container}} Limit")], "bytes",
   overrides=[{"matcher": {"id": "byRegexp", "options": ".* Limit"},
               "properties": [{"id": "custom.lineStyle", "value": {"fill": "dash", "dash": [10, 10]}}]}])
ts("CPU der Container", "CPU-Kerne je Container.", 16, 8,
   [(f'sum by (container) (rate(container_cpu_usage_seconds_total{{{K8S}, container!="", container!="POD"}}[$__rate_interval]))', "{{container}}")], "short")
advance(8)

dashboard = {
    "uid": "labs-101-backend",
    "title": "labs-101 / Backend",
    "description": "HTTP, Health-Sync, JVM, Datenbank und Container des labs-101-Backends. Erzeugt von deploy/helm/labs-101/dashboards/generate.py, Änderungen dort.",
    "tags": ["labs-101", "spring-boot"],
    "timezone": "browser",
    "refresh": "30s",
    "time": {"from": "now-6h", "to": "now"},
    "schemaVersion": 39,
    "editable": True,
    "graphTooltip": 1,
    "templating": {"list": [
        {"name": "datasource", "label": "Datenquelle", "type": "datasource", "query": "prometheus",
         "current": {"text": "Prometheus", "value": "prometheus"}, "hide": 0},
        {"name": "namespace", "label": "Namespace", "type": "query", "datasource": DS,
         "query": {"query": 'label_values(jvm_info{application="backend"}, namespace)', "refId": "ns"},
         "definition": 'label_values(jvm_info{application="backend"}, namespace)',
         "current": {"text": "labs-101", "value": "labs-101"}, "refresh": 2, "hide": 0},
        {"name": "pod", "label": "Pod", "type": "query", "datasource": DS,
         "query": {"query": 'label_values(jvm_info{application="backend", namespace="$namespace"}, pod)', "refId": "pod"},
         "definition": 'label_values(jvm_info{application="backend", namespace="$namespace"}, pod)',
         "includeAll": True, "multi": True, "allValue": ".*",
         "current": {"text": "All", "value": "$__all"}, "refresh": 2, "hide": 0},
    ]},
    "annotations": {"list": [
        {"name": "Backend-Start", "datasource": DS, "enable": True, "iconColor": "blue",
         "expr": f'changes(process_start_time_seconds{{{F}}}[2m]) > 0', "titleFormat": "Backend neu gestartet",
         "step": "60s"}]},
    "panels": panels,
}

json.dump(dashboard, open(sys.argv[1], "w"), indent=2, ensure_ascii=False)
print(f"{len(panels)} panels -> {sys.argv[1]}")
