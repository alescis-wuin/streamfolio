#!/usr/bin/env python3
"""Regénère toutes les miniatures SVG de démonstration sans texte incrusté."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POSTER_DIR = ROOT / "backend" / "src" / "main" / "resources" / "static" / "assets" / "posters-clean"
LEGACY_POSTER_DIR = ROOT / "backend" / "src" / "main" / "resources" / "static" / "assets" / "posters"
POSTER_DIR.mkdir(parents=True, exist_ok=True)
LEGACY_POSTER_DIR.mkdir(parents=True, exist_ok=True)

SPECS = [
    ("aurora-drift", "#0b1b2a", "#69dbc9", "#b9f4ff", "wave"),
    ("botanical-cities", "#06170f", "#58d68d", "#d9ffd8", "leaf"),
    ("silent-protocol", "#12070b", "#e50914", "#f5d0d0", "grid"),
    ("kitchen-orbit", "#1a1006", "#ffb86b", "#fff0d5", "orbit"),
    ("neon-orchard", "#07151a", "#a7ff4f", "#52d7ff", "rings"),
    ("glass-archive", "#090d18", "#9fb7ff", "#f2f6ff", "shards"),
    ("carbon-tide", "#050808", "#7f8c8d", "#d7f7ff", "tide"),
    ("quiet-circuit", "#070814", "#8e7dff", "#65f0d2", "circuit"),
    ("nomad-frames", "#151006", "#ffd166", "#f4f1de", "frames"),
    ("signal-garden", "#06130d", "#00d084", "#9cffd1", "signal"),
    ("lunar-kernel", "#080b14", "#d7e1ff", "#7c8cff", "moon"),
    ("velvet-latency", "#130813", "#d067ff", "#ffb3e8", "latency"),
    ("moss-engine", "#071208", "#9bd35a", "#dfffb0", "engine"),
    ("chrome-harbor", "#081014", "#c7d2d9", "#5cc8ff", "harbor"),
    ("ember-archive", "#180908", "#ff6b35", "#ffd6a5", "ember"),
    ("orbit-bakery", "#160d07", "#f7c873", "#fff2c7", "orbit"),
    ("tundra-signal", "#071016", "#9fe7ff", "#e8fbff", "signal"),
    ("pixel-greenhouse", "#07140a", "#7cff9b", "#b7f7d4", "pixels"),
]

PATTERNS = {
    "wave": '<path d="M70 270 C160 110, 305 120, 430 252 S680 388, 790 160" fill="none" stroke="{accent}" stroke-width="18" stroke-linecap="round" opacity="0.76" filter="url(#soft)"/><path d="M70 270 C160 110, 305 120, 430 252 S680 388, 790 160" fill="none" stroke="{light}" stroke-width="5" stroke-linecap="round" opacity="0.58"/>',
    "leaf": '<path d="M164 370 C220 170, 442 104, 656 140 C580 292, 372 438, 164 370Z" fill="{accent}" opacity="0.22"/><path d="M196 350 C322 292, 460 226, 628 156" stroke="{light}" stroke-width="7" stroke-linecap="round" opacity="0.42"/>',
    "grid": '<g fill="none" stroke="{accent}" stroke-width="3" opacity="0.42"><path d="M130 160 H690 V352 H130Z"/><path d="M205 160 V352M305 160V352M410 160V352M520 160V352M615 160V352"/><path d="M130 225H690M130 292H690"/></g><circle cx="410" cy="256" r="74" fill="{accent}" opacity="0.20"/>',
    "orbit": '<circle cx="410" cy="256" r="80" fill="{accent}" opacity="0.22"/><g fill="none" stroke="{light}" stroke-width="6" opacity="0.42"><ellipse cx="410" cy="256" rx="250" ry="86"/><ellipse cx="410" cy="256" rx="96" ry="250" transform="rotate(55 410 256)"/></g>',
    "rings": '<g fill="none" stroke="{accent}" stroke-width="10" opacity="0.48"><circle cx="300" cy="266" r="150"/><circle cx="510" cy="236" r="114"/><circle cx="430" cy="284" r="218"/></g>',
    "shards": '<g opacity="0.35"><path d="M160 140 362 94 320 356Z" fill="{accent}"/><path d="M386 106 690 174 452 398Z" fill="{light}" opacity="0.42"/><path d="M252 408 392 182 568 424Z" fill="{accent}" opacity="0.52"/></g>',
    "tide": '<path d="M44 334 C186 260, 244 406, 382 320 S626 230, 792 326 V512 H44Z" fill="{accent}" opacity="0.22"/><path d="M44 288 C186 214, 244 360, 382 274 S626 184, 792 280" fill="none" stroke="{light}" stroke-width="8" opacity="0.36"/>',
    "circuit": '<g fill="none" stroke="{accent}" stroke-width="6" stroke-linecap="round" opacity="0.46"><path d="M126 168 H300 V256 H520 V344 H704"/><path d="M184 344 H330 V302"/><path d="M506 168 H634"/></g><g fill="{light}" opacity="0.48"><circle cx="300" cy="256" r="14"/><circle cx="520" cy="344" r="14"/><circle cx="634" cy="168" r="14"/></g>',
    "frames": '<g fill="none" stroke="{accent}" stroke-width="8" opacity="0.42"><rect x="128" y="118" width="210" height="140" rx="20"/><rect x="306" y="190" width="240" height="160" rx="24"/><rect x="504" y="144" width="190" height="230" rx="22"/></g>',
    "signal": '<g fill="none" stroke="{accent}" stroke-width="8" stroke-linecap="round" opacity="0.44"><path d="M160 346 C256 228, 348 228, 444 346"/><path d="M242 346 C308 276, 392 276, 458 346"/><path d="M324 346 C350 320, 376 320, 402 346"/></g><circle cx="604" cy="178" r="76" fill="{light}" opacity="0.16"/>',
    "moon": '<circle cx="420" cy="250" r="128" fill="{light}" opacity="0.30"/><circle cx="468" cy="210" r="128" fill="{bg}" opacity="0.86"/><path d="M180 370 H650" stroke="{accent}" stroke-width="8" opacity="0.35"/>',
    "latency": '<g fill="none" stroke="{accent}" stroke-width="9" stroke-linecap="round" opacity="0.48"><path d="M130 190 H300 Q350 190 350 240 V320"/><path d="M490 190 H690"/><path d="M410 320 H690"/></g><circle cx="350" cy="320" r="38" fill="{light}" opacity="0.20"/>',
    "engine": '<g fill="none" stroke="{accent}" stroke-width="10" opacity="0.48"><circle cx="410" cy="256" r="112"/><path d="M410 98V166M410 346V414M252 256H320M500 256H568"/></g><circle cx="410" cy="256" r="46" fill="{light}" opacity="0.18"/>',
    "harbor": '<path d="M120 330 H700" stroke="{light}" stroke-width="8" opacity="0.35"/><path d="M160 330 234 210 310 330M510 330 590 176 676 330" fill="none" stroke="{accent}" stroke-width="11" opacity="0.42"/><path d="M96 376 C250 342, 420 414, 724 366" fill="none" stroke="{accent}" stroke-width="8" opacity="0.24"/>',
    "ember": '<path d="M410 92 C524 224, 532 328, 410 420 C288 328, 296 224, 410 92Z" fill="{accent}" opacity="0.28"/><path d="M426 200 C480 284, 460 346, 392 384 C354 322, 368 264, 426 200Z" fill="{light}" opacity="0.18"/>',
    "pixels": '<g opacity="0.40">' + ''.join(f'<rect x="{120 + (i % 8) * 74}" y="{116 + (i // 8) * 72}" width="42" height="42" rx="10" fill="{{accent}}" opacity="{0.14 + (i % 5) * 0.06:.2f}"/>' for i in range(32)) + '</g>',
}

def svg_for(slug: str, bg: str, accent: str, light: str, pattern: str) -> str:
    shapes = PATTERNS[pattern].format(bg=bg, accent=accent, light=light)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 820 512">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="{bg}"/>
      <stop offset="0.62" stop-color="#08090d"/>
      <stop offset="1" stop-color="#030304"/>
    </linearGradient>
    <radialGradient id="glow" cx="50%" cy="50%" r="50%">
      <stop offset="0" stop-color="{accent}" stop-opacity="0.64"/>
      <stop offset="1" stop-color="{accent}" stop-opacity="0"/>
    </radialGradient>
    <filter id="blur"><feGaussianBlur stdDeviation="24"/></filter>
    <filter id="soft"><feGaussianBlur stdDeviation="8"/></filter>
  </defs>
  <rect width="820" height="512" rx="36" fill="url(#bg)"/>
  <circle cx="630" cy="120" r="190" fill="url(#glow)" opacity="0.32" filter="url(#blur)"/>
  <circle cx="146" cy="382" r="132" fill="{light}" opacity="0.07" filter="url(#blur)"/>
  {shapes}
  <g opacity="0.10" fill="#fff">
    <circle cx="128" cy="118" r="4"/>
    <circle cx="694" cy="374" r="5"/>
    <circle cx="574" cy="146" r="3"/>
    <circle cx="252" cy="405" r="3"/>
  </g>
  <rect x="28" y="28" width="764" height="456" rx="30" fill="none" stroke="#fff" stroke-width="2" opacity="0.08"/>
</svg>
'''

for spec in SPECS:
    slug, bg, accent, light, pattern = spec
    svg = svg_for(slug, bg, accent, light, pattern)
    (POSTER_DIR / f"{slug}.svg").write_text(svg, encoding="utf-8")
    # Fallback volontaire : remplace aussi les anciens chemins pour éviter les vieux visuels
    # quand une base persistante contient encore /assets/posters/*.svg.
    (LEGACY_POSTER_DIR / f"{slug}.svg").write_text(svg, encoding="utf-8")

print(f"{len(SPECS)} miniatures régénérées dans {POSTER_DIR.relative_to(ROOT)}")
print(f"{len(SPECS)} miniatures legacy remplacées dans {LEGACY_POSTER_DIR.relative_to(ROOT)}")
