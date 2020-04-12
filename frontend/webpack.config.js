const path = require("path");

module.exports = {
  entry: "./src/index.js",
  output: {
    filename: "main.js",
    path: path.resolve(__dirname, "dist"),
  },
  mode: "development",
    node: {
      console: true,
      fs: 'empty',
      net: 'empty',
      tls: 'empty'
    }

};