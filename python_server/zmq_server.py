import os
import json
import zmq
import argparse
import sys
from datetime import datetime

try:
    import psycopg2
    from psycopg2.extras import Json
    PSYCOPG2_AVAILABLE = True
except Exception:
    PSYCOPG2_AVAILABLE = False


def parse_args():
    parser = argparse.ArgumentParser(description='Simple ZMQ JSON receiver saving to files and optionally to PostgreSQL')
    parser.add_argument('--port', type=int, default=int(os.environ.get('ZMQ_PORT', 2222)), help='TCP port to listen on')
    parser.add_argument('--outdir', type=str, default=os.path.join(os.path.dirname(__file__), 'data', 'clients'), help='Base directory for saved clients files')
    parser.add_argument('--db-host', type=str, default=os.environ.get('DB_HOST'))
    parser.add_argument('--db-port', type=int, default=os.environ.get('DB_PORT') or 5432)
    parser.add_argument('--db-name', type=str, default=os.environ.get('DB_NAME'))
    parser.add_argument('--db-user', type=str, default=os.environ.get('DB_USER'))
    parser.add_argument('--db-pass', type=str, default=os.environ.get('DB_PASS'))
    parser.add_argument('--save-to-db', action='store_true', help='If set, attempt to save received payloads into PostgreSQL')
    return parser.parse_args()


def init_db(conn):
    # Ensure the table exists
    with conn.cursor() as cur:
        cur.execute('''
            CREATE TABLE IF NOT EXISTS telephony_packets (
                id SERIAL PRIMARY KEY,
                client_id TEXT,
                telephony JSONB,
                location JSONB,
                raw JSONB,
                telephony_ts BIGINT,
                received_at TIMESTAMPTZ DEFAULT now()
            )
        ''')
        conn.commit()


def insert_packet(conn, client_id, telephony, location, raw_payload, telephony_ts):
    with conn.cursor() as cur:
        cur.execute('''
            INSERT INTO telephony_packets (client_id, telephony, location, raw, telephony_ts)
            VALUES (%s, %s::jsonb, %s::jsonb, %s::jsonb, %s)
        ''', (client_id, Json(telephony), Json(location), Json(raw_payload), telephony_ts))
        conn.commit()


def main():
    args = parse_args()
    PORT = args.port
    BASE_DIR = args.outdir
    os.makedirs(BASE_DIR, exist_ok=True)

    db_conn = None
    if args.save_to_db:
        if not PSYCOPG2_AVAILABLE:
            print('psycopg2 is not installed; cannot save to DB. Install psycopg2-binary and retry or disable --save-to-db')
            sys.exit(1)
        if not (args.db_host and args.db_name and args.db_user and args.db_pass):
            print('DB save requested but DB parameters incomplete. Provide --db-host --db-name --db-user --db-pass')
            sys.exit(1)
        try:
            db_conn = psycopg2.connect(dbname=args.db_name, host=args.db_host, user=args.db_user, password=args.db_pass, port=args.db_port)
            init_db(db_conn)
            print('Connected to DB and ensured table exists')
        except Exception as e:
            print(f'Failed to connect to DB: {e}')
            db_conn = None

    context = zmq.Context()
    socket = context.socket(zmq.REP)
    socket.bind(f"tcp://*:{PORT}")

    print(f"ZMQ Server listening on tcp://*:{PORT}. Saving data under {BASE_DIR}")
    if db_conn:
        print('DB save enabled')

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

            telephony = payload.get('telephony')
            location = payload.get('location')
            timestamp = (telephony and telephony.get('timestamp')) or int(datetime.now().timestamp() * 1000)
            filename = f"telephony_{timestamp}.json"
            filepath = os.path.join(client_dir, filename)

            file_ok = False
            db_ok = False
            try:
                with open(filepath, 'w', encoding='utf-8') as f:
                    json.dump(payload, f, ensure_ascii=False, indent=2)
                print(f"Saved payload for client {client_id} -> {filepath}")
                file_ok = True
            except Exception as e:
                print(f"Failed to save payload: {e}")

            if db_conn:
                try:
                    insert_packet(db_conn, client_id, telephony, location, payload, timestamp)
                    print(f"Inserted payload into DB for client {client_id} telephony_ts={timestamp}")
                    db_ok = True
                except Exception as e:
                    print(f"Failed to insert payload into DB: {e}")

            resp = {"status": "ok" if (file_ok or db_ok) else "error"}
            if not (file_ok or db_ok):
                resp["message"] = "failed to save to file and DB"
            socket.send_json(resp)

    except KeyboardInterrupt:
        print("Server shutting down")
    finally:
        socket.close()
        context.term()
        if db_conn:
            db_conn.close()


if __name__ == '__main__':
    main()
