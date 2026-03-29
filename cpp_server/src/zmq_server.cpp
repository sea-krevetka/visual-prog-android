#include "zmq_server.hpp"
#include <iostream>
#include <fstream>
#include <chrono>
#include <iomanip>
#include <filesystem>
#include <sstream>

namespace fs = std::filesystem;

ZmqServer::ZmqServer(int port, SharedState& state)
    : port(port), shared_state(state) {}

ZmqServer::~ZmqServer() {
    stop();
}

void ZmqServer::start() {
    if (running) return;
    
    running = true;
    server_thread = std::thread(&ZmqServer::server_loop, this);
    std::cout << "[ZMQ Server] Started on port " << port << std::endl;
}

void ZmqServer::stop() {
    running = false;
    if (server_thread.joinable()) {
        server_thread.join();
    }
    std::cout << "[ZMQ Server] Stopped" << std::endl;
}

void ZmqServer::server_loop() {
    try {
        zmq::context_t zmq_context(1);
        zmq::socket_t zmq_socket(zmq_context, zmq::socket_type::rep);
        
        zmq_socket.set(zmq::sockopt::rcvtimeo, 1000);
        
        std::string endpoint = "tcp://*:" + std::to_string(port);
        zmq_socket.bind(endpoint);
        
        std::cout << "[ZMQ Server] Listening on " << endpoint << std::endl;
        
        while (running && shared_state.is_server_running()) {
            try {
                zmq::message_t request;
                
                auto result = zmq_socket.recv(request, zmq::recv_flags::none);
                if (!result) {
                    continue;
                }

                std::string json_str(static_cast<char*>(request.data()), request.size());
                std::cout << "[ZMQ Server] Received packet (" << json_str.size() << " bytes)" << std::endl;

                try {
                    TelemetryPacket packet = parse_telemetry_json(json_str);
                    shared_state.update_from_packet(packet);
                    
                    save_to_file(packet);
                    
                    zmq::message_t reply(3);
                    memcpy(reply.data(), "ACK", 3);
                    zmq_socket.send(reply, zmq::send_flags::none);
                    
                    std::cout << "[ZMQ Server] Packet processed and saved" << std::endl;

                } catch (const std::exception& e) {
                    std::cerr << "[ZMQ Server] Error parsing packet: " << e.what() << std::endl;
                    
                    std::string error_msg = "ERR";
                    zmq::message_t reply(error_msg.size());
                    memcpy(reply.data(), error_msg.data(), error_msg.size());
                    zmq_socket.send(reply, zmq::send_flags::none);
                }

            } catch (const zmq::error_t& e) {
                if (e.num() != EAGAIN) { 
                    std::cerr << "[ZMQ Server] ZMQ error: " << e.what() << std::endl;
                }
            }
        }

        zmq_socket.close();
        std::cout << "[ZMQ Server] Socket closed" << std::endl;

    } catch (const std::exception& e) {
        std::cerr << "[ZMQ Server] Fatal error: " << e.what() << std::endl;
    }
}

TelemetryPacket ZmqServer::parse_telemetry_json(const std::string& json_str) {
    json parsed = json::parse(json_str);
    
    TelemetryPacket packet;
    
    if (parsed.contains("client_id")) {
        packet.client_id = parsed["client_id"].get<std::string>();
    }
    if (parsed.contains("timestamp")) {
        packet.timestamp = parsed["timestamp"].get<long long>();
    }

    if (parsed.contains("location") && parsed["location"].is_object()) {
        packet.location = LocationInfo::from_json(parsed["location"]);
    }

    if (parsed.contains("traffic") && parsed["traffic"].is_object()) {
        const auto& traffic_j = parsed["traffic"];
        if (traffic_j.contains("totalRxBytes")) {
            packet.traffic.totalRxBytes = traffic_j["totalRxBytes"].get<long long>();
        }
        if (traffic_j.contains("totalTxBytes")) {
            packet.traffic.totalTxBytes = traffic_j["totalTxBytes"].get<long long>();
        }
        if (traffic_j.contains("mobileRxBytes")) {
            packet.traffic.mobileRxBytes = traffic_j["mobileRxBytes"].get<long long>();
        }
        if (traffic_j.contains("mobileTxBytes")) {
            packet.traffic.mobileTxBytes = traffic_j["mobileTxBytes"].get<long long>();
        }
    }

    if (parsed.contains("cellInfo") && parsed["cellInfo"].is_array()) {
        for (const auto& cell_j : parsed["cellInfo"]) {
            CellSignalInfo cell;
            if (cell_j.contains("type")) {
                cell.type = cell_j["type"].get<std::string>();
            }
            if (cell_j.contains("signal") && cell_j["signal"].is_object()) {
                for (const auto& [key, val] : cell_j["signal"].items()) {
                    cell.signal[key] = val;
                }
            }
            if (cell_j.contains("identity") && cell_j["identity"].is_object()) {
                for (const auto& [key, val] : cell_j["identity"].items()) {
                    cell.identity[key] = val;
                }
            }
            packet.cell_info.push_back(cell);
        }
    }

    return packet;
}

void ZmqServer::save_to_file(const TelemetryPacket& packet) {
    try {
        fs::path base_dir = fs::current_path() / "data" / "received";
        fs::path client_dir = base_dir / packet.client_id;
        fs::create_directories(client_dir);

        auto now = std::chrono::system_clock::now();
        auto time_t_now = std::chrono::system_clock::to_time_t(now);
        auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
            now.time_since_epoch()) % 1000;
        
        std::stringstream ss;
        ss << std::put_time(std::localtime(&time_t_now), "%Y%m%d_%H%M%S");
        ss << "_" << std::setfill('0') << std::setw(3) << ms.count();

        fs::path file_path = client_dir / (ss.str() + ".json");
        std::ofstream file(file_path);
        file << packet.to_json().dump(2) << std::endl;
        file.close();

        std::cout << "[ZMQ Server] Saved to: " << file_path << std::endl;

        fs::path latest_path = client_dir / "latest.json";
        std::ofstream latest_file(latest_path);
        latest_file << packet.to_json().dump(2) << std::endl;
        latest_file.close();

    } catch (const std::exception& e) {
        std::cerr << "[ZMQ Server] Failed to save file: " << e.what() << std::endl;
    }
}
