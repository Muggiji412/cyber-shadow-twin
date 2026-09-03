function login() {
    const user = document.getElementById("username").value;
    const pass = document.getElementById("password").value;
    const errorMsg = document.getElementById("loginError");

    errorMsg.innerText = "Authenticating...";
    
    fetch("http://localhost:8080/api/authenticate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: user, password: pass })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            sessionStorage.setItem("authToken", data.token);
            window.location.href = "dashboard.html";
        } else {
            errorMsg.innerText = "Invalid username or password";
        }
    })
    .catch(err => {
        errorMsg.innerText = "Connection error. Is the server running?";
        console.error(err);
    });
}

function focusLogin() {
    document.getElementById("username").focus();
}

function learnMore() {
    document.getElementById("aboutSection").scrollIntoView({ behavior: 'smooth' });
}

// Heading animation
const heading = document.getElementById("animatedHeading");
const phrases = [
    "Detect Threats.",
    "Understand Risk.",
    "Respond Faster.",
    "Monitor Activity.",
    "Protect Systems."
];
let phraseIndex = 0;
setInterval(() => {
    phraseIndex = (phraseIndex + 1) % phrases.length;
    heading.style.opacity = 0;
    setTimeout(() => {
        heading.innerText = phrases[phraseIndex];
        heading.style.opacity = 1;
    }, 400);
}, 2500);
