import os
import json
import zmq
from datetime import datetime

PORT = 2222
BASE_DIR = os.path.join(os.path.dirname(__file__), 'data', 'clients')

os.makedirs(BASE_DIR, exist_ok=True)

context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind(f"tcp://*:{PORT}")

print(f"ZMQ Server listening on tcp://*:{PORT}. Saving data under {BASE_DIR}")

try:
    while True:
        msg = socket.recv()
        try:
            payload = json.loads(msg.decode('utf-8'))
        except Exception as e:
            print(f"Failed to parse JSON: {e}")
            socket.send_json({"status": "error", "message": "invalid json"})
            continue

        client_id = payload.get('client_id', 'unknown')
        client_dir = os.path.join(BASE_DIR, client_id)
        os.makedirs(client_dir, exist_ok=True)

        timestamp = payload.get('telephony', {}).get('timestamp') or int(datetime.now().timestamp() * 1000)
        filename = f"telephony_{timestamp}.json"
        filepath = os.path.join(client_dir, filename)

        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(payload, f, ensure_ascii=False, indent=2)
            print(f"Saved payload for client {client_id} -> {filepath}")
            socket.send_json({"status": "ok"})
        except Exception as e:
            print(f"Failed to save payload: {e}")
            socket.send_json({"status": "error", "message": str(e)})

except KeyboardInterrupt:
    print("Server shutting down")
finally:
    socket.close()
    context.term()
