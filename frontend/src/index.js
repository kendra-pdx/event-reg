import _ from "lodash";
import Vue from "vue";
import * as services from "./services.js"

var app = new Vue({
  el: "#app",
  data: {
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
      const body = JSON.stringify({
        email: app.create.email,
        fullName: app.create.name,
        password: app.create.password
      });
      console.log(body);
      fetch("http://localhost:8080/events/profile", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: body
      }).then(response => {
        console.log(response);
      }).catch((error) => {
        console.log(error);
      })
    },
   
    doLogin: function () {
      console.log("login submit!");
      services.login(app.login.email, app.login.password)
      .then(response => response.json())
      .then((json) => {
        console.log(json);
        return json;
      })
      .then((json) => {
        return services.getProfile(json.profileId)
      })
      .then((response) => response.json())
      .then((json) => {
        console.log(json);
        app.profile = json.profile;
        app.loaded = true;
      })
      .catch((error) => {
        console.log(error);
      })
    }  
  }
})

services.getProfile("2EB0CB01-24DC-44D9-A937-05DBC62048EA")
  .then((response) => response.json())
  .then((json) => {
    console.log(json);
    app.profile = json.profile;
    app.loaded = true;
  })
  .catch((error) => {
    console.log(error);
  })