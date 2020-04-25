import _ from "lodash";
import Vue from "vue";
import * as services from "./services.js"

import CreateProfile from "./CreateProfile.vue"

var app = new Vue({
  el: "#app",
  components: {
    "create-profile": CreateProfile
  },
  
  data: {
    loaded: false,

    profile: { },

    login: {
      email: "",
      password: "password"
    }
  },

  methods: {
    updateCreatedProfile: (profile) => {
      console.log("new profile", profile)
      app.login.email = profile.email;
    },

    doLogin: () => {
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