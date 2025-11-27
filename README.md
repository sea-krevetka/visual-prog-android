
## Архитектура серверов

### Многопоточная модель (server.c)

#### Основные компоненты:

```c
DWORD WINAPI udp_server_thread(LPVOID lpParam);
DWORD WINAPI tcp_server_thread(LPVOID lpParam); 
DWORD WINAPI tcp_client_thread(LPVOID lpParam);
```

#### Алгоритм работы сервера:

1. **Инициализация Winsock**:
```c
if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
    printf("WSAStartup failed\n");
    return -1;
}
```

2. **Создание сокетов**:
   - TCP сокет (`SOCK_STREAM`)
   - UDP сокет (`SOCK_DGRAM`)

3. **Настройка и привязка**:
```c
address.sin_family = AF_INET;
address.sin_addr.s_addr = INADDR_ANY;
address.sin_port = htons(PORT);
```

### UDP сервер поток

#### Алгоритм обработки UDP:

```c
DWORD WINAPI udp_server_thread(LPVOID lpParam) {
    SOCKET udp_socket = (SOCKET)lpParam;
    
    while (1) {
        // Получение данных от клиента
        recv_len = recvfrom(udp_socket, buffer, BUFFER_SIZE - 1, 0, 
                           (struct sockaddr*)&client_addr, &client_addr_len);
        
        // Отправка ответа
        sendto(udp_socket, hello, strlen(hello), 0, 
              (struct sockaddr*)&client_addr, client_addr_len);
    }
}
```

### TCP сервер поток

#### Алгоритм обработки TCP:

```c
DWORD WINAPI tcp_server_thread(LPVOID lpParam) {
    while (1) {
        // Ожидание подключения
        SOCKET new_socket = accept(tcp_server_fd, (struct sockaddr *)&address, &addrlen);
        
        // Создание отдельного потока для клиента
        CreateThread(NULL, 0, tcp_client_thread, (LPVOID)new_socket, 0, NULL);
    }
}
```

#### Обработка TCP клиента:

```c
DWORD WINAPI tcp_client_thread(LPVOID lpParam) {
    SOCKET client_socket = (SOCKET)lpParam;
    
    // Получение данных
    valread = recv(client_socket, buffer, BUFFER_SIZE - 1, 0);
    
    // Отправка ответа
    send(client_socket, hello, strlen(hello), 0);
    
    closesocket(client_socket);
}
```


### TCP клиент:
```c
// Установка соединения
connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr));

// Отправка данных
send(sock, hello, strlen(hello), 0);

// Получение ответа
recv(sock, buffer, BUFFER_SIZE - 1, 0);
```

### UDP клиент:
```c
// Отправка без установки соединения
sendto(sock, hello, strlen(hello), 0, (struct sockaddr*)&serv_addr, addr_len);

// Получение с таймаутом
setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (char*)&timeout, sizeof(timeout));
recvfrom(sock, buffer, BUFFER_SIZE - 1, 0, (struct sockaddr*)&serv_addr, &addr_len);
```

## Python реализации

### Сервер на Python:

```python
def main():
    # Запуск TCP и UDP серверов в отдельных потоках
    tcp_thread = threading.Thread(target=tcp_server_thread)
    udp_thread = threading.Thread(target=udp_server_thread)
    
    tcp_thread.daemon = True
    udp_thread.daemon = True
    
    tcp_thread.start()
    udp_thread.start()
```

### Обработка TCP клиента:

```python
def tcp_client_thread(client_socket, client_address):
    data = client_socket.recv(1024)
    response = "Hello from TCP server"
    client_socket.sendall(response.encode('utf-8'))
```

### Обработка UDP:

```python
def udp_server_thread():
    while True:
        data, client_address = udp_socket.recvfrom(1024)
        response = "Hello from UDP server"
        udp_socket.sendto(response.encode('utf-8'), client_address)
```

### Компиляция C файлов (Windows):
```bash
gcc -o server.exe server.c -lws2_32
gcc -o client_tcp.exe client_tcp.c -lws2_32  
gcc -o client_udp.exe client_udp.c -lws2_32
```

### Запуск Python файлов:
```bash
python server.py
python client_tcp.py
python client_udp.py
```

### TCP (Transmission Control Protocol):
- **Надежный** - гарантирует доставку данных
- **С установкой соединения** - трёхстороннее рукопожатие
- **Потоковый** - данные передаются как непрерывный поток
- **Контроль перегрузки** - автоматическая регулировка скорости
- **Порядок доставки** - гарантирует правильную последовательность

### UDP (User Datagram Protocol):
- **Ненадёжный** - нет гарантии доставки
- **Без соединения** - отправка без предварительной установки
- **Датаграммный** - данные передаются отдельными пакетами
- **Минимальные накладные расходы** - меньше служебной информации
- **Быстрый** - нет контроля перегрузки и подтверждений
