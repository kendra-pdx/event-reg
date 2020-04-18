
function url(path) {
    return "http://localhost:8080/events/" + path;
}

export async function login(email, password) {
    console.log("calling login: ", email, password);
    return fetch(url("login"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            email: email,
            password: password
        })
    });
}

export async function getProfile(id) {
    console.log("getting profile: " + id);
    return fetch(url("profile/"+id))
}