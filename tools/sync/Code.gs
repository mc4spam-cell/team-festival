/**
 * Hellfest 2026 — Picks sync backend.
 *
 * Stores all friends' P1 picks in a single JSON file in YOUR Google Drive.
 * Read & write via HTTP GET / POST. No auth, no extra services — just GAS.
 *
 * Deploy instructions:
 *   1. https://script.google.com/ → New project, paste this file as Code.gs.
 *   2. Run `getOrCreateFile` once → Drive permission prompt → Allow.
 *   3. Deploy ▸ New deployment ▸ Type "Web app".
 *      Execute as: Me · Who has access: Anyone
 *   4. Copy the "Web app URL" (ending in /exec) — that's the sync URL.
 *   5. Paste it into the Android app's gradle.properties as:
 *         hellfest.syncUrl=https://script.google.com/macros/s/AKfycb.../exec
 *      then ./gradlew :app:assembleDebug.
 *
 * The shared file lives at the root of your Drive as `hellfest_picks.json`.
 * You can rename/move it manually — GAS finds it by name.
 *
 * Wire format:
 *   GET  /exec                              → returns full state
 *   POST /exec  body: {"user":"Marc",
 *                       "p1Artists":["Iron Maiden","Mastodon",...]}
 *                                            → updates that user, returns full state
 *
 * Full state:
 *   {
 *     "festival": "Hellfest Open Air 2026",
 *     "users": {
 *       "Marc":  { "updatedAt": "2026-05-20T...", "p1Artists": [...] },
 *       "Alice": { "updatedAt": "2026-05-19T...", "p1Artists": [...] }
 *     }
 *   }
 */

const FILE_NAME = 'hellfest_picks.json';
const FESTIVAL = 'Hellfest Open Air 2026';

function getOrCreateFile() {
  const it = DriveApp.getFilesByName(FILE_NAME);
  if (it.hasNext()) return it.next();
  const seed = { festival: FESTIVAL, users: {} };
  return DriveApp.createFile(FILE_NAME, JSON.stringify(seed, null, 2), 'application/json');
}

function readData_() {
  const file = getOrCreateFile();
  try {
    const data = JSON.parse(file.getBlob().getDataAsString('UTF-8'));
    if (!data.users) data.users = {};
    if (!data.festival) data.festival = FESTIVAL;
    return data;
  } catch (e) {
    return { festival: FESTIVAL, users: {} };
  }
}

function writeData_(data) {
  getOrCreateFile().setContent(JSON.stringify(data, null, 2));
}

function json_(payload, code) {
  const out = ContentService.createTextOutput(JSON.stringify(payload));
  out.setMimeType(ContentService.MimeType.JSON);
  return out;
}

function doGet(_e) {
  try {
    const lock = LockService.getScriptLock();
    lock.waitLock(10000);
    try {
      return json_(readData_());
    } finally {
      lock.releaseLock();
    }
  } catch (err) {
    return json_({ error: String(err && err.message || err), where: 'doGet' });
  }
}

function doPost(e) {
  try {
    const lock = LockService.getScriptLock();
    lock.waitLock(10000);
    try {
      if (!e || !e.postData || !e.postData.contents) {
        return json_({ error: 'missing request body', got: typeof e });
      }
      let body;
      try {
        body = JSON.parse(e.postData.contents);
      } catch (_err) {
        return json_({ error: 'invalid json body', raw: e.postData.contents.slice(0, 200) });
      }
      const user = (body.user || '').trim();
      const p1Artists = body.p1Artists;
      if (!user) return json_({ error: 'missing user' });
      if (!Array.isArray(p1Artists)) return json_({ error: 'p1Artists must be an array' });

      const data = readData_();
      data.users[user] = {
        updatedAt: new Date().toISOString(),
        p1Artists: p1Artists.map(String),
      };
      writeData_(data);
      return json_(data);
    } finally {
      lock.releaseLock();
    }
  } catch (err) {
    return json_({
      error: String(err && err.message || err),
      where: 'doPost',
      stack: err && err.stack ? String(err.stack).split('\n').slice(0, 5) : null,
    });
  }
}
