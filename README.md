# 👗 ClosetHub

**Dự án 1 (PRO1122)** — Ứng dụng Bán Quần Áo

---

## 🛍️ Giới thiệu

**ClosetHub** là ứng dụng **bán và quản lý quần áo thời trang**, giúp người dùng dễ dàng **xem, chọn mua, và quản lý sản phẩm** ngay trên điện thoại.  
Ứng dụng hướng đến trải nghiệm mua sắm tiện lợi, hiện đại, phù hợp với các cửa hàng hoặc cá nhân kinh doanh thời trang.

---

## ✨ Tính năng chính

- 🧥 **Hiển thị danh sách sản phẩm** (tên, giá, mô tả, size, màu sắc, hình ảnh)
- 🔍 **Tìm kiếm và lọc quần áo** theo loại, giá hoặc tên
- ➕ **Thêm, sửa, xoá sản phẩm** (dành cho người quản lý)
- 🛒 **Giỏ hàng và thanh toán đơn giản**
- 🔄 **Đồng bộ dữ liệu với server** (Node.js + MongoDB) để cập nhật sản phẩm nhanh chóng

---

## 🧩 Cấu trúc dự án

Dự án gồm **2 phần chính**:

### 1. 📱 App Android (ClosetHub Mobile)

- Phát triển bằng **Android Studio (Java)**
- Giao diện thân thiện, dễ sử dụng
- Kết nối đến server qua **RESTful API**

### 2. 💻 Server (ClosetHub API)

- Xây dựng bằng **Node.js + Express + MongoDB**
- Quản lý dữ liệu sản phẩm, tài khoản người dùng và giỏ hàng
- Cung cấp **API** cho ứng dụng Android kết nối và trao đổi dữ liệu

---

## 🧠 Công nghệ sử dụng

| Thành phần         | Công nghệ                            |
| ------------------ | ------------------------------------ |
| **Ngôn ngữ**       | Java (Android), JavaScript (Node.js) |
| **Cơ sở dữ liệu**  | MongoDB                              |
| **Backend**        | Node.js + Express                    |
| **Frontend (App)** | Android Studio                       |
| **API**            | RESTful API                          |

---

## 🚀 Mục tiêu dự án

- Xây dựng ứng dụng bán quần áo tiện lợi, dễ sử dụng
- Giúp người bán dễ dàng quản lý sản phẩm và đơn hàng
- Cải thiện trải nghiệm mua sắm của người dùng thông qua giao diện thân thiện và dữ liệu được đồng bộ theo thời gian thực

---

## ⚙️ Hướng dẫn cài đặt (tùy chọn)

### 🔹 1. Clone dự án

```bash
npm install -g nodemon
git clone https://github.com/yourusername/closethub.git
cd closethub/server
```

### 🔸 2. Server

1. Cài Node.js và MongoDB
2. Chạy lệnh:
   ```bash
   npm i jsonwebtoken
   npm i dotenv
   npm i bcrypt
   npm install mongoose --save
   npm install multer
   nodemon npm start
   ```

## ⚙️ Hướng dẫn cài đặt ngrok (để dùng chung server không cần khởi tạo lại dữ liệu server, cứ thể viết app)

### 1. Cài đặt ngrok:

```bash
npm install -g ngrok
```

### 2. ĐĂng ký account:

1. Truy cập [https://dashboard.ngrok.com](https://dashboard.ngrok.com) và tạo tài khoản (miễn phí).
2. Copy $YOUR_AUTHTOKEN sau khi đăng nhập ngrok tại vị trí như ảnh:
   ![alt text](ngrok.png)
   Sau đó chạy lệnh (thay $YOUR_AUTHTOKEN là mã của mình):
   ```bash
   ngrok config add-authtoken $YOUR_AUTHTOKEN
   ```
   (Nếu chạy bị lỗi hãy reset Authtoken và copy mã mới chạy lại lệnh đó.)

### 3. Khởi chạy:

```bash
ngrok.exe http 3000
```

=> Share link ngrok

### 4. Sử dụng server:

Mở code phần: public static final String DEV_NGROK = "https://f543eee710de.ngrok-free.app"; sửa thành link ngrok.
(Nếu server đang chạy máy khác, hãy sửa link thành link ngrok ở máy đang chạy server).
