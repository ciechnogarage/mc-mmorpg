# DR001 Source Audio Notes

Status: final_ogg_created_human_review_required
Generated: 2026-05-30

These WAV files are synthetic local source-authoring cues. Final Ogg Vorbis files were converted from them with local `ffmpeg` / `libvorbis` at q5. The audio files are technically valid, but still require human audio review before `approved_final`.

| Cue | WAV source | Duration | Peak | RMS | SHA-256 | Notes |
| --- | --- | ---: | ---: | ---: | --- | --- |
| roots_whisper | intake/sounds/dr001_source/roots_whisper.wav | 6.0s | 0.2827 | 0.1127 | fcf2532a9ffa0212eb8c79cb9b3a0050e1fa8f97eea547b78b503b53463b041b | Low moss/root ambience with filtered wind, slow sub pulse, and faint creaking overtones. |
| cistern_geyser | intake/sounds/dr001_source/cistern_geyser.wav | 3.2s | 0.6655 | 0.1017 | f79ad3b4e25f881e75bce8b3fcc86b78cd001f9a044d3a424a37e2a36420d511 | Water pressure burst with short stone resonance tail. |
| void_crown_pulse | intake/sounds/dr001_source/void_crown_pulse.wav | 3.8s | 0.2682 | 0.0927 | 6110e51ecf08a111e1a5fa2c9052b84f38d2fe02696a355a189b9e70e9992f3a | Readable void-crown pulse with cyan/violet shimmer and low impact. |
| aurelion_enrage | intake/sounds/dr001_source/aurelion_enrage.wav | 4.5s | 0.5093 | 0.1273 | 6402fe0d38b5c41fa99bc5b7197b890b395e9ec2a5afe12a0d5f20aa5e244cd9 | Dark regal boss enrage sting with crown clang, bass fall, and deathlike tail. |

Conversion command used from `content/dungeons/dungeon_ruins_001/intake/sounds`:

```powershell
ffmpeg -hide_banner -loglevel error -y -i dr001_source\roots_whisper.wav -c:a libvorbis -q:a 5 dr001\roots_whisper.ogg
ffmpeg -hide_banner -loglevel error -y -i dr001_source\cistern_geyser.wav -c:a libvorbis -q:a 5 dr001\cistern_geyser.ogg
ffmpeg -hide_banner -loglevel error -y -i dr001_source\void_crown_pulse.wav -c:a libvorbis -q:a 5 dr001\void_crown_pulse.ogg
ffmpeg -hide_banner -loglevel error -y -i dr001_source\aurelion_enrage.wav -c:a libvorbis -q:a 5 dr001\aurelion_enrage.ogg
```

| Cue | Final Ogg | Codec | Duration | Peak/RMS check | SHA-256 |
| --- | --- | --- | ---: | --- | --- |
| roots_whisper | intake/sounds/dr001/roots_whisper.ogg | Vorbis, mono, 44.1 kHz | 6.0s | peak -10.664892 dB, RMS -18.922415 dB | 72b4d529fb9ada26e1534e6a5946bc58f5e60cf6c654e174ce9108b5a897bfb7 |
| cistern_geyser | intake/sounds/dr001/cistern_geyser.ogg | Vorbis, mono, 44.1 kHz | 3.2s | peak -2.704387 dB, RMS -19.932649 dB | 8eaaa41cf605e990946469cc93b5cc20da7ae6958b3797ce213a477edcfcc770 |
| void_crown_pulse | intake/sounds/dr001/void_crown_pulse.ogg | Vorbis, mono, 44.1 kHz | 3.8s | peak -11.215942 dB, RMS -20.634716 dB | 7eb930eb3619d17b9fc6ea7694e0430af535d3bfe3353c21f766f679499989c6 |
| aurelion_enrage | intake/sounds/dr001/aurelion_enrage.ogg | Vorbis, mono, 44.1 kHz | 4.5s | peak -5.748595 dB, RMS -17.870650 dB | c2378d98bcb7bf4446ee11a724f49350ad03e741ca301e1fd895bb4552da0be5 |
