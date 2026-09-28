# FRAME X AI — Product Design System

## Design intent
FRAME X AI is a premium personal AI workspace, not a generic chat clone. The visual language combines editorial serif typography, dark cinematic surfaces, restrained electric cyan, premium gold, and the supplied FX logo.

The official logo is the primary brand asset. It must never be redrawn, recolored, replaced, or stylistically regenerated. Technical derivatives may crop or resize it without changing its identity.

## Tokens
- Background #050B14
- Panel #091522
- Deep blue #003B73
- Plasma cyan #00D4FF
- Bronze #3B2F0F
- Gold #FFD700
- Green-gold #80C6A0
- Red/wine #3B0A0A
- Text #edf7ff
- Muted #89a5bb
- Divider rgba(0,212,255,.17)
- Shadow 0 20px 60px rgba(0,0,0,.35)
- Radius 20px

Cyan = active/primary/technology. Gold = premium/value and user-message typography. Green-gold = success/completed. Crimson/wine = problems and challenge mode. Do not flood the interface with neon.

## Typography
Canela (400/500/600) is the body/editorial family with Georgia fallback. Playfair Display is the brand and major-heading family. Questa is reserved for task/challenge chat bubbles. system-ui is used for utility and metadata.

The current web foundation loads Playfair Display from Google Fonts and keeps Canela/Questa as font-ready families with safe fallbacks. Proprietary font binaries should only be added when properly licensed.

## Interaction
Use a 4px base spacing scale. Large panels use 20px radius; compact controls use 10–14px. Prefer one strong surface and quiet dividers over nested cards. Touch targets are at least 44px on mobile. Keyboard focus is visible cyan. Reduced motion removes nonessential transitions.

## Chat
User messages use dark teal surfaces and gold typography. AI messages use dark warm/navy surfaces and editorial typography. Challenge mode uses wine/crimson surfaces and Questa. The model indicator exposes provider, model and mode; automatic routing adds a short "Automatically selected for this task" explanation.

## System states
Google Drive: Connected / Syncing / Offline / Needs authorization / Conflict detected.
Provider: Connected / Needs key / Error.
Model capabilities: supported / unsupported / unknown. Unknown is never assumed supported.

## Accessibility
High contrast, visible focus, semantic controls, scalable typography, keyboard navigation, screen-reader labels, minimum mobile touch targets, and reduced motion.

## Screen inventory
Splash, onboarding, sign-up, login, Google sign-in, Home, Chat, History, Search, Projects, Categories, API key manager, Provider, Model selector, Routing indicator, Attachments, PDF/file viewer, Settings, Drive connection, Sync, Profile, Empty, Error, Loading, Offline, Conflict, Voice, Image Generation, Tool/Research.

## Implementation rule
A control is not considered functional merely because it renders. It must connect to an existing service/core implementation or clearly state that the underlying capability is pending. Never simulate provider output, synchronization success, or credential storage with fake network responses.
