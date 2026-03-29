#pragma once

#include "data_structs.hpp"
#include <GLFW/glfw3.h>
#include <thread>
#include <atomic>

class GuiThread {
public:
    GuiThread(SharedState& state);
    ~GuiThread();

    void start();
    void stop();
    bool is_running() const { return running; }

private:
    SharedState& shared_state;
    std::atomic<bool> running{false};
    std::thread gui_thread;
    GLFWwindow* window = nullptr;

    void gui_loop();
    void setup_imgui();
    void cleanup_imgui();
    void render_frame();

    void render_location_widget();
    void render_telemetry_widget();
    void render_statistics_widget();
    void render_packet_history();
};
