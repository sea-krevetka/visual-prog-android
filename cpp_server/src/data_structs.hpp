#pragma once

#include <string>
#include <vector>
#include <map>
#include <mutex>
#include <chrono>
#include <ctime>
#include <nlohmann/json.hpp>

using json = nlohmann::json;

struct LocationInfo {
    double latitude = 0.0;
    double longitude = 0.0;
    double altitude = 0.0;
    long long time = 0;
    float accuracy = 0.0f;
    std::string provider;

    json to_json() const {
        return json{
            {"latitude", latitude},
            {"longitude", longitude},
            {"altitude", altitude},
            {"time", time},
            {"accuracy", accuracy},
            {"provider", provider}
        };
    }

    static LocationInfo from_json(const json& j) {
        LocationInfo info;
        if (j.contains("latitude")) info.latitude = j["latitude"];
        if (j.contains("longitude")) info.longitude = j["longitude"];
        if (j.contains("altitude")) info.altitude = j["altitude"];
        if (j.contains("time")) info.time = j["time"];
        if (j.contains("accuracy")) info.accuracy = j["accuracy"];
        if (j.contains("provider")) info.provider = j["provider"];
        return info;
    }

    std::string format_display() const {
        char buffer[256];
        snprintf(buffer, sizeof(buffer),
            "Lat: %.6f\nLon: %.6f\nAlt: %.1f m\nAccuracy: %.1f m\nProvider: %s",
            latitude, longitude, altitude, accuracy, provider.c_str());
        return std::string(buffer);
    }
};

/**
 * Cell signal information
 */
struct CellSignalInfo {
    std::string type;  // GSM, LTE, NR
    std::map<std::string, json> identity;
    std::map<std::string, json> signal;

    json to_json() const {
        json signal_json;
        for (const auto& [key, val] : signal) {
            signal_json[key] = val;
        }
        json identity_json;
        for (const auto& [key, val] : identity) {
            identity_json[key] = val;
        }
        return json{
            {"type", type},
            {"identity", identity_json},
            {"signal", signal_json}
        };
    }
};


struct NetworkTraffic {
    long long totalRxBytes = 0;
    long long totalTxBytes = 0;
    long long mobileRxBytes = 0;
    long long mobileTxBytes = 0;

    json to_json() const {
        return json{
            {"totalRxBytes", totalRxBytes},
            {"totalTxBytes", totalTxBytes},
            {"mobileRxBytes", mobileRxBytes},
            {"mobileTxBytes", mobileTxBytes}
        };
    }
};


struct TelemetryPacket {
    std::string client_id;
    long long timestamp = 0;
    LocationInfo location;
    std::vector<CellSignalInfo> cell_info;
    NetworkTraffic traffic;

    json to_json() const {
        json cells_json = json::array();
        for (const auto& cell : cell_info) {
            cells_json.push_back(cell.to_json());
        }

        return json{
            {"client_id", client_id},
            {"timestamp", timestamp},
            {"location", location.to_json()},
            {"cell_info", cells_json},
            {"traffic", traffic.to_json()}
        };
    }
};
struct SharedState {
    mutable std::mutex mutex;
    
    LocationInfo latest_location;
    TelemetryPacket latest_packet;
    
    int total_packets_received = 0;
    int active_connections = 0;
    std::string last_update_time;
    
    bool server_running = true;
    bool gui_running = true;

    static constexpr int DISPLAY_LIMIT = 20;
    std::vector<TelemetryPacket> recent_packets;

    void update_from_packet(const TelemetryPacket& packet) {
        std::lock_guard<std::mutex> lock(mutex);
        
        latest_packet = packet;
        latest_location = packet.location;
        total_packets_received++;
        
        auto now = std::time(nullptr);
        auto tm = *std::localtime(&now);
        char buffer[32];
        strftime(buffer, sizeof(buffer), "%H:%M:%S", &tm);
        last_update_time = buffer;

        recent_packets.push_back(packet);
        if (recent_packets.size() > DISPLAY_LIMIT) {
            recent_packets.erase(recent_packets.begin());
        }
    }

    LocationInfo get_latest_location() const {
        std::lock_guard<std::mutex> lock(mutex);
        return latest_location;
    }

    TelemetryPacket get_latest_packet() const {
        std::lock_guard<std::mutex> lock(mutex);
        return latest_packet;
    }

    std::vector<TelemetryPacket> get_recent_packets() const {
        std::lock_guard<std::mutex> lock(mutex);
        return recent_packets;
    }

    int get_total_packets() const {
        std::lock_guard<std::mutex> lock(mutex);
        return total_packets_received;
    }

    int get_active_connections() const {
        std::lock_guard<std::mutex> lock(mutex);
        return active_connections;
    }

    std::string get_last_update_time() const {
        std::lock_guard<std::mutex> lock(mutex);
        return last_update_time;
    }

    void set_active_connections(int count) {
        std::lock_guard<std::mutex> lock(mutex);
        active_connections = count;
    }

    void stop_all() {
        std::lock_guard<std::mutex> lock(mutex);
        server_running = false;
        gui_running = false;
    }

    bool is_server_running() const {
        std::lock_guard<std::mutex> lock(mutex);
        return server_running;
    }

    bool is_gui_running() const {
        std::lock_guard<std::mutex> lock(mutex);
        return gui_running;
    }
};
