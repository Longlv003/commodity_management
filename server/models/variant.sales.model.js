const db = require("./db");

const variantSalesSchema = new db.mongoose.Schema(
  {
    variant_id: {
      type: db.mongoose.Types.ObjectId,
      ref: "pVariantModel",
      required: true,
    },
    product_id: {
      type: db.mongoose.Types.ObjectId,
      ref: "pModel",
      required: true,
    },
    quantity_sold: { type: Number, required: true, min: 1 },
    sale_date: { type: Date, required: true, default: Date.now },
    bill_id: {
      type: db.mongoose.Types.ObjectId,
      ref: "billModel",
      required: true,
    },
    bill_detail_id: {
      type: db.mongoose.Types.ObjectId,
      ref: "billDetailModel",
      required: true,
    },
    price: { type: Number, required: true, min: 0 },
    size: { type: String },
    color: { type: String },
  },
  {
    collection: "variant_sales",
    timestamps: true, // Tự động thêm createdAt và updatedAt
  }
);

// Index để tối ưu truy vấn theo thời gian và variant
variantSalesSchema.index({ variant_id: 1, sale_date: -1 });
variantSalesSchema.index({ product_id: 1, sale_date: -1 });
variantSalesSchema.index({ sale_date: -1 });

let variantSalesModel = db.mongoose.model("variantSalesModel", variantSalesSchema);
module.exports = { variantSalesModel };

