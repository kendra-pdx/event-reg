import _ from "lodash";
import Vue from "vue";

import CreateProfile from "./CreateProfile.vue"
import UserLogin from "./UserLogin.vue"

var app = new Vue({
  el: "#app",

  components: {
    "create-profile": CreateProfile,
    "user-login": UserLogin
  },

  data: {
  },

  methods: {
    updateCreatedProfile: (profile) => {
      console.log("new profile", profile)
      app.$refs.userLogin.email = profile.email
    },
  }
})