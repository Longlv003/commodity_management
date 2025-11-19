const db = require("./db");
const jwt = require("jsonwebtoken");
require("dotenv").config();
const token_auto = process.env.TOKEN_SEC_KEY;
const bcrypt = require("bcrypt");

const walletSchema = new db.mongoose.Schema(
  {
    id_user: {
      type: db.mongoose.Schema.Types.ObjectId,
      required: true,
      ref: "userModel",
      unique: true,
    },
    wallet_number: {
      type: String,
      required: true,
      unique: true,
      trim: true,
    },
    total_deposits: {
      type: Number,
      required: true,
      default: 0,
      min: 0,
    },
    total_withdrawals: {
      type: Number,
      required: true,
      default: 0,
      min: 0,
    },
    balance: {
      type: Number,
      required: true,
      default: 0,
      min: 0,
    },
    pin_hash: {
      type: String,
      required: true,
    },
    is_active: {
      type: Boolean,
      default: true,
    },
    create_date: {
      type: Date,
      default: Date.now,
    },
    updated_date: {
      type: Date,
      default: Date.now,
    },
  },
  {
    collection: "wallet",
    timestamps: false,
  }
);

// Indexes để tối ưu query
// Note: id_user và wallet_number đã có unique: true nên tự động có index
// Chỉ cần index cho create_date để tối ưu query sắp xếp theo ngày
walletSchema.index({ create_date: -1 });

// Pre-save hook: Tự động cập nhật updated_date
walletSchema.pre("save", function (next) {
  if (!this.isNew) {
    this.updated_date = new Date();
  }
  next();
});

// Static method: Tạo token JWT cho ví
walletSchema.statics.makeAuthToken = async function (wallet) {
  const token = jwt.sign(
    {
      _id: wallet._id,
      wallet_number: wallet.wallet_number,
    },
    token_auto,
    { expiresIn: "24h" }
  );
  return token;
};

// Static method: Xác thực token JWT
walletSchema.statics.verifyToken = async function (token) {
  try {
    const decoded = jwt.verify(token, token_auto);
    const wallet = await this.findById(decoded._id);
    if (!wallet) {
      throw new Error("Wallet not found");
    }
    return wallet;
  } catch (error) {
    throw new Error("Invalid or expired token");
  }
};

// Static method: Validate PIN format (6 chữ số)
walletSchema.statics.validatePin = function (pin) {
  if (!pin || typeof pin !== "string") {
    throw new Error("PIN không được để trống");
  }
  if (pin.length !== 6) {
    throw new Error("PIN phải có đúng 6 chữ số");
  }
  if (!/^\d+$/.test(pin)) {
    throw new Error(
      "PIN chỉ được chứa số, không được chứa chữ cái hoặc ký tự đặc biệt"
    );
  }
  return true;
};

// Static method: Tìm ví bằng credentials (wallet_number và PIN)
walletSchema.statics.findByCredentials = async function (wallet_number, pin) {
  // Validate PIN format trước
  this.validatePin(pin);

  const wallet = await this.findOne({ wallet_number });
  if (!wallet) {
    throw new Error("Wallet not found");
  }

  const isPinValid = await bcrypt.compare(pin, wallet.pin_hash);
  if (!isPinValid) {
    throw new Error("Invalid PIN");
  }

  return wallet;
};

let walletModel = db.mongoose.model("walletModel", walletSchema);
module.exports = { walletModel };
