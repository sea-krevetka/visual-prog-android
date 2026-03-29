#include <iostream>
#include <csignal>
#include <atomic>
#include <thread>
#include <chrono>
#include "data_structs.hpp"
#include "zmq_server.hpp"
#include "gui_thread.hpp"

std::atomic<bool> should_exit(false);

void signal_handler(int signal) {
    std::cout << "\n[Main] Received signal " << signal << ", initiating shutdown..." << std::endl;
    should_exit = true;
}

int main(int argc, char* argv[]) {
    int zmq_port = 2222;
    if (argc > 1) {
        zmq_port = std::atoi(argv[1]);
    }

    std::cout << "Calc Server" << std::endl;

    std::signal(SIGINT, signal_handler);
    std::signal(SIGTERM, signal_handler);

    SharedState shared_state;

    std::cout << "\n[Main] Initializing ZMQ Server on port " << zmq_port << "..." << std::endl;
    ZmqServer zmq_server(zmq_port, shared_state);
    zmq_server.start();

    std::this_thread::sleep_for(std::chrono::milliseconds(500));

    std::cout << "[Main] Initializing GUI Thread..." << std::endl;
    GuiThread gui_thread(shared_state);
    gui_thread.start();

    std::cout << "\n[Main] ✓ Server is running" << std::endl;
    std::cout << "       - ZMQ Server listening on 0.0.0.0:" << zmq_port << std::endl;
    std::cout << "       - GUI is active and displaying data" << std::endl;
    std::cout << "       - Press Ctrl+C to shutdown gracefully" << std::endl;
    std::cout << std::endl;

    while (!should_exit && shared_state.is_server_running() && shared_state.is_gui_running()) {
        std::this_thread::sleep_for(std::chrono::seconds(2));

        int total = shared_state.get_total_packets();
        std::string last_update = shared_state.get_last_update_time();
        
        if (total > 0) {
            std::cout << "\r[Monitor] Packets: " << total << " | Last Update: " << last_update << "  " << std::flush;
        }
    }

    std::cout << "\n\n[Main] Shutting down..." << std::endl;

    std::cout << "[Main] Stopping ZMQ Server..." << std::endl;
    zmq_server.stop();

    std::cout << "[Main] Stopping GUI Thread..." << std::endl;
    gui_thread.stop();

    std::cout << "[Main] All threads stopped" << std::endl;
    std::cout << "[Main] Final statistics:" << std::endl;
    std::cout << "       - Total packets received: " << shared_state.get_total_packets() << std::endl;
    std::cout << "       - Data saved to: ./data/received/" << std::endl;

    return 0;
}
