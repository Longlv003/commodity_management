const mongoose = require("mongoose");

mongoose.connect("mongodb://localhost:27017/closet_hub").catch((err) => {
  console.log("Error connecting to database");
  console.log(err.message);
});

module.exports = { mongoose };