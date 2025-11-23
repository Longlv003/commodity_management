const mongoose = require("mongoose");
require("dotenv").config();

// Lấy connection string từ environment variable, mặc định là localhost
// const MONGODB_URI = process.env.MONGODB_URI || "mongodb://localhost:27017/closet_hub";

// mongoose
//   .connect(MONGODB_URI)
//   .then(() => {
//     console.log("✅ Connected to MongoDB successfully");
//     console.log("📊 Database:", MONGODB_URI.includes("localhost") ? "Local" : "Remote");
//   })
//   .catch((err) => {
//     console.error("❌ Error connecting to database");
//     console.error(err.message);
//     process.exit(1);
//   });

mongoose.connect("mongodb://localhost:27017/closet_hub").catch((err) => {
  console.log("Error connecting to database");
  console.log(err.message);
});



module.exports = { mongoose };
