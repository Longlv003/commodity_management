const db = require("./db");

const userFavoriteSchema = new db.mongoose.Schema(
  {
    user_id: {
      type: db.mongoose.Schema.Types.ObjectId,
      ref: "userModel",
      required: true,
    },
    product_id: {
      type: db.mongoose.Schema.Types.ObjectId,
      ref: "pModel",
      required: true,
    },
    created_date: {
      type: Date,
      default: Date.now,
    },
  },
  {
    collection: "user_favorite",
    timestamps: false,
  }
);

// Index để tránh duplicate và tối ưu query
userFavoriteSchema.index({ user_id: 1, product_id: 1 }, { unique: true });
userFavoriteSchema.index({ user_id: 1 });
userFavoriteSchema.index({ product_id: 1 });

const userFavoriteModel = db.mongoose.model("userFavoriteModel", userFavoriteSchema);
module.exports = { userFavoriteModel };