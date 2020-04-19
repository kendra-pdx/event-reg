
function url(path) {
    return "http://localhost:8080/events/" + path;
}

function authClaims() {
    const authToken = localStorage.getItem("authToken")
    if (authToken) {
        return JSON.parse(atob(authToken.split(".")[1]))
    } else {
        return {}
    }
}

async function authToken() {
    const authToken = localStorage.getItem("authToken")
    // todo: if expired, refresh
    if (authToken) {
        return Promise.resolve(authToken)
    } else {
        return Promise.reject("no auth token")
    }
}

export async function login(email, password) {
    console.log("calling login: ", email);
    localStorage.clear();
    return fetch(url("login"), {
        method: "POST",
        headers: Object.assign({
            "Content-Type": "application/json",
        }),
        body: JSON.stringify({
            email: email,
            password: password
        })
    }).then(r => r.json()).then(json => {
        localStorage.setItem("authToken", json.authToken)
        localStorage.setItem("refreshToken", json.refreshToken)
        console.log(authClaims())
        return json
    })
}

export async function getProfile(id) {
    console.log("getting profile: " + id);
    return authToken()
        .then(authToken => {
            return fetch(url("profile/" + id), {
                headers: {
                    "Authorization": "Bearer " + authToken
                }
            })
        })
        .then(r => r.json())

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