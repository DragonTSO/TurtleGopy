# 🐢 TurtleGopy

> Hệ thống **Góp Ý**, **Báo Lỗi** và **Auto Broadcast** cho server Minecraft.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-green)](https://www.minecraft.net/)
[![Folia](https://img.shields.io/badge/Folia-Supported-blue)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)

---

## ✨ Tính năng

### 📝 Góp Ý (`/gopy`)
- Người chơi gửi góp ý qua **GUI** hoặc **lệnh nhanh**
- Theo dõi trạng thái góp ý (Chờ xử lý → Đã đọc → Chấp nhận / Từ chối)
- Admin quản lý, đánh giá và phản hồi góp ý
- **Tự động trao thưởng** khi góp ý được chấp nhận

### 🐛 Báo Lỗi (`/baoloi`)
- Người chơi báo lỗi qua **GUI** hoặc **lệnh nhanh**
- Theo dõi trạng thái (Chờ xử lý → Đã đọc → Đang sửa → Đã sửa / Không phải lỗi)
- Admin quản lý và cập nhật trạng thái lỗi
- **Tự động trao thưởng** khi lỗi được xác nhận đã sửa

### 📢 Auto Broadcast
- Tự động gửi thông báo lên chat theo thời gian
- Hỗ trợ **nhiều nhóm** broadcast, mỗi nhóm interval riêng
- Gửi **cả cụm tin nhắn** cùng lúc (không phải từng dòng)
- Chế độ **tuần tự** hoặc **ngẫu nhiên**
- Lọc theo **permission**

---

## 📋 Lệnh

### Người chơi (không cần permission)

| Lệnh | Mô tả |
|-------|-------|
| `/gopy` | Mở GUI góp ý |
| `/gopy <nội dung>` | Tạo nhanh góp ý |
| `/baoloi` | Mở GUI báo lỗi |
| `/baoloi <nội dung>` | Tạo nhanh báo lỗi |

### Admin (`turtlegopy.admin`)

| Lệnh | Mô tả |
|-------|-------|
| `/gopy check` | Quản lý tất cả góp ý |
| `/baoloi check` | Quản lý tất cả báo lỗi |
| `/gopy reload` | Reload config |

---

## 🔑 Permission

| Permission | Mô tả | Mặc định |
|-----------|-------|----------|
| `turtlegopy.admin` | Quản lý góp ý, báo lỗi, reload | OP |

> Người chơi thường **không cần permission** để sử dụng `/gopy` và `/baoloi`.

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
```

### `messages.yml`
Tất cả tin nhắn đều có thể tùy chỉnh trong file `messages.yml`. Hỗ trợ color code `&`.

---

## 🏗️ Kiến trúc

```
TurtleGopy/
├── TurtleGopy-api/          # API - Models & Interfaces
│   ├── Feedback.java
│   ├── FeedbackStatus.java
│   ├── BugReport.java
│   ├── BugReportStatus.java
│   ├── StorageProvider.java
│   └── BugReportStorageProvider.java
│
├── TurtleGopy-implement/    # Logic - Managers, Commands, GUI, Storage
│   ├── core/
│   │   ├── TurtleGopyCore.java
│   │   └── SchedulerUtil.java      # Folia-compatible scheduler
│   ├── command/
│   │   └── GopyCommand.java        # Handles /gopy & /baoloi
│   ├── manager/
│   │   ├── FeedbackManager.java
│   │   ├── BugReportManager.java
│   │   └── BroadcastManager.java
│   ├── gui/
│   │   ├── PlayerGUI.java
│   │   ├── AdminGUI.java
│   │   ├── AdminManageGUI.java
│   │   ├── PlayerBugReportGUI.java
│   │   ├── AdminBugReportGUI.java
│   │   └── AdminBugManageGUI.java
│   ├── storage/
│   │   ├── YamlStorageProvider.java
│   │   ├── DatabaseStorageProvider.java
│   │   ├── YamlBugReportStorageProvider.java
│   │   └── DatabaseBugReportStorageProvider.java
│   └── listener/
│       ├── ChatInputListener.java
│       └── GUIListener.java
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
