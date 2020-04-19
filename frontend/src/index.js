import _ from "lodash";
import Vue from "vue";
import * as services from "./services.js"

var app = new Vue({
  el: "#app",
  data: {
    profile: {

    },
    message: "Hello Kendra!",
    loaded: false,

    create: {
      name: Math.random().toString(36).substring(7),
      email: Math.random().toString(36).substring(7),
      password: "password"
    },

    login: {
      email: "",
      password: "password"
    }
  },

  methods: {
    doCreateUser: function () {
      services.createUser(app.create.email, app.create.name, app.create.password)
        .then(user => {
          app.login.email = user.email;
        }).catch((error) => {
          console.log(error);
        })
    },

    doLogin: function () {
      console.log("login submit!");
      services.login(app.login.email, app.login.password)
        .then((json) => {
          services.getProfile(json.profileId)
            .then((json) => {
              console.log(json);
              app.profile = json.profile;
              app.loaded = true;
            })
            .catch((error) => {
              console.log("get profile error: ", error);
            })
        })
        .catch((error) => {
          console.log("login error: ", error);
        })
    }
  }
})