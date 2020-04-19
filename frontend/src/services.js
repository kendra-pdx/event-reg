
function url(path) {
    return "http://localhost:8080/events/" + path;
}

export async function login(email, password) {
    console.log("calling login: ", email);
    return fetch(url("login"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            email: email,
            password: password
        })
    }).then(r => r.json());
}

export async function getProfile(id) {
    console.log("getting profile: " + id);
    return fetch(url("profile/" + id)).then(r => r.json())
}

export async function createUser(email, fullName, password) {
    console.log("creating profile: ", email, fullName);
    return fetch(url("profile"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            email: email,
            fullName: fullName,
            password: password
        })
    }).then(r => r.json());
}