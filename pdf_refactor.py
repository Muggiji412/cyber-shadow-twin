import re

with open(r"D:\VSC Projects\CyberShadowTwinWeb\dashboard.html", "r", encoding="utf-8") as f:
    html = f.read()

# Add button
old_nav = """        <span id="adminName">Admin User</span>
        <button class="logout-btn" onclick="logout()">Logout</button>"""
new_nav = """        <button id="exportPdfBtn" onclick="exportReport()" style="background:#222; color:#fff; border:1px solid #444; padding:8px 15px; border-radius:8px; cursor:pointer; margin-right:15px; font-weight:600;">📥 Export Forensic Report</button>
        <span id="adminName">Admin User</span>
        <button class="logout-btn" onclick="logout()">Logout</button>"""
html = html.replace(old_nav, new_nav)

# Add scripts
old_scripts = """<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script src="dashboard.js"></script>"""
new_scripts = """<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"></script>
<script src="dashboard.js"></script>"""
html = html.replace(old_scripts, new_scripts)

with open(r"D:\VSC Projects\CyberShadowTwinWeb\dashboard.html", "w", encoding="utf-8") as f:
    f.write(html)


# Add JS
with open(r"D:\VSC Projects\CyberShadowTwinWeb\dashboard.js", "a", encoding="utf-8") as f:
    f.write("""
/* PDF EXPORT */
async function exportReport() {
    const btn = document.getElementById("exportPdfBtn");
    btn.innerText = "⏳ Generating...";
    
    const { jsPDF } = window.jspdf;
    
    const element = document.querySelector('.dashboard-container');
    
    try {
        const canvas = await html2canvas(element, { backgroundColor: '#050505', scale: 2 });
        const imgData = canvas.toDataURL('image/png');
        
        const pdf = new jsPDF('p', 'mm', 'a4');
        const pdfWidth = pdf.internal.pageSize.getWidth();
        const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
        
        pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
        pdf.save('CyberShadowTwin_Forensic_Report.pdf');
        
        btn.innerText = "✅ Exported";
        setTimeout(() => { btn.innerText = "📥 Export Forensic Report"; }, 3000);
    } catch (e) {
        console.error("PDF Export failed", e);
        btn.innerText = "❌ Failed";
        setTimeout(() => { btn.innerText = "📥 Export Forensic Report"; }, 3000);
    }
}
""")
