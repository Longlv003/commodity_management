const db = require("./db");

const transactionSchema = new db.mongoose.Schema(
  {
    id_wallet: {
      type: db.mongoose.Schema.Types.ObjectId,
      required: true,
      ref: "walletModel",
      index: true,
    },
    type: {
      type: String,
      required: true,
      enum: ["deposit", "withdraw"],
      index: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 0,
    },
    description: {
      type: String,
      default: "",
    },
    balance_after: {
      type: Number,
      required: true,
      min: 0,
    },
    created_date: {
      type: Date,
      default: Date.now,
      index: true,
    },
  },
  {
    collection: "transaction",
  }
);

// Indexes để tối ưu query
transactionSchema.index({ id_wallet: 1, created_date: -1 });
transactionSchema.index({ id_wallet: 1, type: 1, created_date: -1 });

let transactionModel = db.mongoose.model("transactionModel", transactionSchema);
module.exports = { transactionModel };