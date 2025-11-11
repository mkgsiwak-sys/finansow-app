// UŻYTECZNE KODY – filtrujemy tylko to, co wyświetlamy
const USEFUL_CODES = new Set([
    'switch','switch_1','switch_2','switch_3','switch_4',
    'temp_current','temp_value','va_temperature',
    'humidity_value','va_humidity',
    'pm25','co2','battery_percentage',
    'cur_power','power','power_current','cur_current','cur_voltage','add_ele','ele','electricity','electricity_total',
    'bright_value','bright_value_v2'
]);

function translateStatusCode(code){
    if(!code) return 'Status';
    const dict = {
        temp_current:'Temperatura', temp_value:'Temperatura', va_temperature:'Temperatura',
        humidity_value:'Wilgotność', va_humidity:'Wilgotność',
        pm25:'PM2.5', co2:'CO₂', battery_percentage:'Bateria',
        cur_power:'Moc', power:'Moc', power_current:'Moc',
        cur_current:'Prąd', cur_voltage:'Napięcie',
        add_ele:'Energia', ele:'Energia', electricity:'Energia', electricity_total:'Energia',
        bright_value:'Jasność', bright_value_v2:'Jasność',
        switch:'Główny'
    };
    if (dict[code]) return dict[code];
    if (code.startsWith('switch_')) return `Kanał ${code.split('_')[1]}`;
    return code.split('_').map(p=>p[0].toUpperCase()+p.slice(1)).join(' ');
}

function normalizeNumber(code,val){
    if(typeof val!=='number') return val;
    if((code.includes('temp')||code.includes('humidity')) && Math.abs(val)>100) return val/10;
    return val;
}
function formatMeasurementValue(code,value){
    if(value===null||value===undefined) return '—';
    if(typeof value==='number'){
        const v = normalizeNumber(code,value);
        if(code.includes('temp')) return `${v.toFixed(1)}°C`;
        if(code.includes('humidity')) return `${Math.round(v)}%`;
        if(code.includes('battery')||code.includes('percent')) return `${Math.round(v)}%`;
        if(code.includes('cur_power')||code==='power'||code==='power_current') return `${Math.round(v)} W`;
        if(code.includes('cur_voltage')) return `${Math.round(v)} V`;
        if(code.includes('cur_current')) return `${v.toFixed(2)} A`;
        if(code==='add_ele'||code==='ele'||code.includes('electricity')) return `${v.toFixed(2)} kWh`;
        if(code.includes('pm25')) return `${Math.round(v)} µg/m³`;
        if(code.includes('co2')) return `${Math.round(v)} ppm`;
        return new Intl.NumberFormat('pl-PL',{maximumFractionDigits:2}).format(v);
    }
    if(typeof value==='boolean') return value?'Aktywne':'Nieaktywne';
    if(typeof value==='object'){ try { return JSON.stringify(value); } catch { return 'Dane'; } }
    return String(value);
}

// --- RENDER ---
function buildCard(device){
    const id = device.id;
    const name = device.name || 'Urządzenie';
    const productName = device.productName;

    const statuses = Array.isArray(device.status) ? device.status : [];
    const switches = statuses.filter(s => typeof s.value === 'boolean' && s.code.startsWith('switch') && s.code!=='switch_led');
    const measures = statuses.filter(s => s && s.value !== null && s.value !== undefined && typeof s.value !== 'boolean' && USEFUL_CODES.has(s.code));

    let controls = '';
    if (switches.length > 0) {
        const multi = switches.length > 1;
        const btns = switches.map(sw => {
            const on = !!sw.value;
            const label = multi ? `${translateStatusCode(sw.code)}: ${on ? 'Wyłącz' : 'Włącz'}` : (on ? 'Wyłącz' : 'Włącz');
            const cls = `tuya-btn ${on?'bg-indigo-600 text-white':'bg-gray-100 text-gray-700'} px-4 py-2 rounded-full text-sm font-semibold transition`;
            return `<button type="button" data-action="toggle" data-device="${id}" data-code="${sw.code}" data-next="${!on}" class="${cls}">${label}</button>`;
        }).join('');
        controls += `<div class="tuya-actions flex flex-wrap gap-2 mt-4">${btns}</div>`;
    }

    if (measures.length > 0) {
        const rows = measures.map(st => `
      <div class="flex items-center justify-between text-sm text-gray-600 py-1 border-b border-gray-100 last:border-b-0">
        <span>${translateStatusCode(st.code)}</span>
        <span class="font-semibold text-gray-800">${formatMeasurementValue(st.code, st.value)}</span>
      </div>`).join('');
        controls += `<div class="tuya-metrics mt-4 bg-slate-50 border border-slate-100 rounded-xl p-3 space-y-1">${rows}</div>`;
    }

    if (!controls) controls = '<p class="text-sm text-gray-500 mt-4">Brak danych do wyświetlenia.</p>';

    return `
    <div class="tuya-card p-5 rounded-2xl border border-gray-200 bg-white shadow-sm" data-device-id="${id}">
      <div class="flex items-start justify-between gap-3">
        <div>
          <p class="font-semibold text-gray-900">${name}</p>
          ${productName ? `<p class="text-xs text-gray-500">${productName}</p>` : ''}
        </div>
        <span class="inline-flex px-2 py-1 text-[11px] font-medium text-gray-500 uppercase tracking-wide bg-gray-100 rounded-full">Urządzenie</span>
      </div>
      ${controls}
    </div>`;
}

// --- STATE ---
const devicePollers = new Map(); // deviceId -> {abort:AbortController, timer:number}

// Anuluj stare żądania/timery dla urządzenia
function cancelPoller(deviceId){
    const p = devicePollers.get(deviceId);
    if(!p) return;
    try { p.abort?.abort(); } catch {}
    if(p.timer) clearTimeout(p.timer);
    devicePollers.delete(deviceId);
}

// Odśwież JEDNO urządzenie (po kliknięciu)
async function refreshOne(deviceId, tries=6, delay=600){
    cancelPoller(deviceId);
    const ac = new AbortController();
    devicePollers.set(deviceId, {abort: ac, timer: null});
    const card = document.querySelector(`.tuya-card[data-device-id="${CSS.escape(deviceId)}"]`);
    if(!card) return;

    for(let i=0;i<tries;i++){
        try{
            const r = await fetch(`/api/tuya/devices/${encodeURIComponent(deviceId)}?t=${Date.now()}`, {
                signal: ac.signal,
                headers: {'Accept':'application/json','Cache-Control':'no-cache'}
            });
            if(!r.ok) throw new Error('HTTP '+r.status);
            const dev = await r.json();

            const html = buildCard(dev);
            card.outerHTML = html;
            wireControlsForDevice(deviceId); // podłącz clicki tylko na tej karcie
        }catch(e){
            if (ac.signal.aborted) return;
            // ignorujemy błąd i próbujemy dalej
        }
        // pauza
        const t = setTimeout(()=>{}, delay);
        devicePollers.get(deviceId).timer = t;
        await new Promise(res => setTimeout(res, delay));
    }
}

// Optymistyczna aktualizacja przycisku (bez skakania layoutu)
function optimisticButton(btn, next){
    btn.disabled = true;
    btn.classList.add('opacity-60','cursor-not-allowed');
    const base = btn.textContent;
    btn.dataset._base = base;
    btn.textContent = '...';
    // delikatny kolor
    btn.classList.toggle('bg-indigo-600', !!next);
    btn.classList.toggle('text-white', !!next);
    btn.classList.toggle('bg-gray-100', !next);
    btn.classList.toggle('text-gray-700', !next);
}

function restoreButton(btn){
    btn.disabled = false;
    btn.classList.remove('opacity-60','cursor-not-allowed');
    if (btn.dataset._base) btn.textContent = btn.dataset._base;
}

// Wyślij komendę i odśwież tylko to urządzenie
async function sendCommand(deviceId, code, value, btn){
    optimisticButton(btn, value);
    try{
        const r = await fetch(`/api/tuya/devices/${encodeURIComponent(deviceId)}/command`, {
            method:'POST',
            headers:{'Content-Type':'application/json','Accept':'application/json'},
            body: JSON.stringify({ commands: [{ code, value }] })
        });
        // niezależnie od statusu – spróbuj dociągnąć nowy stan urządzenia
        await refreshOne(deviceId, r.ok ? 8 : 3, 700);
    }finally{
        restoreButton(btn);
    }
}

// Podłącz zdarzenia do przycisków dla JEDNEGO urządzenia
function wireControlsForDevice(deviceId){
    const scope = document.querySelector(`.tuya-card[data-device-id="${CSS.escape(deviceId)}"]`);
    if(!scope) return;
    scope.querySelectorAll('button[data-action="toggle"]').forEach(btn=>{
        btn.addEventListener('click', ()=>{
            const id = btn.dataset.device;
            const code = btn.dataset.code;
            const next = (btn.dataset.next === 'true');
            sendCommand(id, code, next, btn);
        });
    });
}

// Pierwsze załadowanie – dociągnij listę (lekką) i narysuj
async function loadTuyaDevices(){
    const list = document.getElementById('tuya-devices-list');
    const loading = document.getElementById('tuya-loading');
    if(!list) return;

    loading.classList.remove('hidden'); list.innerHTML = '';
    try{
        const r = await fetch(`/api/tuya/devices?t=${Date.now()}`, { headers:{'Accept':'application/json','Cache-Control':'no-cache'} });
        if(!r.ok) throw new Error('HTTP '+r.status);
        const data = await r.json();
        const devices = Array.isArray(data.devices)? data.devices : [];

        if(!devices.length){
            list.innerHTML = '<p class="text-center text-gray-500 py-4">Nie znaleziono żadnych urządzeń.</p>';
            loading.classList.add('hidden'); return;
        }

        // stabilne sortowanie po nazwie i id, żeby kafle nie "pływały"
        devices.sort((a,b)=> (String(a.name||'').localeCompare(String(b.name||'')) || String(a.id||'').localeCompare(String(b.id||''))));

        // render
        const html = devices.map(buildCard).join('');
        list.innerHTML = html;
        devices.forEach(d => wireControlsForDevice(d.id));
    }catch(e){
        list.innerHTML = `<p class="text-center text-red-500 py-4">Błąd: ${e?.message||e}</p>`;
    }finally{
        loading.classList.add('hidden');
    }
}

// Public API do podpięcia w index.html
window.TUYA_UI = { loadTuyaDevices };
