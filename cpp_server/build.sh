#!/bin/bash
set -e

if ! command -v cmake &> /dev/null; then
    echo "❌ CMake not found. Please install CMake."
    exit 1
fi

mkdir -p build
cd build

echo ""
echo "Building server..."
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release --parallel $(nproc)

echo ""
echo "Build complete"
echo ""
echo "To run the server:"
echo "  ./zmq_telepathy_server"
echo ""
echo "To run on custom port:"
echo "  ./zmq_telepathy_server 5000"
