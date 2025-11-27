#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <winsock2.h>
#include <ws2tcpip.h>

#pragma comment(lib, "ws2_32.lib")

#define PORT 8080
#define BUFFER_SIZE 1024
#define SERVER_IP "127.0.0.1"

int main() {
    system("chcp 65001 > nul");
    WSADATA wsaData;
    SOCKET sock = 0;
    struct sockaddr_in serv_addr;
    int addr_len = sizeof(serv_addr);
    char *hello = "Hello from UDP client";
    char buffer[BUFFER_SIZE] = {0};
    
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        printf("WSAStartup failed\n");
        return -1;
    }
    
    if ((sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == INVALID_SOCKET) {
        printf("Socket creation error: %d\n", WSAGetLastError());
        WSACleanup();
        return -1;
    }
    
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);
    
    if (inet_pton(AF_INET, SERVER_IP, &serv_addr.sin_addr) <= 0) {
        printf("\nInvalid address/ Address not supported \n");
        closesocket(sock);
        WSACleanup();
        return -1;
    }
    
    printf("UDP client started. Sending to %s:%d\n", SERVER_IP, PORT);
    
    int bytes_sent = sendto(sock, hello, strlen(hello), 0, (struct sockaddr*)&serv_addr, addr_len);
    
    if (bytes_sent == SOCKET_ERROR) {
        printf("Send failed: %d\n", WSAGetLastError());
        closesocket(sock);
        WSACleanup();
        return -1;
    }
    
    printf("Message sent: %s\n", hello);
    
    DWORD timeout = 5000;
    if (setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (char*)&timeout, sizeof(timeout)) == SOCKET_ERROR) {
        printf("Set timeout failed: %d\n", WSAGetLastError());
    }
    
    int bytes_received = recvfrom(sock, buffer, BUFFER_SIZE - 1, 0, (struct sockaddr*)&serv_addr, &addr_len);
    
    if (bytes_received == SOCKET_ERROR) {
        int error = WSAGetLastError();
        if (error == WSAETIMEDOUT) {
            printf("Receive timeout: no response from server\n");
        } else {
            printf("Receive failed: %d\n", error);
        }
    } else {
        buffer[bytes_received] = '\0';
        printf("Server response: %s\n", buffer);
    }
    
    closesocket(sock);
    WSACleanup();
    
    return 0;
}