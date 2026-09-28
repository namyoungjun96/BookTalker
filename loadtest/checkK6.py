import requests
import json
import time
from datetime import datetime
# from requests.auth import HTTPBasicAuth

print('hello')

session = requests.Session()
session.auth = ('user', 'password')

# basic = HTTPBasicAuth('user', 'password')

def getMetrics():
    cacheHit = session.get('http://localhost:8010/actuator/metrics/cache.gets?tag=result:hit')
    cacheMiss = session.get('http://localhost:8010/actuator/metrics/cache.gets?tag=result:miss')
    connectionsActive = session.get('http://localhost:8010/actuator/metrics/hikaricp.connections.active')
    connectionsPending = session.get('http://localhost:8010/actuator/metrics/hikaricp.connections.pending')
    connectionsMax = session.get('http://localhost:8010/actuator/metrics/hikaricp.connections.max')

    record = {
        "timestamp": datetime.now().isoformat(),
        "cacheHit": cacheHit.json(),
        "cacheMiss": cacheMiss.json(),
        "connectionsActive": connectionsActive.json(),
        "connectionsPending": connectionsPending.json(),
        "connectionsMax": connectionsMax.json(),
    }
    
    with open("./data2.jsonl", "a", encoding="utf-8") as f:
        f.write(json.dumps(record, ensure_ascii=False) + "\n")


if __name__ == "__main__":
    while True:
        getMetrics()
        time.sleep(5)
