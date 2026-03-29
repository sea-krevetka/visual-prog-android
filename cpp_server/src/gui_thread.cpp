#include "gui_thread.hpp"
#include <imgui.h>
#include <imgui_impl_glfw.h>
#include <imgui_impl_opengl3.h>
#include <glm/glm.hpp>
#include <iostream>
#include <sstream>
#include <iomanip>

GuiThread::GuiThread(SharedState& state) : shared_state(state) {}

GuiThread::~GuiThread() {
    stop();
}

void GuiThread::start() {
    if (running) return;
    
    running = true;
    gui_thread = std::thread(&GuiThread::gui_loop, this);
    std::cout << "[GUI Thread] Started" << std::endl;
}

void GuiThread::stop() {
    running = false;
    if (gui_thread.joinable()) {
        gui_thread.join();
    }
    std::cout << "[GUI Thread] Stopped" << std::endl;
}

void GuiThread::gui_loop() {
    try {
        if (!glfwInit()) {
            std::cerr << "[GUI] Failed to initialize GLFW" << std::endl;
            return;
        }

        const char* glsl_version = "#version 150";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        // Create window
        window = glfwCreateWindow(1400, 900, "Telepathy Server - ZMQ Monitor", nullptr, nullptr);
        if (!window) {
            std::cerr << "[GUI] Failed to create GLFW window" << std::endl;
            glfwTerminate();
            return;
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        setup_imgui();

        while (running && shared_state.is_gui_running() && !glfwWindowShouldClose(window)) {
            glfwPollEvents();

            ImGui_ImplOpenGL3_NewFrame();
            ImGui_ImplGlfw_NewFrame();
            ImGui::NewFrame();

            render_frame();

            ImGui::Render();

            int display_w, display_h;
            glfwGetFramebufferSize(window, &display_w, &display_h);
            glViewport(0, 0, display_w, display_h);
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

            glfwSwapBuffers(window);
        }

        cleanup_imgui();
        glfwDestroyWindow(window);
        glfwTerminate();

        std::cout << "[GUI Thread] Shutdown complete" << std::endl;

    } catch (const std::exception& e) {
        std::cerr << "[GUI Thread] Error: " << e.what() << std::endl;
    }
}

void GuiThread::setup_imgui() {
    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;

    ImGui::StyleColorsDark();

    const char* glsl_version = "#version 150";
    ImGui_ImplGlfw_InitForOpenGL(window, true);
    ImGui_ImplOpenGL3_Init(glsl_version);

    io.Fonts->AddFontDefault();

    std::cout << "[GUI] ImGui initialized" << std::endl;
}

void GuiThread::cleanup_imgui() {
    ImGui_ImplOpenGL3_Shutdown();
    ImGui_ImplGlfw_Shutdown();
    ImGui::DestroyContext();
}

void GuiThread::render_frame() {
    ImGui::SetNextWindowPos(ImVec2(0, 0), ImGuiCond_FirstUseEver);
    ImGui::SetNextWindowSize(ImGui::GetIO().DisplaySize, ImGuiCond_FirstUseEver);

    ImGui::Begin("Telepathy Server Dashboard", nullptr, ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoResize);

    // Create tabs
    if (ImGui::BeginTabBar("##tabs")) {
        if (ImGui::BeginTabItem("Location")) {
            render_location_widget();
            ImGui::EndTabItem();
        }

        if (ImGui::BeginTabItem("Telemetry")) {
            render_telemetry_widget();
            ImGui::EndTabItem();
        }

        if (ImGui::BeginTabItem("Statistics")) {
            render_statistics_widget();
            ImGui::EndTabItem();
        }

        if (ImGui::BeginTabItem("History")) {
            render_packet_history();
            ImGui::EndTabItem();
        }

        ImGui::EndTabBar();
    }

    ImGui::Separator();
    if (ImGui::Button("Exit", ImVec2(100, 30))) {
        shared_state.stop_all();
    }

    ImGui::End();
}

void GuiThread::render_location_widget() {
    LocationInfo loc = shared_state.get_latest_location();

    ImGui::SeparatorText("GPS Location (Latest)");

    // Location box
    ImGui::BeginChild("LocationBox", ImVec2(0, 250), true);
    {
        ImGui::Text("Latitude:  %.6f", loc.latitude);
        ImGui::Text("Longitude: %.6f", loc.longitude);
        ImGui::Text("Altitude:  %.2f m", loc.altitude);
        ImGui::Text("Accuracy:  %.2f m", loc.accuracy);
        ImGui::Text("Provider:  %s", loc.provider.c_str());
        ImGui::Text("Time:      %lld", loc.time);
    }
    ImGui::EndChild();

    ImGui::SeparatorText("Map Coordinates");
    ImGui::TextWrapped("Coordinates: %.6f, %.6f", loc.latitude, loc.longitude);
    
    char coords_buffer[128];
    snprintf(coords_buffer, sizeof(coords_buffer), "https://maps.google.com/?q=%.6f,%.6f", 
             loc.latitude, loc.longitude);
    ImGui::Text("Google Maps: %s", coords_buffer);
}

void GuiThread::render_telemetry_widget() {
    TelemetryPacket packet = shared_state.get_latest_packet();

    ImGui::SeparatorText("Latest Telemetry Packet");

    ImGui::Text("Client ID: %s", packet.client_id.c_str());
    ImGui::Text("Timestamp: %lld", packet.timestamp);
    ImGui::Text("Received:  %s", shared_state.get_last_update_time().c_str());

    ImGui::SeparatorText("Location");
    ImGui::Text("Lat: %.6f  |  Lon: %.6f  |  Alt: %.1f m", 
                packet.location.latitude, packet.location.longitude, packet.location.altitude);

    ImGui::SeparatorText("Network Traffic");
    ImGui::Text("Total RX: %.2f MB", packet.traffic.totalRxBytes / 1024.0f / 1024.0f);
    ImGui::Text("Total TX: %.2f MB", packet.traffic.totalTxBytes / 1024.0f / 1024.0f);
    ImGui::Text("Mobile RX: %.2f MB", packet.traffic.mobileRxBytes / 1024.0f / 1024.0f);
    ImGui::Text("Mobile TX: %.2f MB", packet.traffic.mobileTxBytes / 1024.0f / 1024.0f);

    ImGui::SeparatorText("Cell Information");
    ImGui::BeginChild("CellInfo", ImVec2(0, 200), true);
    {
        for (size_t i = 0; i < packet.cell_info.size(); i++) {
            const auto& cell = packet.cell_info[i];
            if (ImGui::TreeNode(&cell, "Cell [%zu] - %s###cell_%zu", i, cell.type.c_str(), i)) {
                ImGui::Text("Type: %s", cell.type.c_str());
                
                if (!cell.signal.empty()) {
                    ImGui::Text("Signal:");
                    for (const auto& [key, val] : cell.signal) {
                        std::string val_str = val.dump();
                        ImGui::TextWrapped("  %s: %s", key.c_str(), val_str.c_str());
                    }
                }
                
                ImGui::TreePop();
            }
        }
    }
    ImGui::EndChild();
}

void GuiThread::render_statistics_widget() {
    ImGui::SeparatorText("Server Statistics");

    int total_packets = shared_state.get_total_packets();
    std::string last_update = shared_state.get_last_update_time();

    ImGui::Text("Total Packets Received: %d", total_packets);
    ImGui::Text("Last Update: %s", last_update.c_str());

    ImGui::SeparatorText("Active Connections");
    ImGui::Text("Connected Clients: %d", shared_state.get_active_connections());

    ImGui::SeparatorText("Server Status");
    ImGui::TextColored(ImVec4(0, 1, 0, 1), "● Server Running");
    ImGui::TextColored(ImVec4(0, 1, 0, 1), "● GUI Active");
    ImGui::TextColored(ImVec4(0, 1, 0, 1), "● ZMQ Listening");
}

void GuiThread::render_packet_history() {
    ImGui::SeparatorText("Recent Packets (Last 20)");

    auto packets = shared_state.get_recent_packets();

    ImGui::BeginChild("PacketHistory", ImVec2(0, 0), true);
    {
        static ImGuiTableFlags flags = ImGuiTableFlags_ScrollY | ImGuiTableFlags_RowBg | 
                                       ImGuiTableFlags_Borders | ImGuiTableFlags_Resizable;
        
        if (ImGui::BeginTable("##packet_table", 5, flags, ImVec2(0, 0))) {
            ImGui::TableSetupColumn("Index");
            ImGui::TableSetupColumn("Timestamp");
            ImGui::TableSetupColumn("Client");
            ImGui::TableSetupColumn("Latitude");
            ImGui::TableSetupColumn("Longitude");
            ImGui::TableHeadersRow();

            for (size_t i = 0; i < packets.size(); i++) {
                const auto& pkt = packets[i];
                ImGui::TableNextRow();
                ImGui::TableSetColumnIndex(0);
                ImGui::Text("%zu", i + 1);
                ImGui::TableSetColumnIndex(1);
                ImGui::Text("%lld", pkt.timestamp);
                ImGui::TableSetColumnIndex(2);
                ImGui::Text("%s", pkt.client_id.c_str());
                ImGui::TableSetColumnIndex(3);
                ImGui::Text("%.6f", pkt.location.latitude);
                ImGui::TableSetColumnIndex(4);
                ImGui::Text("%.6f", pkt.location.longitude);
            }

            ImGui::EndTable();
        }
    }
    ImGui::EndChild();
}
