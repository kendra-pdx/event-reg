const path = require("path");
const VueLoaderPlugin = require('vue-loader/lib/plugin')

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
  },

  resolve: {
    alias: {
      vue: 'vue/dist/vue.js'
    },
  },

  module: {
    rules: [{
      test: /\.vue$/,
      loader: "vue-loader"
    }, {
      test: /\.js$/,
      loader: 'babel-loader'
    }, {
      test: /\.css$/,
      use: [
        'vue-style-loader',
        'css-loader'
      ]
    }]
  },

  plugins: [
    new VueLoaderPlugin()
  ]

};