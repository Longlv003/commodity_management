const mongoose = require("mongoose");
require("dotenv").config();

// mongoose.connect("mongodb://localhost:27017/closet_hub").catch((err) => {
//   console.log("Error connecting to database");
//   console.log(err.message);
// });

mongoose
  .connect(
    "mongodb://admin:password123@192.168.194.181:27017/closet_hub?authSource=admin"
  )
  .catch((err) => {
    console.log("Error connecting to database");
    console.log(err.message);
  });

module.exports = { mongoose };
