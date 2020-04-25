<template>
  <div>
    <div v-if="loggedInProfile">Hello, {{ loggedInProfile.fullName }} !</div>
    <div>
      <div>
        Email:
        <input type="text" v-model="email" />
      </div>
      <div>
        Password:
        <input type="password" v-model="password" />
      </div>
      <button v-on:click="doLogin">login</button>
    </div>
  </div>
</template>

<script>
import * as services from "./services.js";

export default {
  props: {
      newUserProfile: Object
  },

  data: function() {
    return {
      loggedInProfile: null,

      // login form
      email: "",
      password: "password"
    };
  },

  methods: {
    doLogin: function() {
      console.log("login submit!");
      services
        .login(this.email, this.password)
        .then(json => {
          services
            .getProfile(json.profileId)
            .then(json => {
              console.log(json);
              this.loggedInProfile = json.profile;
            })
            .catch(error => {
              console.log("get profile error: ", error);
            });
        })
        .catch(error => {
          console.log("login error: ", error);
        });
    }
  }
};
</script>