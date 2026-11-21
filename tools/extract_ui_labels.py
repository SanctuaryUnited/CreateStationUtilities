"""Add screen labels to en_us.json as gui.createtransit.ui.* keys. Run after adding or changing screen text.

Labels are written in English in the screen code and translated at runtime by TransitText.ui(). This
script must mirror its rules: uiKey() for keys, UI_SPLIT for phrase separators and uiCore() for stripping
symbols. Existing translations are never overwritten. Labels that would share a key while showing
different text (e.g. "X +1/16" and "X -1/16") get no key and stay untranslated.

Usage: python tools/extract_ui_labels.py [--show]
"""
from pathlib import Path
import json, re, sys
sys.stdout.reconfigure(encoding="utf-8")

root = Path(__file__).resolve().parents[1]
J = root / "src/main/java/com/sanctuarygames/createtransit"
L = root / "src/main/resources/assets/createtransit/lang/en_us.json"
FILES = ["elevator/client/StationUi.java", "elevator/client/ElevatorScreen.java", "elevator/client/RadioControls.java",
         "escalator/client/EscalatorScreen.java", "client/StationSettingsScreen.java",
         "elevator/client/MaterialPickerScreen.java", "radio/client/SpeakerScreen.java"]
SPLIT = re.compile(r": | • |• | ▾|▾| \| | / | — |\(|\)|, |…")
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')
DATA_CALL = re.compile(r"\b(get\w*|put\w*|contains|equals|equalsIgnoreCase|setting|cabinValue|send|simple|remove|startsWith|endsWith|matches|parse|valueOf|fromNamespaceAndPath|withDefaultNamespace|translatable|tr|key|ResourceLocation\.parse)\(\s*$")


def ui_key(phrase):
    slug = re.sub(r"[^a-z0-9]+", "_", phrase.lower()).strip("_")
    if len(slug) > 60:
        slug = slug[:60].rstrip("_")
    return "gui.createtransit.ui." + slug


def is_label(text, before):
    if not re.search(r"[A-Za-z]", text) or DATA_CALL.search(before):
        return False
    t = text.strip()
    if not t or re.match(r"^[a-z]", t) or "://" in t or re.match(r"^[a-z_]+:[a-z_/]+$", t):
        return False
    if re.fullmatch(r"[A-Z0-9_]+", t):                    # ENUM_VALUES
        return False
    if re.fullmatch(r"[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]*)+", t):  # NbtKeys
        return False
    if "\\u" in t or "%" in t or t.startswith("."):  # format strings and file suffixes
        return False
    return True


def core_of(piece):
    return re.sub(r"^[^A-Za-z0-9]+|[^A-Za-z0-9.!?%]+$", "", piece)


def phrases(text):
    t = core_of(text)
    if len(t) > 30 and " " in t:
        return [t]  # a sentence: translate it whole
    out = []
    for piece in SPLIT.split(text):
        core = core_of(piece)
        if core and re.search(r"[A-Za-z]", core) and not re.fullmatch(r"[A-Z0-9_]+", core):
            out.append(core)
    return out


lang = json.loads(L.read_text(encoding="utf-8"))
before = len(lang)
found, clashes = {}, []
for f in FILES:
    src = (J / f).read_text(encoding="utf-8")
    for m in LITERAL.finditer(src):
        text = m.group(1).encode().decode("unicode_escape") if "\\" in m.group(1) else m.group(1)
        if not is_label(text, src[max(0, m.start() - 40):m.start()]):
            continue
        for phrase in phrases(text):
            key = ui_key(phrase)
            if key == "gui.createtransit.ui.":
                continue
            if key in found and found[key] != phrase:
                if found[key].lower() == phrase.lower():
                    if found[key][0].islower(): found[key] = phrase
                    continue
                clashes.append((key, found[key], phrase)); continue
            found.setdefault(key, phrase)
# Labels that differ only in signs or symbols ("X +1/16" / "X -1/16") share a key: give them none, so
# each passes through untranslated instead of showing the other's text.
for key, *_ in clashes:
    found.pop(key, None)
for key, phrase in sorted(found.items()):
    lang.setdefault(key, phrase)
L.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("labels", len(found), "added", len(lang) - before, "clashes", len(clashes))
for c in clashes[:15]:
    print("  clash", c)
if "--show" in sys.argv:
    for k, v in sorted(found.items()):
        print(" ", v)
