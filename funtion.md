# DANH SÁCH CÁC CHỨC NĂNG ĐÃ XỬ LÝ

## 📋 TỔNG QUAN
Dự án **ClosetHub** - Ứng dụng bán quần áo với Android App và Node.js Server API.

---

## 1. 👤 QUẢN LÝ TÀI KHOẢN (Account Management)

### ✅ Đã xử lý:
- **Đăng ký tài khoản** (`POST /api/account/register`)
  - Đăng ký người dùng mới
  - Upload avatar khi đăng ký
  - Mã hóa mật khẩu bằng bcrypt
  - Tạo token JWT tự động sau khi đăng ký

- **Đăng nhập** (`POST /api/account/login`)
  - Xác thực email và password
  - Kiểm tra tài khoản có bị khóa không
  - Trả về token JWT và thông tin user

- **Upload Avatar** (`POST /api/account/upload-avatar`, `PUT /api/account/:_id/upload/avatar`)
  - Upload ảnh đại diện cho user
  - Lưu vào thư mục avatars

- **Cập nhật thông tin người dùng** (`PUT /api/account/update/:_id`)
  - Cập nhật email, phone, name, address
  - Upload avatar mới (tùy chọn)
  - Kiểm tra email trùng lặp

- **Lấy danh sách tất cả tài khoản** (`GET /api/account/list`)
  - Chỉ dành cho admin
  - Xác thực token và role

---

## 2. 🎨 QUẢN LÝ BANNER (Banner Management)

### ✅ Đã xử lý:
- **Thêm banner** (`POST /api/banner/sale/add`)
  - Upload ảnh banner
  - Lưu tên banner
  - Kiểm tra trùng tên banner
  - Chỉ admin mới được thêm

- **Lấy danh sách banner** (`GET /api/banner/sale/list`)
  - Hiển thị tất cả banner đang có
  - Không cần đăng nhập

---

## 3. 📂 QUẢN LÝ DANH MỤC (Category Management)

### ✅ Đã xử lý:
- **Thêm danh mục** (`POST /api/category/add`)
  - Thêm danh mục sản phẩm mới
  - Chỉ admin mới được thêm
  - Validate tên danh mục

- **Cập nhật danh mục** (`PUT /api/category/edit/:_id`)
  - Sửa tên danh mục
  - Chỉ admin mới được sửa

- **Xóa danh mục** (`DELETE /api/category/delete/:_id`)
  - Xóa danh mục khỏi hệ thống
  - Chỉ admin mới được xóa

- **Lấy danh sách danh mục** (`GET /api/category/list`)
  - Hiển thị tất cả danh mục

- **Lấy top 4 danh mục phổ biến** (`GET /api/categories/top4`)
  - Lấy 4 danh mục có nhiều sản phẩm nhất
  - Tính số lượng sản phẩm theo từng danh mục

---

## 4. 🛍️ QUẢN LÝ SẢN PHẨM (Product Management)

### ✅ Đã xử lý:
- **Thêm sản phẩm** (`POST /api/product/add`)
  - Tạo sản phẩm mới với thông tin cơ bản (name, catID, description, productCode)
  - Upload ảnh sản phẩm
  - Tạo variants (size, color, price, quantity, sku) cùng lúc
  - Kiểm tra trùng productCode và SKU
  - Sử dụng transaction để đảm bảo tính nhất quán

- **Chỉnh sửa sản phẩm** (`PUT /api/product/edit/:_id`)
  - Cập nhật tên và mô tả sản phẩm
  - Upload ảnh mới (tùy chọn)
  - Không cho phép sửa sản phẩm đã bị xóa

- **Xóa sản phẩm** (`DELETE /api/product/delete/:_id`)
  - Xóa mềm (soft delete) - đánh dấu is_deleted = true
  - Xóa mềm tất cả variants của sản phẩm
  - Không xóa dữ liệu thực sự

- **Lấy danh sách sản phẩm** (`GET /api/product/list`)
  - Hiển thị tất cả sản phẩm còn hoạt động
  - Tính tổng quantity, min/max price từ variants
  - Lấy ảnh từ variant đầu tiên
  - Hỗ trợ check is_favorite nếu có user_id
  - Chỉ hiển thị sản phẩm còn hàng (quantity > 0)

- **Lấy sản phẩm theo danh mục** (`GET /api/product/list-by-cat`)
  - Lọc sản phẩm theo category ID
  - Validate ObjectId
  - Tương tự như danh sách sản phẩm

- **Lấy top sản phẩm bán chạy** (`GET /api/product/list/top-selling`)
  - Lấy top 10 sản phẩm bán chạy nhất
  - Sắp xếp theo total_sold giảm dần
  - Hiển thị thông tin tổng số lượng đã bán

- **Lấy chi tiết sản phẩm** (`GET /api/product/:_id`)
  - Hiển thị thông tin chi tiết sản phẩm
  - Hiển thị tất cả variants (size, color, price, quantity)
  - Tính min/max price và tổng quantity
  - Lấy ảnh từ variants

- **Lấy danh sách sản phẩm cho admin** (`GET /api/product/list/admin`)
  - Lấy tất cả variants với thông tin product
  - Dành riêng cho admin
  - Hiển thị đầy đủ thông tin để quản lý

- **Cập nhật trạng thái yêu thích (Deprecated)** (`PUT /api/product/:_id/edit/favorite/:is_favorite`)
  - Đánh dấu sản phẩm là yêu thích
  - ⚠️ Deprecated: Dùng User Favorite API thay thế

- **Lấy danh sách sản phẩm yêu thích (Deprecated)** (`GET /api/product/list/favorite`)
  - Lấy sản phẩm có is_favorite = true
  - ⚠️ Deprecated: Dùng User Favorite API thay thế

---

## 5. 🔄 QUẢN LÝ VARIANTS (Product Variant Management)

### ✅ Đã xử lý:
- **Thêm variant** (`POST /api/variant/add` - trong product.variant.controller.js)
  - Thêm biến thể sản phẩm (size, color, price, quantity, sku)
  - Upload ảnh cho variant
  - Kiểm tra trùng SKU
  - Validate product tồn tại

---

## 6. ❤️ QUẢN LÝ YÊU THÍCH (User Favorite Management)

### ✅ Đã xử lý:
- **Thêm vào yêu thích** (`POST /api/favorite/add`)
  - Thêm sản phẩm vào danh sách yêu thích của user
  - Kiểm tra sản phẩm có tồn tại
  - Không thêm trùng lặp

- **Xóa khỏi yêu thích** (`DELETE /api/favorite/remove/:product_id`)
  - Xóa sản phẩm khỏi danh sách yêu thích

- **Toggle yêu thích** (`POST /api/favorite/toggle`)
  - Thêm nếu chưa có, xóa nếu đã có
  - Trả về trạng thái is_favorite

- **Kiểm tra trạng thái yêu thích** (`GET /api/favorite/check/:product_id`)
  - Kiểm tra sản phẩm có trong yêu thích của user không

- **Lấy danh sách sản phẩm yêu thích** (`GET /api/favorite/list`)
  - Hiển thị tất cả sản phẩm yêu thích của user
  - Kèm thông tin quantity, min/max price, image
  - Chỉ hiển thị sản phẩm còn hàng

---

## 7. 🛒 QUẢN LÝ GIỎ HÀNG (Cart Management)

### ✅ Đã xử lý:
- **Thêm vào giỏ hàng** (`POST /api/cart/add`)
  - Thêm sản phẩm (variant) vào giỏ hàng
  - Kiểm tra tồn kho trước khi thêm
  - Tự động gộp nếu variant đã có trong giỏ hàng
  - Validate product và variant tồn tại

- **Lấy danh sách giỏ hàng** (`GET /api/cart/list/:id_user`)
  - Hiển thị tất cả sản phẩm trong giỏ hàng của user
  - Populate thông tin product và variant
  - Sắp xếp theo ngày thêm mới nhất

- **Cập nhật số lượng** (`PUT /api/cart/update/:_id`)
  - Thay đổi số lượng sản phẩm trong giỏ hàng
  - Kiểm tra tồn kho trước khi cập nhật
  - Xóa item nếu quantity = 0

- **Xóa khỏi giỏ hàng** (`DELETE /api/cart/delete/:_id`)
  - Xóa sản phẩm khỏi giỏ hàng

---

## 8. 📦 QUẢN LÝ ĐƠN HÀNG (Order Management)

### ✅ Đã xử lý:
- **Đặt hàng** (`POST /api/order`)
  - Tạo đơn hàng từ giỏ hàng
  - Tính tổng tiền sản phẩm
  - Parse và tính phí vận chuyển từ address
  - Tạo bill và bill_details
  - Cập nhật số lượng tồn kho (giảm quantity, tăng total_sold)
  - Xóa giỏ hàng sau khi đặt hàng thành công
  - Validate tồn kho trước khi đặt hàng

- **Lấy lịch sử đơn hàng** (`GET /api/order/history/:id_user`)
  - Hiển thị tất cả đơn hàng của user
  - Kèm thông tin chi tiết sản phẩm (bill_details)
  - Tính subtotal (tiền sản phẩm) và shipping_fee (phí vận chuyển)
  - Sắp xếp theo ngày mới nhất

---

## 9. 💰 QUẢN LÝ VÍ ĐIỆN TỬ (Wallet Management)

### ✅ Đã xử lý:
- **Tạo ví** (`POST /api/wallet/create`)
  - Tạo ví điện tử cho user
  - Tạo mã ví tự động (W + timestamp + random)
  - Mã hóa PIN bằng bcrypt
  - Validate PIN (6 chữ số)
  - Cập nhật has_wallet cho user

- **Đăng nhập ví** (`POST /api/wallet/login`)
  - Xác thực PIN để đăng nhập vào ví
  - Tạo token cho ví
  - Validate PIN format

- **Xem thông tin ví** (`GET /api/wallet/info`)
  - Hiển thị thông tin ví (wallet_number, balance, total_deposits, total_withdrawals)

- **Kiểm tra số dư** (`GET /api/wallet/balance`)
  - Lấy số dư hiện tại của ví

- **Nạp tiền** (`POST /api/wallet/deposit`)
  - Nạp tiền vào ví
  - Xác thực PIN (tùy chọn)
  - Giới hạn tối đa 50.000.000đ
  - Lưu giao dịch vào transaction history
  - Cập nhật total_deposits

- **Rút tiền** (`POST /api/wallet/withdraw`)
  - Rút tiền từ ví
  - Bắt buộc xác thực PIN
  - Kiểm tra số dư đủ hay không
  - Lưu giao dịch vào transaction history
  - Cập nhật total_withdrawals

- **Đổi PIN** (`PUT /api/wallet/change-pin`)
  - Thay đổi PIN ví
  - Xác thực PIN cũ trước khi đổi
  - Validate PIN mới khác PIN cũ
  - Mã hóa PIN mới

- **Lịch sử giao dịch** (`GET /api/wallet/history`)
  - Hiển thị tất cả giao dịch nạp/rút tiền
  - Sắp xếp theo ngày mới nhất
  - Hiển thị số dư sau mỗi giao dịch

---

## 10. 🔐 XÁC THỰC VÀ PHÂN QUYỀN (Authentication & Authorization)

### ✅ Đã xử lý:
- **Middleware xác thực** (`middleware/api.auth.js`)
  - Xác thực JWT token
  - Kiểm tra role (admin, user)
  - Bảo vệ các route cần đăng nhập

- **Upload file** (`helpers/upload.helper.js`)
  - Hỗ trợ upload ảnh (avatar, banner, product)
  - Lưu vào các thư mục tương ứng
  - Sử dụng multer

---

## 11. 📱 ANDROID APP FEATURES

### ✅ Đã xử lý:
- **LoginActivity** - Màn hình đăng nhập
- **RegisterActivity** - Màn hình đăng ký
- **MainActivity** - Màn hình chính với bottom navigation
  - HomeFragment - Trang chủ
  - CartFragment - Giỏ hàng
  - BillFragment - Đơn hàng
  - FavoriteFragment - Yêu thích

- **LayoutBannerActivity** - Hiển thị banner
- **ItemCategoryActivity** - Danh mục sản phẩm
- **ItemProductActivity** - Danh sách sản phẩm
- **ProductDetailActivity** - Chi tiết sản phẩm
- **ItemColorProductActivity** - Chọn màu sản phẩm
- **ItemSizeProductActivity** - Chọn size sản phẩm
- **ItemProductCartActivity** - Quản lý giỏ hàng
- **HeaderBillActivity** - Quản lý đơn hàng
- **ItemBillActivity** - Chi tiết đơn hàng
- **ItemProductBillActivity** - Sản phẩm trong đơn hàng
- **PayActivity** - Thanh toán
- **AccountProfileActivity** - Thông tin tài khoản
- **WalletActivity** - Quản lý ví điện tử
- **WalletLoginActivity** - Đăng nhập ví
- **ViewAllProductActivity** - Xem tất cả sản phẩm

### ✅ Đã xử lý:
- **Retrofit API Service** - Kết nối với server API
- **SharedPreferences** - Lưu trữ token và thông tin user
- **Navigation Drawer** - Menu điều hướng
- **Bottom Navigation** - Thanh điều hướng dưới cùng
- **Fragment Management** - Quản lý các màn hình con

---

## 12. 🗄️ DATABASE MODELS

### ✅ Đã xử lý:
- **Account Model** - Quản lý tài khoản người dùng
- **Banner Model** - Quản lý banner
- **Category Model** - Quản lý danh mục
- **Product Model** - Quản lý sản phẩm
- **Product Variant Model** - Quản lý biến thể sản phẩm
- **Cart Model** - Quản lý giỏ hàng
- **Bill Model** - Quản lý hóa đơn
- **Bill Detail Model** - Chi tiết hóa đơn
- **Wallet Model** - Quản lý ví điện tử
- **Transaction Model** - Quản lý giao dịch ví
- **User Favorite Model** - Quản lý sản phẩm yêu thích

---

## 📝 GHI CHÚ

### Các chức năng deprecated:
- ❌ `PUT /api/product/:_id/edit/favorite/:is_favorite` - Thay bằng User Favorite API
- ❌ `GET /api/product/list/favorite` - Thay bằng `GET /api/favorite/list`

### Các tính năng đặc biệt:
- ✅ Soft delete (xóa mềm) cho Product và Variant
- ✅ Transaction cho các thao tác phức tạp
- ✅ Aggregate để tính toán thống kê
- ✅ Populate để lấy thông tin liên quan
- ✅ JWT Authentication
- ✅ Role-based access control (Admin/User)
- ✅ File upload với multer
- ✅ Password và PIN hashing với bcrypt

---

**Cập nhật lần cuối:** Hiện tại  
**Trạng thái:** ✅ Tất cả các chức năng đã được xử lý và test OK


