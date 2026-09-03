const canvas = document.getElementById('upguardGrid');
const ctx = canvas.getContext('2d');

function resize() {
    canvas.width = canvas.parentElement.clientWidth;
    canvas.height = canvas.parentElement.clientHeight;
}
window.addEventListener('resize', resize);
resize();

const GRID_SPACING = 30;
const DOT_SIZE = 3;

let risks = [];

// Spawn risks
setInterval(() => {
    if(Math.random() > 0.4 && risks.length < 4) {
        risks.push({
            x: canvas.width * 0.8 + (Math.random() * 100 - 50),
            y: canvas.height * (0.2 + Math.random() * 0.6),
            targetX: canvas.width * 0.2 + (Math.random() * 200 - 100),
            progress: 0,
            speed: 0.0015 + Math.random() * 0.0015,
            textOpacity: 1
        });
    }
}, 2500);

let time = 0;

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    time += 0.03;
    
    const cols = Math.floor(canvas.width / GRID_SPACING);
    const rows = Math.floor(canvas.height / GRID_SPACING);
    
    const offsetX = (canvas.width - cols * GRID_SPACING) / 2;
    const offsetY = (canvas.height - rows * GRID_SPACING) / 2;
    
    for(let i = 0; i <= cols; i++) {
        for(let j = 0; j <= rows; j++) {
            let x = offsetX + i * GRID_SPACING;
            let y = offsetY + j * GRID_SPACING;
            
            let distFromRight = canvas.width - x;
            
            let r = 34;  // Base color #222
            let g = 34;
            let b = 34;
            
            // Green pulsing area on the right
            if(distFromRight < 500) {
                // Pulsing effect based on wave
                let intensity = Math.sin(time * 2 + i * 0.15 + j * 0.25) * 0.5 + 0.5;
                let falloff = Math.max(0, (500 - distFromRight) / 500);
                
                // Add green (#008f5c)
                g += 100 * intensity * falloff;
                b += 60 * intensity * falloff;
            }
            
            // Risk proximities
            for(let rObj of risks) {
                let currentX = rObj.x + (rObj.targetX - rObj.x) * rObj.progress;
                let currentY = rObj.y;
                
                let dist = Math.sqrt(Math.pow(x - currentX, 2) + Math.pow(y - currentY, 2));
                
                if(dist < 90) {
                    let intensity = Math.max(0, 1 - (dist / 90));
                    // Red pulsing color
                    let pulse = Math.sin(time * 8 + dist * 0.1) * 0.2 + 0.8;
                    r = Math.max(r, 34 + 200 * intensity * pulse);
                    g = Math.min(g, 255 - 200 * intensity);
                    b = Math.min(b, 255 - 200 * intensity);
                }
            }
            
            ctx.fillStyle = `rgb(${r}, ${g}, ${b})`;
            ctx.fillRect(x - DOT_SIZE/2, y - DOT_SIZE/2, DOT_SIZE, DOT_SIZE);
        }
    }
    
    for(let i = risks.length - 1; i >= 0; i--) {
        let rObj = risks[i];
        rObj.progress += rObj.speed;
        
        let currentX = rObj.x + (rObj.targetX - rObj.x) * rObj.progress;
        let currentY = rObj.y;
        
        rObj.textOpacity = Math.sin(time * 4) * 0.4 + 0.6;
        
        if(rObj.progress > 0.05 && rObj.progress < 0.95) {
            // Fade in and fade out
            let alpha = rObj.textOpacity;
            if(rObj.progress < 0.1) alpha *= (rObj.progress - 0.05) * 20;
            if(rObj.progress > 0.9) alpha *= (0.95 - rObj.progress) * 20;

            ctx.fillStyle = `rgba(255, 92, 92, ${alpha})`;
            ctx.font = "11px Inter, sans-serif";
            ctx.fillText("Risk detected ■", currentX - 95, currentY + 4);
            
            ctx.beginPath();
            ctx.strokeStyle = `rgba(255, 92, 92, ${alpha * 0.3})`;
            ctx.lineWidth = 1;
            ctx.moveTo(currentX, currentY);
            ctx.lineTo(currentX - 25, currentY);
            ctx.stroke();
        }
        
        if(rObj.progress >= 1) {
            risks.splice(i, 1);
        }
    }
    
    requestAnimationFrame(draw);
}

draw();
