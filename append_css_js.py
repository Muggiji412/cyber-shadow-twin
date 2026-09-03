with open(r"D:\VSC Projects\CyberShadowTwinWeb\style.css", "a", encoding="utf-8") as f:
    f.write("""
/* ==========================
   ACTIVE DEFENSE
========================== */
.active-defense {
    margin-top: 30px;
    background: #0d0d0d;
    border: 1px solid #ff3333;
    border-radius: 20px;
    padding: 30px;
    text-align: center;
    transition: 0.3s;
}
.active-defense h2 { color: #ff5c5c; margin-bottom: 10px; }
.active-defense p { color: #9a9a9a; margin-bottom: 25px; }
#isolateBtn {
    padding: 15px 40px;
    background: #ff0000;
    color: white;
    border: none;
    border-radius: 12px;
    font-size: 20px;
    font-weight: 800;
    cursor: pointer;
    box-shadow: 0 0 20px rgba(255, 0, 0, 0.4);
    animation: pulseRed 1.5s infinite;
}
@keyframes pulseRed {
    0% { transform: scale(1); box-shadow: 0 0 20px rgba(255, 0, 0, 0.4); }
    50% { transform: scale(1.05); box-shadow: 0 0 40px rgba(255, 0, 0, 0.8); }
    100% { transform: scale(1); box-shadow: 0 0 20px rgba(255, 0, 0, 0.4); }
}
.lockdown-active {
    background: #4a0000 !important;
    border: 2px solid #ff0000 !important;
}
""")

with open(r"D:\VSC Projects\CyberShadowTwinWeb\dashboard.js", "a", encoding="utf-8") as f:
    f.write("""
/* ACTIVE DEFENSE */
function isolateSystem() {
    const btn = document.getElementById("isolateBtn");
    btn.innerText = "INITIATING LOCKDOWN...";
    btn.style.animation = "none";
    btn.style.background = "#555";
    
    fetch("http://localhost:8080/api/isolate")
        .then(res => res.json())
        .then(data => {
            document.body.style.animation = "pulse 1s infinite alternate";
            document.getElementById("defensePanel").classList.add("lockdown-active");
            btn.innerText = "SYSTEM ISOLATED";
            btn.style.background = "#000";
            btn.style.color = "#ff0000";
            btn.style.border = "1px solid #ff0000";
            
            // Screen shake
            let shakes = 0;
            let interval = setInterval(() => {
                document.body.style.transform = `translate(${Math.random()*10-5}px, ${Math.random()*10-5}px)`;
                shakes++;
                if(shakes > 20) {
                    clearInterval(interval);
                    document.body.style.transform = "none";
                }
            }, 50);
        });
}
""")
