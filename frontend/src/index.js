import _ from "lodash";
const request = require("request");


function component() {
  const element = document.createElement("div");

  element.innerHTML = _.join(["Hello", "webpack"], " ");

  return element;
}

document.body.appendChild(component());

request("http://localhost:8080/events/profile/2EB0CB01-24DC-44D9-A937-05DBC62048EA", (error, response, body) => {
    const element = document.createElement("div");
    element.innerHTML = body;
    document.body.appendChild(element);
});