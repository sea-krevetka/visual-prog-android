cmake_minimum_required(VERSION 3.15)
project(zmq_telepathy_server)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(zmq REQUIRED)
find_package(nlohmann_json REQUIRED)
find_package(OpenGL REQUIRED)
find_package(glfw3 REQUIRED)
find_package(glew REQUIRED)

# Add Dear ImGui
set(IMGUI_DIR "${CMAKE_CURRENT_SOURCE_DIR}/imgui")
set(IMGUI_SOURCES
    ${IMGUI_DIR}/imgui.cpp
    ${IMGUI_DIR}/imgui_demo.cpp
    ${IMGUI_DIR}/imgui_draw.cpp
    ${IMGUI_DIR}/imgui_widgets.cpp
    ${IMGUI_DIR}/imgui_tables.cpp
    ${IMGUI_DIR}/backends/imgui_impl_glfw.cpp
    ${IMGUI_DIR}/backends/imgui_impl_opengl3.cpp
)

include_directories(
    ${IMGUI_DIR}
    ${IMGUI_DIR}/backends
)

# Main executable
add_executable(zmq_telepathy_server
    src/main.cpp
    src/data_structs.hpp
    src/gui_thread.cpp
    src/gui_thread.hpp
    src/zmq_server.cpp
    src/zmq_server.hpp
    ${IMGUI_SOURCES}
)

# Link libraries
target_link_libraries(zmq_telepathy_server
    ${ZMQ_LIBRARIES}
    nlohmann_json::nlohmann_json
    OpenGL::OpenGL
    glfw
    GLEW::GLEW
    pthread
)

# Copy shaders and assets if needed
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/config/server_config.json"
    "${CMAKE_CURRENT_BINARY_DIR}/config/server_config.json"
    COPYONLY
)
