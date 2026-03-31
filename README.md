# 🐢 TurtleGopy

> Hệ thống **Góp Ý**, **Báo Lỗi**, **Hỗ Trợ** và **Auto Broadcast** cho server Minecraft.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-green)](https://www.minecraft.net/)
[![Folia](https://img.shields.io/badge/Folia-Supported-blue)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)

---

## ✨ Tính năng

### 📝 Góp Ý (`/gopy`)
- Người chơi gửi góp ý qua **GUI** hoặc **lệnh nhanh**
- Theo dõi trạng thái góp ý (Chờ xử lý → Đã đọc → Chấp nhận → **Đang triển khai** → Đã triển khai / Từ chối)
- Admin quản lý, đánh giá và phản hồi góp ý
- **Tự động trao thưởng** khi góp ý được chấp nhận

### 🐛 Báo Lỗi (`/baoloi`)
- Người chơi báo lỗi qua **GUI** hoặc **lệnh nhanh** (`/baoloi <nội dung>`)
- Theo dõi trạng thái (Chờ xử lý → Đã đọc → **Đang kiểm tra** → Đang sửa → Đã sửa / Không phải lỗi)
- Admin quản lý và cập nhật trạng thái lỗi
- **Tự động trao thưởng** khi lỗi được xác nhận đã sửa

### 🎫 Hỗ Trợ (`/hotro`)
- Người chơi tạo phiếu hỗ trợ qua **GUI** hoặc **lệnh nhanh**
- **Giới hạn 1 phiếu** mỗi người chơi (tạo mới khi phiếu cũ đã giải quyết/từ chối)
- Theo dõi trạng thái (Chờ xử lý → Đã đọc → Đang xử lý → Đã giải quyết / Từ chối)
- **💬 Chat riêng** — luồng chat private giữa người chơi và staff/admin
  - Tin nhắn chỉ hiển thị trong luồng chat, **không hiện ở global**
  - **🔇 Cách ly chat global** — khi đang trong phiên hỗ trợ, người chơi **không thấy** tin nhắn chat global
  - **Async messaging** — không cần cả 2 bên online cùng lúc
  - Khi vào lại chat → hiện **toàn bộ lịch sử** cuộc trò chuyện
  - Admin online nhận notification khi có tin nhắn mới
- **Tự động trao thưởng** khi phiếu được giải quyết (có thể bật/tắt)

### 📢 Auto Broadcast
- Tự động gửi thông báo lên chat theo thời gian
- Hỗ trợ **nhiều nhóm** broadcast, mỗi nhóm interval riêng
- Gửi **cả cụm tin nhắn** cùng lúc (không phải từng dòng)
- Chế độ **tuần tự** hoặc **ngẫu nhiên**
- Lọc theo **permission**

### 🔧 Mending Repair
- Shift + Chuột phải khi cầm đồ có Mending để sửa bằng XP
- Cấu hình durability/XP ratio
- Hỗ trợ permission riêng

---

## 📋 Lệnh

### Người chơi (không cần permission)

| Lệnh | Mô tả |
|-------|-------|
| `/gopy` | Mở GUI góp ý |
| `/gopy <nội dung>` | Tạo nhanh góp ý |
| `/baoloi` | Mở GUI báo lỗi |
| `/baoloi <nội dung>` | Tạo nhanh báo lỗi |
| `/hotro` | Mở GUI hỗ trợ |
| `/hotro <nội dung>` | Tạo nhanh phiếu hỗ trợ |
| `/hotro exit` | Thoát chat hỗ trợ (về global) |

### Admin (`turtlegopy.admin`)

| Lệnh | Mô tả |
|-------|-------|
| `/gopy check` | Quản lý tất cả góp ý |
| `/baoloi check` | Quản lý tất cả báo lỗi |
| `/hotro check` | Quản lý tất cả phiếu hỗ trợ |
| `/gopy reload` | Reload config |

---

## 🔑 Permission

| Permission | Mô tả | Mặc định |
|-----------|-------|----------|
| `turtlegopy.admin` | Quản lý góp ý, báo lỗi, hỗ trợ, reload | OP |

> Người chơi thường **không cần permission** để sử dụng `/gopy`, `/baoloi` và `/hotro`.

---

## ⚙️ Cấu hình

### `config.yml`

#### Storage
```yaml
storage:
  type: YAML  # YAML hoặc DATABASE
  database:
    type: SQLITE  # MYSQL hoặc SQLITE
    host: localhost
    port: 3306
    name: turtlegopy
    username: root
    password: ""
```

#### Mending Repair
```yaml
mending-repair:
  enabled: true
  durability-per-xp: 2    # Số durability phục hồi mỗi 1 XP
  permission: ""           # Để trống = ai cũng dùng được
```

#### Phần thưởng
```yaml
# Thưởng khi góp ý được chấp nhận
rewards:
  enabled: true
  commands:
    - "crates give {player} amethyst 1"
    - "broadcast {player} đã nhận thưởng!"

# Thưởng khi báo lỗi đã sửa
bugreport-rewards:
  enabled: true
  commands:
    - "crates give {player} amethyst 1"

# Thưởng khi phiếu hỗ trợ được giải quyết
support-rewards:
  enabled: false
  commands:
    - "crates give {player} amethyst 1"
```

#### Support Chat
```yaml
support-chat:
  exit-word: "exit"  # Từ để thoát chat (ngoài /hotro exit)
```

#### Auto Broadcast
```yaml
broadcast:
  enabled: true
  groups:
    thongbao-chung:
      enabled: true
      interval: 5          # phút
      random: false         # tuần tự hay ngẫu nhiên
      permission: ""        # để trống = tất cả
      messages:
        1:                  # Cụm 1 - gửi cả cụm cùng lúc
          - "&8&m─────────────────────────────────"
          - "&6&l✉ GÓP Ý &8- &fGõ &e/gopy &fđể gửi góp ý!"
          - "&8&m─────────────────────────────────"
        2:                  # Cụm 2 - gửi lần tiếp theo
          - "&8&m─────────────────────────────────"
          - "&c&l🐛 BÁO LỖI &8- &fGõ &e/baoloi &fđể báo lỗi!"
          - "&8&m─────────────────────────────────"
        3:
          - "&8&m─────────────────────────────────"
          - "&3&l🎫 HỖ TRỢ &8- &fCần giúp đỡ? Gõ &e/hotro &fđể gửi yêu cầu!"
          - "&8&m─────────────────────────────────"
```

### `messages.yml`
Tất cả tin nhắn đều có thể tùy chỉnh trong file `messages.yml`. Hỗ trợ color code `&`.

---

## 💬 Hệ thống Chat Hỗ Trợ

Mỗi phiếu hỗ trợ có một **luồng chat riêng** để người chơi và staff trao đổi trực tiếp:

```
┌─────────────────────────────────────────────────┐
│  Vào chat:                                       │
│  • Player: /hotro → GUI → Click ticket           │
│  • Admin: /hotro check → Quản lý → "💬 Mở Chat"  │
├─────────────────────────────────────────────────┤
│  Trong chat:                                     │
│  ✅ Tin nhắn chỉ hiện cho người trong chat        │
│  ✅ Global chat KHÔNG thấy                        │
│  🔇 Người chơi KHÔNG nhận tin nhắn global        │
│  ✅ Admin online nhận notification                │
│  ✅ Lưu persistent (YAML/Database)                │
├─────────────────────────────────────────────────┤
│  Thoát: gõ "exit" hoặc /hotro exit               │
│  → Trở về chat global                            │
├─────────────────────────────────────────────────┤
│  Async: không cần 2 bên online cùng lúc          │
│  → Vào lại → thấy toàn bộ lịch sử chat          │
└─────────────────────────────────────────────────┘
```

---

## 🏗️ Kiến trúc

```
TurtleGopy/
├── TurtleGopy-api/          # API - Models & Interfaces
│   ├── model/
│   │   ├── Feedback.java
│   │   ├── FeedbackStatus.java       # +DEPLOYING
│   │   ├── BugReport.java
│   │   ├── BugReportStatus.java      # +CHECKING
│   │   ├── SupportTicket.java
│   │   ├── SupportTicketStatus.java
│   │   └── SupportChatMessage.java
│   └── storage/
│       ├── StorageProvider.java
│       ├── BugReportStorageProvider.java
│       ├── SupportTicketStorageProvider.java
│       └── SupportChatStorageProvider.java
│
├── TurtleGopy-implement/    # Logic - Managers, Commands, GUI, Storage
│   ├── core/
│   │   ├── TurtleGopyCore.java
│   │   └── SchedulerUtil.java
│   ├── command/
│   │   ├── GopyCommand.java          # /gopy, /baoloi, /hotro
│   │   └── BaoLoiCommand.java        # /baoloi (standalone)
│   ├── manager/
│   │   ├── FeedbackManager.java
│   │   ├── BugReportManager.java
│   │   ├── SupportTicketManager.java
│   │   ├── SupportChatManager.java
│   │   └── BroadcastManager.java
│   ├── gui/
│   │   ├── PlayerGUI.java
│   │   ├── AdminGUI.java
│   │   ├── AdminManageGUI.java
│   │   ├── PlayerBugReportGUI.java
│   │   ├── AdminBugReportGUI.java
│   │   ├── AdminBugManageGUI.java
│   │   ├── PlayerSupportGUI.java
│   │   ├── AdminSupportGUI.java
│   │   ├── AdminSupportManageGUI.java
│   │   └── GUIListener.java
│   ├── storage/
│   │   ├── YamlStorageProvider.java
│   │   ├── DatabaseStorageProvider.java
│   │   ├── YamlBugReportStorageProvider.java
│   │   ├── DatabaseBugReportStorageProvider.java
│   │   ├── YamlSupportTicketStorageProvider.java
│   │   ├── DatabaseSupportTicketStorageProvider.java
│   │   ├── YamlSupportChatStorageProvider.java
│   │   └── DatabaseSupportChatStorageProvider.java
│   └── listener/
│       ├── ChatInputListener.java
│       ├── SupportChatListener.java
│       └── MendingRepairListener.java
│
└── TurtleGopy-plugin/       # Entry point
    ├── TurtleGopyPlugin.java
    └── resources/
        ├── plugin.yml
        ├── config.yml
        └── messages.yml
```

---

## 🔧 Build

**Yêu cầu:** Java 21+, Maven 3.9+

```bash
mvn clean package -DskipTests
```

JAR output: `TurtleGopy-plugin/target/TurtleGopy.jar`

---

## 📦 Cài đặt

1. Build hoặc download `TurtleGopy.jar`
2. Copy vào folder `plugins/` của server
3. Khởi động lại server
4. Chỉnh sửa `plugins/TurtleGopy/config.yml` theo ý muốn
5. `/gopy reload` để áp dụng thay đổi

---

## 🌐 Tương thích

| Platform | Hỗ trợ |
|----------|--------|
| Spigot 1.21.x | ✅ |
| Paper 1.21.x | ✅ |
| Folia 1.21.x | ✅ |
| Purpur 1.21.x | ✅ |

**Soft-depend:** TurtleCore (không bắt buộc)

---

## 📄 License

© 2024 Turtle. All rights reserved.
