# Chặn Cuộc Gọi Rác (spam-call-blocker)

Ứng dụng Android tự động chặn các cuộc gọi đến có đầu số rác (`025`, `022`, `023`, `026`, `028`, `020`), cho phép người dùng tick bật/tắt hoặc thêm/xóa đầu số và lưu bền vững vào bộ nhớ ứng dụng (Room SQLite), đồng thời xin đầy đủ các quyền hệ thống để duy trì chạy ngầm ổn định không bị thiết bị tự động kill.

## Tính năng chính

1. **Tự động chặn cuộc gọi theo đầu số (`SpamCallScreeningService`)**
   - Sử dụng Android `CallScreeningService` (`RoleManager.ROLE_CALL_SCREENING`, Android 10 / API 29+).
   - Tự động chuẩn hóa số gọi đến (`+84...`, `0084...`, `84...`, định dạng có dấu cách/gạch ngang) về dạng `0...`.
   - Khớp với danh sách đầu số đang bật; tự động từ chối cuộc gọi và ghi nhận vào bảng lịch sử.

2. **Quản lý đầu số & lưu trữ cục bộ (`Room Database`)**
   - Khởi tạo sẵn 6 đầu số mặc định: **`025`, `022`, `023`, `026`, `028`, `020`** (đều được tick bật sẵn).
   - Tick/bỏ tick từng đầu số để bật/tắt chặn ngay lập tức.
   - Hỗ trợ thêm đầu số mới (ví dụ `024`, `029`, `1900`...) và xóa đầu số tự thêm.

3. **Chạy ngầm bền vững, chống bị hệ thống kill**
   - **Call Screening System Role**: Hệ thống tự gọi `SpamCallScreeningService` khi có cuộc gọi đến ngay cả khi app đang đóng.
   - **Foreground Service (`KeepAliveForegroundService`)**: Chạy với `START_STICKY` và `foregroundServiceType="specialUse"`, kèm `onTaskRemoved()` đặt `AlarmManager` tự khởi chạy lại nếu bị vuốt tắt khỏi Recents.
   - **Battery Optimization Exemption**: Xin quyền `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` để thoát cơ chế Doze/App Standby.
   - **Boot & Update Receiver (`BootCompletedReceiver`)**: Tự khởi chạy lại sau khi khởi động máy (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`).
   - **WorkManager Watchdog (`KeepAliveWorker`)**: Kiểm tra định kỳ mỗi 15 phút để đảm bảo dịch vụ nền luôn hoạt động.
   - **OEM Auto-Start Helper (`OemAutoStartHelper`)**: Nút mở nhanh màn hình Tự khởi chạy / Không hạn chế pin trên Xiaomi, Oppo, Vivo, Huawei, Samsung, OnePlus, Asus.

4. **Giao diện đơn giản (Jetpack Compose + Material 3)**
   - **Phần trên**: Bảng (`Table`) hiển thị lịch sử các cuộc gọi bị chặn (`STT` | `Số điện thoại` | `Đầu số` | `Thời gian`).
   - **Phần dưới**: Component quản lý đầu số (checkbox tick bật/tắt từng đầu số + ô thêm đầu số mới).

## Hướng dẫn build & chạy

- **Yêu cầu**: Android Studio Ladybug (2024.2+) hoặc JDK 17 + Android SDK Platform 35 (Android 15), thiết bị Android 10 (API 29) trở lên.
- Mở thư mục `D:\Source\spam-call-blocker` bằng Android Studio -> Sync Gradle -> Run lên điện thoại Android.
- Hoặc build bằng dòng lệnh (sau khi đã cài JDK 17 & Android SDK):
  ```bash
  ./gradlew assembleDebug
  ./gradlew testDebugUnitTest
  ```
- Trong lần mở đầu tiên, cấp đủ các mục quyền hiển thị trên thanh trên cùng (hoặc nhấn biểu tượng chiếc khiên góc phải) để đảm bảo ứng dụng không bị hệ thống kill khi chạy ngầm.
