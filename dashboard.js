if (!sessionStorage.getItem("authToken")) {
    window.location.href = "index.html";
}

function getToken() {
    return sessionStorage.getItem("authToken");
}

function authHeaders() {
    return { "Authorization": "Bearer " + getToken() };
}

function updateClock() {
    document.getElementById("clock").innerText = new Date().toLocaleString();
}
setInterval(updateClock, 1000);
updateClock();

function setCounter(id, value) {
    const el = document.getElementById(id);
    if(el && el.innerText != value) {
        el.innerText = value;
        el.classList.add("counter-pulse");
        setTimeout(() => el.classList.remove("counter-pulse"), 500);
    }
}

// D3.js Topology Setup
let sim, nodes, links, svg, nodeElements, linkElements;

function initTopology() {
    const width = document.getElementById('topologyGraph').clientWidth;
    const height = document.getElementById('topologyGraph').clientHeight || 420;
    
    svg = d3.select("#topologyGraph").append("svg")
        .attr("width", "100%")
        .attr("height", "100%")
        .attr("viewBox", `0 0 ${width} ${height}`);
        
    nodes = [
        { id: "Database Server", type: "server", x: width/2, y: height/2 },
        { id: "Auth Server", type: "server" },
        { id: "API Gateway", type: "server" },
        { id: "File Server", type: "server" },
        { id: "Mail Server", type: "server" },
        { id: "DNS", type: "server" },
        { id: "Firewall", type: "server" },
        { id: "Load Balancer", type: "server" },
        { id: "Web Server", type: "server" },
        { id: "Backup Server", type: "server" },
        { id: "Log Server", type: "server" },
        { id: "User1", type: "user" },
        { id: "User2", type: "user" },
        { id: "User3", type: "user" },
        { id: "Attacker", type: "attacker" }
    ];
    
    links = [
        { source: "Firewall", target: "API Gateway" },
        { source: "API Gateway", target: "Auth Server" },
        { source: "API Gateway", target: "Web Server" },
        { source: "Web Server", target: "Database Server" },
        { source: "Web Server", target: "File Server" },
        { source: "Auth Server", target: "Database Server" },
        { source: "Database Server", target: "Backup Server" },
        { source: "Web Server", target: "Mail Server" },
        { source: "Mail Server", target: "DNS" },
        { source: "Firewall", target: "Load Balancer" },
        { source: "Load Balancer", target: "Web Server" },
        { source: "Log Server", target: "API Gateway" },
        { source: "User1", target: "Load Balancer" },
        { source: "User2", target: "Load Balancer" },
        { source: "User3", target: "Load Balancer" },
        { source: "Attacker", target: "Firewall" }
    ];

    sim = d3.forceSimulation(nodes)
        .force("link", d3.forceLink(links).id(d => d.id).distance(100))
        .force("charge", d3.forceManyBody().strength(-300))
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force("collide", d3.forceCollide().radius(30));

    linkElements = svg.append("g")
        .selectAll("line")
        .data(links)
        .enter().append("line")
        .attr("stroke", "rgba(255,255,255,0.08)")
        .attr("stroke-width", 2)
        .attr("id", d => `link-${d.source.id.replace(/\s+/g,'')}-${d.target.id.replace(/\s+/g,'')}`);

    nodeElements = svg.append("g")
        .selectAll("circle")
        .data(nodes)
        .enter().append("circle")
        .attr("r", d => d.type === "server" ? 20 : (d.type === "attacker" ? 18 : 14))
        .attr("fill", d => d.type === "server" ? "#1a1a1a" : (d.type === "attacker" ? "#2a0d0d" : "#0d2a0d"))
        .attr("stroke", d => d.type === "server" ? "#444" : (d.type === "attacker" ? "#ff3333" : "#2a5a2a"))
        .attr("stroke-width", 2)
        .attr("id", d => `node-${d.id.replace(/\s+/g,'')}`)
        .call(d3.drag()
            .on("start", dragstarted)
            .on("drag", dragged)
            .on("end", dragended));

    const labels = svg.append("g")
        .selectAll("text")
        .data(nodes)
        .enter().append("text")
        .text(d => d.id)
        .attr("class", "node-label")
        .attr("dy", 32);

    sim.on("tick", () => {
        linkElements.attr("x1", d => d.source.x).attr("y1", d => d.source.y)
                    .attr("x2", d => d.target.x).attr("y2", d => d.target.y);
        nodeElements.attr("cx", d => d.x).attr("cy", d => d.y);
        labels.attr("x", d => d.x).attr("y", d => d.y);
    });
}

function dragstarted(event, d) {
  if (!event.active) sim.alphaTarget(0.3).restart();
  d.fx = d.x; d.fy = d.y;
}
function dragged(event, d) {
  d.fx = event.x; d.fy = event.y;
}
function dragended(event, d) {
  if (!event.active) sim.alphaTarget(0);
  d.fx = null; d.fy = null;
}

function animateThreatPulse(targetNodeName) {
    if(!svg) return;
    const targetNode = nodes.find(n => n.id === targetNodeName) || nodes.find(n => n.type === 'server');
    const attackerNode = nodes.find(n => n.type === 'attacker');
    
    if(!targetNode || !attackerNode) return;
    
    const pulse = svg.append("circle")
        .attr("r", 6)
        .attr("fill", "#ff0000")
        .attr("cx", attackerNode.x)
        .attr("cy", attackerNode.y);
        
    pulse.transition()
        .duration(1500)
        .attr("cx", targetNode.x)
        .attr("cy", targetNode.y)
        .on("end", function() {
            d3.select(this).remove();
            d3.select(`#node-${targetNode.id.replace(/\s+/g,'')}`)
              .transition().duration(200).attr("fill", "#ff0000")
              .transition().duration(200).attr("fill", "#1a1a1a")
              .transition().duration(200).attr("fill", "#ff0000")
              .transition().duration(200).attr("fill", "#1a1a1a");
        });
}

function animateIsolation(targetNodeName) {
    if(!svg) return;
    const targetNode = nodes.find(n => n.id === targetNodeName) || nodes.find(n => n.type === 'server');
    if(!targetNode) return;
    
    const nodeId = `node-${targetNode.id.replace(/\s+/g,'')}`;
    
    // Snap links
    linkElements.filter(d => d.source.id === targetNode.id || d.target.id === targetNode.id)
        .transition().duration(500)
        .attr("stroke-opacity", 0)
        .remove();
        
    // Drift away
    d3.select(`#${nodeId}`)
        .transition().duration(2000)
        .attr("cy", targetNode.y - 100)
        .style("opacity", 0)
        .remove();
}

function showForensicModal(data) {
    document.getElementById("forensicModal").style.display = "flex";
    document.getElementById("forensicModal").innerHTML = `
        <div class="modal-content fade-in">
            <h2>🚨 FORENSIC SUMMARY</h2>
            <div class="modal-field"><span class="label">Attacker IP</span><span class="value">${data.ip}</span></div>
            <div class="modal-field"><span class="label">MAC Address</span><span class="value">${data.mac}</span></div>
            <div class="modal-field"><span class="label">Timestamp</span><span class="value">${data.timestamp}</span></div>
            <div class="modal-field"><span class="label">Classification</span><span class="value">${data.classification}</span></div>
            <div style="margin-top:25px;">
                <button class="modal-btn secondary" onclick="document.getElementById('forensicModal').style.display='none'">Dismiss</button>
                <button class="modal-btn primary" onclick="exportReport()">📥 Download Incident PDF</button>
            </div>
        </div>
    `;
}

function showHoneypotBreach(ip) {
    const banner = document.getElementById("honeypotBanner");
    banner.innerText = `⚠ HONEYPOT BREACHED — Intruder IP: ${ip}`;
    banner.style.display = "block";
    setTimeout(() => banner.style.display = "none", 8000);
}

function testHoneypotTrap() {
    fetch("http://localhost:8080/api/honeypot")
        .then(res => res.json())
        .then(data => fetchDashboardData())
        .catch(err => fetchDashboardData());
}

function unblockAllIPs() {
    fetch("http://localhost:8080/api/unblock-all")
        .then(res => res.json())
        .then(data => fetchDashboardData())
        .catch(err => fetchDashboardData());
}

// Fetch dashboard data
function fetchDashboardData() {
    fetch("http://localhost:8080/api/dashboard", { headers: authHeaders() })
    .then(res => res.json())
    .then(data => {
        setCounter("riskCounter", data.risk);
        setCounter("eventsCounter", data.events);
        setCounter("usersCounter", data.users);
        setCounter("anomalyCounter", data.anomalies);
        
        // Status banner
        const banner = document.querySelector(".status-pill");
        if(data.risk <= 10) { banner.innerText = "SECURE"; banner.style.color = "#67ff9f"; banner.style.background = "#0d1a0d"; }
        else if(data.risk <= 40) { banner.innerText = "ELEVATED"; banner.style.color = "#ffc107"; banner.style.background = "#1a1a0d"; }
        else { banner.innerText = "HIGH RISK"; banner.style.color = "#ff5c5c"; banner.style.background = "#1a0d0d"; }
        
        // Table
        const tbody = document.getElementById("eventTableBody");
        if(tbody) {
            tbody.innerHTML = "";
            data.eventsList.forEach(ev => {
                let sevClass = "severity-low";
                let sevText = "LOW";
                if(ev.type.includes("Threat") || ev.type.includes("ISOLATION") || ev.type.includes("HONEYPOT")) { sevClass = "severity-high"; sevText = "HIGH"; }
                else if(ev.type.includes("Failed")) { sevClass = "severity-medium"; sevText = "MEDIUM"; }
                tbody.innerHTML += `<tr class="fade-in"><td>${ev.time}</td><td>${ev.type}: ${ev.details.substring(0,40)}</td><td class="${sevClass}">${sevText}</td></tr>`;
            });
        }
        
        // Honeypot trapped IPs
        const trapped = document.getElementById("trappedIPs");
        if(trapped) {
            trapped.innerHTML = "";
            data.blockedIPs.forEach(ip => {
                trapped.innerHTML += `<div class="trapped-ip">Blocked: ${ip}</div>`;
            });
        }
    })
    .catch(err => console.error("API Error:", err));
}

// Initialize
fetchDashboardData();
setTimeout(initTopology, 500);

// SSE Connection
const eventSource = new EventSource("http://localhost:8080/api/events-stream?token=" + getToken());
eventSource.onmessage = function(event) {
    const data = JSON.parse(event.data);
    fetchDashboardData();
    if (data.type === "threat") {
        animateThreatPulse("Web Server");
    } else if (data.type === "isolation") {
        animateIsolation("Web Server");
        showForensicModal(data);
    } else if (data.type === "honeypot") {
        showHoneypotBreach(data.ip);
    }
};
eventSource.onerror = function() {
    console.log("SSE disconnected. Falling back to polling.");
    setInterval(fetchDashboardData, 3000);
    eventSource.close();
};

function isolateSystem() {
    const btn = document.getElementById("isolateBtn");
    btn.innerText = "INITIATING LOCKDOWN...";
    btn.style.animation = "none";
    btn.style.backgroundColor = "#444";
    
    fetch("http://localhost:8080/api/isolate", { headers: authHeaders() })
    .then(res => res.json())
    .then(data => {
        document.body.style.animation = "pulseRed 0.5s";
        setTimeout(() => document.body.style.animation = "", 500);
        btn.innerText = "SYSTEM ISOLATED";
        btn.style.backgroundColor = "#000";
        btn.style.border = "2px solid #ff0000";
        btn.style.color = "#ff0000";
        
        // Modal will be shown via SSE, but as fallback:
        showForensicModal(data);
        animateIsolation("Web Server");
    });
}

function logout() {
    fetch("http://localhost:8080/api/logout", { headers: authHeaders() })
    .then(() => {
        sessionStorage.removeItem("authToken");
        window.location.href = "index.html";
    });
}

async function exportReport() {
    const btn = document.getElementById("exportPdfBtn");
    if(btn) btn.innerText = "⏳ Generating...";
    
    const dashboard = document.querySelector('.dashboard');
    try {
        const canvas = await html2canvas(dashboard, { backgroundColor: '#050505', scale: 2 });
        const imgData = canvas.toDataURL('image/jpeg', 0.8);
        const { jsPDF } = window.jspdf;
        const pdf = new jsPDF('p', 'mm', 'a4');
        const pdfWidth = pdf.internal.pageSize.getWidth();
        const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
        pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);
        pdf.save("CyberShadowTwin_Forensic_Report.pdf");
    } catch(err) {
        console.error(err);
        alert("PDF Export failed.");
    }
    
    if(btn) btn.innerText = "📥 Export Forensic Report";
}

// Keep Line Chart static for visual effect
setTimeout(() => {
    const ctx = document.getElementById('securityChart').getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
            datasets: [{
                label: 'Threat Activity',
                data: [4, 7, 5, 12, 8, 15, 10],
                borderColor: '#ffffff',
                backgroundColor: 'rgba(255, 255, 255, 0.1)',
                borderWidth: 3,
                tension: 0.4,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { y: { beginAtZero: true, grid: { color: '#222' }, ticks: { color: '#9a9a9a' } }, x: { grid: { color: '#222' }, ticks: { color: '#9a9a9a' } } },
            plugins: { legend: { labels: { color: '#ffffff' } } }
        }
    });
}, 500);
