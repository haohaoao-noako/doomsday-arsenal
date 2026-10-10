# Doomsday Arsenal - Minecraft 1.20.1 (Fabric)

Vũ khí hủy diệt với hiệu ứng particle, rung màn hình và flash toàn màn hình.

| Vật phẩm | Cách dùng |
|---|---|
| **Orbital Strike Designator** | Nhắm vào một khối, chuột phải. Laser đỏ khóa mục tiêu 3.5 giây -> chùm tia từ quỹ đạo giáng xuống -> hố va chạm + sóng xung kích. |
| **Nuclear Bomb** (khối) | Châm bằng bật lửa / fire charge hoặc cấp tín hiệu redstone. Đếm ngược 5 giây (phá khối để vô hiệu hóa). Flash, quả cầu lửa, sóng xung kích xóa địa hình, mây nấm, bụi phóng xạ. |
| **Meteor Caller** | 26 thiên thạch có đuôi lửa rơi quanh mục tiêu. |
| **Thor's Wrath** | Mây bão đen + sét liên tục, sét tự dò mục tiêu sống. |
| **Singularity Core** | Hố đen có đĩa bồi tụ + tia phun, hút mọi thứ, ăn địa hình, co lại rồi nổ. |

Tất cả nằm trong tab creative **Doomsday Arsenal** và có công thức craft.

## Build ra file .jar

Cần JDK 17+.

**Cách 1 - có Gradle trên máy**
```
gradle build          # cần Gradle 8.5+
```
hoặc tạo wrapper một lần: `gradle wrapper --gradle-version 8.5` rồi `./gradlew build`.

**Cách 2 - không cài gì (GitHub Actions)**
Đẩy thư mục này lên một repo GitHub -> tab *Actions* -> chạy workflow **Build mod jar** -> tải artifact `doomsday-arsenal-jar`.

**Cách 3 - IntelliJ IDEA**
Open thư mục này như một project Gradle, chạy task `build`.

File kết quả: `build/libs/doomsday-arsenal-1.0.0.jar` (bỏ qua file `-sources.jar`).
Bỏ vào `.minecraft/mods/` cùng **Fabric Loader 0.15+** và **Fabric API 0.92.2+1.20.1**.

Lần build đầu sẽ tải Minecraft + mappings nên cần mạng và mất vài phút.

## Tinh chỉnh

Các hằng số nằm ở đầu mỗi file trong `src/main/java/net/doomsday/fx/`:

* `NukeEffect`: `FUSE` (thời gian đếm ngược), `CRATER_RADIUS` (bán kính hố, mặc định 30)
* `OrbitalStrikeEffect`: `CHARGE`, hố tạo trong `impact()`
* `MeteorShowerEffect`: `TOTAL` (số thiên thạch), `AREA`
* `ThorEffect`: `DURATION`, `AREA`
* `SingularityEffect`: `GROW`, `PULL_RANGE`

Nếu máy yếu / server đông người: giảm bán kính crater, giảm `points` của các vòng sóng xung kích (`Fx.ring`).
Mỗi hạt được gửi riêng cho từng người chơi trong 400 block, nên số người xem càng nhiều thì càng tốn băng thông.

Chạy `python3 tools/gen_textures.py` để tạo lại texture.
