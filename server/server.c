#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <winsock2.h>
#include <ws2tcpip.h>

#pragma comment(lib, "ws2_32.lib")

#define PORT 8080
#define BUFFER_SIZE 1024

DWORD WINAPI udp_server_thread(LPVOID lpParam);
DWORD WINAPI tcp_server_thread(LPVOID lpParam);
DWORD WINAPI tcp_client_thread(LPVOID lpParam);

DWORD WINAPI udp_server_thread(LPVOID lpParam) {
    SOCKET udp_socket = (SOCKET)lpParam;
    struct sockaddr_in client_addr;
    int client_addr_len = sizeof(client_addr);
    char buffer[BUFFER_SIZE] = {0};
    char *hello = "Hello from UDP server";
    int recv_len;
    
    printf("UDP server thread started\n");
    
    while (1) {
        memset(buffer, 0, BUFFER_SIZE);
        
        recv_len = recvfrom(udp_socket, buffer, BUFFER_SIZE - 1, 0, 
                           (struct sockaddr*)&client_addr, &client_addr_len);
        
        if (recv_len == SOCKET_ERROR) {
            printf("UDP recvfrom failed: %d\n", WSAGetLastError());
            continue;
        }
        
        buffer[recv_len] = '\0';
        printf("UDP Received from %s:%d: %s\n", 
               inet_ntoa(client_addr.sin_addr), 
               ntohs(client_addr.sin_port), 
               buffer);
        
        if (sendto(udp_socket, hello, strlen(hello), 0, 
                  (struct sockaddr*)&client_addr, client_addr_len) == SOCKET_ERROR) {
            printf("UDP sendto failed: %d\n", WSAGetLastError());
        } else {
            printf("UDP Hello message sent\n");
        }
    }
    
    return 0;
}

DWORD WINAPI tcp_client_thread(LPVOID lpParam) {
    SOCKET client_socket = (SOCKET)lpParam;
    char buffer[BUFFER_SIZE] = {0};
    char *hello = "Hello from TCP server";
    
    printf("TCP client thread started for socket %llu\n", (unsigned long long)client_socket);
    
    int valread = recv(client_socket, buffer, BUFFER_SIZE - 1, 0);
    if (valread > 0) {
        buffer[valread] = '\0';
        printf("TCP Received from client: %s\n", buffer);
        
        if (send(client_socket, hello, strlen(hello), 0) == SOCKET_ERROR) {
            printf("TCP send failed: %d\n", WSAGetLastError());
        } else {
            printf("TCP Hello message sent to client\n");
        }
    } else if (valread == 0) {
        printf("TCP client disconnected\n");
    } else {
        printf("TCP recv failed: %d\n", WSAGetLastError());
    }
    
    closesocket(client_socket);
    printf("TCP client thread finished for socket %llu\n", (unsigned long long)client_socket);
    
    return 0;
}

DWORD WINAPI tcp_server_thread(LPVOID lpParam) {
    SOCKET tcp_server_fd = (SOCKET)lpParam;
    struct sockaddr_in address;
    int addrlen = sizeof(address);
    
    printf("TCP server thread started\n");
    
    while (1) {
        printf("TCP server waiting for connection...\n");
        
        SOCKET new_socket = accept(tcp_server_fd, (struct sockaddr *)&address, &addrlen);
        if (new_socket == INVALID_SOCKET) {
            printf("TCP accept failed: %d\n", WSAGetLastError());
            continue;
        }
        
        printf("TCP client connected from %s:%d\n", 
               inet_ntoa(address.sin_addr), 
               ntohs(address.sin_port));
        
        HANDLE client_thread = CreateThread(NULL, 0, tcp_client_thread, 
                                          (LPVOID)new_socket, 0, NULL);
        if (client_thread == NULL) {
            printf("Failed to create TCP client thread\n");
            closesocket(new_socket);
        } else {
            CloseHandle(client_thread);
        }
    }
    
    return 0;
}

int main() {
    system("chcp 65001 > nul");
    WSADATA wsaData;
    SOCKET tcp_server_fd, udp_server_fd;
    struct sockaddr_in address;
    int opt = 1;
    HANDLE udp_thread, tcp_thread;
    
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        printf("WSAStartup failed\n");
        return -1;
    }
    
    if ((tcp_server_fd = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET) {
        printf("TCP socket failed: %d\n", WSAGetLastError());
        WSACleanup();
        return -1;
    }
    
    if ((udp_server_fd = socket(AF_INET, SOCK_DGRAM, 0)) == INVALID_SOCKET) {
        printf("UDP socket failed: %d\n", WSAGetLastError());
        closesocket(tcp_server_fd);
        WSACleanup();
        return -1;
    }
    
    if (setsockopt(tcp_server_fd, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt)) == SOCKET_ERROR) {
        printf("TCP setsockopt failed: %d\n", WSAGetLastError());
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        WSACleanup();
        return -1;
    }
    
    if (setsockopt(udp_server_fd, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt)) == SOCKET_ERROR) {
        printf("UDP setsockopt failed: %d\n", WSAGetLastError());
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        WSACleanup();
        return -1;
    }
    
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);
    
    if (bind(tcp_server_fd, (struct sockaddr *)&address, sizeof(address)) == SOCKET_ERROR) {
        printf("TCP bind failed: %d\n", WSAGetLastError());
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        WSACleanup();
        return -1;
    }
    
    if (bind(udp_server_fd, (struct sockaddr *)&address, sizeof(address)) == SOCKET_ERROR) {
        printf("UDP bind failed: %d\n", WSAGetLastError());
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        WSACleanup();
        return -1;
    }
    
    if (listen(tcp_server_fd, 10) == SOCKET_ERROR) {
        printf("TCP listen failed: %d\n", WSAGetLastError());
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        WSACleanup();
        return -1;
    }
    
    printf("Server listening on port %d\n", PORT);
    printf("Starting TCP and UDP server threads...\n");
    
    udp_thread = CreateThread(NULL, 0, udp_server_thread, (LPVOID)udp_server_fd, 0, NULL);
    if (udp_thread == NULL) {
        printf("Failed to create UDP thread\n");
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        WSACleanup();
        return -1;
    }
    
    tcp_thread = CreateThread(NULL, 0, tcp_server_thread, (LPVOID)tcp_server_fd, 0, NULL);
    if (tcp_thread == NULL) {
        printf("Failed to create TCP thread\n");
        closesocket(tcp_server_fd);
        closesocket(udp_server_fd);
        CloseHandle(udp_thread);
        WSACleanup();
        return -1;
    }
    
    printf("Server started successfully. Press Ctrl+C to exit.\n");
    
    WaitForSingleObject(tcp_thread, INFINITE);
    WaitForSingleObject(udp_thread, INFINITE);
    
    CloseHandle(tcp_thread);
    CloseHandle(udp_thread);
    closesocket(tcp_server_fd);
    closesocket(udp_server_fd);
    WSACleanup();
    
    return 0;
}