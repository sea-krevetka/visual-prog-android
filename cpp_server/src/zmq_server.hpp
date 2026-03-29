#pragma once

#include "data_structs.hpp"
#include <zmq.hpp>
#include <thread>
#include <atomic>

class ZmqServer {
public:
    ZmqServer(int port, SharedState& state);
    ~ZmqServer();

    void start();
    void stop();
    bool is_running() const { return running; }

private:
    int port;
    SharedState& shared_state;
    std::atomic<bool> running{false};
    std::thread server_thread;

    void server_loop();
    TelemetryPacket parse_telemetry_json(const std::string& json_str);
    void save_to_file(const TelemetryPacket& packet);
};
