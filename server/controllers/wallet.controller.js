const { walletModel } = require("../models/wallet.model");
const bcrypt = require("bcrypt");
const { userModel } = require("../models/account.model");
const { transactionModel } = require("../models/transaction.model");

// ==================== TẠO VÍ ====================
exports.CreateWallet = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { pin } = req.body;
    const userId = req.user._id; // Lấy từ middleware authentication

    // Validate PIN bằng method từ model
    try {
      walletModel.validatePin(pin);
    } catch (error) {
      return res.status(400).json({ msg: error.message, data: null });
    }

    // Kiểm tra user
    const user = await userModel.findById(userId);
    if (!user) {
      return res
        .status(404)
        .json({ msg: "Không tìm thấy người dùng", data: null });
    }

    // Kiểm tra user đã có ví chưa
    if (user.has_wallet) {
      return res.status(400).json({ msg: "Người dùng đã có ví", data: null });
    }

    // Double check: Kiểm tra wallet với user_id đã tồn tại chưa
    const existingWallet = await walletModel.findOne({ id_user: userId });
    if (existingWallet) {
      // Đồng bộ lại has_wallet
      user.has_wallet = true;
      await user.save();
      return res
        .status(400)
        .json({ msg: "Ví đã tồn tại cho người dùng này", data: null });
    }

    // Tạo wallet number tự động (W + timestamp + random)
    const wallet_number = "W" + Date.now() + Math.floor(Math.random() * 1000);

    // Hash PIN
    const salt = await bcrypt.genSalt(10);
    const pin_hash = await bcrypt.hash(pin, salt);

    // Tạo wallet
    const wallet = new walletModel({
      id_user: userId,
      wallet_number,
      pin_hash,
      balance: 0,
      total_deposits: 0,
      total_withdrawals: 0,
    });

    await wallet.save();

    // Cập nhật user - đã có ví
    user.has_wallet = true;
    await user.save();

    dataRes.msg = "Tạo ví thành công";
    dataRes.data = {
      wallet_number: wallet.wallet_number,
      balance: wallet.balance,
      create_date: wallet.create_date,
    };

    return res.status(201).json(dataRes);
  } catch (error) {
    console.error("CreateWallet Error:", error);
    dataRes.msg = error.message || "Lỗi tạo ví";
    return res.status(500).json(dataRes);
  }
};

// ==================== ĐĂNG NHẬP VÍ ====================
exports.LoginWallet = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { pin } = req.body;
    const userId = req.user._id; // Lấy user ID từ token (đã xác thực qua middleware api_auth)

    if (!pin) {
      return res
        .status(400)
        .json({ msg: "Thiếu PIN", data: null });
    }

    // Validate PIN format
    try {
      walletModel.validatePin(pin);
    } catch (error) {
      return res.status(400).json({ msg: error.message, data: null });
    }

    // Tìm ví của user (token user đã xác thực)
    const wallet = await walletModel.findOne({ id_user: userId });
    if (!wallet) {
      return res
        .status(404)
        .json({ msg: "Không tìm thấy ví cho người dùng này", data: null });
    }

    // Xác thực PIN
    const isPinValid = await bcrypt.compare(pin, wallet.pin_hash);
    if (!isPinValid) {
      return res
        .status(401)
        .json({ msg: "PIN không đúng", data: null });
    }

    // Tạo token cho ví
    const token = await walletModel.makeAuthToken(wallet);

    dataRes.msg = "Đăng nhập ví thành công";
    dataRes.data = {
      wallet: {
        _id: wallet._id,
        wallet_number: wallet.wallet_number,
        balance: wallet.balance,
        id_user: wallet.id_user,
      },
      token: token,
    };

    return res.json(dataRes);
  } catch (error) {
    console.error("LoginWallet Error:", error);
    dataRes.msg = error.message || "Đăng nhập ví thất bại";
    return res.status(401).json(dataRes);
  }
};

// ==================== XEM THÔNG TIN VÍ ====================
exports.GetWalletInfo = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const userId = req.user._id;

    // Tìm ví của user
    const wallet = await walletModel
      .findOne({ id_user: userId })
      .select("-pin_hash") // Không trả về PIN hash
      .populate("id_user", "name email image");

    if (!wallet) {
      return res.status(404).json({ msg: "Không tìm thấy ví", data: null });
    }

    dataRes.data = {
      _id: wallet._id,
      wallet_number: wallet.wallet_number,
      balance: wallet.balance,
      total_deposits: wallet.total_deposits,
      total_withdrawals: wallet.total_withdrawals,
      create_date: wallet.create_date,
      id_user: wallet.id_user,
    };

    return res.json(dataRes);
  } catch (error) {
    console.error("GetWalletInfo Error:", error);
    dataRes.msg = error.message || "Lỗi lấy thông tin ví";
    return res.status(500).json(dataRes);
  }
};

// ==================== KIỂM TRA SỐ DƯ ====================
exports.CheckBalance = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const userId = req.user._id;

    const wallet = await walletModel
      .findOne({ id_user: userId })
      .select("balance wallet_number");

    if (!wallet) {
      return res.status(404).json({ msg: "Không tìm thấy ví", data: null });
    }

    dataRes.data = {
      wallet_number: wallet.wallet_number,
      balance: wallet.balance,
    };

    return res.json(dataRes);
  } catch (error) {
    console.error("CheckBalance Error:", error);
    dataRes.msg = error.message || "Lỗi kiểm tra số dư";
    return res.status(500).json(dataRes);
  }
};

// ==================== NẠP TIỀN ====================
exports.Deposit = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { amount, pin } = req.body;
    const userId = req.user._id;

    // Validate
    if (!amount || amount <= 0) {
      return res
        .status(400)
        .json({ msg: "Số tiền nạp phải lớn hơn 0", data: null });
    }

    if (amount > 50000000) {
      // Giới hạn 10 triệu
      return res
        .status(400)
        .json({ msg: "Số tiền nạp tối đa là 50.000.000đ", data: null });
    }

    // Tìm ví
    const wallet = await walletModel.findOne({ id_user: userId });
    if (!wallet) {
      return res.status(404).json({ msg: "Không tìm thấy ví", data: null });
    }

    // Xác thực PIN nếu có
    if (pin) {
      // Validate PIN format trước
      try {
        walletModel.validatePin(pin);
      } catch (error) {
        return res.status(400).json({ msg: error.message, data: null });
      }

      const isPinValid = await bcrypt.compare(pin, wallet.pin_hash);
      if (!isPinValid) {
        return res.status(401).json({ msg: "PIN không đúng", data: null });
      }
    }

    // Cập nhật số dư
    wallet.balance += amount;
    wallet.total_deposits += amount;

    await wallet.save();

    // Lưu giao dịch
    const transaction = new transactionModel({
      id_wallet: wallet._id,
      type: "deposit",
      amount: amount,
      description: `Nạp tiền vào ví`,
      balance_after: wallet.balance,
    });
    await transaction.save();

    dataRes.msg = "Nạp tiền thành công";
    dataRes.data = {
      wallet_number: wallet.wallet_number,
      amount: amount,
      new_balance: wallet.balance,
    };

    return res.json(dataRes);
  } catch (error) {
    console.error("Deposit Error:", error);
    dataRes.msg = error.message || "Lỗi nạp tiền";
    return res.status(500).json(dataRes);
  }
};

// ==================== RÚT TIỀN ====================
exports.Withdraw = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { amount, pin } = req.body;
    const userId = req.user._id;

    // Validate
    if (!amount || amount <= 0) {
      return res
        .status(400)
        .json({ msg: "Số tiền rút phải lớn hơn 0", data: null });
    }

    if (!pin) {
      return res
        .status(400)
        .json({ msg: "Vui lòng nhập PIN để rút tiền", data: null });
    }

    // Validate PIN format trước
    try {
      walletModel.validatePin(pin);
    } catch (error) {
      return res.status(400).json({ msg: error.message, data: null });
    }

    // Tìm ví
    const wallet = await walletModel.findOne({ id_user: userId });
    if (!wallet) {
      return res.status(404).json({ msg: "Không tìm thấy ví", data: null });
    }

    // Xác thực PIN
    const isPinValid = await bcrypt.compare(pin, wallet.pin_hash);
    if (!isPinValid) {
      return res.status(401).json({ msg: "PIN không đúng", data: null });
    }

    // Kiểm tra số dư
    if (wallet.balance < amount) {
      return res.status(400).json({
        msg: "Số dư không đủ",
        data: {
          current_balance: wallet.balance,
          requested_amount: amount,
        },
      });
    }

    // Cập nhật số dư
    wallet.balance -= amount;
    wallet.total_withdrawals += amount;

    await wallet.save();

    // Lưu giao dịch
    const transaction = new transactionModel({
      id_wallet: wallet._id,
      type: "withdraw",
      amount: amount,
      description: `Rút tiền từ ví`,
      balance_after: wallet.balance,
    });
    await transaction.save();

    dataRes.msg = "Rút tiền thành công";
    dataRes.data = {
      wallet_number: wallet.wallet_number,
      amount: amount,
      new_balance: wallet.balance,
    };

    return res.json(dataRes);
  } catch (error) {
    console.error("Withdraw Error:", error);
    dataRes.msg = error.message || "Lỗi rút tiền";
    return res.status(500).json(dataRes);
  }
};

// ==================== ĐỔI PIN ====================
exports.ChangePin = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { old_pin, new_pin } = req.body;
    const userId = req.user._id;

    // Validate
    if (!old_pin || !new_pin) {
      return res
        .status(400)
        .json({ msg: "Thiếu old_pin hoặc new_pin", data: null });
    }

    // Validate PIN cũ và PIN mới bằng method từ model
    try {
      walletModel.validatePin(old_pin);
      walletModel.validatePin(new_pin);
    } catch (error) {
      return res.status(400).json({ msg: error.message, data: null });
    }

    if (old_pin === new_pin) {
      return res
        .status(400)
        .json({ msg: "PIN mới phải khác PIN cũ", data: null });
    }

    // Tìm ví
    const wallet = await walletModel.findOne({ id_user: userId });
    if (!wallet) {
      return res.status(404).json({ msg: "Không tìm thấy ví", data: null });
    }

    // Xác thực PIN cũ
    const isOldPinValid = await bcrypt.compare(old_pin, wallet.pin_hash);
    if (!isOldPinValid) {
      return res.status(401).json({ msg: "PIN cũ không đúng", data: null });
    }

    // Hash PIN mới
    const salt = await bcrypt.genSalt(10);
    const new_pin_hash = await bcrypt.hash(new_pin, salt);

    // Cập nhật PIN
    wallet.pin_hash = new_pin_hash;
    await wallet.save();

    dataRes.msg = "Đổi PIN thành công";

    return res.json(dataRes);
  } catch (error) {
    console.error("ChangePin Error:", error);
    dataRes.msg = error.message || "Lỗi đổi PIN";
    return res.status(500).json(dataRes);
  }
};

// ==================== LỊCH SỬ GIAO DỊCH ====================
exports.GetTransactionHistory = async (req, res) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const userId = req.user._id;

    // Tìm ví của user
    const wallet = await walletModel
      .findOne({ id_user: userId })
      .select("_id wallet_number");

    if (!wallet) {
      return res.status(404).json({ msg: "Không tìm thấy ví", data: null });
    }

    // Lấy danh sách giao dịch (sắp xếp mới nhất trước)
    const transactions = await transactionModel
      .find({ id_wallet: wallet._id })
      .sort({ created_date: -1 })
      .select("type amount description balance_after created_date")
      .lean();

    // Format lại dữ liệu để trả về
    const transactionList = transactions.map((t) => ({
      _id: t._id,
      type: t.type,
      amount: t.amount,
      description:
        t.description ||
        (t.type === "deposit" ? "Nạp tiền vào ví" : "Rút tiền từ ví"),
      balance_after: t.balance_after,
      created_date: t.created_date,
    }));

    dataRes.data = transactionList;

    return res.json(dataRes);
  } catch (error) {
    console.error("GetTransactionHistory Error:", error);
    dataRes.msg = error.message || "Lỗi lấy lịch sử giao dịch";
    return res.status(500).json(dataRes);
  }
};
