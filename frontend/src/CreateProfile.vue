<template>
    <div>
      <div>Name: <input type="text" v-model="name"/></div>
      <div>Email: <input type="text" v-model="email"/></div>
      <div>Password: <input type="password" v-model="password"/></div>
      <button v-on:click="doCreateUser">create</button>
    </div>
</template>

<script>
import * as services from "./services.js"

export default {
    data: function() {
        return {
            name: Math.random().toString(36).substring(7),
            email: Math.random().toString(36).substring(7),
            password: "password"
        }
    },

    methods: {
        doCreateUser: function () {
        services.createUser(this.email, this.name, this.password)
            .then(user => {
                this.$emit("created", user);
            }).catch((error) => {
                console.log(error);
            })
        },
    }
}
</script>