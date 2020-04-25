<template>
  <div id="app">
    <create-profile v-on:created="updateCreatedProfile"></create-profile>
    <hr />
    <user-login ref="userLogin"></user-login>
  </div>
</template>

<script>
import CreateProfile from "./CreateProfile.vue";
import UserLogin from "./UserLogin.vue";

export default {
  components: {
    "create-profile": CreateProfile,
    "user-login": UserLogin
  },
  methods: {
    updateCreatedProfile: function(profile) {
      console.log("new profile", profile);
      this.$refs.userLogin.email = profile.email;
    }
  }
};
</script>